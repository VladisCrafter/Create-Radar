package com.happysg.radar.networking.packets;

import com.happysg.radar.block.controller.id.IDManager;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class IDRecordPacket extends SimplePacketBase {
    String shipSlug;
    String secretID;
    String name;

    public IDRecordPacket(String shipSlug, String secretID, String name) {
        this.shipSlug = shipSlug;
        this.secretID = secretID;
        this.name = name;
    }

    public IDRecordPacket(FriendlyByteBuf buffer) {
        this.shipSlug = buffer.readUtf();
        this.secretID = buffer.readUtf();
        this.name = buffer.readUtf();
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeUtf(shipSlug);
        buffer.writeUtf(secretID);
        buffer.writeUtf(name);
    }

    @Override
    public boolean handle(NetworkEvent.Context context) {
        context.enqueueWork(() -> {
            IDManager.addIDRecord(shipSlug, secretID, name);
        });
        return true;
    }
}
