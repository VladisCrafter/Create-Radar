package com.happysg.radar.networking.networkhandlers;

import com.happysg.radar.block.behavior.networks.config.IdentificationConfig;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ListNBTHandler {

    // Legacy keys (old format)
    private static final String ENTRIES_KEY = "EntriesList";
    private static final String FRIEND_OR_FOE_KEY = "FriendOrFoeList";
    private static final String SINGLE_KEY = "IDSTRING";

    // New format root
    private static final String FILTERS_ROOT = "Filters";
    private static final String IDENT_KEY = "identification";

    /** New write: saves the username list + friend/foe list into Filters.identification */
    public static void saveToHeldItem(Player player, List<String> entries, List<Boolean> friendOrFoe) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) return;

        CompoundTag root = stack.getOrCreateTag();
        CompoundTag filters = root.contains(FILTERS_ROOT, Tag.TAG_COMPOUND) ? root.getCompound(FILTERS_ROOT) : new CompoundTag();

        // preserve existing label if present
        IdentificationConfig existing = filters.contains(IDENT_KEY, Tag.TAG_COMPOUND)
                ? IdentificationConfig.fromTag(filters.getCompound(IDENT_KEY))
                : IdentificationConfig.DEFAULT;

        IdentificationConfig cfg = new IdentificationConfig(entries, friendOrFoe, existing.label());
        filters.put(IDENT_KEY, cfg.toTag());
        root.put(FILTERS_ROOT, filters);

        // Optional: keep legacy keys in sync for a version or two (remove later)
        // writeLegacy(root, entries, friendOrFoe);

        stack.setTag(root);
    }

    /** New write: saves the string into Filters.identification.label */
    public static void saveStringToHeldItem(Player player, String value) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) return;

        CompoundTag root = stack.getOrCreateTag();
        CompoundTag filters = root.contains(FILTERS_ROOT, Tag.TAG_COMPOUND) ? root.getCompound(FILTERS_ROOT) : new CompoundTag();

        IdentificationConfig existing = filters.contains(IDENT_KEY, Tag.TAG_COMPOUND)
                ? IdentificationConfig.fromTag(filters.getCompound(IDENT_KEY))
                : IdentificationConfig.DEFAULT;

        IdentificationConfig cfg = new IdentificationConfig(existing.usernames(), existing.friendly(), value);
        filters.put(IDENT_KEY, cfg.toTag());
        root.put(FILTERS_ROOT, filters);

        stack.setTag(root);
    }

    /** Loads list data for your screen. Reads new format first, falls back to legacy keys. */
    public static LoadedLists loadFromHeldItem(Player player) {
        ItemStack stack = player.getMainHandItem();
        LoadedLists loaded = new LoadedLists();
        if (stack.isEmpty() || !stack.hasTag()) return loaded;

        CompoundTag root = stack.getTag();
        if (root == null) return loaded;

        // New format first
        if (root.contains(FILTERS_ROOT, Tag.TAG_COMPOUND)) {
            CompoundTag filters = root.getCompound(FILTERS_ROOT);
            if (filters.contains(IDENT_KEY, Tag.TAG_COMPOUND)) {
                IdentificationConfig cfg = IdentificationConfig.fromTag(filters.getCompound(IDENT_KEY));
                loaded.entries.addAll(cfg.usernames());
                loaded.friendOrFoe.addAll(cfg.friendly());
                return loaded;
            }
        }

        // Legacy fallback
        ListTag entriesTag = root.getList(ENTRIES_KEY, Tag.TAG_STRING);
        for (int i = 0; i < entriesTag.size(); i++) loaded.entries.add(entriesTag.getString(i));

        ListTag foeTag = root.getList(FRIEND_OR_FOE_KEY, Tag.TAG_BYTE);
        for (int i = 0; i < foeTag.size(); i++) loaded.friendOrFoe.add(foeTag.getInt(i) != 0);

        return loaded;
    }

    /** Loads the ID string for your other UI (new format first, legacy fallback). */
    public static String loadStringFromHeldItem(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !stack.hasTag()) return "";

        CompoundTag root = stack.getTag();
        if (root == null) return "";

        // New format first
        if (root.contains(FILTERS_ROOT, Tag.TAG_COMPOUND)) {
            CompoundTag filters = root.getCompound(FILTERS_ROOT);
            if (filters.contains(IDENT_KEY, Tag.TAG_COMPOUND)) {
                return IdentificationConfig.fromTag(filters.getCompound(IDENT_KEY)).label();
            }
        }

        // Legacy fallback
        return root.getString(SINGLE_KEY);
    }

    public static class LoadedLists {
        public final List<String> entries = new ArrayList<>();
        public final List<Boolean> friendOrFoe = new ArrayList<>();
    }
}
