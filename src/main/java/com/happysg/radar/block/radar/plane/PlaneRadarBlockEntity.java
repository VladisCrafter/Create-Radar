package com.happysg.radar.block.radar.plane;

import com.happysg.radar.block.radar.behavior.IRadar;
import com.happysg.radar.block.radar.behavior.RadarScanningBlockBehavior;
import com.happysg.radar.block.radar.track.RadarTrack;
import com.happysg.radar.compat.Mods;
import com.happysg.radar.compat.vs2.PhysicsHandler;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.List;

public class PlaneRadarBlockEntity extends SmartBlockEntity implements IRadar {
    private RadarScanningBlockBehavior scanningBehavior;


    public PlaneRadarBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void initialize() {
        super.initialize();
        if (!Mods.VALKYRIENSKIES.isLoaded())
            return;
        scanningBehavior.setScanPos(PhysicsHandler.getWorldVec(this));
        scanningBehavior.setRunning(true);
    }

    @Override
    public BlockPos getWorldPos() {
        return getBlockPos();
    }

    @Override
    public void tick() {
        super.tick();
        if (!Mods.VALKYRIENSKIES.isLoaded())
            return;
        Direction facing = getBlockState().getValue(PlaneRadarBlock.FACING);
        Vec3 facingVec = new Vec3(facing.getStepX(), facing.getStepY(), facing.getStepZ());
        Vec3 shipVec = PhysicsHandler.getWorldVecDirectionTransform(facingVec, this);
        double angle = Math.toDegrees(Math.atan2(shipVec.x, shipVec.z));
        scanningBehavior.setAngle((angle + 360) % 360);
    }


    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        scanningBehavior = new RadarScanningBlockBehavior(this);
        scanningBehavior.setRunning(true);
        scanningBehavior.setRange(250);
        scanningBehavior.setAngle((getBlockState().getValue(PlaneRadarBlock.FACING).toYRot() + 360) % 360);
        scanningBehavior.setScanPos(PhysicsHandler.getWorldVec(this));
        scanningBehavior.setTrackExpiration(1);
        behaviours.add(scanningBehavior);
    }


    @Override
    public Collection<RadarTrack> getTracks() {
        return scanningBehavior.getRadarTracks();
    }

    @Override
    public float getRange() {
        return 250;
    }

    @Override
    public boolean isRunning() {
        return true;
    }


    @Override
    public float getGlobalAngle() {
        return 0;
    }

    @Override
    public boolean renderRelativeToMonitor() {
        return false;
    }
}
