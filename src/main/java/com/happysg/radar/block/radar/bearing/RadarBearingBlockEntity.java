package com.happysg.radar.block.radar.bearing;

import com.happysg.radar.compat.Mods;
import com.happysg.radar.compat.vs2.VS2Utils;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import com.simibubi.create.content.kinetics.BlockStressValues;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.ServerSpeedProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3d;

import java.util.*;

public class RadarBearingBlockEntity extends MechanicalBearingBlockEntity {
    private static final int MAX_TRACK_TICKS = 100;

    private int dishCount;
    private Direction receiverFacing = Direction.NORTH;
    Map<String, RadarTrack> entityPositions = new HashMap<>();
    Map<String, VSRadarTracks> VSPositions = new HashMap<>();

    public RadarBearingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        movementMode.setValue(MovementMode.MOVE_NEVER_PLACE.ordinal());
    }


    @Override
    public void tick() {
        super.tick();
        if (isRunning()) {
            scanForEntityTracks();
            scanForVSTracks();
        }
        clearOldTracks();
        notifyUpdate();
    }


    private void clearOldTracks() {
        List<String> toRemove = new ArrayList<>();
        long currentTime = level.getGameTime();
        entityPositions.forEach((entity, track) -> {
            if (currentTime - track.scannedTime() > MAX_TRACK_TICKS || (level.getEntity(track.id()) != null && (level.getEntity(track.id()).isRemoved() || !level.getEntity(track.id()).isAlive()))) {
                toRemove.add(entity);
            }
        });
        toRemove.forEach(uuid -> entityPositions.remove(uuid));
        if (Mods.VALKYRIENSKIES.isLoaded()) {
            List<String> toRemoveVS = new ArrayList<>();
            VSPositions.forEach((entity, track) -> {
                if (currentTime - track.scannedTime() > MAX_TRACK_TICKS) {
                    toRemoveVS.add(entity);
                }
            });
            toRemoveVS.forEach(uuid -> VSPositions.remove(uuid));
        }
    }

    private void scanForEntityTracks() {
        AABB aabb = getRadarAABB();
        for (Entity entity : level.getEntities(null, aabb)) {
            if (entity.isAlive() && isEntityInRadarFov(entity.blockPosition())) {
                entityPositions.put(entity.getStringUUID(), new RadarTrack(entity));
            }
        }
    }

    private void scanForVSTracks() {
        if (!Mods.VALKYRIENSKIES.isLoaded())
            return;
        if (level == null)
            return;
        VS2Utils.getLoadedShips(level, getRadarAABB()).forEach(serverShip -> {
            Vector3d shipPos = serverShip.getWorldAABB().center(new Vector3d());
            if (isEntityInRadarFov(new BlockPos((int) shipPos.x, (int) shipPos.y, (int) shipPos.z))) {
                VSPositions.put(String.valueOf(serverShip.getId()), new VSRadarTracks(serverShip, level));
            }
        });
    }

    private AABB getRadarAABB() {
        float range = getRange();
        BlockPos radarPos = VS2Utils.getWorldPos(this);
        double xOffset = range * Math.sin(Math.toRadians(getGlobalAngle()));
        double zOffset = range * Math.cos(Math.toRadians(getGlobalAngle()));
        return new AABB(radarPos.getX() - xOffset, radarPos.getY() - 20, radarPos.getZ() - zOffset, radarPos.getX() + xOffset, radarPos.getY() + 20, radarPos.getZ() + zOffset);
    }

    private boolean isEntityInRadarFov(BlockPos entityPos) {
        float radarAngle = getGlobalAngle();
        double fovDegrees = 90;
        BlockPos radarPos = VS2Utils.getWorldPos(this);

        // Calculate the angle between the radar and the entity
        double angleToEntity = Math.toDegrees(Math.atan2(entityPos.getX() - radarPos.getX(), radarPos.getZ() - entityPos.getZ()));
        if (angleToEntity < 0) {
            angleToEntity += 360;
        }
        double relativeAngle = Math.abs(angleToEntity - radarAngle);

        // Check if the entity is within the field of view
        return relativeAngle <= fovDegrees / 2;
    }

    public float getGlobalAngle() {
        return (receiverFacing.toYRot() - angle + 360) % 360;
    }

    public float getAngularSpeed() {
        float speed = convertToAngular(getSpeed());
        if (getSpeed() == 0)
            speed = 0;
        if (level.isClientSide) {
            speed *= ServerSpeedProvider.get();
            speed += clientAngleDiff / 3f;
        }

        //integer division used to step down the speed
        //TODO rebalance speed
        return speed / (4f + getDishCount() / 10);
    }

    //code copied in order to replace with radar contraption and radar advancements
    @Override
    public void assemble() {
        if (!(level.getBlockState(getBlockPos())
                .getBlock() instanceof RadarBearingBlock))
            return;

        RadarContraption contraption = createContraption();
        if (contraption == null)
            return;

        //replace with radar advancements
        if (isWindmill())
            award(AllAdvancements.WINDMILL);
        if (contraption.getSailBlocks() >= 16 * 8)
            award(AllAdvancements.WINDMILL_MAXED);

        updateGeneratedRotation();
        updateContraptionData();
        notifyUpdate();
    }


    @Override
    public void disassemble() {
        super.disassemble();
        updateContraptionData();
    }

    @Override
    public float calculateStressApplied() {
        float impact = (float) BlockStressValues.getImpact(getStressConfigKey()) + getDishCount();
        this.lastStressApplied = impact;
        return impact;
    }

    private RadarContraption createContraption() {
        RadarContraption contraption = new RadarContraption();
        try {
            if (!contraption.assemble(level, getBlockPos()))
                return null;

            lastException = null;
        } catch (AssemblyException e) {
            lastException = e;
            sendData();
            return null;
        }
        contraption.removeBlocksFromWorld(level, BlockPos.ZERO);
        movedContraption = ControlledContraptionEntity.create(level, this, contraption);
        BlockPos anchor = getBlockPosition().above();
        movedContraption.setPos(anchor.getX(), anchor.getY(), anchor.getZ());
        movedContraption.setRotationAxis(Direction.Axis.Y);
        level.addFreshEntity(movedContraption);

        AllSoundEvents.CONTRAPTION_ASSEMBLE.playOnServer(level, getBlockPos());

        running = true;
        angle = 0;
        return contraption;
    }

    //capturing from radar contraption to save on client side BE, contraption data only server side
    private void updateContraptionData() {
        dishCount = getContraption().map(RadarContraption::getDishCount).orElse(0);
        receiverFacing = getContraption().map(RadarContraption::getReceiverFacing).orElse(Direction.NORTH);
        notifyUpdate();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        if (dishCount > 0) {
            tooltip.add(Component.literal("    Dish Count: " + dishCount));
        }
        tooltip.add(Component.literal("    Range: " + getRange()));
        tooltip.add(Component.literal("    Speed: " + Math.round(getAngularSpeed() * 100) / 100f + "°/s"));
        return true;
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        dishCount = compound.getInt("dishCount");
        if (compound.contains("receiverFacing"))
            receiverFacing = Direction.from3DDataValue(compound.getInt("receiverFacing"));
        if (clientPacket && compound.contains("entityPositions"))
            RadarTrack.deserializeListNBT(compound.getCompound("entityPositions")).forEach(track -> entityPositions.put(track.entityId(), track));
        if (clientPacket && Mods.VALKYRIENSKIES.isLoaded())
            VSRadarTracks.deserializeListNBT(compound.getCompound("VSPositions")).forEach(track -> VSPositions.put(track.id(), track));
    }

    @Override
    public void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.putInt("dishCount", dishCount);
        if (receiverFacing != null)
            compound.putInt("receiverFacing", receiverFacing.get3DDataValue());
        if (clientPacket)
            compound.put("entityPositions", RadarTrack.serializeNBTList(entityPositions.values()));
        if (clientPacket && Mods.VALKYRIENSKIES.isLoaded())
            compound.put("VSPositions", VSRadarTracks.serializeNBTList(VSPositions.values()));

    }

    public int getDishCount() {
        return dishCount;
    }

    public Optional<RadarContraption> getContraption() {
        return Optional.ofNullable(movedContraption)
                .map(ControlledContraptionEntity::getContraption)
                .filter(c -> c instanceof RadarContraption)
                .map(c -> (RadarContraption) c);
    }

    public float getAngle() {
        return angle;
    }

    public Direction getReceiverFacing() {
        return receiverFacing;
    }

    public List<RadarTrack> getEntityPositions() {
        return new ArrayList<>(entityPositions.values());
    }

    public List<VSRadarTracks> getVS2Positions() {
        return new ArrayList<>(VSPositions.values());
    }

    public float getRange() {
        return 20 + dishCount * 5;
    }
}
