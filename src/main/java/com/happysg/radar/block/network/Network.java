package com.happysg.radar.block.network;

import com.happysg.radar.block.monitor.MonitorBlockEntity;
import com.happysg.radar.block.radar.track.RadarTrack;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class Network {
    private UUID uuid;
    //private final Set<UUID> networks = new HashSet<>();
    private BlockPos radarPos = null;
    private final Set<UUID> weaponNetworks = new HashSet<>();
    private final HashSet<BlockPos> networkBlocks = new HashSet<>();
    private long timeOfLastSelect = 0;
    private String selectedEntity = null;
    private ServerLevel level;

    public Network(Level level) {
        if (level.isClientSide() || level.getServer() == null) return;
        this.level = level.getServer().getLevel(Level.OVERWORLD);
        this.uuid = UUID.randomUUID();
        NetworkSavedData.get(this.level).registerNetwork(this);
    }

    public Network(UUID uuid, Level level, boolean register) {
        if (level.isClientSide() || level.getServer() == null) return;
        this.level = level.getServer().getLevel(Level.OVERWORLD);
        this.uuid = uuid;
        if(this.level == null) return;
        if(register) NetworkSavedData.get(this.level).registerNetwork(this);
    }

    public UUID getUuid() {
        return uuid;
    }

    public void tick(){
        Collection<RadarTrack> cachedTracks = null;
        for (BlockPos blockPos : networkBlocks) {
            BlockEntity blockEntity = level.getBlockEntity(blockPos);
            if(blockEntity == null) continue;
            if(blockEntity instanceof MonitorBlockEntity monitor) {
                if(monitor.getBlockPos() != radarPos){
                    monitor.setRadarPos(radarPos);
                }
                monitor.updateCache();
                if(monitor.timeOfLastSelect > timeOfLastSelect){
                    timeOfLastSelect = monitor.timeOfLastSelect;
                    selectedEntity = monitor.getSelectedEntity();
                }
                else if(selectedEntity != null){
                    monitor.setSelectedEntity(selectedEntity);
                }
                if(cachedTracks == null){
                    cachedTracks = monitor.getTracks();
                }
            }
        }
        if(cachedTracks == null) return;
        Vec3 targetPos = null;
        for (RadarTrack track : cachedTracks) {
            if (track.id().equals(selectedEntity)) {
                targetPos = track.getPosition();
                break;
            }
        }

        WeaponNetworkSavedData savedData = WeaponNetworkSavedData.get(level);
        for (UUID weaponNetworkId : weaponNetworks) {
            WeaponNetwork weaponNetwork = savedData.get(weaponNetworkId).orElse(null);
            if(weaponNetwork == null) continue;
            weaponNetwork.setTarget(targetPos);
            weaponNetwork.tick();
        }
    }

//    public void addSubNetwork(UUID uuid) {
//        networks.add(uuid);
//        markDirty();
//    }

//    public void removeSubNetwork(UUID uuid) {
//        networks.remove(uuid);
//        markDirty();
//        if(networkBlocks.isEmpty() && networks.isEmpty()) {
//            NetworkSavedData.get(level).destroyNetwork(this.uuid);
//        }
//    }
    public Set<UUID> getWeaponNetworks() {
        return Collections.unmodifiableSet(weaponNetworks);
    }

    public boolean containsWeaponNetwork(UUID uuid) {
        return weaponNetworks.contains(uuid);
    }

    public void addWeaponNetwork(WeaponNetwork network) {
            weaponNetworks.add(network.getUuid());
            markDirty();
    }
    public void addWeaponNetwork(UUID uuid) {
            weaponNetworks.add(uuid);
            markDirty();
    }
    public void removeWeaponNetwork(WeaponNetwork network) {
            weaponNetworks.remove(network.getUuid());
            markDirty();
    }
    public void removeWeaponNetwork(UUID uuid) {
        weaponNetworks.remove(uuid);
        markDirty();
        if(networkBlocks.isEmpty() && weaponNetworks.isEmpty()){
            NetworkSavedData.get(level).destroyNetwork(this.uuid);
        }
    }
//    public Set<UUID> getNetworks() {
//        return Collections.unmodifiableSet(networks);
//    }

//    public boolean containsNetwork(UUID uuid) {
//        return networks.contains(uuid);
//    }
    public void addNetworkBlock(BlockPos pos) {

        addNetworkBlock(pos, true);
    }
    public void addNetworkBlock(BlockPos pos, boolean makeDirty) {
        if(level.getBlockEntity(pos) instanceof MonitorBlockEntity monitor) {
            pos = monitor.getControllerPos();
        }
        networkBlocks.add(pos);
        if(makeDirty) markDirty();
    }

    public void removeNetworkBlock(BlockPos pos) {
        if(level.getBlockEntity(pos) instanceof MonitorBlockEntity monitor) {
            pos = monitor.getControllerPos();
        }
        networkBlocks.remove(pos);
        markDirty();
        if(networkBlocks.isEmpty() && weaponNetworks.isEmpty()){
            NetworkSavedData.get(level).destroyNetwork(this.uuid);
        }
    }

    public Set<BlockPos> getNetworkBlocks() {
        return Collections.unmodifiableSet(networkBlocks);
    }

    private void markDirty() {
        if (level != null && !level.isClientSide()) {
            NetworkSavedData.get(level).setDirty();
        }
    }
    public void setRadarPos(BlockPos pos) {
        setRadarPos(pos, true);
    }
        public void setRadarPos(BlockPos pos, boolean markDirty) {
        radarPos = pos;
        if(markDirty) markDirty();
    }
    public BlockPos getRadarPos() {
        return radarPos;
    }
}
