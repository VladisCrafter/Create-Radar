package com.happysg.radar.mixin;

import com.dsvv.cbcat.cannon.heavy_autocannon.contraption.MountedHeavyAutocannonContraption;
import com.dsvv.cbcat.cannon.twin_autocannon.contraption.MountedTwinAutocannonContraption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import rbasamoyai.createbigcannons.cannon_control.contraption.MountedAutocannonContraption;
import rbasamoyai.createbigcannons.cannons.autocannon.material.AutocannonMaterial;

@Mixin(value = {
        MountedAutocannonContraption.class,
        MountedTwinAutocannonContraption.class,
        MountedHeavyAutocannonContraption.class
}, remap = false)
public interface AutoCannonAccessor {
    @Accessor("cannonMaterial")
    AutocannonMaterial getMaterial();
}
