package com.happysg.radar.compat.cbc;

import com.happysg.radar.compat.vs2.PhysicsHandler;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlockEntity;
import rbasamoyai.createbigcannons.cannon_control.contraption.AbstractMountedCannonContraption;
import rbasamoyai.createbigcannons.munitions.config.components.BallisticPropertiesComponent;

import java.util.Comparator;
import java.util.List;

public class CannonLead {
    private static final double VEL_EPS = 1.0;
    private static final double VEL_EPS_SQR = VEL_EPS * VEL_EPS;
    private static final Logger LOGGER = LogUtils.getLogger();

    public static class LeadSolution {
        public final Vec3 aimPoint;      // i’m aiming at this world point (predicted intercept)
        public final double pitchDeg;    // pitch solution for the aimPoint
        public final double yawRad;      // yaw solution for the aimPoint
        public final int flightTicks;    // predicted time-of-flight in ticks

        public LeadSolution(Vec3 aimPoint, double pitchDeg, double yawRad, int flightTicks) {
            this.aimPoint = aimPoint;
            this.pitchDeg = pitchDeg;
            this.yawRad = yawRad;
            this.flightTicks = flightTicks;
        }
    }

    public static class SimResult {
        public final int ticks;
        public final Vec3 pos;
        public final Vec3 vel;

        public SimResult(int ticks, Vec3 pos, Vec3 vel) {
            this.ticks = ticks;
            this.pos = pos;
            this.vel = vel;
        }
    }

    // -------------------------
    // tick-based kinematics
    // -------------------------

    public static Vec3 predictPositionTicks(Vec3 pos0, Vec3 velPerTick, Vec3 accelPerTick2, double tTicks) {
        // i’m using x = x0 + v*t + 0.5*a*t^2
        return pos0
                .add(velPerTick.scale(tTicks))
                .add(accelPerTick2.scale(0.5 * tTicks * tTicks));
    }

    public static Vec3 predictVelocityTicks(Vec3 vel0PerTick, Vec3 accelPerTick2, double tTicks) {
        // i’m using v = v0 + a*t
        return vel0PerTick.add(accelPerTick2.scale(tTicks));
    }

    // -------------------------
    // aiming helpers
    // -------------------------

    public static Vec3 directionFromYawPitch(double yawRad, double pitchRad) {
        // i’m matching the usual MC-style yaw in the XZ plane and pitch upward
        return new Vec3(
                Math.cos(pitchRad) * Math.cos(yawRad),
                Math.sin(pitchRad),
                Math.cos(pitchRad) * Math.sin(yawRad)
        ).normalize();
    }

    // -------------------------
    // projectile sim (tick units)
    // -------------------------

    /**
     * i’m simming the projectile forward until it reaches the requested horizontal distance.
     * gravityPerTick is blocks/tick^2 (CBC ballistic gravity is typically already tick tuned)
     * drag is applied as a per-tick damping (big cannons); set applyDrag=false for no-drag weapons
     */
    public static SimResult simulateFlightTicks(
            Vec3 muzzlePos,
            Vec3 shooterVelPerTickAtFire,
            Vec3 dirUnit,
            double muzzleSpeedPerTick,
            double gravityPerTick,
            double drag,
            double targetHorizontalDist,
            int maxTicks,
            boolean applyDrag
    ) {
        Vec3 pos = muzzlePos;
        Vec3 vel = shooterVelPerTickAtFire.add(dirUnit.scale(muzzleSpeedPerTick));

        double targetDistSqr = targetHorizontalDist * targetHorizontalDist;

        for (int tick = 0; tick <= maxTicks; tick++) {
            double dx = pos.x - muzzlePos.x;
            double dz = pos.z - muzzlePos.z;
            if (dx * dx + dz * dz >= targetDistSqr) {
                return new SimResult(tick, pos, vel);
            }

            // i’m applying gravity per tick
            vel = vel.add(0.0, gravityPerTick, 0.0);

            // i’m applying drag as a per-tick damping; if this is slightly off from CBC, this is the knob to swap
            if (applyDrag && drag != 0.0) {
                vel = vel.scale(1.0 - drag);
            }

            // i’m integrating one tick forward
            pos = pos.add(vel);
        }

        return new SimResult(maxTicks, pos, vel);
    }

    // -------------------------
    // main solver
    // -------------------------

    /**
     * requires:
     * - shooterVelPerTick / shooterAccelPerTick2 in world-space
     * - targetVelPerTick / targetAccelPerTick2 in world-space
     *
     * notes:
     * - shooter acceleration is only relevant up to fire time (fireDelayTicks)
     * - target acceleration is relevant until impact (fireDelay + flight)
     */
    public static LeadSolution solveLeadPerTickWithAcceleration(
            CannonMountBlockEntity mount,
            AbstractMountedCannonContraption cannon,
            ServerLevel level,

            Vec3 shooterVelPerTick,
            Vec3 shooterAccelPerTick2,

            Vec3 targetPosNow,
            Vec3 targetVelPerTick,
            Vec3 targetAccelPerTick2,

            int fireDelayTicks
    ) {
        if (mount == null || cannon == null || level == null) return null;
        if (targetPosNow == null || targetVelPerTick == null || targetAccelPerTick2 == null) return null;
        if (shooterVelPerTick == null || shooterAccelPerTick2 == null) return null;
        boolean targetMoving = targetVelPerTick.lengthSqr() >= VEL_EPS_SQR;
        boolean shooterMoving = shooterVelPerTick.lengthSqr() >= VEL_EPS_SQR;

        if (!targetMoving) {
            Vec3 aimPoint = targetPosNow;

            List<Double> pitches = CannonTargeting.calculatePitch(mount, aimPoint, level);
            if (pitches == null || pitches.isEmpty()) return null;

            double pitchDeg = pitches.stream()
                    .min(Comparator.comparingDouble(Math::abs))
                    .orElse(pitches.get(0));

            Vec3 originNow = PhysicsHandler.getWorldVec(level, mount.getBlockPos().above(2).getCenter());
            Vec3 to = aimPoint.subtract(originNow);
            double yawRad = Math.atan2(to.z, to.x);

            return new LeadSolution(aimPoint, pitchDeg, yawRad, 0);
        }
        if (!shooterMoving) {
            shooterVelPerTick = Vec3.ZERO;
            shooterAccelPerTick2 = Vec3.ZERO;
        }
        // i’m assuming your CannonUtil returns "blocks/tick" tuned values (CBC-style)
        double muzzleSpeedPerTick = CannonUtil.getInitialVelocity(cannon, level);
        if (muzzleSpeedPerTick <= 0.0) return null;

        // i’m matching your existing pitch solver origin so lead + pitch agree
        Vec3 originNow = PhysicsHandler.getWorldVec(level, mount.getBlockPos().above(2).getCenter());
        int barrelLength = CannonUtil.getBarrelLength(cannon);

        double gravityPerTick = CannonUtil.getProjectileGravity(cannon, level);
        double drag = CannonUtil.getProjectileDrag(cannon, level);

        boolean noDrag =
                CannonUtil.isAutoCannon(cannon) ||
                        CannonUtil.isRotaryCannon(cannon) ||
                        CannonUtil.isMediumCannon(cannon) ||
                        CannonUtil.isTwinAutocannon(cannon) ||
                        CannonUtil.isHeavyAutocannon(cannon);

        // i’m predicting shooter state at the moment the projectile spawns
        Vec3 shooterPosAtFire = predictPositionTicks(originNow, shooterVelPerTick, shooterAccelPerTick2, fireDelayTicks);
        Vec3 shooterVelAtFire = predictVelocityTicks(shooterVelPerTick, shooterAccelPerTick2, fireDelayTicks);

        // initial guess: straight-line ticks (horizontal only)
        double dx0 = targetPosNow.x - shooterPosAtFire.x;
        double dz0 = targetPosNow.z - shooterPosAtFire.z;
        double horiz0 = Math.sqrt(dx0 * dx0 + dz0 * dz0);
        double tGuessTicks = horiz0 / Math.max(1.0e-6, muzzleSpeedPerTick);

        Vec3 aimPoint = targetPosNow;
        double chosenPitchDeg = 0.0;
        double chosenYawRad = 0.0;
        int flightTicks = (int) Math.round(tGuessTicks);

        // i’m iterating to converge predicted impact time and ballistic time-of-flight
        for (int iter = 0; iter < 8; iter++) {
            double tImpactTicks = fireDelayTicks + tGuessTicks;

            // i’m predicting the target at impact using constant acceleration
            aimPoint = predictPositionTicks(targetPosNow, targetVelPerTick, targetAccelPerTick2, tImpactTicks);

            // i’m yawing from the shooter position at fire time (where the projectile will actually spawn)
            Vec3 toPred = aimPoint.subtract(shooterPosAtFire);
            chosenYawRad = Math.atan2(toPred.z, toPred.x);

            // i’m using your pitch solver to find valid pitch angles for this predicted point
            List<Double> pitches = CannonTargeting.calculatePitch(mount, aimPoint, level);
            if (pitches == null || pitches.isEmpty()) return null;

            // i’m taking the smallest-absolute pitch as the "direct fire" solution
            chosenPitchDeg = pitches.stream()
                    .min(Comparator.comparingDouble(Math::abs))
                    .orElse(pitches.get(0));

            double pitchRad = Math.toRadians(chosenPitchDeg);
            Vec3 dir = directionFromYawPitch(chosenYawRad, pitchRad);

            // i’m offsetting muzzle forward along the barrel direction
            Vec3 muzzlePosAtFire = shooterPosAtFire.add(dir.scale(barrelLength));

            // i’m measuring horizontal distance from muzzle to predicted point (this is what the sim uses to stop)
            double dx = aimPoint.x - muzzlePosAtFire.x;
            double dz = aimPoint.z - muzzlePosAtFire.z;
            double horiz = Math.sqrt(dx * dx + dz * dz);

            SimResult sim = simulateFlightTicks(
                    muzzlePosAtFire,
                    shooterVelAtFire,
                    dir,
                    muzzleSpeedPerTick,
                    gravityPerTick,
                    drag,
                    horiz,
                    600,
                    !noDrag
            );

            int newFlightTicks = sim.ticks;

            // i’m stopping when flight time stabilizes
            if (Math.abs(newFlightTicks - tGuessTicks) < 0.5) {
                flightTicks = newFlightTicks;
                tGuessTicks = newFlightTicks;
                break;
            }

            flightTicks = newFlightTicks;
            tGuessTicks = newFlightTicks;
        }

        return new LeadSolution(aimPoint, chosenPitchDeg, chosenYawRad, flightTicks);
    }

    // -------------------------
    // debug helpers
    // -------------------------

    public static void logLeadByBlocks(Vec3 targetPosNow, Vec3 aimPoint, Vec3 targetVelPerTick) {
        if (targetPosNow == null || aimPoint == null) return;

        Vec3 leadVec = aimPoint.subtract(targetPosNow);
        double totalLead = leadVec.length();

        double directionalLead = 0.0;
        if (targetVelPerTick != null && targetVelPerTick.lengthSqr() > 1.0e-9) {
            directionalLead = leadVec.dot(targetVelPerTick.normalize());
        }

        LOGGER.warn("Lead debug → totalLead={} directionalLead={} leadVec={} targetVelPerTick={}",
                totalLead, directionalLead, leadVec, targetVelPerTick);
    }
}
