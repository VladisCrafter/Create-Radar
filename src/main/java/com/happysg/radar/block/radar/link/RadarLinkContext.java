package com.happysg.radar.block.radar.link;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class RadarLinkContext {

    private Level level;
    private RadarLinkBlockEntity blockEntity;

    public Object flapDisplayContext;

    public RadarLinkContext(Level level, RadarLinkBlockEntity blockEntity) {
        this.level = level;
        this.blockEntity = blockEntity;
    }

    public Level level() {
        return level;
    }

    public RadarLinkBlockEntity blockEntity() {
        return blockEntity;
    }

    public BlockEntity getSourceBlockEntity() {
        return level.getBlockEntity(getSourcePos());
    }

    public BlockPos getSourcePos() {
        return blockEntity.getSourcePosition();
    }

    public BlockEntity getTargetBlockEntity() {
        return level.getBlockEntity(getTargetPos());
    }

    public BlockPos getTargetPos() {
        return blockEntity.getTargetPosition();
    }

    public CompoundTag sourceConfig() {
        return blockEntity.getSourceConfig();
    }

}
