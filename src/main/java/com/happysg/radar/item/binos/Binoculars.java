package com.happysg.radar.item.binos;

import com.happysg.radar.CreateRadar;
import com.happysg.radar.block.controller.networkcontroller.NetworkFiltererBlockEntity;
import com.happysg.radar.block.monitor.MonitorBlockEntity;
import com.happysg.radar.networking.NetworkHandler;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpyglassItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;

public class Binoculars extends SpyglassItem {
    private static final Logger LOGGER = LogUtils.getLogger();
    // how far the ray should go
    private static final double MAX_DISTANCE = 512.0;

    // step size for walking along the ray (smaller = more accurate, slightly more expensive)
    private static final double STEP = 0.15;
    private static final String TAG_LAST_HIT = "LastHitPos";
    private BlockPos targetBlock;

    public Binoculars(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
        if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
            pTooltipComponents.add(Component.translatable(CreateRadar.MODID + ".binoculars.base_text"));
        }
        if (pStack.getOrCreateTag().contains("filtererPos")) {
            BlockPos monitorPos = NbtUtils.readBlockPos(pStack.getOrCreateTag().getCompound("filtererPos"));
            pTooltipComponents.add(Component.translatable(CreateRadar.MODID + ".binoculars.controller" + monitorPos.toShortString()));
        } else {
            pTooltipComponents.add(Component.translatable(CreateRadar.MODID + ".binoculars.no_controller"));
        }


    }
    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        BlockPos clickedPos = pContext.getClickedPos();
        Player player = pContext.getPlayer();
        if (player == null) return super.useOn(pContext);

        if (pContext.getLevel().getBlockEntity(clickedPos) instanceof NetworkFiltererBlockEntity blockEntity) {
            player.displayClientMessage(
                    Component.translatable(CreateRadar.MODID + ".binoculars.paired").withStyle(ChatFormatting.BLUE),
                    true
            );

            pContext.getItemInHand().getOrCreateTag().put("filterPos", NbtUtils.writeBlockPos(blockEntity.getBlockPos()));
            pContext.getItemInHand().getOrCreateTag().put("filtererPos", NbtUtils.writeBlockPos(blockEntity.getBlockPos()));

            return InteractionResult.SUCCESS;
        }
        return super.useOn(pContext);
    }

    public static void setLastHit(ItemStack stack, @Nullable BlockPos pos) {
        CompoundTag tag = stack.getOrCreateTag();

        if (pos == null) {
            tag.remove(TAG_LAST_HIT);
            return;
        }

        CompoundTag hit = new CompoundTag();
        hit.putInt("x", pos.getX());
        hit.putInt("y", pos.getY());
        hit.putInt("z", pos.getZ());

        tag.put(TAG_LAST_HIT, hit);
    }

    @Nullable
    public static BlockPos getLastHit(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_LAST_HIT)) return null;

        CompoundTag hit = tag.getCompound(TAG_LAST_HIT);
        return new BlockPos(
                hit.getInt("x"),
                hit.getInt("y"),
                hit.getInt("z")
        );
    }

    public static boolean hasLastHit(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TAG_LAST_HIT);
    }

    public static void clearLastHit(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null) tag.remove(TAG_LAST_HIT);
    }

}

