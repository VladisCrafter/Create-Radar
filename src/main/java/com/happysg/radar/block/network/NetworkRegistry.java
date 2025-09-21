package com.happysg.radar.block.network;

import com.happysg.radar.block.controller.networkfilter.NetworkFiltererBlock;
import com.happysg.radar.block.monitor.MonitorBlock;
import com.happysg.radar.block.monitor.MonitorBlockEntity;
import com.happysg.radar.block.radar.bearing.RadarBearingBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;

import java.util.Collection;
import java.util.Hashtable;
import java.util.UUID;

public class NetworkRegistry {
    private static final Hashtable<UUID, Network> networks = new Hashtable<>();

    public static boolean isNetworkBlock(Block block) {
        return (block instanceof MonitorBlock || block instanceof RadarBearingBlock || block instanceof NetworkFiltererBlock);
    }

    public static void register(Network network) {
        networks.put(network.getUuid(), network);
    }

    public static Network get(UUID uuid) {
        return networks.get(uuid);
    }

    public static void remove(UUID uuid) {
        networks.remove(uuid);
    }

    public static Network networkThatContainsPos(BlockPos pos, ServerLevel level) {
        pos = level.getBlockEntity(pos) instanceof MonitorBlockEntity monitorBlockEntity ? monitorBlockEntity.getControllerPos() : pos;
        BlockPos blockPos = pos;
        return getAll().stream().filter(network -> network.getNetworkBlocks().contains(blockPos)).findFirst().orElse(null);    }

    public static Network networkThatContainsWeaponNetwork(UUID id){
        return getAll().stream().filter(network -> network.getWeaponNetworks().contains(id)).findFirst().orElse(null);
    }

    public static Collection<Network> getAll() {
        return networks.values();
    }

    public static void tickAll(){
        for (Network network : networks.values()) {
            network.tick();
        }
    }
}
