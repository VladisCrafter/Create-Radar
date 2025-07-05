package com.happysg.radar.block.controller.pitch;

import com.happysg.radar.block.datalink.screens.TargetingConfig;
import com.happysg.radar.block.network.WeaponNetwork;
import com.happysg.radar.block.network.WeaponNetworkSavedData;
import com.happysg.radar.block.network.WeaponNetworkUnit;
import com.happysg.radar.compat.Mods;
import com.happysg.radar.compat.cbc.CannonTargeting;
import com.mojang.logging.LogUtils;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;
import com.happysg.radar.compat.cbc.VS2CannonTargeting;
import com.happysg.radar.compat.vs2.PhysicsHandler;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlockEntity;
import rbasamoyai.createbigcannons.cannon_control.contraption.AbstractMountedCannonContraption;
import rbasamoyai.createbigcannons.cannon_control.contraption.PitchOrientedContraptionEntity;

import java.util.ArrayList;
import java.util.List;

public class AutoPitchControllerBlockEntity extends KineticBlockEntity implements WeaponNetworkUnit {
    private static final Logger LOGGER = LogUtils.getLogger();
    private WeaponNetwork weaponNetwork;

    private static final double TOLERANCE = 0.1;
    private double targetAngle;
    public boolean isRunning;
    private boolean artilleryMode = false;

    public AutoPitchControllerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
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
                } else if (cannonWeaponNetwork != null && cannonWeaponNetwork.getAutoPitchController() == null) {
                    cannonWeaponNetwork.setController(this);
                    setWeaponNetwork(cannonWeaponNetwork);
                } else if (weaponNetworkSavedData.networkContains(cannon.getBlockPos()) == null) {
                    WeaponNetwork newNetwork = new WeaponNetwork(level);
                    newNetwork.setCannonMount(cannon);
                    newNetwork.setController(this);
                    setWeaponNetwork(newNetwork);
                }
            }
        }
    }

    public void onRemoved() {
        if (weaponNetwork != null && weaponNetwork.getAutoPitchController() == this) {
            weaponNetwork.setAutoPitchController(null);
            setWeaponNetwork(null);
        }
    }

    @Override
    public void initialize() {
        super.initialize();
        if (Mods.CREATEBIGCANNONS.isLoaded()) {
            LOGGER.debug("  → CBC is loaded");
            BlockPos cannonMountPos = getBlockPos().relative(getBlockState().getValue(AutoPitchControllerBlock.HORIZONTAL_FACING));
            if (level != null && level.getBlockEntity(cannonMountPos) instanceof CannonMountBlockEntity mount) {
                LOGGER.debug("  → Level not null and cannon pos good");

            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level != null && !this.level.isClientSide()) {
            if (Mods.CREATEBIGCANNONS.isLoaded()) {
                LOGGER.debug("pitch.tick() → isRunning={}", isRunning);
                if (isRunning) {
                    tryRotateCannon();
                }
            }
        }
    }

    private void tryRotateCannon() {
        if (level == null || level.isClientSide())
            return;
        LOGGER.debug("  → level is good");

        BlockPos cannonMountPos = getBlockPos().relative(getBlockState().getValue(AutoPitchControllerBlock.HORIZONTAL_FACING));
        if (!(level.getBlockEntity(cannonMountPos) instanceof CannonMountBlockEntity mount))
            return;
        LOGGER.debug("  → CannonMountEntity Gucci");

        PitchOrientedContraptionEntity contraption = mount.getContraption();
        if (contraption == null)
            return;

        LOGGER.debug("  → Contraption is not null");

        if (!(contraption.getContraption() instanceof AbstractMountedCannonContraption cannonContraption))
            return;

        double currentPitch = contraption.pitch;
        LOGGER.debug("  → Current pitch {}", currentPitch);
        int invert = -cannonContraption.initialOrientation().getStepX() + cannonContraption.initialOrientation().getStepZ();
        currentPitch = currentPitch * -invert;

        LOGGER.debug("  → Current After Invert pitch {}", currentPitch);

        double pitchDifference = targetAngle - currentPitch;
//        double speedFactor = Math.max(Math.abs(getSpeed())/32.0, 0.1);
        double speedFactor = Math.abs(getSpeed()) / 32.0;
        LOGGER.debug("  → pitchStep (|speed|/32) = {}° per tick,   invert = {}", speedFactor, invert);
        double newPitch;

        if (Math.abs(pitchDifference) > TOLERANCE) {
            if (Math.abs(pitchDifference) > speedFactor) {
                newPitch = currentPitch + Math.signum(pitchDifference) * speedFactor;
            } else {
                newPitch = targetAngle;
            }
        } else {
            newPitch = targetAngle;
        }

        // **LOG THE DELTA HERE**:
        double deltaApplied = newPitch - currentPitch;
        LOGGER.debug("→ applying pitch delta = {}° per tick", deltaApplied);

        // now actually apply it:
        mount.setPitch((float) newPitch);
        mount.notifyUpdate();
    }

    public boolean atTargetPitch() {
        BlockPos turretPos = getBlockPos().relative(getBlockState().getValue(AutoPitchControllerBlock.HORIZONTAL_FACING));
        if (level == null || !(level.getBlockEntity(turretPos) instanceof CannonMountBlockEntity mount))
            return false;
        PitchOrientedContraptionEntity contraption = mount.getContraption();
        if (contraption == null)
            return false;
        int invert = -contraption.getInitialOrientation().getStepZ() + contraption.getInitialOrientation().getStepX();
        LOGGER.debug("orientation steps: stepX={}, stepZ={}, invert={}", contraption.getInitialOrientation().getStepX(), contraption.getInitialOrientation().getStepZ(), invert);
        return Math.abs(contraption.pitch * invert - targetAngle) < TOLERANCE;
    }

    public void setTargetAngle(float targetAngle) {
        LOGGER.debug("  → SetTargetAngle Ran");
        this.targetAngle = targetAngle;
    }


    public double getTargetAngle() {
        LOGGER.debug("  → getTargetAngle Ran");
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



    public WeaponNetwork getWeaponNetwork() {
        return weaponNetwork;
    }

    public void setWeaponNetwork(WeaponNetwork weaponNetwork) {
        this.weaponNetwork = weaponNetwork;
    }
}
