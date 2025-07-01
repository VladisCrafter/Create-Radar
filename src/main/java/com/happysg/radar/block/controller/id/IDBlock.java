package com.happysg.radar.block.controller.id;

import com.happysg.radar.compat.Mods;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

public class IDBlock extends Block {
    public IDBlock(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public @NotNull InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, @NotNull Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (!Mods.VALKYRIENSKIES.isLoaded()) {
            pPlayer.displayClientMessage(Component.translatable("create_radar.id_block.not_on_vs2"), true);
            return super.use(pState, pLevel, pPos, pPlayer, pHand, pHit);
        }
        return VS2IDHandler.use(pState, pLevel, pPos, pPlayer, pHand, pHit);
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pMovedByPiston) {
        if (Mods.VALKYRIENSKIES.isLoaded()) {
            VS2IDHandler.onRemove(pState, pLevel, pPos, pNewState, pMovedByPiston);
        }
        super.onRemove(pState, pLevel, pPos, pNewState, pMovedByPiston);
    }


}
