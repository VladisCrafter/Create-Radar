package com.happysg.radar.block.controller.id;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.valkyrienskies.core.api.ships.Ship;

import java.util.HashMap;
import java.util.Map;

public class IDManager extends SavedData {

    public static final IDManager INSTANCE = new IDManager();
    public static final Map<String, IDRecord> ID_RECORDS = new HashMap<>();

    public record IDRecord(String name, String secretID) {
    }

    public static void addIDRecord(String shipSlug, String secretID, String name) {
        ID_RECORDS.put(shipSlug, new IDRecord(name, secretID));
    }

    public static void registerIDRecord(Ship ship, String name, String secretID) {
        ID_RECORDS.put(ship.getSlug(), new IDRecord(name, secretID));
    }

    public static void removeIDRecord(Ship ship) {
        ID_RECORDS.remove(ship.getSlug());
    }

    public static IDRecord getIDRecordByShip(Ship ship) {
        return ID_RECORDS.get(ship.getSlug());
    }

    public static IDRecord getIDRecordByShipSlug(String shipSlug) {
        return ID_RECORDS.get(shipSlug);
    }


    public static IDManager load(CompoundTag pCompoundTag) {
        if (!pCompoundTag.contains("idRecords")) return INSTANCE;
        ListTag listTag = pCompoundTag.getList("idRecords", Tag.TAG_COMPOUND);
        for (int i = 0; i < listTag.size(); i++) {
            CompoundTag compoundTag = listTag.getCompound(i);
            String shipSlug = compoundTag.getString("shipSlug");
            String name = compoundTag.getString("name");
            String secretID = compoundTag.getString("secretID");
            ID_RECORDS.put(shipSlug, new IDRecord(name, secretID));
        }
        return INSTANCE;
    }

    @Override
    public CompoundTag save(CompoundTag pCompoundTag) {
        ListTag listTag = new ListTag();
        for (Map.Entry<String, IDRecord> entry : ID_RECORDS.entrySet()) {
            CompoundTag compoundTag = new CompoundTag();
            compoundTag.putString("name", entry.getValue().name());
            compoundTag.putString("secretID", entry.getValue().secretID());
            compoundTag.putString("shipSlug", entry.getKey());
            listTag.add(compoundTag);
        }
        pCompoundTag.put("idRecords", listTag);
        return pCompoundTag;
    }

    public static void load(MinecraftServer server) {
        server.overworld()
                .getDataStorage()
                .computeIfAbsent(IDManager::load, () -> INSTANCE, "create_radar_vs2_ids");
    }
}
