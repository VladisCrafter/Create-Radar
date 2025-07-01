package com.happysg.radar.block.radar.plane;

import com.happysg.radar.registry.ModBlockEntityTypes;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public class PlaneRadarBlock extends HorizontalDirectionalBlock implements IBE<PlaneRadarBlockEntity> {

    public PlaneRadarBlock(Properties pProperties) {
        super(pProperties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction direction = context.isSecondaryUseActive() ? context.getHorizontalDirection().getOpposite() : context.getHorizontalDirection();
        return this.defaultBlockState()
                .setValue(FACING, direction);
    }


    @Override
    public Class<PlaneRadarBlockEntity> getBlockEntityClass() {
        return PlaneRadarBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PlaneRadarBlockEntity> getBlockEntityType() {
        return ModBlockEntityTypes.PLANE_RADAR.get();
    }
}
