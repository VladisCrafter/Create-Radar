package com.happysg.radar.compat.cbc;

import net.minecraft.world.phys.Vec3;

public class CannonLeadSolver {
    public static class BallisticSimResult {
        public final int ticks;
        public final Vec3 endPos;
        public final Vec3 endVel;

        public BallisticSimResult(int ticks, Vec3 endPos, Vec3 endVel) {
            this.ticks = ticks;
            this.endPos = endPos;
            this.endVel = endVel;
        }
    }

    public static BallisticSimResult simulateBigCannonFlightTicks(
            Vec3 muzzlePos,
            Vec3 shooterVel,
            Vec3 dirUnit,
            double muzzleSpeed,
            double gravityPerTick,
            double drag,
            double targetHorizontalDist,
            int maxTicks
    ) {
        Vec3 pos = muzzlePos;
        Vec3 vel = shooterVel.add(dirUnit.scale(muzzleSpeed));

        double targetDistSqr = targetHorizontalDist * targetHorizontalDist;

        for (int tick = 0; tick < maxTicks; tick++) {
            // horizontal distance from muzzle
            double dx = pos.x - muzzlePos.x;
            double dz = pos.z - muzzlePos.z;
            double horizSqr = dx * dx + dz * dz;

            if (horizSqr >= targetDistSqr) {
                return new BallisticSimResult(tick, pos, vel);
            }


            vel = vel.add(0.0, gravityPerTick, 0.0);

            vel = vel.scale(1.0 - drag);

            // integrate position (1 tick step)
            pos = pos.add(vel);
        }

        return new BallisticSimResult(maxTicks, pos, vel);
    }

}
