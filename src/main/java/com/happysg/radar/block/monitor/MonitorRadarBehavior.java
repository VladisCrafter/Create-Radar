package com.happysg.radar.block.monitor;

import com.happysg.radar.block.radar.link.RadarTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;

public class MonitorRadarBehavior extends RadarTarget {

    @Override
    public AABB getMultiblockBounds(LevelAccessor level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof MonitorBlockEntity monitor) {
            return monitor.getMultiblockBounds(level, pos);
        }
        return super.getMultiblockBounds(level, pos);
    }

}
