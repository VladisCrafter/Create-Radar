package com.happysg.radar.block.Test;

import com.happysg.radar.registry.ModBlockEntityTypes;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.item.context.BlockPlaceContext;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;


public class TestBlock extends DirectionalKineticBlock implements IBE<TestBlockEntity> {

    // Output (top) is optional; input (bottom) is always present.
    public static final BooleanProperty OUTPUT_ATTACHED = BooleanProperty.create("output_attached");

    public TestBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.UP)          // vertical default
                .setValue(OUTPUT_ATTACHED, Boolean.TRUE) // default attached (change if you want)
        );
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // Force vertical orientation. If you want it always vertical, this keeps it simple.
        return defaultBlockState()
                .setValue(FACING, Direction.UP)
                .setValue(OUTPUT_ATTACHED, Boolean.TRUE);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return Direction.Axis.Y;
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        if (face == Direction.DOWN) return true; // input always
        if (face == Direction.UP)   return state.getValue(OUTPUT_ATTACHED); // output conditional
        return false;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(OUTPUT_ATTACHED);
    }

    // ===== IBE =====
    @Override
    public Class<TestBlockEntity> getBlockEntityClass() {
        return TestBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends TestBlockEntity> getBlockEntityType() {
        // Replace with YOUR registered BE type getter:
        // return ModBlockEntityTypes.TEST_BLOCK_ENTITY.get();
        return ModBlockEntityTypes.TEST_BE.get();
    }

}
