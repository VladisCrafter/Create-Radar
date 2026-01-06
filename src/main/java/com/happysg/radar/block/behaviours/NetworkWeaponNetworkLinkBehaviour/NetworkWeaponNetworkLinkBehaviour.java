package com.happysg.radar.block.behaviours.NetworkWeaponNetworkLinkBehaviour;

import com.happysg.radar.block.datalink.*;
import com.happysg.radar.block.network.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlockEntity;

public class NetworkWeaponNetworkLinkBehaviour extends DataLinkBehavior {
    @Override
    protected void transferData(@NotNull DataLinkContext context) {
        if(context.level().isClientSide) return;
        ServerLevel serverLevel = (ServerLevel) context.level();
        BlockEntity first = serverLevel.getBlockEntity(context.getPos1());
        BlockEntity second = serverLevel.getBlockEntity(context.getPos2());
        if(first == null || second == null) return;
        BlockEntity weaponNetworkUnit = (first instanceof WeaponNetworkUnit || first instanceof CannonMountBlockEntity) ? first : second;
        BlockEntity networkBlock = (first instanceof WeaponNetworkUnit || first instanceof CannonMountBlockEntity) ? second : first;
        WeaponNetwork weaponNetwork = WeaponNetworkRegistry.networkContains(weaponNetworkUnit.getBlockPos());
        if(weaponNetwork == null){
            weaponNetwork = new WeaponNetwork(serverLevel);
            if(weaponNetworkUnit instanceof WeaponNetworkUnit networkUnit){
                weaponNetwork.setController(networkUnit.getBlockEntity());
                networkUnit.setWeaponNetwork(weaponNetwork);
                weaponNetwork.checkNeighbors(networkUnit.getBlockEntity().getBlockPos());
            }
            if(weaponNetworkUnit instanceof CannonMountBlockEntity cannonMountBlockEntity){
                weaponNetwork.setCannonMount(cannonMountBlockEntity);
                weaponNetwork.checkNeighbors(cannonMountBlockEntity.getBlockPos());
            }
        }
        Network parentNetwork = NetworkRegistry.networkThatContainsWeaponNetwork(weaponNetwork.getUuid());
        if(parentNetwork != null) return;
        Network network = NetworkRegistry.networkThatContainsPos(networkBlock.getBlockPos(), serverLevel);
        if(network == null) network = new Network(serverLevel);
        network.addWeaponNetwork(weaponNetwork);
        network.addNetworkBlock(networkBlock.getBlockPos());

    }
}
