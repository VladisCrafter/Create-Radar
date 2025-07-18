package com.happysg.radar.networking.packets;

import com.happysg.radar.networking.networkhandlers.ListNBTHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SaveListsPacket {
    private final List<String> entries;
    private final List<Boolean> friendOrFoe;

    public SaveListsPacket(List<String> entries, List<Boolean> friendOrFoe) {
        this.entries = entries;
        this.friendOrFoe = friendOrFoe;
    }

    public static void encode(SaveListsPacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.entries.size());
        for (String s : pkt.entries) buf.writeUtf(s, 32767);

        buf.writeInt(pkt.friendOrFoe.size());
        for (boolean b : pkt.friendOrFoe) buf.writeBoolean(b);
    }

    public static SaveListsPacket decode(FriendlyByteBuf buf) {
        int es = buf.readInt();
        List<String> entries = new ArrayList<>(es);
        for (int i = 0; i < es; i++) entries.add(buf.readUtf(32767));

        int fs = buf.readInt();
        List<Boolean> friendOrFoe = new ArrayList<>(fs);
        for (int i = 0; i < fs; i++) friendOrFoe.add(buf.readBoolean());

        return new SaveListsPacket(entries, friendOrFoe);
    }

    public static void handle(SaveListsPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            // server‑side: write NBT onto the held item
            ListNBTHandler.saveToHeldItem(player, pkt.entries, pkt.friendOrFoe);
            // force inventory sync
            player.getInventory().setChanged();
        });
        ctx.get().setPacketHandled(true);
    }
}
