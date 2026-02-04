package com.happysg.radar.block.controller.yaw;

import com.happysg.radar.block.behavior.networks.WeaponNetworkData;
import com.happysg.radar.compat.Mods;
import com.happysg.radar.compat.cbc.VS2CannonTargeting;
import com.happysg.radar.compat.vs2.PhysicsHandler;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import kotlin.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingBlockEntity;
import org.valkyrienskies.clockwork.platform.api.ContraptionController;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.assembly.ICopyableBlock;
import rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlock;
import rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlockEntity;
import rbasamoyai.createbigcannons.cannon_control.contraption.PitchOrientedContraptionEntity;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public class AutoYawControllerBlockEntity extends KineticBlockEntity  implements ICopyableBlock {

    private static final double TOLERANCE_DEG = 0.1;

    private double targetAngle; // degrees [0,360)

    private double prevTargetAngle = 0.0;
    private boolean hasPrevTarget = false;

    private double lastCbcYawWritten = 0.0;
    private boolean hasLastCbcYawWritten = false;

    private boolean isRunning;

    private final double MIN_MOVE_PER_TICK = 0.02;
    private static final double MAX_MOVE_PER_TICK = 2.0;
    private static final double SNAP_DISTANCE = 32.0;
    private static final double DEADBAND_DEG = 0.75;
    private BlockPos lastKnownPos = BlockPos.ZERO;

    private MountKind currentmount;

    @Nullable
    private Vec3 smoothedTarget = null;

    public AutoYawControllerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    // ===== Tick =====
    @Override
    public void tick() {
        super.tick();

        if (level == null || level.isClientSide())
            return;



        Mount mount = resolveMount();
        if (mount == null) return;

        if (mount.kind == MountKind.CBC) {
            if (Mods.CREATEBIGCANNONS.isLoaded()) {
                rotateCBC(mount.cbc);
                currentmount = MountKind.CBC;
            }
            return;
        }

        if (mount.kind == MountKind.PHYS && Mods.VS_CLOCKWORK.isLoaded()) {
            currentmount = MountKind.PHYS;
            rotatePhysBearing(mount.phys);
        }
        if (!level.isClientSide && level.getGameTime() % 40 == 0) {
            if (level instanceof ServerLevel serverLevel) {
                if (lastKnownPos.equals(worldPosition))
                    return;

                ResourceKey<Level> dim = serverLevel.dimension();
                WeaponNetworkData data = WeaponNetworkData.get(serverLevel);

                boolean updated = data.updateWeaponEndpointPosition(
                        dim,
                        lastKnownPos,
                        worldPosition
                );

                // only commit the new position if the network accepted it
                if (updated) {
                    lastKnownPos = worldPosition;
                    setChanged();
                }
            }
        }

    }

    // ===== Public API =====
    public void setTargetAngle(float targetAngle) {
        this.targetAngle = wrap360(targetAngle);
        this.isRunning = true;
        notifyUpdate();
        setChanged();
    }

    public double getTargetAngle() {
        return targetAngle;
    }

    public void setTarget(@Nullable Vec3 targetPos) {
        if (level == null || level.isClientSide())
            return;

        if (targetPos == null) {
            isRunning = false;
            smoothedTarget = null;
            notifyUpdate();
            setChanged();
            return;
        }
        BlockPos abovepos = worldPosition.above();
        Vec3 mountpos = Vec3.ZERO;
        if(level.getBlockEntity(abovepos) instanceof CannonMountBlockEntity mountBlock) {
            if (PhysicsHandler.isBlockInShipyard(level, this.getBlockPos())) {
                List<List<Double>> angles = VS2CannonTargeting.calculatePitchAndYawVS2(mountBlock, targetPos, (ServerLevel) level);
                if (angles == null || angles.isEmpty() || angles.get(0).isEmpty())
                    return;

                // yaw
                this.targetAngle =  wrap360(angles.get(0).get(1).floatValue());

                isRunning = true;
                notifyUpdate();
                setChanged();
                return;
            }
        }

        isRunning = true;

        Vec3 cannonCenter = worldPosition.above(3).getCenter();
        // i'm computing yaw in ship-space when we're on a VS2 ship
        double angle = computeYawToTargetDeg(cannonCenter, targetPos);
        double newAngle = wrap360(angle)+180;

        // store previous setpoint for 1-tick lag compensation
        prevTargetAngle = this.targetAngle;
        hasPrevTarget = true;

        this.targetAngle = newAngle;

        notifyUpdate();
        setChanged();
    }

    /** Works for either mount type */
    public boolean atTargetYaw() {
        if (level == null) return false;

        Mount mount = resolveMount();
        if (mount == null) return false;

        if (mount.kind == MountKind.CBC && Mods.CREATEBIGCANNONS.isLoaded()) {
            PitchOrientedContraptionEntity contraption = mount.cbc.getContraption();
            if (contraption == null) return false;

            double desired = hasPrevTarget ? wrap360(prevTargetAngle) : wrap360(targetAngle);

            // Prefer what we actually commanded (contraption.yaw can lag behind)
            double current = hasLastCbcYawWritten ? wrap360(lastCbcYawWritten) : wrap360(contraption.yaw);

            return Math.abs(shortestDelta(current, desired)) < TOLERANCE_DEG;
        }

        if (mount.kind == MountKind.PHYS && Mods.VS_CLOCKWORK.isLoaded()) {
            Double actualRad = mount.phys.getActualAngle();
            if (actualRad == null) return false;
            double currentDeg = wrap360(Math.toDegrees(actualRad));
            double desiredDeg = wrap360(360.0 - targetAngle);
            return Math.abs(shortestDelta(currentDeg, desiredDeg)) < Math.max(TOLERANCE_DEG, DEADBAND_DEG);
        }

        return false;
    }

    // ===== Behavior: CBC =====
    private void rotateCBC(CannonMountBlockEntity mount) {
        if (!isRunning) return;

        PitchOrientedContraptionEntity contraption = mount.getContraption();
        if (contraption == null) return;

        double currentYaw = wrap360(contraption.yaw);
        double desiredYaw = wrap360(targetAngle);

        double yawDiff = shortestDelta(currentYaw, desiredYaw);
        if (Math.abs(yawDiff) <= TOLERANCE_DEG) {
            mount.setYaw((float) desiredYaw);

            lastCbcYawWritten = wrap360(desiredYaw);
            hasLastCbcYawWritten = true;

            mount.notifyUpdate();

            // isRunning = false;
            return;
        }

        double speedFactor = Math.abs(getSpeed()) / 32.0;
        if (speedFactor <= 0) return;

        double nextYaw;
        if (Math.abs(yawDiff) > speedFactor) {
            nextYaw = wrap360(currentYaw + Math.signum(yawDiff) * speedFactor);
        } else {
            nextYaw = desiredYaw;
        }

        mount.setYaw((float) nextYaw);

        lastCbcYawWritten = wrap360(nextYaw);
        hasLastCbcYawWritten = true;

        mount.notifyUpdate();
    }

    // ===== Behavior: PhysBearing =====
    private void rotatePhysBearing(PhysBearingBlockEntity mount) {
        ScrollOptionBehaviour<ContraptionController.LockedMode> mode = mount.getMovementMode();
        if (mode != null && mode.getValue() != ContraptionController.LockedMode.FOLLOW_ANGLE.ordinal()) {
            mode.setValue(ContraptionController.LockedMode.FOLLOW_ANGLE.ordinal());
        }
        if (speed <= 0) return;
        if (!isRunning) return;

        Double actualRad = mount.getActualAngle();
        if (actualRad == null) return;

        double currentDeg = wrap360(Math.toDegrees(actualRad));
        double desiredDeg = wrap360(360.0 - targetAngle);

        double diff = shortestDelta(currentDeg, desiredDeg);
        double chosen = wrap360(currentDeg + diff);

        float rpm = (float) Math.abs(getSpeed());

        if (rpm >= 256.0f) {
            mount.setAngle((float) desiredDeg);
            mount.notifyUpdate();
            return;
        }

        mount.setAngle((float) chosen);
        mount.notifyUpdate();
    }

    // ===== Target acquisition =====
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
            smoothedTarget = null;
            return;
        }

        Vec3 cannonCenterWorld = worldPosition.above(3).getCenter();
        Vec3 desiredWorld = player.getEyePosition();

        if (smoothedTarget == null) {
            smoothedTarget = desiredWorld;
        } else {
            Vec3 delta = desiredWorld.subtract(smoothedTarget);
            double dist = delta.length();

            if (dist > SNAP_DISTANCE) {
                smoothedTarget = desiredWorld;
            } else if (dist > 1e-6) {
                double range = smoothedTarget.distanceTo(cannonCenterWorld);
                double step = getStep(range, dist);
                smoothedTarget = smoothedTarget.add(delta.scale(step / dist));
            }
        }

        Vec3 tWorld = smoothedTarget;

        // i'm computing yaw in ship-space when we're on a VS2 ship
        double angle = wrap360(computeYawToTargetDeg(cannonCenterWorld, tWorld));

        if (Math.abs(shortestDelta(targetAngle, angle)) < DEADBAND_DEG) {
            isRunning = true;
            return;
        }

        if (currentmount == MountKind.CBC) {
            targetAngle = angle + 180.0;
        } else {
            targetAngle = angle;
        }

        isRunning = true;
    }

    private double getStep(double range, double dist) {
        double rpm = Math.abs(getSpeed());
        double r = Math.min(256.0, Math.max(0.0, rpm));
        double gamma = 1.6;
        double x = r / 256.0;
        double effectiveRpm = 1.0 + (r - 1.0) * Math.pow(x, gamma);

        double degPerTick = effectiveRpm * 0.3;
        double radPerTick = degPerTick * (Math.PI / 180.0);

        double maxStep = range * radPerTick;
        maxStep = Math.max(MIN_MOVE_PER_TICK, Math.min(MAX_MOVE_PER_TICK, maxStep));

        return Math.min(dist, maxStep);
    }

    // ===== VS2 ship-space yaw helper =====
    private double computeYawToTargetDeg(Vec3 cannonCenterWorld, Vec3 targetWorld) {
        Ship ship = getShipIfPresent();

        Vec3 cannonCenter = cannonCenterWorld;
        Vec3 target = targetWorld;

        // i'm converting both points into ship-space so ship rotation is automatically accounted for
        if (Mods.VALKYRIENSKIES.isLoaded() && ship != null) {
            cannonCenter = toShipSpace(ship, cannonCenterWorld);
            target = toShipSpace(ship, targetWorld);
        }

        double dx = target.x - cannonCenter.x;
        double dz = target.z - cannonCenter.z;

        return Math.toDegrees(Math.atan2(dz, dx)) + 90.0;
    }

    private Vec3 toShipSpace(Ship ship, Vec3 worldPos) {
        Vector3d tmp = new Vector3d(worldPos.x, worldPos.y, worldPos.z);
        ship.getTransform().getWorldToShip().transformPosition(tmp);
        return new Vec3(tmp.x, tmp.y, tmp.z);
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        targetAngle = wrap360(compound.getDouble("TargetAngle"));
        isRunning = compound.getBoolean("IsRunning");
        if (compound.contains("LastKnownPos", Tag.TAG_LONG)) {
            lastKnownPos = BlockPos.of(compound.getLong("LastKnownPos"));
        } else {
            lastKnownPos = worldPosition;
        }
    }

    @Override
    protected void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.putDouble("TargetAngle", wrap360(targetAngle));
        compound.putBoolean("IsRunning", isRunning);
        compound.putLong("LastKnownPos", lastKnownPos.asLong());
    }

    @Override
    protected void copySequenceContextFrom(KineticBlockEntity sourceBE) {
        // i'm keeping this empty like the original controller
    }


    @Override
    public @org.jetbrains.annotations.Nullable CompoundTag onCopy(@NotNull ServerLevel serverLevel, @NotNull BlockPos blockPos, @NotNull BlockState blockState, @org.jetbrains.annotations.Nullable BlockEntity blockEntity, @NotNull List<? extends ServerShip> list, @NotNull Map<Long, ? extends Vector3d> map) {
        return null;
    }

    @Override
    public @org.jetbrains.annotations.Nullable CompoundTag onPaste(@NotNull ServerLevel serverLevel, @NotNull BlockPos blockPos, @NotNull BlockState blockState, @NotNull Map<Long, Long> map, @NotNull Map<Long, ? extends Pair<? extends Vector3d, ? extends Vector3d>> map1, @org.jetbrains.annotations.Nullable CompoundTag compoundTag) {
        return null;
    }

    // ===== Mount resolution =====
    private enum MountKind { CBC, PHYS, RADAR }

    private class Mount {
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
        if (level == null) return null;

        BlockEntity above = level.getBlockEntity(worldPosition.above());

        if (Mods.CREATEBIGCANNONS.isLoaded() && above instanceof CannonMountBlockEntity cbc)
            return new Mount(cbc);

        if (Mods.VS_CLOCKWORK.isLoaded() && above instanceof PhysBearingBlockEntity phys)
            return new Mount(phys);

        return null;
    }

    @Nullable
    private Ship getShipIfPresent() {
        if (level == null) return null;

        if (!(Mods.VALKYRIENSKIES.isLoaded()))
            return null;

        return VSGameUtilsKt.getShipManagingPos(level, worldPosition);
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
}
