package com.happysg.radar.block.network;

import com.happysg.radar.block.monitor.MonitorBlockEntity;
import com.happysg.radar.block.radar.bearing.RadarBearingBlockEntity;
import com.happysg.radar.block.radar.track.RadarTrack;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class Network {
    private UUID uuid;
    private BlockPos radarPos = null;
    private final Set<UUID> weaponNetworks = new HashSet<>();
    private final HashSet<BlockPos> networkBlocks = new HashSet<>();
    private long timeOfLastSelect = 0;
    private ResourceKey<Level> dimension;
    private String selectedEntity = null;
    private ServerLevel level;

    public Network(Level level) {
        if (level == null || level.isClientSide()){
            return;
        }
        this.dimension = level.dimension();
        this.level = (ServerLevel) level;
        this.uuid = UUID.randomUUID();
        NetworkRegistry.register(this);
    }

    public UUID getUuid() {
        return uuid;
    }

    public void tick(){
        if(!weaponNetworks.isEmpty() && networkBlocks.isEmpty()){weaponNetworks.clear();}
        if(weaponNetworks.isEmpty() && networkBlocks.isEmpty()) NetworkRegistry.remove(uuid);
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

        for (UUID weaponNetworkId : weaponNetworks) {
            WeaponNetwork weaponNetwork = WeaponNetworkRegistry.get(weaponNetworkId);
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
    }
    public void addWeaponNetwork(UUID uuid) {
            weaponNetworks.add(uuid);
    }
    public void removeWeaponNetwork(WeaponNetwork network) {
            weaponNetworks.remove(network.getUuid());
    }
    public void removeWeaponNetwork(UUID uuid) {
        weaponNetworks.remove(uuid);
        if(networkBlocks.isEmpty() && weaponNetworks.isEmpty()){
           NetworkRegistry.remove(uuid);
        }
    }
    public void addNetworkBlock(BlockPos pos) {
        if(level.getBlockEntity(pos) instanceof MonitorBlockEntity monitor) {
            pos = monitor.getControllerPos();
        } else if (level.getBlockEntity(pos) instanceof RadarBearingBlockEntity) {
            if(radarPos == null) {
                radarPos = pos;
            }
        }
        networkBlocks.add(pos);
    }

    public void removeNetworkBlock(BlockPos pos) {
        if(level.getBlockEntity(pos) instanceof MonitorBlockEntity monitor) {
            pos = monitor.getControllerPos();
        }
        networkBlocks.remove(pos);
        if(networkBlocks.isEmpty() && weaponNetworks.isEmpty()){
            NetworkRegistry.remove(uuid);
        }
    }

    public Set<BlockPos> getNetworkBlocks() {
        HashSet<BlockPos> temp = new HashSet<>(networkBlocks);
        temp.add(radarPos);
        return Collections.unmodifiableSet(temp);
    }
        public void setRadarPos(BlockPos pos) {
        if(radarPos == null){
            radarPos = pos;
            this.getNetworkBlocks().forEach(networkBlock -> {
                if(level.getBlockEntity(networkBlock) instanceof MonitorBlockEntity monitor){
                    monitor.setRadarPos(radarPos);
                    monitor.updateCache();
                }
            });
        }
    }
    public BlockPos getRadarPos() {
        return radarPos;
    }
    public ResourceKey<Level> getDimension() {
        return dimension;
    }

}
