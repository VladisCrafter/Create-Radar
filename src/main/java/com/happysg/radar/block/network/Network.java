package com.happysg.radar.block.network;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.*;

public class Network {
    private UUID uuid;
    private final Set<UUID> networks = new HashSet<>();
    private final List<BlockPos> networkBlocks = new ArrayList<>();
    private ServerLevel level;

    public Network(Level level) {
        if (level.isClientSide() || level.getServer() == null) return;
        this.level = level.getServer().getLevel(Level.OVERWORLD);
        this.uuid = UUID.randomUUID();
        NetworkSavedData.get(this.level).registerNetwork(this);
    }

    public Network(UUID uuid, Level level) {
        if (level.isClientSide() || level.getServer() == null) return;
        this.level = level.getServer().getLevel(Level.OVERWORLD);
        this.uuid = uuid;
        NetworkSavedData.get(this.level).registerNetwork(this);
    }

    public UUID getUuid() {
        return uuid;
    }

    public void addSubNetwork(UUID uuid) {
        networks.add(uuid);
        markDirty();
    }

    public void removeSubNetwork(UUID uuid) {
        networks.remove(uuid);
        markDirty();
        if(networkBlocks.isEmpty() && networks.isEmpty()) {
            NetworkSavedData.get(level).destroyNetwork(this.uuid);
        }
    }

    public Set<UUID> getNetworks() {
        return Collections.unmodifiableSet(networks);
    }

    public boolean contains(UUID uuid) {
        return networks.contains(uuid);
    }

    public void addNetworkBlock(BlockPos pos) {
        if (!networkBlocks.contains(pos)) {
            networkBlocks.add(pos);
            markDirty();
        }
    }

    public void removeNetworkBlock(BlockPos pos) {
        networkBlocks.remove(pos);
        markDirty();
        if(networkBlocks.isEmpty() && networks.isEmpty()) {
            NetworkSavedData.get(level).destroyNetwork(this.uuid);
        }
    }

    public List<BlockPos> getNetworkBlocks() {
        return Collections.unmodifiableList(networkBlocks);
    }

    private void markDirty() {
        if (level != null && !level.isClientSide()) {
            NetworkSavedData.get(level).setDirty();
        }
    }
}
