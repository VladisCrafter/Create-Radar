package com.happysg.radar.block.datalink;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class DataLinkBehavior {

    public ResourceLocation id;

    protected abstract void transferData(@NotNull DataLinkContext context);


}
