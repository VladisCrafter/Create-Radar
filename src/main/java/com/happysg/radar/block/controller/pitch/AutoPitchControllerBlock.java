package com.happysg.radar.block.controller.pitch;

import com.happysg.radar.registry.ModBlockEntityTypes;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class AutoPitchControllerBlock extends HorizontalKineticBlock implements IBE<AutoPitchControllerBlockEntity> {

    public AutoPitchControllerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(HORIZONTAL_FACING).getOpposite();
    }

    @Override
    public void onPlace(BlockState state, Level worldIn, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, worldIn, pos, oldState, isMoving);
        for(Direction direction : Direction.values()) {
            worldIn.updateNeighborsAt(pos.relative(direction), this);
        }
        BlockEntity be = worldIn.getBlockEntity(pos);
        if (be instanceof AutoPitchControllerBlockEntity autoPitchControllerBlockEntity) {
            autoPitchControllerBlockEntity.onPlaced();
        }
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (!pIsMoving) {
            for(Direction direction : Direction.values()) {
                pLevel.updateNeighborsAt(pPos.relative(direction), this);
            }
        }
        BlockEntity be = pLevel.getBlockEntity(pPos);
        if (be instanceof AutoPitchControllerBlockEntity autoPitchControllerBlockEntity) {
            autoPitchControllerBlockEntity.onRemoved();
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean crouching = context.getPlayer() != null && context.getPlayer().isCrouching();
        return this.defaultBlockState()
                .setValue(HORIZONTAL_FACING, crouching ? context.getHorizontalDirection().getOpposite() : context.getHorizontalDirection());
    }

    @Override
    public Class<AutoPitchControllerBlockEntity> getBlockEntityClass() {
        return AutoPitchControllerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AutoPitchControllerBlockEntity> getBlockEntityType() {
        return ModBlockEntityTypes.AUTO_PITCH_CONTROLLER.get();
    }
}