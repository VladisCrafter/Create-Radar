package com.happysg.radar.block.radar.behavior;

import com.happysg.radar.block.radar.bearing.RadarBearingBlockEntity;
import com.happysg.radar.block.radar.plane.PlaneRadarBlockEntity;
import com.happysg.radar.block.radar.track.RadarTrack;
import com.happysg.radar.block.radar.track.RadarTrackUtil;
import com.happysg.radar.compat.Mods;
import com.happysg.radar.compat.vs2.PhysicsHandler;
import com.happysg.radar.compat.vs2.VS2Utils;
import com.happysg.radar.config.RadarConfig;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.valkyrienskies.core.api.ships.Ship;

import java.util.*;


/**
 * RadarScanningBlockBehavior is responsible for scanning entities and ships within a specified range and field of view.
 *
 * @see RadarBearingBlockEntity
 * @see PlaneRadarBlockEntity
 */
public class RadarScanningBlockBehavior extends BlockEntityBehaviour {

    public static final BehaviourType<RadarScanningBlockBehavior> TYPE = new BehaviourType<>();
    private int trackExpiration = 100;
    private int fov = RadarConfig.server().radarFOV.get();
    private int yRange = 20;
    private double range;
    private double angle;
    private boolean running = false;
    private SmartBlockEntity bearingEntity;
    Vec3 scanPos = Vec3.ZERO;


    private final Set<Entity> scannedEntities = new HashSet<>();
    private final Set<Ship> scannedShips = new HashSet<>();
    private final Set<Projectile> scannedProjectiles = new HashSet<>();
    private final HashMap<String, RadarTrack> radarTracks = new HashMap<>();

    public RadarScanningBlockBehavior(SmartBlockEntity be) {
        super(be);
        bearingEntity = be;
        setLazyTickRate(5);
    }

    @Override
    public void tick() {
        super.tick();
        if (blockEntity.getLevel() == null)
            return;
        if (blockEntity.getLevel().isClientSide)
            return;
        removeDeadTracks();
        if (running)
            updateRadarTracks();
    }

    private void updateRadarTracks() {
        scanPos = PhysicsHandler.getWorldPos(bearingEntity).getCenter();
        Level level = blockEntity.getLevel();
        if (level == null) return;
        for (Entity entity : scannedEntities) {
            if (entity.isAlive() && isInFovAndRange(entity.position())) {
              if (radarTracks.containsKey(entity.getUUID().toString())) {
                    RadarTrack track = radarTracks.get(entity.getUUID().toString());
                    track.updateRadarTrack(entity);
                } else {
                    RadarTrack track = new RadarTrack(entity);
                    radarTracks.put(track.getId(), track);
                }

                if (entity instanceof Projectile)
                    scannedProjectiles.add((Projectile) entity);
            }
        }
        for (Ship ship : scannedShips) {
            if (isInFovAndRange(RadarTrackUtil.getPosition(ship))) {
                if (radarTracks.containsKey(ship.getSlug())) {
                    RadarTrack track = radarTracks.get(ship.getSlug());
                    track.updateRadarTrack(ship, level);
                } else {
                    RadarTrack track = RadarTrackUtil.getRadarTrack(ship, level);
                    radarTracks.put(track.getId(), track);
                }
            }
        }
    }

    private boolean isInFovAndRange(Vec3 target) {
        double distance = scanPos.distanceTo(target);
        if (distance < 2) // update self position constantly
            return true;
        // Calculate angle to entity in degrees
        // Note: atan2 should take (targetX - scanX, targetZ - scanZ) to get angle from scan to target
        double angleToEntity = Math.toDegrees(Math.atan2(target.x() - scanPos.x(), target.z() - scanPos.z()));
        angleToEntity = (angleToEntity + 360) % 360;

        // Calculate the smallest angle between the two directions
        double angleDiff = Math.abs(angleToEntity - angle);

        // Handle wraparound
        if (angleDiff > 180) {
            angleDiff = 360 - angleDiff;
        }


        return angleDiff <= fov / 2.0 && distance <= range;
    }

    private void removeDeadTracks() {

        for (Entity entity : scannedEntities) {
            if (!entity.isAlive()) {
                radarTracks.remove(entity.getUUID().toString());
            }
        }

        List<String> toRemove = new ArrayList<>();
        long currentTime = blockEntity.getLevel().getGameTime();
        for (RadarTrack track : radarTracks.values()) {
            if (currentTime - track.scannedTime() > trackExpiration) {
                toRemove.add(track.id());
            }
        }
        for (String id : toRemove) {
            radarTracks.remove(id);
        }

        List<Projectile> toRemoveProjectiles = new ArrayList<>();
        for (Projectile projectile : scannedProjectiles) {
            if (!projectile.isAlive()) {
                toRemoveProjectiles.add(projectile);
            }
        }

        for (Projectile projectile : toRemoveProjectiles) {
            radarTracks.remove(projectile.getUUID().toString());
            scannedProjectiles.remove(projectile);
        }
    }

    @Override
    public void lazyTick() {
        if (running) {
            scanForEntityTracks();
            if (Mods.VALKYRIENSKIES.isLoaded())
                scanForVSTracks();
        }
        super.lazyTick();
    }


    private void scanForEntityTracks() {
        if (blockEntity.getLevel() == null) return;
        List<AABB> AABBs = splitAABB(getRadarAABB(), 999);
        AABBs.forEach((aabb -> {
            scannedEntities.addAll(blockEntity.getLevel().getEntities(null, aabb));
        }));
    }

    private void scanForVSTracks() {
        if (!Mods.VALKYRIENSKIES.isLoaded())
            return;
        if (blockEntity.getLevel() == null)
            return;
        List<AABB> AABBs = splitAABB(getRadarAABB(), 999);
        AABBs.forEach((aabb -> {
            VS2Utils.getLoadedShips(blockEntity.getLevel(), aabb).forEach(scannedShips::add);
        }));
        //remove self
        scannedShips.remove(VS2Utils.getShipManagingPos(blockEntity));
    }

    private AABB getRadarAABB() {
        BlockPos radarPos = PhysicsHandler.getWorldPos(blockEntity);
        double xOffset = range * Math.sin(Math.toRadians(angle));
        double zOffset = range * Math.cos(Math.toRadians(angle));
        return new AABB(radarPos.getX() - xOffset, radarPos.getY() - RadarConfig.server().radarYScanRange.get(), radarPos.getZ() - zOffset, radarPos.getX() + xOffset, radarPos.getY() + RadarConfig.server().radarYScanRange.get(), radarPos.getZ() + zOffset);
    }

    public static List<AABB> splitAABB(AABB aabb, double maxSize) {
        List<AABB> result = new ArrayList<>();

        double xMin = aabb.minX;
        double xMax = aabb.maxX;
        double yMin = aabb.minY;
        double yMax = aabb.maxY;
        double zMin = aabb.minZ;
        double zMax = aabb.maxZ;

        for (double xStart = xMin; xStart < xMax; xStart += maxSize) {
            double xEnd = Math.min(xStart + maxSize, xMax);

            for (double yStart = yMin; yStart < yMax; yStart += maxSize) {
                double yEnd = Math.min(yStart + maxSize, yMax);

                for (double zStart = zMin; zStart < zMax; zStart += maxSize) {
                    double zEnd = Math.min(zStart + maxSize, zMax);

                    result.add(new AABB(xStart, yStart, zStart, xEnd, yEnd, zEnd));
                }
            }
        }
        return result;
    }

    @Override
    public void read(CompoundTag nbt, boolean clientPacket) {
        super.read(nbt, clientPacket);
        if (nbt.contains("fov"))
            fov = nbt.getInt("fov");
        if (nbt.contains("yRange"))
            yRange = nbt.getInt("yRange");
        if (nbt.contains("range"))
            range = nbt.getDouble("range");
        if (nbt.contains("angle"))
            angle = nbt.getDouble("angle");
        if (nbt.contains("scanPosX"))
            scanPos = new Vec3(nbt.getDouble("scanPosX"), nbt.getDouble("scanPosY"), nbt.getDouble("scanPosZ"));
        if (nbt.contains("running"))
            running = nbt.getBoolean("running");
        if (nbt.contains("trackExpiration"))
            trackExpiration = nbt.getInt("trackExpiration");
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

    public void setFov(int fov) {
        this.fov = fov;
    }

    public void setYRange(int yRange) {
        this.yRange = yRange;
    }

    public void setRange(double range) {
        this.range = range;
    }

    public void setAngle(double angle) {
        this.angle = angle;
    }

    public void setScanPos(Vec3 scanPos) {
        this.scanPos = scanPos;
    }

    public Collection<RadarTrack> getRadarTracks() {
        return radarTracks.values();
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public void setTrackExpiration(int trackExpiration) {
        this.trackExpiration = trackExpiration;
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }

    public float getAngle() {
        return (float) angle;
    }
}
