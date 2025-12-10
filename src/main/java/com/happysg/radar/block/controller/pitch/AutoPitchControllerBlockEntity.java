package com.happysg.radar.block.controller.pitch;

import com.happysg.radar.block.controller.firing.FiringControlBlockEntity;
import com.happysg.radar.block.datalink.screens.TargetingConfig;
import com.happysg.radar.block.radar.track.RadarTrack;
import com.happysg.radar.compat.Mods;
import com.happysg.radar.compat.cbc.CannonTargeting;
import com.mojang.logging.LogUtils;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
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
import rbasamoyai.createbigcannons.CreateBigCannons;
import rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlock;
import rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlockEntity;
import rbasamoyai.createbigcannons.cannon_control.contraption.AbstractMountedCannonContraption;
import rbasamoyai.createbigcannons.cannon_control.contraption.PitchOrientedContraptionEntity;

import java.util.ArrayList;
import java.util.List;

public class AutoPitchControllerBlockEntity extends KineticBlockEntity {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final double TOLERANCE = 0.1;
    private double targetAngle;
    public boolean isRunning;
    private boolean artillery = false;
    private RadarTrack track;

    //abstract class for firing control to avoid cluttering pitch logic
    public FiringControlBlockEntity firingControl;
    public CannonMountBlockEntity mountBlock;


    public AutoPitchControllerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void initialize() {
        super.initialize();
        if (Mods.CREATEBIGCANNONS.isLoaded()) {
            LOGGER.debug("  → CBC is loaded");
            BlockPos cannonMountPos = getBlockPos().relative(getBlockState().getValue(AutoPitchControllerBlock.HORIZONTAL_FACING));
            if (level != null && level.getBlockEntity(cannonMountPos) instanceof CannonMountBlockEntity mount) {
                LOGGER.debug("  → Level not null and cannon pos good");
                firingControl = new FiringControlBlockEntity(this, mount);
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
                if (firingControl != null) {
                    LOGGER.debug("  → firingcontrol is not null");
                    firingControl.tick();
                }
            }
        }
    }
    public BlockPos getMount(){
        BlockPos cannonMountPos = getBlockPos().relative(getBlockState().getValue(AutoPitchControllerBlock.HORIZONTAL_FACING));
        return cannonMountPos;
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

    public void setTarget(Vec3 targetPos) {
        if (level == null || level.isClientSide()) {
            LOGGER.debug(" • bailing: client side or null target → isRunning=false");
            return;
        }

        if (targetPos == null) {
            isRunning = false;
            return;
        }

        LOGGER.debug("→ setTarget called with {}", targetPos);


        if (level.getBlockEntity(getBlockPos().relative(getBlockState().getValue(AutoPitchControllerBlock.HORIZONTAL_FACING))) instanceof CannonMountBlockEntity mount) {
            if(PhysicsHandler.isBlockInShipyard(level, this.getBlockPos())) {
                List<List<Double>> angles = VS2CannonTargeting.calculatePitchAndYawVS2(mount, targetPos, (ServerLevel) level);
                if(angles == null) return;
                if(angles.isEmpty()) return;
                if(angles.get(0).isEmpty()) return;
                this.targetAngle = angles.get(0).get(0);

                LOGGER.debug("  Computed targetAngle (CBC) = {} rad {}}", this.targetAngle, Math.toDegrees(this.targetAngle));

                if(firingControl == null) return;
                LOGGER.debug(" Firing control not null");
                this.firingControl.cannonMount.setYaw(angles.get(0).get(1).floatValue());
                isRunning = true;
            } else{
                List<Double> angles = CannonTargeting.calculatePitch(mount, targetPos, (ServerLevel) level);
                if (angles == null) {
                    LOGGER.debug("   • calculatePitch returned null → aborting, isRunning=false");
                    isRunning = false;
                    return;
                }
                if (angles.isEmpty()) {
                    LOGGER.debug("   • calculatePitch returned empty list → aborting, isRunning=false");
                    isRunning = false;
                    return;
                }
                LOGGER.debug("   • raw angles = {}", angles);
                List<Double> usableAngles = new ArrayList<>();
                for (double angle : angles) {
                    if (mount.getContraption() == null) break;
                    if (angle < mount.getContraption().maximumElevation() && angle > -mount.getContraption().maximumDepression()) {
                        usableAngles.add(angle);
                    }
                }

                LOGGER.debug("   • usable angles = {}", usableAngles);

                if (artillery && usableAngles.size() == 2) {
                    targetAngle = angles.get(1);
                } else if (!usableAngles.isEmpty()) {
                    targetAngle = usableAngles.get(0);
                }

                isRunning = true;
                LOGGER.debug("   • computed targetAngle={}° ({} rad) → isRunning=true", this.targetAngle, Math.toDegrees(this.targetAngle));
                LOGGER.debug(">>> pitch.setTarget() on SERVER at {} → target={}", this.worldPosition, targetPos);
            }
        }
    }
    public void setTrack(RadarTrack track){
        this.track = track;
    }
    public void setFiringTarget(Vec3 targetPos, TargetingConfig targetingConfig ) {
        LOGGER.debug("PitchController.setFiringTarget: targetPos={}", targetPos);

        if (firingControl == null) {
            BlockPos mountPos = getBlockPos().relative(getBlockState().getValue(AutoPitchControllerBlock.HORIZONTAL_FACING));
            if (level != null && level.getBlockEntity(mountPos) instanceof CannonMountBlockEntity mount) {
                LOGGER.debug("   • no mount at {} → isRunning=false", mountPos);
                firingControl = new FiringControlBlockEntity(this, mount);
            }
        }

        if (firingControl == null) {
            LOGGER.debug("PitchController: No firingControl available, skipping setFiringTarget");
            return;
        }

        firingControl.setTarget(targetPos, targetingConfig, track);
    }

    public void setSafeZones(List<AABB> safeZones) {
        if (firingControl == null)
            return;
        firingControl.setSafeZones(safeZones);    }
}
