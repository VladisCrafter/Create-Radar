package com.happysg.radar.block.controller.pitch;

import com.happysg.radar.block.controller.yaw.AutoYawControllerBlockEntity;
import com.happysg.radar.compat.Mods;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlockEntity;
import rbasamoyai.createbigcannons.cannon_control.contraption.PitchOrientedContraptionEntity;

public class AutoPitchControllerBlockEntity extends KineticBlockEntity {
    private static final double TOLERANCE = 0.1;
    private double targetAngle;
    private boolean isRunning;


    public AutoPitchControllerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void tick() {
        super.tick();
        if (Mods.CREATEBIGCANNONS.isLoaded())
            tryRotateCannon();

    }

    private void tryRotateCannon() {
        if (level.isClientSide())
            return;


        BlockPos cannonMountPos = getBlockPos().relative(getBlockState().getValue(AutoPitchControllerBlock.HORIZONTAL_FACING));
        if (!(level.getBlockEntity(cannonMountPos) instanceof CannonMountBlockEntity mount))
            return;

        if (!isRunning) {
            stopFireCannon(mount);
            return;
        }

        PitchOrientedContraptionEntity contraption = mount.getContraption();
        if (contraption == null)
            return;

        double currentPitch = contraption.pitch;
        if (correctPitch(currentPitch) && correctYaw())
            tryFireCannon(mount);
        else
            stopFireCannon(mount);

        double pitchDifference = targetAngle - currentPitch;
        double speedFactor = Math.abs(getSpeed()) / 32.0;


        if (Math.abs(pitchDifference) > TOLERANCE) {
            if (Math.abs(pitchDifference) > speedFactor) {
                currentPitch += Math.signum(pitchDifference) * speedFactor;
            } else {
                currentPitch = targetAngle;
            }
        } else {
            currentPitch = targetAngle;
        }

        mount.setPitch((float) currentPitch);
        mount.notifyUpdate();
    }

    private boolean correctPitch(double currentPitch) {
        return Math.abs(targetAngle - currentPitch) < TOLERANCE;
    }

    private boolean correctYaw() {
        BlockPos cannonMountPos = getBlockPos().relative(getBlockState().getValue(AutoPitchControllerBlock.HORIZONTAL_FACING));
        if (!(level.getBlockEntity(cannonMountPos.below()) instanceof AutoYawControllerBlockEntity yawController))
            return false;

        return yawController.atTargetYaw();
    }

    private void stopFireCannon(CannonMountBlockEntity mount) {
        mount.onRedstoneUpdate(true, true, false, true, 0);
    }

    private void tryFireCannon(CannonMountBlockEntity mount) {
        mount.onRedstoneUpdate(true, true, true, false, 15);
    }


    public void setTargetAngle(float targetAngle) {
        this.targetAngle = targetAngle;
        notifyUpdate();
    }


    public double getTargetAngle() {
        return targetAngle;
    }

    @Override
    protected void copySequenceContextFrom(KineticBlockEntity sourceBE) {
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        targetAngle = compound.getDouble("TargetAngle");
        isRunning = compound.getBoolean("IsRunning");
    }

    @Override
    protected void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.putDouble("TargetAngle", targetAngle);
        compound.putBoolean("IsRunning", isRunning);
    }

    public void setTarget(Vec3 targetPos) {
        if (level.isClientSide())
            return;
        if (targetPos == null) {
            isRunning = false;
            return;
        }

        isRunning = true;
        Vec3 cannonCenter = getBlockPos().relative(getBlockState().getValue(AutoPitchControllerBlock.HORIZONTAL_FACING)).above(3).getCenter();
        double dx = cannonCenter.x - targetPos.x;
        double dy = cannonCenter.y - targetPos.y;
        double dz = cannonCenter.z - targetPos.z;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);


        targetAngle = -Math.toDegrees(Math.atan2(dy, horizontalDistance));
        targetAngle += horizontalDistance / 95f;

        // Ensure pitch is within -90 to 90 degrees
        if (targetAngle < -90) {
            targetAngle = -90;
        } else if (targetAngle > 90) {
            targetAngle = 90;
        }
        notifyUpdate();
    }

}
