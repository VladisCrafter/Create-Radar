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

public class CannonMountLinkBehavior extends DataPeripheral{


    @Override
    protected @Nullable AbstractDataLinkScreen getScreen(DataLinkBlockEntity be) {
        return null;
    }

    @Override
    protected void transferData(@NotNull DataLinkContext context, @NotNull DataController activeTarget) {
        ServerLevel serverLevel = (ServerLevel) context.level();
        BlockPos targetPos = context.getTargetPos();
        BlockPos sourcePos = context.getSourcePos();
        if(!(serverLevel.getBlockEntity(targetPos) instanceof WeaponNetworkUnit targetUnit)) return;//add to link
        if(!(serverLevel.getBlockEntity(sourcePos) instanceof CannonMountBlockEntity cannonMount)) return;//add to link
        WeaponNetworkSavedData weaponNetworkSavedData = WeaponNetworkSavedData.get(serverLevel);
        WeaponNetwork weaponNetwork = weaponNetworkSavedData.networkContains(targetPos);
        WeaponNetwork cannonWeaponNetwork = weaponNetworkSavedData.networkContains(sourcePos);

        if (weaponNetwork != null) { // Shouldn't happen normally
            targetUnit.setWeaponNetwork(weaponNetwork);
        } else if (cannonWeaponNetwork != null) {
            cannonWeaponNetwork.setController(serverLevel.getBlockEntity(targetPos)); //Doesnt go through if its not overriding null
            targetUnit.setWeaponNetwork(cannonWeaponNetwork);
        } else if (weaponNetworkSavedData.networkContains(cannonMount.getBlockPos()) == null) {
            WeaponNetwork newNetwork = new WeaponNetwork(serverLevel);
            newNetwork.setCannonMount(cannonMount);
            newNetwork.setController(serverLevel.getBlockEntity(targetPos));
            targetUnit.setWeaponNetwork(newNetwork);
        }
    }
}
