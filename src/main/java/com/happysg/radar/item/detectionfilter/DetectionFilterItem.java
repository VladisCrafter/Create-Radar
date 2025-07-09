package com.happysg.radar.item.detectionfilter;

import com.happysg.radar.item.detectionfilter.screens.RadarFilterScreen;
import com.happysg.radar.item.targetfilter.screens.AutoTargetScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class DetectionFilterItem extends Item {

    public DetectionFilterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide)
            return InteractionResultHolder.pass(player.getItemInHand(hand));

        Minecraft.getInstance().setScreen(new RadarFilterScreen());
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
}
