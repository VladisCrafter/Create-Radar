package com.happysg.radar.item.targetfilter;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class TargetFilterItem extends Item {

    public TargetFilterItem(Properties properties) {
        super(properties);
    }

    @OnlyIn(Dist.CLIENT)
    private InteractionResultHolder<ItemStack> clientFunc(Level level, Player player, InteractionHand hand) {
        Minecraft.getInstance().setScreen(new AutoTargetScreen());
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide)
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        return clientFunc(level, player, hand);
    }
}
