package com.happysg.radar.block.network;

import net.minecraft.core.BlockPos;

import java.util.*;

public class WeaponNetworkRegistry {
    private static final Hashtable<UUID, WeaponNetwork> networks = new Hashtable<>();

    public static void register(WeaponNetwork weaponNetwork) {
        networks.put(weaponNetwork.getUuid(), weaponNetwork);
    }

    public static WeaponNetwork get(UUID uuid) {
        return networks.get(uuid);
    }

    public static void remove(UUID uuid) {
        networks.remove(uuid);
    }

    public static WeaponNetwork networkContains(BlockPos pos){
        return  getAll().stream().filter(weaponNetwork -> weaponNetwork.contains(pos)).findFirst().orElse(null);
    }

    public static Collection<WeaponNetwork> getAll() {
        return networks.values();
    }

}
