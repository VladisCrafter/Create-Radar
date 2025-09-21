package com.happysg.radar.block.behaviours.WeaponNetworkLinkBehaviour;

import com.happysg.radar.block.network.IBehaviourCondition;
import com.happysg.radar.block.network.WeaponNetwork;
import net.minecraft.world.level.block.Block;
import rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlock;

public class WeaponNetworkLinkCondition extends IBehaviourCondition {
    @Override
    public boolean firstBlockCondition(Block a) {
        return WeaponNetwork.isController(a) || a instanceof CannonMountBlock;
    }

    @Override
    public boolean secondBlockCondition(Block b) {
        return WeaponNetwork.isController(b) || b instanceof CannonMountBlock;
    }

    @Override
    public boolean bothBlocksCondition(Block a, Block b) {
        return !(a instanceof CannonMountBlock && b instanceof CannonMountBlock);
    }
}
