package com.happysg.radar.item.targetfilter;

import com.happysg.radar.item.identfilter.screens.IdentificationFilterScreen;
import com.happysg.radar.item.targetfilter.screens.AutoTargetScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class TargetFilterItem extends Item{

        public TargetFilterItem(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            if (!level.isClientSide)
                return InteractionResultHolder.pass(player.getItemInHand(hand));

            Minecraft.getInstance().setScreen(new AutoTargetScreen());
            return InteractionResultHolder.success(player.getItemInHand(hand));
        }
    }
