package com.happysg.radar.compat.cbc;

import com.happysg.radar.CreateRadar;
import com.happysg.radar.compat.Mods;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.world.item.Rarity;

public class CBCItemsCompat {
    public static ItemEntry<GuidedFuzeItem> GUIDED_FUZE;

    public static void registerCBC(CreateRegistrate registrate) {
        if (!Mods.CREATEBIGCANNONS.isLoaded())
            return;
        CreateRadar.getLogger().info("Registering CBC Compat Items!");
        //conditionally register items, probably a bad idea
        GUIDED_FUZE = CreateRadar.REGISTRATE
                .item("guided_fuze", GuidedFuzeItem::new)
                .properties(properties -> properties.rarity(Rarity.EPIC))
                .register();
    }
}
