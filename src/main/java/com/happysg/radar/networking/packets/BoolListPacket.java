package com.happysg.radar.networking.packets;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/**
 * Packet to save an array of boolean flags from client -> server and write them to the held item's NBT.
 * Usage: register with your channel, then send from client using NetworkHandler.CHANNEL.sendToServer(new SaveFlagsPacket(...));
 */
public class BoolListPacket {
    public static String NBT_KEY = "";
    private static final int EXPECTED_FLAG_COUNT = 6;

    public final boolean mainHand;
    public final boolean[] flags;

    public BoolListPacket(boolean mainHand, boolean[] flags, String key) {
        this.mainHand = mainHand;
        this.flags = flags;
        NBT_KEY = key;
    }

    // encoder
    public static void encode(BoolListPacket pkt, FriendlyByteBuf buf) {
        buf.writeBoolean(pkt.mainHand);
        if (pkt.flags == null) {
            buf.writeInt(0);
        } else {
            buf.writeInt(pkt.flags.length);
            for (boolean b : pkt.flags) buf.writeBoolean(b);
        }
    }

    // decoder
    public static BoolListPacket decode(FriendlyByteBuf buf) {
        boolean main = buf.readBoolean();
        int len = buf.readInt();
        boolean[] f = new boolean[len];
        for (int i = 0; i < len; i++) f[i] = buf.readBoolean();
        return new BoolListPacket(main, f, NBT_KEY);
    }

    // handler (executed on server thread)
    public static void handle(BoolListPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                // Shouldn't happen; packet came from non-player or client-only
                return;
            }

            // Basic validation
            if (pkt.flags == null) {;
                return;
            }
            if (pkt.flags.length != EXPECTED_FLAG_COUNT) {
                return;
            }

            InteractionHand hand = pkt.mainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            ItemStack stack = player.getItemInHand(hand);
            if (stack == null || stack.isEmpty()) {

                return;
            }

            try {
                // Optional: validate item type here to prevent client abuse:
                // if (stack.getItem() != ModItems.YOUR_GUI_ITEM.get()) { ... reject ... }

                // Convert booleans to byte[] and write to tag
                byte[] arr = new byte[pkt.flags.length];
                for (int i = 0; i < pkt.flags.length; i++) arr[i] = (byte) (pkt.flags[i] ? 1 : 0);

                CompoundTag tag = stack.getOrCreateTag();
                tag.putByteArray(NBT_KEY, arr);
                stack.setTag(tag); // set back explicitly to be safe

                // IMPORTANT: put the (possibly mutated) stack back into the player's hand to avoid copy issues.
                player.setItemInHand(hand, stack);

                player.inventoryMenu.broadcastChanges();


            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        ctx.setPacketHandled(true);
    }
}
