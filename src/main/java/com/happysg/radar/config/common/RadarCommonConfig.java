package com.happysg.radar.config.common;

import com.simibubi.create.foundation.config.ConfigBase;

public class RadarCommonConfig extends ConfigBase {
    @Override
    public String getName() {
        return "Radar Common";
    }

    public ConfigInt commonTest = i(1, 0, 100, "commonTest", "This is a test value for the common config.");
}
