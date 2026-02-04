package com.happysg.radar.block.radar.behavior;

import com.happysg.radar.block.arad.aradnetworks.RadarContactRegistry;
import com.happysg.radar.block.radar.bearing.RadarBearingBlockEntity;

import com.happysg.radar.block.radar.track.RadarTrack;
import com.happysg.radar.block.radar.track.RadarTrackUtil;
import com.happysg.radar.block.radar.track.TrackCategory;
import com.happysg.radar.compat.Mods;
import com.happysg.radar.compat.vs2.PhysicsHandler;
import com.happysg.radar.compat.vs2.VS2Utils;
import com.happysg.radar.config.RadarConfig;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.happysg.radar.block.behavior.networks.config.DetectionConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.valkyrienskies.core.api.ships.Ship;

import java.lang.reflect.Method;
import java.util.*;

public class RadarScanningBlockBehavior extends BlockEntityBehaviour {

    public static final BehaviourType<RadarScanningBlockBehavior> TYPE = new BehaviourType<>();

    private int trackExpiration = 100;
    private int fov = RadarConfig.server().radarFOV.get();
    private int yRange = 20;
    private double range = RadarConfig.server().radarBaseRange.get();
    private double angle;
    private boolean running = false;
    private SmartBlockEntity bearingEntity;
    private RadarBearingBlockEntity radarBearing;
    Vec3 scanPos = Vec3.ZERO;

    private final Set<Entity> scannedEntities = new HashSet<>();
    private final Set<Ship> scannedShips = new HashSet<>();
    private final Set<Projectile> scannedProjectiles = new HashSet<>();
    private final HashMap<String, RadarTrack> radarTracks = new HashMap<>();

    public RadarScanningBlockBehavior(SmartBlockEntity be) {
        super(be);
        this.bearingEntity = be;
    }

    public void applyDetectionConfig(DetectionConfig cfg) {
        if (cfg == null) cfg = DetectionConfig.DEFAULT;
        setScanFlags(
                cfg.player(),
                cfg.vs2(),
                cfg.contraption(),
                cfg.mob(),
                cfg.animal(),
                cfg.projectile(),
                cfg.item()
        );
    }


    private boolean scanPlayers = true;
    private boolean scanVS2 = true;
    private boolean scanContraptions = true;
    private boolean scanMobs = true;
    private boolean scanAnimals = true;
    private boolean scanProjectiles = true;
    private boolean scanItems = true;

    private boolean allowCategory(TrackCategory c) {
        return switch (c) {
            case PLAYER -> scanPlayers;
            case VS2 -> scanVS2;
            case CONTRAPTION -> scanContraptions;
            case PROJECTILE -> scanProjectiles;
            case ITEM -> scanItems;

            case ANIMAL -> scanAnimals;
            case HOSTILE, MOB -> scanMobs;

            default -> true;
        };
    }

    private void pruneDisabledTracksNow() {
        radarTracks.entrySet().removeIf(e -> !allowCategory(e.getValue().trackCategory()));
    }

    public void setScanFlags(boolean players, boolean vs2, boolean contraptions, boolean mobs, boolean animals, boolean projectiles, boolean items) {
        boolean changed = players != scanPlayers || vs2 != scanVS2 || contraptions != scanContraptions || mobs != scanMobs || animals != scanAnimals || projectiles != scanProjectiles || items != scanItems;

        this.scanPlayers = players;
        this.scanVS2 = vs2;
        this.scanContraptions = contraptions;
        this.scanMobs = mobs;
        this.scanAnimals = animals;
        this.scanProjectiles = projectiles;
        this.scanItems = items;

        if (changed) {
            pruneDisabledTracksNow();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (blockEntity.getLevel() == null || blockEntity.getLevel().isClientSide)
            return;

        removeDeadTracks();
        if (running)
            updateRadarTracks();
        if (running) {
            scannedEntities.clear();
            scannedShips.clear();
            scannedProjectiles.clear();

            scanForEntityTracks();
            if (Mods.VALKYRIENSKIES.isLoaded() && scanVS2)
                scanForVSTracks();
        }
    }


    private void updateRadarTracks() {
        scanPos = PhysicsHandler.getWorldPos(bearingEntity).getCenter();
        Level level = blockEntity.getLevel();
        boolean isServer = level instanceof net.minecraft.server.level.ServerLevel;
        net.minecraft.server.level.ServerLevel sl = isServer ? (net.minecraft.server.level.ServerLevel) level : null;
        if (level == null )return;


        for (Entity entity : scannedEntities) {
            if (entity.isAlive() && isInFovAndRange(entity.position())) {
                radarTracks.compute(entity.getUUID().toString(), (id, track) -> {
                    if (track == null) return new RadarTrack(entity);
                    track.updateRadarTrack(entity);
                    return track;
                });

                if (entity instanceof Projectile)
                    scannedProjectiles.add((Projectile) entity);
            }
        }

        for (Ship ship : scannedShips) {
            Vec3 pos = RadarTrackUtil.getPosition(ship);
            if (isInFovAndRange(pos)) {

                long key = ship.getId();

                radarTracks.compute(ship.getSlug(), (id, track) -> {
                    if (track == null) return RadarTrackUtil.getRadarTrack(ship, level);
                    track.updateRadarTrack(ship, level);
                    return track;
                });
                if (isServer) {
                    RadarContactRegistry.markInRange(sl, key, 20);
                }
            }
        }
    }

    private boolean isInFovAndRange(Vec3 target) {
        double distance = scanPos.distanceTo(target);

        if (distance < 2)
            return true;
        double a = scanAngleDeg();
        double angleToEntity = Math.toDegrees(Math.atan2(target.x() - scanPos.x(), target.z() - scanPos.z()));
        angleToEntity = (angleToEntity + 360) % 360;

        double angleDiff = Math.abs(angleToEntity - a);
        if (angleDiff > 180) angleDiff = 360 - angleDiff;

        return angleDiff <= fov / 2.0 && distance <= range;

    }

    private void removeDeadTracks() {
        for (Entity entity : scannedEntities) {
            if (!entity.isAlive())
                radarTracks.remove(entity.getUUID().toString());
        }

        List<String> toRemove = new ArrayList<>();
        assert blockEntity.getLevel() != null;
        long currentTime = blockEntity.getLevel().getGameTime();
        for (RadarTrack track : radarTracks.values()) {
            if (currentTime - track.scannedTime() > trackExpiration)
                toRemove.add(track.id());
        }

        toRemove.forEach(radarTracks::remove);

        scannedProjectiles.removeIf(p -> {
            boolean dead = !p.isAlive();
            if (dead) radarTracks.remove(p.getUUID().toString());
            return dead;
        });
    }
    private void scanForEntityTracks() {
        Level level = blockEntity.getLevel();
        if (level == null) return;

        boolean scanAll =
                scanPlayers && scanContraptions && scanMobs && scanAnimals && scanProjectiles && scanItems;

        for (AABB aabb : splitAABB(getRadarAABB(), 999)) {
            if (scanAll) {
                scannedEntities.addAll(level.getEntities(null, aabb));
                continue;
            }

            if (scanPlayers)
                scannedEntities.addAll(level.getEntitiesOfClass(Player.class, aabb));

            if (scanProjectiles)
                scannedEntities.addAll(level.getEntitiesOfClass(Projectile.class, aabb));

            if (scanItems)
                scannedEntities.addAll(level.getEntitiesOfClass(ItemEntity.class, aabb));

            if (scanContraptions)
                scannedEntities.addAll(level.getEntitiesOfClass(AbstractContraptionEntity.class, aabb));

            if (scanAnimals)
                scannedEntities.addAll(level.getEntitiesOfClass(Animal.class, aabb));

            if (scanMobs) {
                scannedEntities.addAll(level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, aabb,
                        e -> !(e instanceof Animal)));
            }
        }
    }

    private void scanForVSTracks() {
        if (blockEntity.getLevel() == null || !Mods.VALKYRIENSKIES.isLoaded()) return;
        splitAABB(getRadarAABB(), 999).forEach(aabb ->
                VS2Utils.getLoadedShips(blockEntity.getLevel(), aabb).forEach(scannedShips::add));

        scannedShips.remove(VS2Utils.getShipManagingPos(blockEntity));
    }

    private AABB getRadarAABB() {
        BlockPos radarPos = PhysicsHandler.getWorldPos(blockEntity);
        double x = radarPos.getX() + 0.5;
        double y = radarPos.getY() + 0.5;
        double z = radarPos.getZ() + 0.5;

        double yScan = RadarConfig.server().radarYScanRange.get();

        // forward endpoint
        double dx = range * Math.sin(Math.toRadians(angle));
        double dz = range * Math.cos(Math.toRadians(angle));

        double minX = Math.min(x, x + dx);
        double maxX = Math.max(x, x + dx);
        double minZ = Math.min(z, z + dz);
        double maxZ = Math.max(z, z + dz);

        // widen it a bit so it’s not a razor-thin line
        double widen = range * 0.5;

        return new AABB(
                minX - widen, y - yScan, minZ - widen,
                maxX + widen, y + yScan, maxZ + widen
        );
    }
    private double scanAngleDeg() {
        // my bearing angle is opposite of atan2(dx, dz) convention, so i flip it
        return (angle + 180.0) % 360.0;
    }


    public static List<AABB> splitAABB(AABB aabb, double maxSize) {
        List<AABB> result = new ArrayList<>();
        for (double x = aabb.minX; x < aabb.maxX; x += maxSize) {
            for (double y = aabb.minY; y < aabb.maxY; y += maxSize) {
                for (double z = aabb.minZ; z < aabb.maxZ; z += maxSize) {
                    result.add(new AABB(
                            x, y, z,
                            Math.min(x + maxSize, aabb.maxX),
                            Math.min(y + maxSize, aabb.maxY),
                            Math.min(z + maxSize, aabb.maxZ)
                    ));
                }
            }
        }
        return result;
    }

    @Override
    public void read(CompoundTag nbt, boolean clientPacket) {
        super.read(nbt, clientPacket);
        if (nbt.contains("fov")) fov = nbt.getInt("fov");
        if (nbt.contains("yRange")) yRange = nbt.getInt("yRange");
        if (nbt.contains("range")) range = nbt.getDouble("range");
        if (nbt.contains("angle")) angle = nbt.getDouble("angle");
        if (nbt.contains("scanPosX")) scanPos = new Vec3(nbt.getDouble("scanPosX"), nbt.getDouble("scanPosY"), nbt.getDouble("scanPosZ"));
        if (nbt.contains("running")) running = nbt.getBoolean("running");
        if (nbt.contains("trackExpiration")) trackExpiration = nbt.getInt("trackExpiration");
    }

    @Override
    public void write(CompoundTag nbt, boolean clientPacket) {
        super.write(nbt, clientPacket);
        nbt.putInt("fov", fov);
        nbt.putInt("yRange", yRange);
        nbt.putDouble("range", range);
        nbt.putDouble("angle", angle);
        nbt.putDouble("scanPosX", scanPos.x);
        nbt.putDouble("scanPosY", scanPos.y);
        nbt.putDouble("scanPosZ", scanPos.z);
        nbt.putBoolean("running", running);
        nbt.putInt("trackExpiration", trackExpiration);
    }

    public void setFov(int fov) { this.fov = fov; }
    public void setYRange(int yRange) { this.yRange = yRange; }
    public void setRange(double range) { this.range = range; }
    public void setAngle(double angle) { this.angle = angle; }
    public void setScanPos(Vec3 scanPos) { this.scanPos = scanPos; }
    public void setRunning(boolean running) { this.running = running; }
    public void setTrackExpiration(int trackExpiration) { this.trackExpiration = trackExpiration; }

    public Collection<RadarTrack> getRadarTracks() {
        return radarTracks.values();
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }

    public float getAngle() {
        return (float) angle;
    }
}
