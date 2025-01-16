package com.happysg.radar.registry;

import com.happysg.radar.CreateRadar;
import com.jozufozu.flywheel.core.PartialModel;

public class ModPartials {

    public static final PartialModel RADAR_GLOW = block("radar_link/glow");
    public static final PartialModel RADAR_LINK_TUBE = block("radar_link/tube");

    private static PartialModel block(String path) {
        return new PartialModel(CreateRadar.asResource("block/" + path));
    }

    private static PartialModel entity(String path) {
        return new PartialModel(CreateRadar.asResource("entity/" + path));
    }

    public static void init() {
        // init static fields
    }
}
