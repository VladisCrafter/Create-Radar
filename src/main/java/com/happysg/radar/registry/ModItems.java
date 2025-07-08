package com.happysg.radar.registry;

import com.happysg.radar.CreateRadar;
import com.happysg.radar.item.radarilteritem.RadarFilterItem;
import com.happysg.radar.item.SafeZoneDesignatorItem;
import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.util.entry.ItemEntry;

import static com.happysg.radar.CreateRadar.REGISTRATE;

public class ModItems {

    public static final ItemEntry<SafeZoneDesignatorItem> SAFE_ZONE_DESIGNATOR = REGISTRATE.item("radar_safe_zone_designator", SafeZoneDesignatorItem::new)
            .register();
    public static final ItemEntry<RadarFilterItem> RADAR_FILTER_ITEM = REGISTRATE.item("radar_filter_item", RadarFilterItem::new )
            .register();

    public static void register() {
        CreateRadar.getLogger().info("Registering Items!");
    }
}
