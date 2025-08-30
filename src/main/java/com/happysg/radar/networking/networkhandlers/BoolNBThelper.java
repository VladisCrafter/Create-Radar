package com.happysg.radar.networking.networkhandlers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.BitSet;
import java.util.List;

public final class BoolNBThelper {
    private BoolNBThelper() {}
    // Save flags as a byte[] under `key`
    public static void saveBooleansAsBytes(ItemStack stack, boolean[] flags, String key) {
        if (stack == null || flags == null || key == null) return;
        CompoundTag tag = stack.getOrCreateTag();
        byte[] arr = new byte[flags.length];
        for (int i = 0; i < flags.length; i++) arr[i] = (byte) (flags[i] ? 1 : 0);
        tag.putByteArray(key, arr);
    }

    // Load flags (returns array of length expectedLength)
    public static boolean[] loadBooleansFromBytes(ItemStack stack, String key, int expectedLength) {
        boolean[] res = new boolean[Math.max(0, expectedLength)];
        if (stack == null || key == null || expectedLength <= 0) return res;
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(key)) return res;
        try {
            byte[] arr = tag.getByteArray(key);
            int len = Math.min(arr.length, res.length);
            for (int i = 0; i < len; i++) res[i] = arr[i] != 0;
        } catch (Throwable t) { /* defensive; return defaults */ }
        return res;
    }

    // Copy into an existing dest array (keeps remaining values unchanged if tag shorter)
    public static void loadBooleansInto(ItemStack stack, String key, boolean[] dest) {
        if (dest == null) return;
        boolean[] tmp = loadBooleansFromBytes(stack, key, dest.length);
        System.arraycopy(tmp, 0, dest, 0, Math.min(tmp.length, dest.length));
    }
}

