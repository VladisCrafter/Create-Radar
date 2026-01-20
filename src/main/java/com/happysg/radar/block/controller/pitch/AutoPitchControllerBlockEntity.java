package com.happysg.radar.block.controller.pitch;

import com.happysg.radar.block.behavior.networks.WeaponFiringControl;
import com.happysg.radar.block.behavior.networks.WeaponNetworkData;
import com.happysg.radar.block.behavior.networks.config.TargetingConfig;
import com.happysg.radar.block.controller.yaw.AutoYawControllerBlockEntity;
import com.happysg.radar.block.radar.track.RadarTrack;
import com.happysg.radar.compat.Mods;
import com.happysg.radar.compat.cbc.CannonTargeting;
import com.happysg.radar.compat.cbc.VS2CannonTargeting;
import com.happysg.radar.compat.vs2.PhysicsHandler;
import com.mojang.datafixers.types.Type;
import com.mojang.logging.LogUtils;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4dc;
import org.joml.Vector3d;
import org.slf4j.Logger;
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingBlockEntity;
import org.valkyrienskies.clockwork.platform.api.ContraptionController;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlockEntity;
import rbasamoyai.createbigcannons.cannon_control.contraption.AbstractMountedCannonContraption;
import rbasamoyai.createbigcannons.cannon_control.contraption.PitchOrientedContraptionEntity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class AutoPitchControllerBlockEntity extends KineticBlockEntity {
    private static final Logger LOGGER = LogUtils.getLogger();
    // CBC tolerance in "CBC units" (radians in your current code)
    private static final double CBC_TOLERANCE = 0.10;

    // PhysBearing tolerance in degrees
    private static final double PHYS_TOLERANCE_DEG = 0.10;
    private static final double DEADBAND_DEG = 0.25;

    // ===== State =====
    private double targetAngle;
    public boolean isRunning;

    // artillery selection (CBC)
    private boolean artillery = false;

    private RadarTrack track;

    // firing control
    public WeaponFiringControl firingControl;
    public AutoYawControllerBlockEntity autoyaw;
    // ===== PhysBearing smoothing state (NO player tracking) =====
    private static final double SNAP_DISTANCE = 32.0;
    private static final double MIN_MOVE_PER_TICK = 0.02;
    private static final double MAX_MOVE_PER_TICK = 2.0;

    private static final double MIN_DEG_PER_TICK = 0.30; // ~1 RPM feel
    private static final double MAX_DEG_PER_TICK = 18.0; // 256 RPM feel
    private static final double CURVE_GAMMA = 1.65;

    private static final int MOUNT_RECHECK_EVERY_TICKS = 20;

    private double lastCommandedDeg = Double.NaN;

    @Nullable
    private Vec3 desiredTarget = null;   // latest commanded target (ship-space if on ship)

    @Nullable
    private Vec3 smoothedTarget = null;

    private boolean mountDirty = true;
    private int mountRecheckCooldown = 0;
    private Entity targetentity;

    // cached mount
    private PhysBearingBlockEntity currentMount;

    public void getfiringcontrol() {
        if (firingControl == null && level instanceof ServerLevel serverLevel) {
            if ( WeaponNetworkData.get(serverLevel).getWeaponGroupViewFromEndpoint(serverLevel.dimension(), worldPosition) != null && getWeaponGroup().yawPos() != null) {
                if (level != null && level.getBlockEntity(getWeaponGroup().yawPos()) instanceof AutoYawControllerBlockEntity aYCBE) {
                    autoyaw = aYCBE;
                }
            }
            if (level != null && level.getBlockEntity(getMount()) instanceof CannonMountBlockEntity mount) {
                firingControl = new WeaponFiringControl(this, mount, autoyaw);
                LOGGER.warn("made new Weapon Config!");
            }
        }
    }


    public AutoPitchControllerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }
    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide())
            return;
        if(level instanceof ServerLevel serverLevel){
            if ( WeaponNetworkData.get(serverLevel).getWeaponGroupViewFromEndpoint(serverLevel.dimension(), worldPosition) == null ) return;
        }
        if (firingControl == null){
            getfiringcontrol();
        }

        Mount mount = resolveMount();
        if (mount == null)
            return;
        if (track != null) {
            if (mount.kind == MountKind.CBC && Mods.CREATEBIGCANNONS.isLoaded()) {
                rotateCBC(mount.cbc);
            } else if (mount.kind == MountKind.PHYS && Mods.VS_CLOCKWORK.isLoaded()) {
                rotatePhysBearing(mount.phys);
            }
        }
        if (firingControl != null && track != null) {
            firingControl.tick();
        }
    }

    /** Horizontal controller: mount is in front of the controller. */
    public BlockPos getMount(){

        return getWeaponGroup().mountPos();
    }

    public void setTargetAngle(float angle) {
        this.targetAngle = angle;
        this.isRunning = true;

        // phys smoothing reset
        this.desiredTarget = null;
        this.smoothedTarget = null;
        this.lastCommandedDeg = Double.NaN;

        notifyUpdate();
        setChanged();

    }
    @Nullable
    private WeaponNetworkData.WeaponGroupView getWeaponGroup() {
        if (level == null || level.isClientSide) return null;
        if (!(level instanceof ServerLevel sl)) return null;

        WeaponNetworkData data = WeaponNetworkData.get(sl);
        return data.getWeaponGroupViewFromEndpoint(sl.dimension(), worldPosition);
    }
    public static Entity getEntityByUUID(ServerLevel level, UUID uuid) {
        return level.getEntity(uuid);
    }
    public void setAndAcquireTrack(@Nullable RadarTrack tTrack, TargetingConfig config) {
        if (level == null || level.isClientSide) return;
        if(tTrack == null){
            track = null;
            firingControl.resetTarget();
            return;
        }
        if (tTrack != track) {
            targetentity = null;
            track = tTrack;
        }


        if (firingControl == null) getfiringcontrol();
        if (firingControl == null) return;

        if (!(level instanceof ServerLevel sl)) return;

        if (targetentity == null) {
            targetentity = getEntityByUUID(sl, tTrack.getUuid());
            if (targetentity == null) {
                LOGGER.warn("target entity lookup failed for {}", tTrack.getUuid());
                return;
            }
        }

        var view = getWeaponGroup();
        if (view == null) return;
        firingControl.setTarget(targetentity.position(), config, tTrack, view, targetentity);
    }




    public double getTargetAngle() {
        return targetAngle;
    }
    public void setTarget(@Nullable Vec3 targetPos) {
        if (level == null || level.isClientSide())
            return;

        if (targetPos == null) {
            isRunning = false;

            desiredTarget = null;
            smoothedTarget = null;
            lastCommandedDeg = Double.NaN;

            notifyUpdate();
            setChanged();
            return;
        }

        Mount mount = resolveMount();
        if (mount == null)
            return;

        if (mount.kind == MountKind.CBC && Mods.CREATEBIGCANNONS.isLoaded()) {
            setTargetCBC(mount.cbc, targetPos);
            return;
        }

        if (mount.kind == MountKind.PHYS && Mods.VS_CLOCKWORK.isLoaded()) {

            Ship ship = getShipIfPresent();
            Vec3 desired = (ship != null) ? toShipSpace(ship, targetPos) : targetPos;

            desiredTarget = desired;
            if (smoothedTarget == null)
                smoothedTarget = desired;

            isRunning = true;
            lastCommandedDeg = Double.NaN;

            notifyUpdate();
            setChanged();
        }
    }

    /** CBC path: checks contraption.pitch vs targetAngle (both in CBC units). */
    public boolean atTargetPitch() {
        if (level == null)
            return false;

        Mount mount = resolveMount();
        if (mount == null)
            return false;

        if (mount.kind == MountKind.CBC && Mods.CREATEBIGCANNONS.isLoaded()) {
            PitchOrientedContraptionEntity contraption = mount.cbc.getContraption();
            if (contraption == null)
                return false;

            if (!(contraption.getContraption() instanceof AbstractMountedCannonContraption cannonContraption))
                return false;

            double currentPitch = contraption.pitch;
            int invert = -cannonContraption.initialOrientation().getStepX() + cannonContraption.initialOrientation().getStepZ();
            currentPitch = currentPitch * -invert;

            return Math.abs(currentPitch - targetAngle) < CBC_TOLERANCE;
        }

        if (mount.kind == MountKind.PHYS && Mods.VS_CLOCKWORK.isLoaded()) {
            Double actualRad = mount.phys.getActualAngle();
            if (actualRad == null)
                return false;

            double currentDeg = wrap360(Math.toDegrees(actualRad));
            double desiredDeg = wrap360(targetAngle);

            return Math.abs(shortestDelta(currentDeg, desiredDeg)) < Math.max(PHYS_TOLERANCE_DEG, DEADBAND_DEG);
        }

        return false;
    }

    public void setTrack(RadarTrack track) {
        this.track = track;
    }



    public void setSafeZones(List<AABB> safeZones) {
        if (firingControl == null)
            return;
        firingControl.setSafeZones(safeZones);
    }

    // ============================================================
    // CBC behavior (aligned to yaw’s “snap + stop” flow)
    // ============================================================

    private void rotateCBC(CannonMountBlockEntity mount) {
        if (!isRunning)
            return;

        PitchOrientedContraptionEntity contraption = mount.getContraption();
        if (contraption == null)
            return;

        if (!(contraption.getContraption() instanceof AbstractMountedCannonContraption cannonContraption))
            return;

        double currentPitch = contraption.pitch;
        int invert = -cannonContraption.initialOrientation().getStepX() + cannonContraption.initialOrientation().getStepZ();
        currentPitch = currentPitch * -invert;

        double diff = targetAngle - currentPitch;

        // close enough; snap + stop (like yaw)
        if (Math.abs(diff) <= CBC_TOLERANCE) {
            mount.setPitch((float) targetAngle);
            mount.notifyUpdate();
            isRunning = false;
            return;
        }

        double speedFactor = Math.abs(getSpeed()) / 32.0;
        if (speedFactor <= 0.0)
            return;

        double next;
        if (Math.abs(diff) > speedFactor) {
            next = currentPitch + Math.signum(diff) * speedFactor;
        } else {
            next = targetAngle;
        }

        mount.setPitch((float) next);
        mount.notifyUpdate();
    }

    private void setTargetCBC(CannonMountBlockEntity mount, Vec3 targetPos) {
        if (level == null || !(level instanceof ServerLevel serverLevel))
            return;


        if (PhysicsHandler.isBlockInShipyard(level, this.getBlockPos())) {
            List<List<Double>> angles = VS2CannonTargeting.calculatePitchAndYawVS2(mount, targetPos, serverLevel);
            if (angles == null || angles.isEmpty() || angles.get(0).isEmpty())
                return;

            // pitch
            this.targetAngle = angles.get(0).get(0);

            // yaw (if you have firingControl)
            if (firingControl != null) {
                firingControl.cannonMount.setYaw(angles.get(0).get(1).floatValue());
            }

            isRunning = true;
            notifyUpdate();
            setChanged();
            return;
        }

        // normal CBC pitch targeting
        List<Double> angles = CannonTargeting.calculatePitch(mount, targetPos, serverLevel);
        if (angles == null || angles.isEmpty()) {
            isRunning = false;
            return;
        }

        List<Double> usableAngles = new ArrayList<>();
        for (double angle : angles) {
            if (mount.getContraption() == null)
                break;
            if (angle < mount.getContraption().maximumElevation() && angle > -mount.getContraption().maximumDepression()) {
                usableAngles.add(angle);
            }
        }

        if (artillery && usableAngles.size() == 2) {
            targetAngle = angles.get(1);
        } else if (!usableAngles.isEmpty()) {
            targetAngle = usableAngles.get(0);
        }

        isRunning = true;
        notifyUpdate();
        setChanged();
    }

    // ============================================================
    // PhysBearing behavior (aligned to yaw: follow-angle + unwrap)
    // ============================================================

    private void rotatePhysBearing(PhysBearingBlockEntity mount) {
        // Ensure follow-angle mode
        ScrollOptionBehaviour<ContraptionController.LockedMode> mode = mount.getMovementMode();
        if (mode != null && mode.getValue() != ContraptionController.LockedMode.FOLLOW_ANGLE.ordinal()) {
            mode.setValue(ContraptionController.LockedMode.FOLLOW_ANGLE.ordinal());
        }

        double rpmAbs = Math.abs(getSpeed());
        if (rpmAbs <= 0.0)
            return;

        if (!isRunning)
            return;

        // If we have a desired target, keep smoothing towards it and update targetAngle (degrees)
        updateSmoothedTargetAndAngle(rpmAbs);

        Double actualRad = mount.getActualAngle();
        if (actualRad == null)
            return;

        double currentDeg = wrap360(Math.toDegrees(actualRad));
        double desiredDeg = wrap360(targetAngle);

        // "snap at max rpm" like yaw
        if (rpmAbs >= 256.0) {
            lastCommandedDeg = desiredDeg;
            mount.setAngle((float) desiredDeg);
            mount.notifyUpdate();
            return;
        }

        // latch the last-commanded value
        if (Double.isNaN(lastCommandedDeg)) {
            lastCommandedDeg = currentDeg;
        }

        // unwrap target near last command to avoid long spins
        double desiredContinuous = unwrapNear(lastCommandedDeg, desiredDeg);

        // deadband: stop running when close enough (similar spirit to yaw CBC stop)
        if (Math.abs(shortestDelta(currentDeg, desiredDeg)) <= Math.max(PHYS_TOLERANCE_DEG, DEADBAND_DEG)) {
            mount.setAngle((float) desiredDeg);
            mount.notifyUpdate();
            isRunning = false;
            lastCommandedDeg = desiredContinuous;
            return;
        }

        lastCommandedDeg = desiredContinuous;
        mount.setAngle((float) wrap360(desiredContinuous));
        mount.notifyUpdate();
    }

    private void updateSmoothedTargetAndAngle(double rpmAbs) {
        if (desiredTarget == null)
            return;

        Direction facing = getBlockState().getValue(AutoPitchControllerBlock.HORIZONTAL_FACING);
        Vec3 pivot = getMount().getCenter(); // pivot at the bearing block center (one ahead)

        if (smoothedTarget == null) {
            smoothedTarget = desiredTarget;
        } else {
            Vec3 delta = desiredTarget.subtract(smoothedTarget);
            double dist = delta.length();

            if (dist > SNAP_DISTANCE) {
                smoothedTarget = desiredTarget;
            } else if (dist > 1e-6) {
                double radius = diskRadius(pivot, smoothedTarget, facing);
                double step = stepTowardTarget(radius, dist, rpmAbs);
                smoothedTarget = smoothedTarget.add(delta.scale(step / dist));
            }
        }

        double newAngle = rollAroundFacingDeg(pivot, smoothedTarget, facing);

        // deadband to avoid micro-chasing
        if (Math.abs(shortestDelta(targetAngle, newAngle)) < DEADBAND_DEG) {
            return;
        }

        targetAngle = approachWrapped(targetAngle, newAngle);
    }

    // ============================================================
    // Mount resolution (aligned conceptually to yaw, but horizontal/front)
    // ============================================================

    private enum MountKind { CBC, PHYS }

    private static class Mount {
        final MountKind kind;
        final CannonMountBlockEntity cbc;
        final PhysBearingBlockEntity phys;

        private Mount(CannonMountBlockEntity cbc) {
            this.kind = MountKind.CBC;
            this.cbc = cbc;
            this.phys = null;
        }

        private Mount(PhysBearingBlockEntity phys) {
            this.kind = MountKind.PHYS;
            this.cbc = null;
            this.phys = phys;
        }
    }

    @Nullable
    private Mount resolveMount() {
        if (level == null)
            return null;

        // Recheck/refresh phys cache periodically (like yaw’s repeated resolve)
        if (!mountDirty && mountRecheckCooldown-- > 0) {

        } else {


            BlockPos front = getMount();
            BlockEntity be = level.getBlockEntity(front);

            // CBC first if present
            if (Mods.CREATEBIGCANNONS.isLoaded() && be instanceof CannonMountBlockEntity cbc)
                return new Mount(cbc);

            // PhysBearing next if present
            if (Mods.VS_CLOCKWORK.isLoaded()) {
                if (be instanceof PhysBearingBlockEntity phys)
                    return new Mount(phys);
                if (currentMount != null)
                    return new Mount(currentMount);
            }


        }
        return null;
    }

    // ============================================================
    // NBT (same keys as before)
    // ============================================================

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        targetAngle = compound.getDouble("TargetAngle");
        isRunning = compound.getBoolean("IsRunning");
        // smoothing state not persisted by design
        desiredTarget = null;
        smoothedTarget = null;
        lastCommandedDeg = Double.NaN;
    }

    @Override
    protected void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.putDouble("TargetAngle", targetAngle);
        compound.putBoolean("IsRunning", isRunning);
    }

    @Override
    protected void copySequenceContextFrom(KineticBlockEntity sourceBE) {
        // keep empty, matching yaw controller style
    }

    // ============================================================
    // Geometry + helpers (PhysBearing mode)
    // ============================================================

    private static Vec3 forwardHoriz(Direction facing) {
        Vec3 f = new Vec3(facing.getStepX(), 0, facing.getStepZ());
        if (f.lengthSqr() < 1e-8)
            return new Vec3(0, 0, 1);
        return f.normalize();
    }

    private static Vec3 rightHoriz(Direction facing) {
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 fwd = forwardHoriz(facing);
        Vec3 r = up.cross(fwd);
        double ls = r.lengthSqr();
        return ls < 1e-8 ? new Vec3(1, 0, 0) : r.scale(1.0 / Math.sqrt(ls));
    }

    /** Returns degrees wrapped to [0,360). */
    private static double rollAroundFacingDeg(Vec3 pivot, Vec3 target, Direction facing) {
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 fwd = forwardHoriz(facing);
        Vec3 right = rightHoriz(facing);

        Vec3 v = target.subtract(pivot);

        // remove along-axis component so it only reacts to disk plane
        Vec3 vDisk = v.subtract(fwd.scale(v.dot(fwd)));

        double r = vDisk.dot(right);
        double u = vDisk.dot(up);

        if (Math.abs(r) < 1e-10 && Math.abs(u) < 1e-10)
            return 0.0;

        return wrap360(Math.toDegrees(Math.atan2(u, r)));
    }

    private static double diskRadius(Vec3 pivot, Vec3 target, Direction facing) {
        Vec3 fwd = forwardHoriz(facing);
        Vec3 v = target.subtract(pivot);
        Vec3 vDisk = v.subtract(fwd.scale(v.dot(fwd)));
        return Math.sqrt(vDisk.lengthSqr());
    }

    private static double stepTowardTarget(double radius, double distToSmoothed, double rpmAbs) {
        double degPerTick = degPerTickFromRpm(rpmAbs);
        double radPerTick = degPerTick * (Math.PI / 180.0);

        double maxStep = radius * radPerTick;
        maxStep = Math.max(MIN_MOVE_PER_TICK, Math.min(MAX_MOVE_PER_TICK, maxStep));

        return Math.min(distToSmoothed, maxStep);
    }

    private static double degPerTickFromRpm(double rpmAbs) {
        double r = Math.max(0.0, Math.min(256.0, rpmAbs));

        double t;
        if (r <= 1.0) {
            t = 0.0;
        } else {
            t = (r - 1.0) / 255.0;
            t = Math.max(0.0, Math.min(1.0, t));
        }

        double shaped = Math.pow(t, CURVE_GAMMA);
        return MIN_DEG_PER_TICK + (MAX_DEG_PER_TICK - MIN_DEG_PER_TICK) * shaped;
    }

    private static double approachWrapped(double currentWrapped, double newWrapped) {
        return wrap360(currentWrapped + shortestDelta(currentWrapped, newWrapped));
    }

    private static double wrap360(double a) {
        a %= 360.0;
        if (a < 0) a += 360.0;
        return a;
    }

    private static double shortestDelta(double from, double to) {
        return ((to - from + 540.0) % 360.0) - 180.0;
    }

    private static double unwrapNear(double lastContinuous, double newWrapped) {
        double lastWrapped = wrap360(lastContinuous);
        return lastContinuous + shortestDelta(lastWrapped, newWrapped);
    }

    // ============================================================
    // VS helpers (PhysBearing target space)
    // ============================================================

    @Nullable
    private Ship getShipIfPresent() {
        if (level == null) return null;
        return VSGameUtilsKt.getShipManagingPos(level, worldPosition);
    }

    private static Vec3 toShipSpace(Ship ship, Vec3 worldPos) {
        Matrix4dc worldToShip = ship.getTransform().getWorldToShip();
        Vector3d v = new Vector3d(worldPos.x, worldPos.y, worldPos.z);
        worldToShip.transformPosition(v);
        return new Vec3(v.x, v.y, v.z);
    }
}
