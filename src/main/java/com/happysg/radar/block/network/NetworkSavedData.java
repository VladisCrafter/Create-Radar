package com.happysg.radar.block.network;

import com.happysg.radar.block.monitor.MonitorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

public class NetworkSavedData extends SavedData {

    private static final String NAME = "radar_networks";
    private final Map<UUID, Network> networks = new HashMap<>();
    private final Map<UUID, Double> weaponNetworks = new HashMap<>();//TODO implement
    public NetworkSavedData() {}

    public static NetworkSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                tag -> load(tag, level),
                NetworkSavedData::new,
                NAME
        );
    }
    public void tickAll() {
        for (Network network : networks.values()) {
            network.tick();
        }
    }

    public void registerNetwork(Network network) {
        networks.put(network.getUuid(), network);
        setDirty();
    }

    public void destroyNetwork(UUID id) {
        networks.remove(id);
        setDirty();
    }
    public Network networkThatContainsPos(BlockPos pos, Level level) {
        pos = level.getBlockEntity(pos) instanceof MonitorBlockEntity monitorBlockEntity ? monitorBlockEntity.getControllerPos() : pos;
        BlockPos blockPos = pos;
        return getAllNetworks().stream().filter(network -> network.getNetworkBlocks().contains(blockPos)).findFirst().orElse(null);
    }
    public Network networkThatContainsWeaponNetwork(UUID id){
        return getAllNetworks().stream().filter(network -> network.getWeaponNetworks().contains(id)).findFirst().orElse(null);
    }
//    public Network getParentNetwork(UUID id) {
//        return getAllNetworks().stream().filter(network -> network.containsNetwork(network.getUuid())).findFirst().orElse(null);
//    }
//    public Network getTopLevelNetwork(UUID id) {
//        Network network = getParentNetwork(id);
//        if (network == null) return getNetwork(id).orElse(null);
//        return getTopLevelNetwork(network.getUuid());
//    }
    public Optional<Network> getNetwork(UUID id) {
        return Optional.ofNullable(networks.get(id));
    }

    public Collection<Network> getAllNetworks() {
        return networks.values();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();

        for (Network network : networks.values()) {
            if(network.getNetworkBlocks().isEmpty() && network.getWeaponNetworks().isEmpty()){
                continue;
            }
            CompoundTag netTag = new CompoundTag();
            netTag.putUUID("id", network.getUuid());
            netTag.putString("dimension", network.getDimension().location().toString());
//            ListTag subNetworks = new ListTag();
//            for (UUID subNetwork : network.getNetworks()) {
//                subNetworks.add(StringTag.valueOf(subNetwork.toString()));
//            }
//            netTag.put("subNetworks", subNetworks);

            ListTag weaponNetworks = new ListTag();
            for (UUID weaponNetwork : network.getWeaponNetworks()) {
                weaponNetworks.add(StringTag.valueOf(weaponNetwork.toString()));
            }
            netTag.put("weaponNetworks", weaponNetworks);

            ListTag networkBlocks = new ListTag();
            for (BlockPos pos : network.getNetworkBlocks()) {
                networkBlocks.add(LongTag.valueOf(pos.asLong()));
            }
            netTag.put("blocks", networkBlocks);

            BlockPos radarPos = network.getRadarPos();
            if (radarPos != null) {
                netTag.putLong("radarPos", radarPos.asLong());
            }
            list.add(netTag);
        }

        tag.put("networks", list);
        return tag;
    }

    public static NetworkSavedData load(CompoundTag tag, ServerLevel serverLevel) {
        NetworkSavedData data = new NetworkSavedData();
        ListTag list = tag.getList("networks", CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag netTag = list.getCompound(i);
            UUID id = netTag.getUUID("id");
            ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(netTag.getString("dimension")));
            ServerLevel level = serverLevel.getServer().getLevel(dimensionKey);
            if(level == null) continue;
            Network network = new Network(id, level, false);

//            // Load sub-network UUIDs
//            ListTag subNetList = netTag.getList("subNetworks", StringTag.TAG_STRING);
//            for (int j = 0; j < subNetList.size(); j++) {
//                UUID subId = UUID.fromString(subNetList.getString(j));
//                network.addSubNetwork(subId);
//            }

            // Load network block positions
            ListTag posList = netTag.getList("blocks", LongTag.TAG_LONG);
            for (net.minecraft.nbt.Tag value : posList) {
                BlockPos pos = BlockPos.of(((LongTag) value).getAsLong());
                network.addNetworkBlock(pos, false);
            }
            if (netTag.contains("radarPos", LongTag.TAG_LONG)) {
                BlockPos radarPos = BlockPos.of(netTag.getLong("radarPos"));
                network.setRadarPos(radarPos, false);
            }
            data.registerNetwork(network);
        }
        return data;
    }
}
