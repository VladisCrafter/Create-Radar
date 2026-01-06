package com.happysg.radar.block.behaviours.RadarNetworkLinkBehaviour;


import com.happysg.radar.block.datalink.DataLinkBehavior;
import com.happysg.radar.block.datalink.DataLinkContext;
import com.happysg.radar.block.network.Network;
import com.happysg.radar.block.network.NetworkRegistry;
import com.happysg.radar.block.radar.bearing.RadarBearingBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

public class RadarNetworkLinkBehaviour extends DataLinkBehavior {
    @Override
    protected void transferData(@NotNull DataLinkContext context) {
        if(context.level().isClientSide) return;
        ServerLevel serverLevel = (ServerLevel) context.level();
        BlockEntity be1 = serverLevel.getBlockEntity(context.getPos1());
        BlockEntity be2 = serverLevel.getBlockEntity(context.getPos2());
        BlockEntity networkBlockEntity = be1 instanceof RadarBearingBlockEntity ? be2 : be1;
        BlockEntity radarBlockEntity = be1 instanceof RadarBearingBlockEntity ? be1 : be2;
        if(networkBlockEntity == null || radarBlockEntity == null) return;
        Network radarNetwork = NetworkRegistry.networkThatContainsPos(radarBlockEntity.getBlockPos(), serverLevel);
        if( radarNetwork != null)  {
            if(NetworkRegistry.networkThatContainsPos(networkBlockEntity.getBlockPos(), serverLevel) == null) {
                radarNetwork.addNetworkBlock(networkBlockEntity.getBlockPos());
            }
        }
        Network network = NetworkRegistry.networkThatContainsPos(networkBlockEntity.getBlockPos(), serverLevel);
        if(network == null) network = new Network(serverLevel);
        network.addNetworkBlock(networkBlockEntity.getBlockPos());
        if(network.getRadarPos() == null) {
            network.setRadarPos(radarBlockEntity.getBlockPos());
        }
    }
}
