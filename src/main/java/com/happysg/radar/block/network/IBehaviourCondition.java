package com.happysg.radar.block.network;

import net.minecraft.world.level.block.Block;

public abstract class IBehaviourCondition {

    public abstract boolean firstBlockCondition(Block a);
    public abstract boolean secondBlockCondition(Block b);
    public abstract boolean bothBlocksCondition(Block a, Block b);

    public boolean isValid(Block a, Block b) {
        return ((firstBlockCondition(a) && secondBlockCondition(b)) || firstBlockCondition(b) && secondBlockCondition(a)) && bothBlocksCondition(a, b);
    }

}
