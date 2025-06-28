package com.happysg.radar.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import com.dsvv.cbcat.cannon.heavy_autocannon.contraption.MountedHeavyAutocannonContraption;
import rbasamoyai.createbigcannons.cannons.autocannon.material.AutocannonMaterial;

@Mixin(MountedHeavyAutocannonContraption.class)
public interface HeavyAutoCannonAccessor {
    @Accessor(value = "cannonMaterial", remap = false)
    AutocannonMaterial getMaterial();
}
