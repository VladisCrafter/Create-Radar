package com.happysg.radar.item;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpyglassItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Properties;

public class Binoculars extends SpyglassItem {

    // how far the ray should go
    private static final double MAX_DISTANCE = 512.0;

    // step size for walking along the ray (smaller = more accurate, slightly more expensive)
    private static final double STEP = 0.15;

    public Binoculars(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            // i only want the server to decide + store the result, so the client doesn’t lie to me
            return InteractionResultHolder.pass(stack);
        }

        ServerLevel serverLevel = (ServerLevel) level;

        BlockPos hitPos = raycastFirstNonTransparentBlock(serverLevel, player, MAX_DISTANCE, STEP);

        if (hitPos == null) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("No block hit (within range)."), true);
            clearLastHit(stack);
            return InteractionResultHolder.success(stack);
        }

        storeLastHit(stack, serverLevel.dimension(), hitPos);
        player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("Hit: " + hitPos.toShortString()),
                true
        );

        return InteractionResultHolder.success(stack);
    }

    @Nullable
    private static BlockPos raycastFirstNonTransparentBlock(ServerLevel level, Player player, double maxDistance, double step) {
        Vec3 start = player.getEyePosition();
        Vec3 dir = player.getLookAngle().normalize();
        Vec3 end = start.add(dir.scale(maxDistance));

        // i’ll track the last blockpos so i don’t re-check the same block 50 times per second
        BlockPos lastPos = BlockPos.containing(start);

        // walk from start -> end in small steps and pick the first block that isn’t “transparent”
        for (double t = 0.0; t <= maxDistance; t += step) {
            Vec3 p = start.add(dir.scale(t));
            BlockPos pos = BlockPos.containing(p);

            if (pos.equals(lastPos)) {
                continue;
            }
            lastPos = pos;

            if (!level.isLoaded(pos)) {
                continue;
            }

            BlockState state = level.getBlockState(pos);

            if (state.isAir()) {
                continue;
            }

            // skip anything considered “transparent / pass-through”
            if (isTransparentPassThrough(level, pos, state)) {
                continue;
            }

            // first “real” block i hit
            return pos;
        }

        return null;
    }

    private static boolean isTransparentPassThrough(ServerLevel level, BlockPos pos, BlockState state) {
        // anything with no collision is basically “grass / flowers / snow layer vibes”
        if (state.getCollisionShape(level, pos).isEmpty()) {
            return true;
        }

        // typical “transparent” stuff tends to not occlude + not be solid-rendered
        // (glass, panes, leaves, etc.)
        if (!state.canOcclude() || !state.isSolidRender(level, pos)) {
            return true;
        }

        // water/lava aren’t blocks i want to “hit” for targeting
        if (!state.getFluidState().isEmpty()) {
            return true;
        }

        // if you want a hard blacklist example (optional):
        // if (state.is(Blocks.GLASS) || state.is(Blocks.GLASS_PANE)) return true;

        return false;
    }

    private static void storeLastHit(ItemStack stack, ResourceKey<Level> dim, BlockPos pos) {
        CompoundTag tag = stack.getOrCreateTag();

        CompoundTag hit = new CompoundTag();
        hit.putInt("x", pos.getX());
        hit.putInt("y", pos.getY());
        hit.putInt("z", pos.getZ());
        hit.putString("dim", dim.location().toString());

        tag.put("LastHitPos", hit);
    }

    private static void clearLastHit(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return;
        tag.remove("LastHitPos");
    }
}

