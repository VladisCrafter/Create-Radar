package com.happysg.radar.block.datalink;

import com.happysg.radar.block.datalink.screens.AbstractDataLinkScreen;
import com.happysg.radar.block.network.WeaponNetwork;
import com.happysg.radar.block.network.WeaponNetworkSavedData;
import com.happysg.radar.block.network.WeaponNetworkUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlockEntity;

public class ControllersDataLinkBehavior extends DataPeripheral{


    @Override
    protected @Nullable AbstractDataLinkScreen getScreen(DataLinkBlockEntity be) {
        return null;
    }

    @Override
    protected void transferData(@NotNull DataLinkContext context, @NotNull DataController activeTarget) {
        if(context.level().isClientSide) return;
        ServerLevel serverLevel = (ServerLevel) context.level();
        BlockPos controllerPos = context.getSourcePos();
        BlockPos cannonMountPos = context.getTargetPos();
        if(!(serverLevel.getBlockEntity(controllerPos) instanceof WeaponNetworkUnit targetUnit) || !(serverLevel.getBlockEntity(cannonMountPos) instanceof CannonMountBlockEntity cannonMount)) return;//add to link
        WeaponNetworkSavedData weaponNetworkSavedData = WeaponNetworkSavedData.get(serverLevel);
        WeaponNetwork weaponNetwork = weaponNetworkSavedData.networkContains(controllerPos);
        if(targetUnit.getWeaponNetwork() != null) return;
        WeaponNetwork cannonWeaponNetwork = weaponNetworkSavedData.networkContains(cannonMountPos);

        if (weaponNetwork != null ) {
            targetUnit.setWeaponNetwork(weaponNetwork);
            weaponNetwork.setController(serverLevel.getBlockEntity(controllerPos));
        } else if (cannonWeaponNetwork != null) {
            if(cannonWeaponNetwork.setController(serverLevel.getBlockEntity(controllerPos))) {
                targetUnit.setWeaponNetwork(cannonWeaponNetwork);
            } //Doesnt go through if its not overriding null

        } else if (weaponNetworkSavedData.networkContains(cannonMount.getBlockPos()) == null) {
            WeaponNetwork newNetwork = new WeaponNetwork(serverLevel);
            newNetwork.setCannonMount(cannonMount);
            newNetwork.setController(serverLevel.getBlockEntity(controllerPos));
            targetUnit.setWeaponNetwork(newNetwork);
        }
    }
}
