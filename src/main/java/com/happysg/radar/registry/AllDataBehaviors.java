package com.happysg.radar.registry;

import com.happysg.radar.block.behaviours.RadarNetworkLinkBehaviour.RadarNetworkLinkBehaviour;
import com.happysg.radar.block.behaviours.RadarNetworkLinkBehaviour.RadarNetworkLinkCondition;
import com.happysg.radar.block.behaviours.WeaponNetworkLinkBehaviour.WeaponNetworkLinkCondition;
import com.happysg.radar.block.behaviours.WeaponNetworkLinkBehaviour.WeaponNetworkLinkBehaviour;
import com.happysg.radar.block.datalink.DataLinkBehavior;
import com.happysg.radar.block.behaviours.NetworkWeaponNetworkLinkBehaviour.NetworkWeaponNetworkLinkCondition;
import com.happysg.radar.block.behaviours.NetworkWeaponNetworkLinkBehaviour.NetworkWeaponNetworkLinkBehaviour;
import com.happysg.radar.block.network.IBehaviourCondition;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;

import java.util.*;

/**
 * Registry for DataLinkBehaviors where pairs of blocks (or block entities)
 * map to one or more behaviors, regardless of order.
 */
public class AllDataBehaviors {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<IBehaviourCondition, DataLinkBehavior> BEHAVIOURS = new HashMap<>();

    public static void registerDefaults() {
        registerBehavior(new RadarNetworkLinkCondition(), new RadarNetworkLinkBehaviour());
        registerBehavior(new NetworkWeaponNetworkLinkCondition(), new NetworkWeaponNetworkLinkBehaviour());
        registerBehavior(new WeaponNetworkLinkCondition(), new WeaponNetworkLinkBehaviour());
    }

    public static void registerBlockPair(Block a, Block b, DataLinkBehavior behavior) {
        ResourceLocation idA = a.builtInRegistryHolder().key().location();
        ResourceLocation idB = b.builtInRegistryHolder().key().location();
        registerBehavior(new IBehaviourCondition() {
            @Override
            public boolean firstBlockCondition(Block block1) {
                if (block1 == null) return false;
                ResourceLocation location = block1.builtInRegistryHolder().key().location();
                return location.equals(idA) || location.equals(idB);
            }

            @Override
            public boolean secondBlockCondition(Block block2) {
                if (block2 == null) return false;
                ResourceLocation location = block2.builtInRegistryHolder().key().location();
                return location.equals(idA) || location.equals(idB);
            }
            @Override
            public boolean bothBlocksCondition(Block block1, Block block2) {
                return (block1.builtInRegistryHolder().key().location() == block2.builtInRegistryHolder().key().location()) == (idA == idB);
            }
        }, behavior);
    }

    /** Register one or more behaviors for a block pair */
    public static void registerBehavior(IBehaviourCondition condition, DataLinkBehavior behaviour) {
        if(BEHAVIOURS.containsKey(condition)) {
            LOGGER.warn("Overriding existing DataLinkBehavior for condition: {}", condition);
        }
        BEHAVIOURS.put(condition, behaviour);
    }
    public static ArrayList<DataLinkBehavior> getBehavioursForBlockPos(BlockPos pos, Level level) {
        return getBehavioursForBlock(level.getBlockState(pos).getBlock());
    }

    public static ArrayList<DataLinkBehavior> getBehavioursForBlock(Block block) {
        ArrayList<DataLinkBehavior> behaviors = new ArrayList<>();
        for (IBehaviourCondition condition : BEHAVIOURS.keySet()) {
            if(condition.firstBlockCondition(block) || condition.secondBlockCondition(block)) {
                behaviors.add(BEHAVIOURS.get(condition));
            }
        }
        return behaviors;
    }
    public static ArrayList<DataLinkBehavior> getBehavioursForBlockPoses(BlockPos blockPos, BlockPos blockPos2, Level level) {
        return getBehavioursForBlocks(level.getBlockState(blockPos).getBlock(), level.getBlockState(blockPos2).getBlock());
    }

    public static ArrayList<DataLinkBehavior> getBehavioursForBlocks(Block block, Block block2) {
        ArrayList<DataLinkBehavior> behaviors = new ArrayList<>();
        for (IBehaviourCondition condition : BEHAVIOURS.keySet()) {
            if(condition.isValid(block, block2)) {
                behaviors.add(BEHAVIOURS.get(condition));
            }
        }
        return behaviors;
    }

}