package com.happysg.radar.networking.packets;

import com.happysg.radar.block.controller.networkcontroller.NetworkFiltererBlockEntity;
import com.happysg.radar.item.binos.Binoculars;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class FirePacket {

    private static final String TAG_FILTERER_POS = "filtererPos";


    private final boolean enable;

    public FirePacket(boolean enable) {
        this.enable = enable;
    }
    public static void encode(FirePacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.enable);
    }
    public static FirePacket decode(FriendlyByteBuf buf) {
        return new FirePacket(buf.readBoolean());
    }

    public static void handle(FirePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Player player = ctx.get().getSender();
            if (player == null) return;
            if (!(player.level() instanceof ServerLevel serverLevel)) return;

           // if (!player.isUsingItem()) return;

            ItemStack binos = player.getMainHandItem();
            if (!(binos.getItem() instanceof Binoculars)) return;

            BlockPos filtererPos = getFiltererPos(binos);
            if (filtererPos == null) return;
            if (!serverLevel.isLoaded(filtererPos)) return;

            if (!(serverLevel.getBlockEntity(filtererPos) instanceof NetworkFiltererBlockEntity filtererBe)) return;

            if (msg.enable) {
                BlockPos hit = Binoculars.getLastHit(binos);
                if (hit == null) return;
                filtererBe.onBinocularsTriggered(player,binos, false);

            } else {
                // release: go back to normal
                filtererBe.onBinocularsTriggered(player, binos, true);
            }

            filtererBe.setChanged();
        });

        ctx.get().setPacketHandled(true);
    }

    private static ItemStack findBinosStack(Player player) {
        // i prioritize “using item” (scoped) because that’s the cleanest intent
        ItemStack using = player.getUseItem();
        if (!using.isEmpty() && using.getItem() instanceof Binoculars) return using;

        ItemStack main = player.getMainHandItem();
        if (!main.isEmpty() && main.getItem() instanceof Binoculars) return main;

        ItemStack off = player.getOffhandItem();
        if (!off.isEmpty() && off.getItem() instanceof Binoculars) return off;

        return ItemStack.EMPTY;
    }

    @Nullable
    private static BlockPos getFiltererPos(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return null;
        if (!tag.contains(TAG_FILTERER_POS)) return null;

        return NbtUtils.readBlockPos(tag.getCompound(TAG_FILTERER_POS));
    }
}
