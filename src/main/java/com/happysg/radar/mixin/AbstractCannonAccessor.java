package com.happysg.radar.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import rbasamoyai.createbigcannons.cannon_control.contraption.AbstractMountedCannonContraption;

@Mixin(AbstractMountedCannonContraption.class)
public interface AbstractCannonAccessor {

    @Accessor("frontExtensionLength")
    int getFrontBarrelCount();

}
