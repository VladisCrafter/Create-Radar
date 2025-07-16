package com.happysg.radar.block.network;

import com.happysg.radar.block.controller.firing.FireControllerBlockEntity;
import com.happysg.radar.block.controller.pitch.AutoPitchControllerBlockEntity;
import com.happysg.radar.block.controller.yaw.AutoYawControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlockEntity;

import java.util.*;

public class WeaponNetworkSavedData extends SavedData {

    private static final String NAME = "weapon_networks";

    private final Map<UUID, WeaponNetwork> networks = new HashMap<>();

    public static WeaponNetworkSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                tag -> WeaponNetworkSavedData.load(tag, level),
                WeaponNetworkSavedData::new,
                NAME
        );
    }


    public WeaponNetworkSavedData() {}

    public void register(WeaponNetwork network) {
        networks.put(network.getUuid(), network);
        setDirty();
    }

    public Optional<WeaponNetwork> get(UUID uuid) {
        return Optional.ofNullable(networks.get(uuid));
    }

    public void remove(UUID uuid) {
        networks.remove(uuid);
        setDirty();
    }

    public WeaponNetwork networkContains(BlockPos pos){
        return  getAll().stream().filter(weaponNetwork -> weaponNetwork.contains(pos)).findFirst().orElse(null);
    }

    public Collection<WeaponNetwork> getAll() {
        return networks.values();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (WeaponNetwork network : networks.values()) {
            CompoundTag nbt = new CompoundTag();
            nbt.putUUID("id", network.getUuid());
            nbt.putString("dimension", network.getDimension().toString());
            if (network.getCannonMount() != null)
                nbt.put("cannon_mount", NbtUtils.writeBlockPos(network.getCannonMount().getBlockPos()));

            if (network.getAutoPitchController() != null)
                nbt.put("pitch_controller", NbtUtils.writeBlockPos(network.getAutoPitchController().getBlockPos()));

            if (network.getAutoYawController() != null)
                nbt.put("yaw_controller", NbtUtils.writeBlockPos(network.getAutoYawController().getBlockPos()));

            if (network.getFireController() != null)
                nbt.put("fire_controller", NbtUtils.writeBlockPos(network.getFireController().getBlockPos()));

            if (network.getTargetPos() != null)
                nbt.put("target", writeVec3(network.getTargetPos()));

            list.add(nbt);
        }
        tag.put("networks", list);
        return tag;
    }

    public static WeaponNetworkSavedData load(CompoundTag tag, ServerLevel serverLevel) {
        WeaponNetworkSavedData data = new WeaponNetworkSavedData();
        ListTag list = tag.getList("networks", CompoundTag.TAG_COMPOUND);
        ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(tag.getString("dimension")));
        ServerLevel level = serverLevel.getServer().getLevel(dimensionKey);
        if(level == null) return null;

        for (int i = 0; i < list.size(); i++) {
            CompoundTag nbt = list.getCompound(i);
            UUID id = nbt.getUUID("id");
            WeaponNetwork network = new WeaponNetwork(id, level);

            network.setCannonMount(level.getBlockEntity(NbtUtils.readBlockPos(nbt.getCompound("cannon_mount"))) instanceof CannonMountBlockEntity mount ? mount : null);
            network.setAutoPitchController(level.getBlockEntity(NbtUtils.readBlockPos(nbt.getCompound("pitch_controller"))) instanceof AutoPitchControllerBlockEntity pitch ? pitch : null);
            network.setAutoYawController(level.getBlockEntity(NbtUtils.readBlockPos(nbt.getCompound("yaw_controller"))) instanceof AutoYawControllerBlockEntity yaw ? yaw : null);
            network.setFireController(level.getBlockEntity(NbtUtils.readBlockPos(nbt.getCompound("fire_controller"))) instanceof FireControllerBlockEntity fire ? fire : null);

            if (nbt.contains("target")) {
                network.setTargetPos(readVec3(nbt.getCompound("target")));
            }
            if(!network.isEmpty()) data.register(network); //cleans up empty networks every world "restart"
        }
        return data;
    }
    private static CompoundTag writeVec3(Vec3 vec) {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("x", vec.x);
        tag.putDouble("y", vec.y);
        tag.putDouble("z", vec.z);
        return tag;
    }

    private static Vec3 readVec3(CompoundTag tag) {
        double x = tag.getDouble("x");
        double y = tag.getDouble("y");
        double z = tag.getDouble("z");
        return new Vec3(x, y, z);
    }
}
