package com.happysg.radar.block.network;

import com.happysg.radar.block.datalink.DataController;
import com.happysg.radar.block.datalink.DataLinkBlockEntity;
import com.happysg.radar.block.datalink.DataLinkContext;
import com.happysg.radar.block.datalink.DataPeripheral;
import com.happysg.radar.block.datalink.screens.AbstractDataLinkScreen;
import com.happysg.radar.block.monitor.MonitorBlock;
import com.happysg.radar.block.monitor.MonitorBlockEntity;
import com.happysg.radar.block.radar.bearing.RadarBearingBlockEntity;
import com.happysg.radar.registry.AllDataBehaviors;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RadarNetworkLinkBehaviour extends DataPeripheral {
    @Override
    protected @Nullable AbstractDataLinkScreen getScreen(DataLinkBlockEntity be) {
        return null;
    }

    @Override
    protected void transferData(@NotNull DataLinkContext context, @NotNull DataController activeTarget) {
        if(context.level().isClientSide) return;
        ServerLevel serverLevel = (ServerLevel) context.level();
        BlockPos networkBlockPos = context.getSourcePos();
        BlockEntity radar = serverLevel.getBlockEntity(context.getTargetPos());
        DataController dataController = AllDataBehaviors.targetOf(radar);
        NetworkSavedData networkSavedData = NetworkSavedData.get(serverLevel);
        if(dataController == null) return;
        if(!(radar instanceof RadarBearingBlockEntity)) return;
        Network network = networkSavedData.networkThatContainsPos(networkBlockPos, serverLevel);
        if(network == null) network = new Network(serverLevel);
        network.addNetworkBlock(networkBlockPos);
        if(network.getRadarPos() == null) {
            network.setRadarPos(radar.getBlockPos());
        }
    }
}
