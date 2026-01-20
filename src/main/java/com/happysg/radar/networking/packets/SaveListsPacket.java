package com.happysg.radar.networking.packets;

import com.happysg.radar.networking.networkhandlers.ListNBTHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class SaveListsPacket {
    private final List<String> entries;
    private final List<Boolean> friendOrFoe;
    private final String idString;
    private final boolean isIdString;

    /** Constructor for list mode **/
    public SaveListsPacket(List<String> entries, List<Boolean> friendOrFoe) {
        if (entries == null || friendOrFoe == null) {
            throw new IllegalArgumentException("entries and friendOrFoe cannot be null");
        }
        if (entries.size() != friendOrFoe.size()) {
            throw new IllegalArgumentException("entries and friendOrFoe must be the same length");
        }
        this.entries      = new ArrayList<>(entries);
        this.friendOrFoe  = new ArrayList<>(friendOrFoe);
        this.idString     = null;
        this.isIdString   = false;
    }

    /** Constructor for single‐string mode **/
    public SaveListsPacket(String idString) {
        if (idString == null) {
            throw new IllegalArgumentException("idString cannot be null");
        }
        this.entries      = Collections.emptyList();
        this.friendOrFoe  = Collections.emptyList();
        this.idString     = idString;
        this.isIdString   = true;
    }

    /** Encode either the string or the two lists, prefixed by a mode‐flag **/
    public static void encode(SaveListsPacket pkt, FriendlyByteBuf buf) {
        buf.writeBoolean(pkt.isIdString);
        if (pkt.isIdString) {
            buf.writeUtf(pkt.idString, 32767);
        } else {
            buf.writeInt(pkt.entries.size());
            for (String s : pkt.entries) {
                buf.writeUtf(s, 32767);
            }
            buf.writeInt(pkt.friendOrFoe.size());
            for (boolean b : pkt.friendOrFoe) {
                buf.writeBoolean(b);
            }
        }
    }

    /** Decode in the same order: read flag, then either string or lists **/
    public static SaveListsPacket decode(FriendlyByteBuf buf) {
        boolean isId = buf.readBoolean();
        if (isId) {
            String s = buf.readUtf(32767);
            return new SaveListsPacket(s);
        } else {
            int es = buf.readInt();
            List<String> entries = new ArrayList<>(es);
            for (int i = 0; i < es; i++) {
                entries.add(buf.readUtf(32767));
            }
            int fs = buf.readInt();
            List<Boolean> foes = new ArrayList<>(fs);
            for (int i = 0; i < fs; i++) {
                foes.add(buf.readBoolean());
            }
            return new SaveListsPacket(entries, foes);
        }
    }

    /** Handle on the server: call the appropriate ListNBTHandler method **/
    public static void handle(SaveListsPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            if (pkt.isIdString) {
                ListNBTHandler.saveStringToHeldItem(player, pkt.idString);
            } else {
                ListNBTHandler.saveToHeldItem(player, pkt.entries, pkt.friendOrFoe);
            }
            player.getInventory().setChanged(); // force sync
        });
        ctx.setPacketHandled(true);
    }
}
