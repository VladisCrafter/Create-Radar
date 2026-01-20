package com.happysg.radar.block.Test;

import com.happysg.radar.compat.Mods;
import com.happysg.radar.compat.vs2.PhysicsHandler;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4dc;
import org.joml.Vector3d;
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingBlockEntity;
import org.valkyrienskies.clockwork.platform.api.ContraptionController;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import javax.annotation.Nullable;

public class TestBlockEntity extends KineticBlockEntity {

    private static final double TOLERANCE = 0.10;
    private static final double DEADBAND_DEG = 0.25;

    private static final double SNAP_DISTANCE = 32.0;
    private static final double MIN_MOVE_PER_TICK = 0.02;
    private static final double MAX_MOVE_PER_TICK = 2.0;
    private double lastCommandedDeg = Double.NaN;
    // Unified “feel” curve (RPM -> deg/tick)
    private static final double MIN_DEG_PER_TICK = 0.30; // ~1 RPM
    private static final double MAX_DEG_PER_TICK = 18.0; // 256 RPM
    private static final double CURVE_GAMMA = 1.65;



    private static final int MOUNT_RECHECK_EVERY_TICKS = 20;

    private double targetAngle; // degrees [0,360)
    private boolean isRunning;

    @Nullable
    private Vec3 smoothedTarget;

    @Nullable
    private PhysBearingBlockEntity mount;

    private boolean mountDirty = true;
    private int mountRecheckCooldown = 0;

    public TestBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void onConnectionsChanged() {
        mountDirty = true;
    }

    @Override
    public void tick() {
        super.tick();

        if (level == null || level.isClientSide())
            return;

        if (!Mods.VS_CLOCKWORK.isLoaded())
            return;



        refreshMountCacheIfNeeded();
        if (mount == null)
            return;

        updateTargetFromNearestPlayer();
        tryRotateBearing(mount);
    }

    // ===== Public API =====

    public void setTarget(@Nullable Vec3 targetPos) {
        if (level == null || level.isClientSide())
            return;
        if (!Mods.VS_CLOCKWORK.isLoaded())
            return;

        if (targetPos == null) {
            isRunning = false;
            lastCommandedDeg = Double.NaN;

            smoothedTarget = null;
            notifyUpdate();
            setChanged();
            return;
        }

        Direction facing = getFacing();
        Vec3 pivot = getPivotAhead();

        Ship ship = getShipIfPresent();
        Vec3 target = (ship != null) ? toShipSpace(ship, targetPos) : targetPos;

        targetAngle = rollAroundFacingDeg(pivot, target, facing);

        isRunning = true;

        notifyUpdate();
        setChanged();
    }

    public void setTargetAngle(float angleDeg) {
        targetAngle = wrap360(angleDeg);
        isRunning = true;
        notifyUpdate();
        setChanged();
    }

    public double getTargetAngle() {
        return targetAngle;
    }

    public boolean atTarget() {
        if (level == null || !Mods.VS_CLOCKWORK.isLoaded())
            return false;

        refreshMountCacheIfNeeded();
        if (mount == null)
            return false;

        Double actualRad = mount.getActualAngle();
        if (actualRad == null)
            return false;

        double currentDeg = wrap360(Math.toDegrees(actualRad));
        double desiredDeg = wrap360(targetAngle);

        return Math.abs(shortestDelta(currentDeg, desiredDeg)) < Math.max(TOLERANCE, DEADBAND_DEG);
    }

    // ===== Bearing Control =====

    private void tryRotateBearing(PhysBearingBlockEntity mount) {
        ScrollOptionBehaviour<ContraptionController.LockedMode> mode = mount.getMovementMode();
        if (mode != null && mode.getValue() != ContraptionController.LockedMode.FOLLOW_ANGLE.ordinal()) {
            mode.setValue(ContraptionController.LockedMode.FOLLOW_ANGLE.ordinal());
        }

        double rpm = Math.abs(getSpeed());
        if (rpm <= 0.0)
            return;

        if (!isRunning)
            return;

        Double actualRad = mount.getActualAngle();
        if (actualRad == null)
            return;

        double currentDeg = wrap360(Math.toDegrees(actualRad));
        double desiredDeg = wrap360(targetAngle);

        if (rpm >= 256.0) {
            mount.setAngle((float) desiredDeg);
            mount.notifyUpdate();
            return;
        }

        // initialize command latch on first run
        if (Double.isNaN(lastCommandedDeg)) {
            lastCommandedDeg = currentDeg;
        }

// Make desired continuous near the *last commanded* angle (not the lagging actual angle)
        double desiredContinuous = unwrapNear(lastCommandedDeg, desiredDeg);

// If max rpm -> snap (and update latch)
        if (rpm >= 256.0) {
            lastCommandedDeg = desiredContinuous;
            mount.setAngle((float) wrap360(desiredContinuous));
            mount.notifyUpdate();
            return;
        }

// Normal: command the continuous target (wrapped for API), update latch
        lastCommandedDeg = desiredContinuous;
        mount.setAngle((float) wrap360(desiredContinuous));
        mount.notifyUpdate();

    }

    // ===== Target Acquisition =====

    private void updateTargetFromNearestPlayer() {
        Player player = level.getNearestPlayer(
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5,
                -1,
                false
        );

        if (player == null) {
            isRunning = false;
            lastCommandedDeg = Double.NaN;
            smoothedTarget = null;
            return;
        }

        Direction facing = getFacing();

        // IMPORTANT:
        // Pivot and bearing are in SHIP SPACE when on a ship. BlockPos centers are ship-space.
        Vec3 pivotShip = getPivotAhead();

        // Player eye is WORLD SPACE always.
        Vec3 desiredWorld = player.getEyePosition();

        // If on a ship, convert player position to ship-space so math is consistent.
        Ship ship = getShipIfPresent();
        Vec3 desired = (ship != null) ? toShipSpace(ship, desiredWorld) : desiredWorld;
        Vec3 pivot = (ship != null) ? pivotShip : pivotShip; // pivotShip is already correct in ship space

        // --- smoothing preserved ---
        if (smoothedTarget == null) {
            smoothedTarget = desired;
        } else {
            Vec3 delta = desired.subtract(smoothedTarget);
            double dist = delta.length();

            if (dist > SNAP_DISTANCE) {
                smoothedTarget = desired;
            } else if (dist > 1e-6) {
                double radius = diskRadius(pivot, smoothedTarget, facing);
                double step = stepTowardTarget(radius, dist, Math.abs(getSpeed()));
                smoothedTarget = smoothedTarget.add(delta.scale(step / dist));
            }
        }

        double angle = rollAroundFacingDeg(pivot, smoothedTarget, facing);

        if (Math.abs(shortestDelta(targetAngle, angle)) < DEADBAND_DEG) {
            isRunning = true;
            return;
        }

        targetAngle = approachWrapped(targetAngle, angle);
        isRunning = true;
    }


    // ===== Mount Resolution =====

    private void refreshMountCacheIfNeeded() {
        if (level == null)
            return;

        if (!mountDirty && mountRecheckCooldown-- > 0)
            return;

        mountRecheckCooldown = MOUNT_RECHECK_EVERY_TICKS;
        mountDirty = false;

        mount = null;

        BlockPos front = worldPosition.relative(getFacing());
        BlockEntity be = level.getBlockEntity(front);
        if (be instanceof PhysBearingBlockEntity phys) {
            mount = phys;
        }
    }

    // ===== Geometry =====

    private Direction getFacing() {
        return getBlockState().getValue(HorizontalDirectionalBlock.FACING);
    }

    /** Pivot is exactly one block ahead of the controller (in facing). */
    private Vec3 getPivotAhead() {
        return worldPosition.relative(getFacing()).getCenter();
    }

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

    /**
     * Roll around the facing axis (propeller-like).
     * 0° = blade straight up.
     */
    private static double rollAroundFacingDeg(Vec3 pivot, Vec3 target, Direction facing) {
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 fwd = forwardHoriz(facing);
        Vec3 right = rightHoriz(facing);

        Vec3 v = target.subtract(pivot);

        // remove along-axis (forward/back) so it only reacts to up/down + left/right
        Vec3 vDisk = v.subtract(fwd.scale(v.dot(fwd)));

        double r = vDisk.dot(right);
        double u = vDisk.dot(up);

        if (Math.abs(r) < 1e-10 && Math.abs(u) < 1e-10)
            return 0.0;

        // atan2(u, r): right=0°, up=90°. Shift so up becomes 0°.
        return wrap360(Math.toDegrees(Math.atan2(u, r)) );
    }

    /** Target distance projected onto the disk plane (perpendicular to facing). */
    private static double diskRadius(Vec3 pivot, Vec3 target, Direction facing) {
        Vec3 fwd = forwardHoriz(facing);
        Vec3 v = target.subtract(pivot);
        Vec3 vDisk = v.subtract(fwd.scale(v.dot(fwd)));
        return Math.sqrt(vDisk.lengthSqr());
    }

    // ===== Stepping =====

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

    // ===== NBT =====

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        targetAngle = wrap360(compound.getDouble("TargetAngle"));
        isRunning = compound.getBoolean("IsRunning");
    }

    @Override
    protected void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.putDouble("TargetAngle", wrap360(targetAngle));
        compound.putBoolean("IsRunning", isRunning);
    }

    // ===== Angle helpers =====

    private static double wrap360(double a) {
        a %= 360.0;
        if (a < 0) a += 360.0;
        return a;
    }

    private static double shortestDelta(double from, double to) {
        return ((to - from + 540.0) % 360.0) - 180.0;
    }
    private static double unwrapNear(double lastContinuous, double newWrapped) {
        // choose the equivalent of newWrapped that's closest to lastContinuous
        double lastWrapped = wrap360(lastContinuous);
        return lastContinuous + shortestDelta(lastWrapped, newWrapped);
    }
    @Nullable
    private Ship getShipIfPresent() {
        if (level == null) return null;
        // Works on server & client levels; returns null if not on a ship.
        return VSGameUtilsKt.getShipManagingPos(level, worldPosition);
    }

    private static Vec3 toShipSpace(Ship ship, Vec3 worldPos) {
        // Convert a world-space Vec3 into ship-space coordinates
        Matrix4dc worldToShip = ship.getTransform().getWorldToShip();
        Vector3d v = new Vector3d(worldPos.x, worldPos.y, worldPos.z);
        worldToShip.transformPosition(v);
        return new Vec3(v.x, v.y, v.z);
    }


}
