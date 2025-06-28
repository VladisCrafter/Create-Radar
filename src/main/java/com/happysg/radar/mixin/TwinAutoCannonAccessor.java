package com.happysg.radar.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import com.dsvv.cbcat.cannon.twin_autocannon.contraption.MountedTwinAutocannonContraption;
import rbasamoyai.createbigcannons.cannons.autocannon.material.AutocannonMaterial;

@Mixin(MountedTwinAutocannonContraption.class)
public interface TwinAutoCannonAccessor {
    @Accessor(value = "cannonMaterial", remap = false)
    AutocannonMaterial getMaterial();
}
