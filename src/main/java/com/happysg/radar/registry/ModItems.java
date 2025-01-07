package com.happysg.radar.registry;

import com.happysg.radar.CreateRadar;
import com.happysg.radar.compat.cbc.CBCItemsCompat;

import static com.happysg.radar.CreateRadar.REGISTRATE;

public class ModItems {

    public static void register() {
        CreateRadar.getLogger().info("Registering Items!");
        CBCItemsCompat.registerCBC(REGISTRATE);
    }
}
