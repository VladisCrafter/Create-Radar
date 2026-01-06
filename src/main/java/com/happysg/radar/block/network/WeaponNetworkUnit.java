package com.happysg.radar.block.network;

import net.minecraft.world.level.block.entity.BlockEntity;

public interface WeaponNetworkUnit {
    void setWeaponNetwork(WeaponNetwork network);
    WeaponNetwork getWeaponNetwork();
    BlockEntity getBlockEntity();

}
