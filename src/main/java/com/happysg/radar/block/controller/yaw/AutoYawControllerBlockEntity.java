package com.happysg.radar.block.controller.yaw;

import com.happysg.radar.block.network.WeaponNetwork;
import com.happysg.radar.block.network.WeaponNetworkSavedData;
import com.happysg.radar.block.network.WeaponNetworkUnit;
import com.happysg.radar.compat.Mods;
import com.happysg.radar.compat.vs2.PhysicsHandler;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlockEntity;
import rbasamoyai.createbigcannons.cannon_control.contraption.PitchOrientedContraptionEntity;


public class AutoYawControllerBlockEntity extends GeneratingKineticBlockEntity implements WeaponNetworkUnit {
    private static final double TOLERANCE = 0.1;
    private double targetAngle;
    public boolean isRunning;
    private WeaponNetwork weaponNetwork;


    public AutoYawControllerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }
    public void onPlaced() {
        if (!(level instanceof ServerLevel serverLevel)) return;

        for (Direction direction : Direction.values()) {
            BlockEntity neighborBE = level.getBlockEntity(worldPosition.relative(direction));
            if (neighborBE instanceof CannonMountBlockEntity cannon) {
                WeaponNetworkSavedData weaponNetworkSavedData = WeaponNetworkSavedData.get(serverLevel);
                WeaponNetwork weaponNetwork = weaponNetworkSavedData.networkContains(worldPosition);
                WeaponNetwork cannonWeaponNetwork = weaponNetworkSavedData.networkContains(cannon.getBlockPos());

                if (weaponNetwork != null) { // Shouldn't happen normally
                    setWeaponNetwork(weaponNetwork);
                } else if (cannonWeaponNetwork != null && cannonWeaponNetwork.getAutoYawController() == null) {
                    cannonWeaponNetwork.setAutoYawController(this);
                    setWeaponNetwork(cannonWeaponNetwork);
                } else if (weaponNetworkSavedData.networkContains(cannon.getBlockPos()) == null) {
                    WeaponNetwork newNetwork = new WeaponNetwork(level);
                    newNetwork.setCannonMount(cannon);
                    newNetwork.setAutoYawController(this);
                    setWeaponNetwork(newNetwork);
                }
            }
        }
    }

    public void onRemoved() {
        if (weaponNetwork != null && weaponNetwork.getAutoYawController() == this) {
            weaponNetwork.setAutoYawController(null);
            setWeaponNetwork(null);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (Mods.CREATEBIGCANNONS.isLoaded())
            tryRotateCannon();
    }

    private void tryRotateCannon() {
        if (level == null || level.isClientSide())
            return;
        if (!isRunning)
            return;
        if (!(level.getBlockEntity(getBlockPos().above()) instanceof CannonMountBlockEntity mount))
            return;

        PitchOrientedContraptionEntity contraption = mount.getContraption();
        if (contraption == null)
            return;

        double currentYaw = contraption.yaw;
        if (currentYaw == targetAngle) {
            isRunning = false;
            return;
        }

        // Normalize both currentYaw and targetAngle to [0, 360)
        currentYaw = (currentYaw + 360) % 360;
        targetAngle = (targetAngle + 360) % 360;

        double yawDifference = targetAngle - currentYaw;
        // Normalize yawDifference to range [-180, 180]
        yawDifference = (yawDifference + 180) % 360 - 180;

        if (yawDifference > 180) {
            yawDifference -= 360; // Rotate counterclockwise
        } else if (yawDifference < -180) {
            yawDifference += 360; // Rotate clockwise
        }

        double speedFactor = Math.abs(getSpeed()) / 32.0;

        if (Math.abs(yawDifference) > TOLERANCE) {
            if (Math.abs(yawDifference) > speedFactor) {
                currentYaw += Math.signum(yawDifference) * speedFactor;
            } else {
                currentYaw = targetAngle;
            }
        } else {
            currentYaw = targetAngle;
        }

        // Set the new yaw back to the contraption
        mount.setYaw((float) ((currentYaw + 360) % 360)); // Normalize back to [0, 360] if necessary
        mount.notifyUpdate();
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

    public boolean atTargetYaw() {
        BlockPos turretPos = getBlockPos().above();
        if (level == null || !(level.getBlockEntity(turretPos) instanceof CannonMountBlockEntity mount))
            return false;
        PitchOrientedContraptionEntity contraption = mount.getContraption();
        if (contraption == null)
            return false;
        return Math.abs(contraption.yaw - targetAngle) < TOLERANCE;
    }
    public WeaponNetwork getWeaponNetwork() {
        return weaponNetwork;
    }

    public void setWeaponNetwork(WeaponNetwork weaponNetwork) {
        this.weaponNetwork = weaponNetwork;
    }
}
