package com.happysg.radar.block.behaviours.RadarNetworkLinkBehaviour;

import com.happysg.radar.block.network.IBehaviourCondition;
import com.happysg.radar.block.network.NetworkRegistry;
import com.happysg.radar.block.radar.bearing.RadarBearingBlock;
import net.minecraft.world.level.block.Block;

public class RadarNetworkLinkCondition extends IBehaviourCondition {

    @Override
    public boolean firstBlockCondition(Block a) {
        return a instanceof RadarBearingBlock || NetworkRegistry.isNetworkBlock(a);
    }

    @Override
    public boolean secondBlockCondition(Block b) {
        return b instanceof RadarBearingBlock || NetworkRegistry.isNetworkBlock(b);
    }

    @Override
    public boolean bothBlocksCondition(Block a, Block b) {
        return a instanceof RadarBearingBlock || b instanceof RadarBearingBlock;
    }
}
