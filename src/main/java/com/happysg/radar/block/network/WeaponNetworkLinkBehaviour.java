package com.happysg.radar.block.network;

import com.happysg.radar.block.datalink.*;
import com.happysg.radar.block.datalink.screens.AbstractDataLinkScreen;
import com.happysg.radar.block.monitor.MonitorBlockEntity;
import com.happysg.radar.registry.AllDataBehaviors;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WeaponNetworkLinkBehaviour extends DataPeripheral {
    @Override
    protected @Nullable AbstractDataLinkScreen getScreen(DataLinkBlockEntity be) {
        return null;
    }

    @Override
    protected void transferData(@NotNull DataLinkContext context, @NotNull DataController activeTarget) {
        if(context.level().isClientSide) return;
        ServerLevel serverLevel = (ServerLevel) context.level();
        BlockEntity networkBlock = serverLevel.getBlockEntity(context.getTargetPos());
        BlockPos cannonMountPos = context.getSourcePos();
        DataController dataController = AllDataBehaviors.targetOf(networkBlock);
        NetworkSavedData networkSavedData = NetworkSavedData.get(serverLevel);
        WeaponNetworkSavedData weaponNetworkSavedData = WeaponNetworkSavedData.get(serverLevel);
        if(dataController == null) return;
        WeaponNetwork weaponNetwork = weaponNetworkSavedData.networkContains(cannonMountPos);
        if(weaponNetwork == null) return;
        Network parentNetwork = networkSavedData.networkThatContainsWeaponNetwork(weaponNetwork.getUuid()); //If weapon network in a network already break
        if(parentNetwork != null) return;
        Network network = networkSavedData.networkThatContainsPos(networkBlock.getBlockPos(), serverLevel);
        if(network == null) network = new Network(serverLevel); //If block not in network create one
        network.addWeaponNetwork(weaponNetwork);
        network.addNetworkBlock(networkBlock.getBlockPos());

    }
}
