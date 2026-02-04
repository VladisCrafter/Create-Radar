package com.happysg.radar.block.controller.yaw;

import com.happysg.radar.block.behavior.networks.WeaponNetworkData;
import com.happysg.radar.registry.ModBlockEntityTypes;
import com.happysg.radar.block.datalink.DataLinkBlock;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;

public class AutoYawControllerBlock extends HorizontalKineticBlock implements IBE<AutoYawControllerBlockEntity> {

    public AutoYawControllerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return Direction.Axis.Y;
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == Direction.DOWN;
    }

    @Override
    public Class<AutoYawControllerBlockEntity> getBlockEntityClass() {
        return AutoYawControllerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AutoYawControllerBlockEntity> getBlockEntityType() {
        return ModBlockEntityTypes.AUTO_YAW_CONTROLLER.get();
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (level instanceof ServerLevel sl && state.getBlock() != newState.getBlock() ) {
            breakAttachedDataLinks(level, pos);
            WeaponNetworkData data = WeaponNetworkData.get(sl);
            var view = data.getWeaponGroupViewFromEndpoint(sl.dimension(),pos);
            if(view != null){
                data.removeController(level.dimension(), pos);

            }
        }
        super.onRemove(state, level, pos, newState, isMoving);

    }
    private static void breakAttachedDataLinks(Level level, BlockPos controllerPos) {
        for (Direction dir : Direction.values()) {
            BlockPos linkPos = controllerPos.relative(dir);
            BlockState linkState = level.getBlockState(linkPos);

            if (!(linkState.getBlock() instanceof DataLinkBlock))
                continue;

            if (linkState.hasProperty(DataLinkBlock.LINK_STYLE)
                    && linkState.getValue(DataLinkBlock.LINK_STYLE) != DataLinkBlock.LinkStyle.CONTROLLER)
                continue;

            if (linkState.hasProperty(DataLinkBlock.FACING)) {
                Direction facing = linkState.getValue(DataLinkBlock.FACING);
                if (!linkPos.relative(facing.getOpposite()).equals(controllerPos))
                    continue;
            }

            level.destroyBlock(linkPos, true);
        }
    }
}
