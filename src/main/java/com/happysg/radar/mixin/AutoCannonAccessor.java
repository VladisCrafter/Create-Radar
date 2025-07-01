package com.happysg.radar.mixin;

import com.dsvv.cbcat.cannon.heavy_autocannon.contraption.MountedHeavyAutocannonContraption;
import com.dsvv.cbcat.cannon.twin_autocannon.contraption.MountedTwinAutocannonContraption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import rbasamoyai.createbigcannons.cannon_control.contraption.MountedAutocannonContraption;
import rbasamoyai.createbigcannons.cannons.autocannon.material.AutocannonMaterial;

@Mixin({
        MountedAutocannonContraption.class,
        MountedTwinAutocannonContraption.class,
        MountedHeavyAutocannonContraption.class
})
public interface AutoCannonAccessor {
    @Accessor(value = "cannonMaterial", remap = false)
    AutocannonMaterial getMaterial();
}
