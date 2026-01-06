package com.happysg.radar.block.behaviours.NetworkWeaponNetworkLinkBehaviour;

import com.happysg.radar.block.network.IBehaviourCondition;
import com.happysg.radar.block.network.NetworkRegistry;
import com.happysg.radar.block.network.WeaponNetwork;
import net.minecraft.world.level.block.Block;
import rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlock;

public class NetworkWeaponNetworkLinkCondition extends IBehaviourCondition {
    @Override
    public boolean firstBlockCondition(Block a) {
        return a instanceof CannonMountBlock || WeaponNetwork.isController(a);
    }

    @Override
    public boolean secondBlockCondition(Block b) {
        return NetworkRegistry.isNetworkBlock(b);
    }

    @Override
    public boolean bothBlocksCondition(Block a, Block b) {
        return NetworkRegistry.isNetworkBlock(a) || NetworkRegistry.isNetworkBlock(b);
    }
}
