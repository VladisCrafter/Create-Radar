package com.happysg.radar.block.behaviours.WeaponNetworkLinkBehaviour;

import com.happysg.radar.block.datalink.DataLinkBehavior;
import com.happysg.radar.block.datalink.DataLinkContext;
import com.happysg.radar.block.network.WeaponNetwork;
import com.happysg.radar.block.network.WeaponNetworkRegistry;
import com.happysg.radar.block.network.WeaponNetworkUnit;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlockEntity;

public class WeaponNetworkLinkBehaviour extends DataLinkBehavior {
    @Override
    protected void transferData(@NotNull DataLinkContext context) {
        if(context.level().isClientSide) return;
        ServerLevel serverLevel = (ServerLevel) context.level();
        BlockEntity be1 = serverLevel.getBlockEntity(context.getPos1());
        BlockEntity be2 = serverLevel.getBlockEntity(context.getPos2());
        if (be1 instanceof CannonMountBlockEntity cannonMountBlockEntity &&
                be2 instanceof WeaponNetworkUnit networkUnit) {
            connectMountAndUnit(networkUnit, cannonMountBlockEntity, serverLevel);
        } else if (be2 instanceof CannonMountBlockEntity cannonMountBlockEntity &&
                be1 instanceof WeaponNetworkUnit networkUnit) {
            connectMountAndUnit(networkUnit, cannonMountBlockEntity, serverLevel);
        }
        else if (be1 instanceof WeaponNetworkUnit weaponNetworkUnit1 && be2 instanceof WeaponNetworkUnit weaponNetworkUnit2) {
            connectUnits(weaponNetworkUnit1, weaponNetworkUnit2, serverLevel);
        }

    }
    private void connectMountAndUnit(WeaponNetworkUnit targetUnit, CannonMountBlockEntity cannonMount, ServerLevel serverLevel){
        if(targetUnit.getWeaponNetwork() != null) return;
        WeaponNetwork weaponNetwork = WeaponNetworkRegistry.networkContains(cannonMount.getBlockPos());
        if (weaponNetwork != null && targetUnit.getWeaponNetwork() == null) {
            weaponNetwork.setController(targetUnit.getBlockEntity());
            targetUnit.setWeaponNetwork(weaponNetwork);
        } else if (weaponNetwork == null && targetUnit.getWeaponNetwork() == null) {
            weaponNetwork = new WeaponNetwork(serverLevel);
            weaponNetwork.setCannonMount(cannonMount);
            weaponNetwork.setController(targetUnit.getBlockEntity());
            weaponNetwork.checkNeighbors(cannonMount.getBlockPos());
            weaponNetwork.checkNeighbors(targetUnit.getBlockEntity().getBlockPos());
            targetUnit.setWeaponNetwork(weaponNetwork);
        } else if (weaponNetwork == null && targetUnit.getWeaponNetwork() != null) {
            targetUnit.getWeaponNetwork().setCannonMount(cannonMount);
            targetUnit.getWeaponNetwork().checkNeighbors(cannonMount.getBlockPos());
        }
    }

    private void connectUnits(WeaponNetworkUnit unit1, WeaponNetworkUnit unit2, ServerLevel serverLevel) {
        if (unit1.getWeaponNetwork() == null && unit2.getWeaponNetwork() != null) {
            unit2.getWeaponNetwork().setController(unit1.getBlockEntity());
            unit1.setWeaponNetwork(unit2.getWeaponNetwork());
        } else if (unit2.getWeaponNetwork() == null && unit1.getWeaponNetwork() != null) {
            unit1.getWeaponNetwork().setController(unit2.getBlockEntity());
            unit2.setWeaponNetwork(unit1.getWeaponNetwork());
        } else if (unit1.getWeaponNetwork() == null && unit2.getWeaponNetwork() == null) {
            WeaponNetwork weaponNetwork = new WeaponNetwork(serverLevel);
            weaponNetwork.setController(unit1.getBlockEntity());
            weaponNetwork.setController(unit2.getBlockEntity());
            unit1.setWeaponNetwork(weaponNetwork);
            unit2.setWeaponNetwork(weaponNetwork);
        }
    }
}
