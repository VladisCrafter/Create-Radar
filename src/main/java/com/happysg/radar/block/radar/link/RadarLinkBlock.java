package com.happysg.radar.block.radar.link;

import com.happysg.radar.CreateRadar;
import com.happysg.radar.registry.ModBlockEntityTypes;
import com.simibubi.create.AllShapes;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import com.simibubi.create.foundation.gui.ScreenOpener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;

public class RadarLinkBlock extends WrenchableDirectionalBlock implements IBE<RadarLinkBlockEntity> {

    public RadarLinkBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand,
                                 BlockHitResult pHit) {
        if (pPlayer == null)
            return InteractionResult.PASS;
        if (pPlayer.isShiftKeyDown())
            return InteractionResult.PASS;
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> withBlockEntityDo(pLevel, pPos, be -> this.displayScreen(be, pPlayer)));
        return InteractionResult.SUCCESS;
    }

    @OnlyIn(value = Dist.CLIENT)
    protected void displayScreen(RadarLinkBlockEntity be, Player player) {
        if (!(player instanceof LocalPlayer))
            return;
        if (be.targetOffset.equals(BlockPos.ZERO)) {
            player.displayClientMessage(Component.translatable(CreateRadar.MODID + "radar_link.fail"), true);
            return;
        }
        be.getScreen().ifPresent(ScreenOpener::open);
    }

    @Override
    public boolean isPathfindable(BlockState pState, BlockGetter pLevel, BlockPos pPos, PathComputationType pType) {
        return false;
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return AllShapes.DATA_GATHERER.get(pState.getValue(FACING));
    }


    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState placed = super.getStateForPlacement(context);
        return placed.setValue(FACING, context.getClickedFace());
    }

    @Override
    public Class<RadarLinkBlockEntity> getBlockEntityClass() {
        return RadarLinkBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends RadarLinkBlockEntity> getBlockEntityType() {
        return ModBlockEntityTypes.RADAR_LINK.get();
    }

}
