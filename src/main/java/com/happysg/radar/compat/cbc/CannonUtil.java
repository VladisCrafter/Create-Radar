package com.happysg.radar.compat.cbc;

import com.happysg.radar.mixin.AbstractCannonAccessor;
import com.happysg.radar.mixin.AutoCannonAccessor;
import net.minecraft.core.BlockPos;
import rbasamoyai.createbigcannons.cannon_control.contraption.AbstractMountedCannonContraption;
import rbasamoyai.createbigcannons.cannon_control.contraption.MountedAutocannonContraption;
import rbasamoyai.createbigcannons.cannon_control.contraption.MountedBigCannonContraption;
import rbasamoyai.createbigcannons.cannons.autocannon.IAutocannonBlockEntity;
import rbasamoyai.createbigcannons.cannons.autocannon.material.AutocannonMaterialProperties;

public class CannonUtil {

    public static int getFrontBarrelLength(AbstractMountedCannonContraption cannon) {
        if (cannon == null)
            return 0;
        return ((AbstractCannonAccessor) cannon).getFrontBarrelCount();
    }

    public static float getInitialVelocity(AbstractMountedCannonContraption cannon) {
        if (cannon instanceof MountedBigCannonContraption bigCannon) {
            return getChargeCount(bigCannon) * 2.0f;
        } else if (cannon instanceof MountedAutocannonContraption auto) {
            return getACSpeed(auto);
        }
        return 0;
    }

    public static double getProjectileGravity(AbstractMountedCannonContraption cannon) {
        return isAutoCannon(cannon) ? -0.025 : -0.05;
    }

    public static double getProjectileDrag(AbstractMountedCannonContraption cannon) {
        return 0.99;
    }

    public static boolean isBigCannon(AbstractMountedCannonContraption cannon) {
        return cannon instanceof MountedBigCannonContraption;
    }

    public static boolean isAutoCannon(AbstractMountedCannonContraption cannon) {
        return cannon instanceof MountedAutocannonContraption;
    }

    public static int getChargeCount(MountedBigCannonContraption cannon) {
        if (cannon.isDropMortar())
            return 0;
        return 1;
    }

    public static float getACSpeed(MountedAutocannonContraption autocannon) {
        if (autocannon == null)
            return 0;

        if (((AutoCannonAccessor) autocannon).getMaterial() == null)
            return 0;

        AutocannonMaterialProperties properties = ((AutoCannonAccessor) autocannon).getMaterial().properties();
        float speed = properties.baseSpeed();
        BlockPos currentPos = autocannon.getStartPos().relative(autocannon.initialOrientation());
        int barrelTravelled = 0;

        while (autocannon.presentBlockEntities.get(currentPos) instanceof IAutocannonBlockEntity) {
            ++barrelTravelled;
            if (barrelTravelled <= properties.maxSpeedIncreases())
                speed += properties.speedIncreasePerBarrel();
            if (barrelTravelled > properties.maxBarrelLength()) {
                break;
            }
            currentPos = currentPos.relative(autocannon.initialOrientation());
        }
        return speed;
    }

}
