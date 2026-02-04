package com.happysg.radar.block.monitor;

import com.happysg.radar.block.radar.track.RadarTrack;
import com.happysg.radar.networking.NetworkHandler;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> Server: select a track on a given monitor controller.
 */
public class MonitorSelectionPacket {


    private final BlockPos controllerPos;
    private final String selectedId;

    public MonitorSelectionPacket(BlockPos controllerPos, String selectedId) {
        this.controllerPos = controllerPos;
        this.selectedId = selectedId;
    }

    public static void encode(MonitorSelectionPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.controllerPos);
        buf.writeBoolean(msg.selectedId != null);
        if (msg.selectedId != null) {
            buf.writeUtf(msg.selectedId);
        }
    }

    public static MonitorSelectionPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        String id = buf.readBoolean() ? buf.readUtf() : null;
        return new MonitorSelectionPacket(pos, id);
    }

    public static void handle(MonitorSelectionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {

            if(ctx.get().getSender() == null) return;

            ServerPlayer sp = ctx.get().getSender();
            if(sp == null) return;
            if (!(sp.level().getBlockEntity(msg.controllerPos) instanceof MonitorBlockEntity be)){

                return;
            }


            MonitorBlockEntity controller = be.isController() ? be : be.getController();
            if (controller == null || !controller.isLinked())
                return;

            if (msg.selectedId == null) {
                controller.activetrack = null;
                controller.selectedEntity = null;
                controller.setSelectedTargetServer(null);
                controller.notifyUpdate();
                return;
            }


            RadarTrack found = null;
            for (RadarTrack t : controller.cachedTracks) {
                if (msg.selectedId.equals(t.id())) {
                    found = t;
                    break;
                }
            }

            if (found != null) {
                controller.selectedEntity = found.id();
                controller.setSelectedTargetServer(found);
                controller.notifyUpdate();
            }else{

            }
        });
        ctx.get().setPacketHandled(true);
    }

    public static void send(BlockPos controllerPos, String selectedId) {
        NetworkHandler.CHANNEL.sendToServer(new MonitorSelectionPacket(controllerPos, selectedId));
    }
}
