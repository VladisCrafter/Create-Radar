package com.happysg.radar.block.radar.link;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RadarTarget extends RadarLinkBehavior {

    public AABB getMultiblockBounds(LevelAccessor level, BlockPos pos) {
        VoxelShape shape = level.getBlockState(pos)
                .getShape(level, pos);
        if (shape.isEmpty())
            return new AABB(pos);
        return shape.bounds()
                .move(pos);
    }

}
