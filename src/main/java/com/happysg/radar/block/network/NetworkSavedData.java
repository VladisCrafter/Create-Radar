package com.happysg.radar.block.network;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

public class NetworkSavedData extends SavedData {

    private static final String NAME = "radar_networks";
    private final Map<UUID, Network> networks = new HashMap<>();
    private final Map<UUID, Double> weaponNetworks = new HashMap<>(); //TODO implement
    public NetworkSavedData() {}

    public static NetworkSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                tag -> load(tag, level),
                NetworkSavedData::new,
                NAME
        );
    }

    public void registerNetwork(Network network) {
        networks.put(network.getUuid(), network);
        setDirty();
    }

    public void destroyNetwork(UUID id) {
        networks.remove(id);
        setDirty();
    }

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
            CompoundTag netTag = new CompoundTag();
            netTag.putUUID("id", network.getUuid());

            // Save sub-network UUIDs
            ListTag subNetList = new ListTag();
            for (UUID sub : network.getNetworks()) {
                subNetList.add(StringTag.valueOf(sub.toString()));
            }
            netTag.put("subnets", subNetList);

            // Save network block positions
            ListTag posList = new ListTag();
            for (BlockPos pos : network.getNetworkBlocks()) {
                posList.add(LongTag.valueOf(pos.asLong()));
            }
            netTag.put("blocks", posList);

            list.add(netTag);
        }

        tag.put("networks", list);
        return tag;
    }

    public static NetworkSavedData load(CompoundTag tag, ServerLevel level) {
        NetworkSavedData data = new NetworkSavedData();

        ListTag list = tag.getList("networks", CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag netTag = list.getCompound(i);
            UUID id = netTag.getUUID("id");

            Network network = new Network(id, level);

            // Load sub-network UUIDs
            ListTag subNetList = netTag.getList("subnets", StringTag.TAG_STRING);
            for (int j = 0; j < subNetList.size(); j++) {
                UUID subId = UUID.fromString(subNetList.getString(j));
                network.addSubNetwork(subId);
            }

            // Load network block positions
            ListTag posList = netTag.getList("blocks", LongTag.TAG_LONG);
            for (net.minecraft.nbt.Tag value : posList) {
                BlockPos pos = BlockPos.of(((LongTag) value).getAsLong());
                network.addNetworkBlock(pos);
            }

            data.registerNetwork(network);
        }

        return data;
    }
}
