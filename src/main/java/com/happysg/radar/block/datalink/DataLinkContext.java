package com.happysg.radar.block.datalink;

import com.happysg.radar.block.monitor.MonitorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;

public class DataLinkContext {

    private Level level;
    private DataLinkBlockEntity blockEntity;

    public DataLinkContext(Level level, DataLinkBlockEntity blockEntity) {
        this.level = level;
        this.blockEntity = blockEntity;
    }

    public Level level() {
        return level;
    }

    public DataLinkBlockEntity blockEntity() {
        return blockEntity;
    }

    public BlockEntity getSourceBlockEntity() {
        return level.getBlockEntity(getPos1());
    }

    public BlockPos getPos1() {
        return blockEntity.getSourcePosition();
    }

    @Nullable
    public MonitorBlockEntity getMonitorBlockEntity() {
        BlockPos pos = level.getBlockEntity(getPos1()) instanceof MonitorBlockEntity ? getPos1() : getPos2();
        return level.getBlockEntity(pos) instanceof MonitorBlockEntity monitorBlockEntity ? monitorBlockEntity.getController() : null;
    }

    public BlockPos getPos2() {
        return blockEntity.getTargetPosition();
    }

    //public CompoundTag sourceConfig() {
     //   return blockEntity.getSourceConfig();
    //}

}
