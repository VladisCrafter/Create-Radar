package com.happysg.radar.networking.packets;

import com.happysg.radar.block.controller.id.IDManager;
import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

public class IDRecordPacket extends SimplePacketBase {
    long shipId;
    String shipSlug;
    String secretID;
    String newSlug;

    public IDRecordPacket(long shipId, String shipSlug, String secretID, String newName) {
        this.shipId = shipId;
        this.shipSlug = shipSlug == null ? "" : shipSlug;
        this.secretID = secretID == null ? "" : secretID;
        this.newSlug = newName == null ? "" : newName;
    }

    public IDRecordPacket(FriendlyByteBuf buffer) {
        this.shipId = buffer.readLong();
        this.shipSlug = buffer.readUtf(32767);
        this.secretID = buffer.readUtf(32767);
        this.newSlug = buffer.readUtf(32767);
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeLong(shipId);
        buffer.writeUtf(shipSlug, 32767);
        buffer.writeUtf(secretID, 32767);
        buffer.writeUtf(newSlug, 32767);
    }

    @Override
    public boolean handle(NetworkEvent.Context context) {
        context.enqueueWork(() -> {
            IDManager.addIDRecord(shipId, secretID, newSlug);
        });
        return true;
    }
}
