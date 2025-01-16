package com.happysg.radar.compat.cbc;

import net.minecraft.world.phys.Vec3;
import rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlockEntity;
import rbasamoyai.createbigcannons.cannon_control.contraption.AbstractMountedCannonContraption;
import rbasamoyai.createbigcannons.cannon_control.contraption.PitchOrientedContraptionEntity;

//todo fix calculations
public class CannonTargeting {

    // Constants for drag and gravity
    private static final double DRAG = 0.99;


    // Calculate the optimal pitch to hit the target
    public static double calculatePitch(double power, double length, double gravity, Vec3 cannonPos, Vec3 targetPos) {
        double dX = targetPos.x - cannonPos.x;
        double dZ = targetPos.z - cannonPos.z;
        double dY = targetPos.y - cannonPos.y;
        double distance = Math.sqrt(dX * dX + dZ * dZ);

        double bestPitch = 0.0;
        double bestAccuracy = Double.MAX_VALUE;
        double pitch = -60.0;
        double pitchIncrement = 0.1; // Increased resolution

        while (pitch <= 60.0) {
            double airtimeUp = computeAirtimeUp(power, pitch, dY, length, gravity);
            double airtimeDown = computeAirtimeDown(power, pitch, dY, length, gravity);

            if (airtimeUp != -1) {
                double projection = computeProjection(power, pitch, airtimeUp, length);
                double accuracy = Math.abs(projection - distance);
                if (accuracy < bestAccuracy) {
                    bestAccuracy = accuracy;
                    bestPitch = pitch;
                }
            }

            if (airtimeDown != -1) {
                double projection = computeProjection(power, pitch, airtimeDown, length);
                double accuracy = Math.abs(projection - distance);
                if (accuracy < bestAccuracy) {
                    bestAccuracy = accuracy;
                    bestPitch = pitch;
                }
            }

            pitch += pitchIncrement;
        }

        // Secondary refinement around the best pitch
        double refinedBestPitch = bestPitch;
        bestAccuracy = Double.MAX_VALUE;
        pitch = bestPitch - pitchIncrement;
        while (pitch <= bestPitch + pitchIncrement) {
            double airtimeUp = computeAirtimeUp(power, pitch, dY, length, gravity);
            double airtimeDown = computeAirtimeDown(power, pitch, dY, length, gravity);

            if (airtimeUp != -1) {
                double projection = computeProjection(power, pitch, airtimeUp, length);
                double accuracy = Math.abs(projection - distance);
                if (accuracy < bestAccuracy) {
                    bestAccuracy = accuracy;
                    refinedBestPitch = pitch;
                }
            }

            if (airtimeDown != -1) {
                double projection = computeProjection(power, pitch, airtimeDown, length);
                double accuracy = Math.abs(projection - distance);
                if (accuracy < bestAccuracy) {
                    bestAccuracy = accuracy;
                    refinedBestPitch = pitch;
                }
            }

            pitch += pitchIncrement / 10; // Finer increment for refinement
        }

        return refinedBestPitch;
    }
    // Calculate airtime for upward motion
    private static double computeAirtimeUp(double power, double pitch, double dY, double length, double gravity) {
        double vertVelocity = power * Math.sin(Math.toRadians(pitch));
        double vertPosition = 0.0;
        dY -= length * Math.sin(Math.toRadians(pitch));  // Adjust target height based on barrel length

        if (dY < 0) return -1;

        double ticks = 0;
        while (true) {
            ticks++;
            vertPosition += vertVelocity;
            vertVelocity = vertVelocity * DRAG + gravity;
            if (vertPosition > dY) return ticks;
            if (vertVelocity < 0) break;  // Stop if velocity is downward before reaching the target
        }

        return -1;
    }

    // Calculate airtime for downward motion
    private static double computeAirtimeDown(double power, double pitch, double dY, double length, double gravity) {
        double vertVelocity = power * Math.sin(Math.toRadians(pitch));
        double vertPosition = 0.0;
        dY -= length * Math.sin(Math.toRadians(pitch));

        boolean passed = false;
        double ticks = 0;

        while (true) {
            ticks++;
            vertPosition += vertVelocity;
            vertVelocity = vertVelocity * DRAG + gravity;

            if(vertPosition > dY && vertVelocity < 0) passed = true;
            if(vertVelocity < 0 && vertPosition < dY && passed) return ticks;
            if(vertPosition < dY && vertVelocity < 0 ) break;

        }

        return -1;
    }

    // Calculate the horizontal projection
    private static double computeProjection(double power, double pitch, double airtime, double length) {
        double horizVelocity = power * Math.cos(Math.toRadians(pitch));
        double horizPosition = Math.sin(Math.abs(pitch))*length;

        for (int ticks = 1; ticks <= airtime; ticks++) {
            horizPosition += horizVelocity;
            horizVelocity *= DRAG;
        }

        return horizPosition;
    }

    public static double calculatePitch(CannonMountBlockEntity mount, Vec3 targetPos) {
        if (mount == null || targetPos == null) {
            return 0;
        }

        PitchOrientedContraptionEntity contraption = mount.getContraption();
        if (contraption == null) {
            return 0;
        }

        if (!(contraption.getContraption() instanceof AbstractMountedCannonContraption cannonContraption)) {
            return 0;
        }

        return calculatePitch(CannonUtil.getInitialVelocity(cannonContraption), CannonUtil.getFrontBarrelLength(cannonContraption), CannonUtil.getProjectileGravity(cannonContraption), mount.getBlockPos().above(2).getCenter(), targetPos);
    }

    public static double calculatePitch(CannonMountBlockEntity mount, Vec3 targetPos, int chargeCount) {
        if (chargeCount == 0) {
            return calculatePitch(mount, targetPos);
        }
        if (mount == null || targetPos == null) {
            return 0;
        }

        PitchOrientedContraptionEntity contraption = mount.getContraption();
        if (contraption == null) {
            return 0;
        }

        if (!(contraption.getContraption() instanceof AbstractMountedCannonContraption cannonContraption)) {
            return 0;
        }
        return calculatePitch(chargeCount * 2.0f, CannonUtil.getFrontBarrelLength(cannonContraption), CannonUtil.getProjectileGravity(cannonContraption), mount.getBlockPos().above(2).getCenter(), targetPos);
    }
}