package com.happysg.radar.networking;

import com.happysg.radar.block.monitor.MonitorSelectionPacket;
import com.happysg.radar.networking.packets.FirePacket;
import com.happysg.radar.networking.packets.RaycastPacket;
import com.happysg.radar.networking.packets.BoolListPacket;
import com.happysg.radar.networking.packets.SaveListsPacket;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraft.resources.ResourceLocation;

import static com.happysg.radar.CreateRadar.MODID;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        CHANNEL.registerMessage(
                packetId++,
                SaveListsPacket.class,
                SaveListsPacket::encode,
                SaveListsPacket::decode,
                SaveListsPacket::handle
        );
        // register a second packet (ToggleFlagsPacket). Replace with your packet class name.
        CHANNEL.registerMessage(
                packetId++,
                BoolListPacket.class,
                BoolListPacket::encode,
                BoolListPacket::decode,
                BoolListPacket::handle

        );
        CHANNEL.registerMessage(packetId++, RaycastPacket.class,
                RaycastPacket::encode,
                RaycastPacket::decode,
                RaycastPacket::handle
        );
        CHANNEL.registerMessage(packetId++, FirePacket.class,
                FirePacket::encode,
                FirePacket::decode,
                FirePacket::handle
        );
        CHANNEL.registerMessage(packetId++, MonitorSelectionPacket.class,
                MonitorSelectionPacket::encode,
                MonitorSelectionPacket::decode,
                MonitorSelectionPacket::handle
        );
    }

}
