package com.happysg.radar.block.radar.link;

import com.happysg.radar.block.radar.link.screens.AbstractRadarLinkScreen;
import org.jetbrains.annotations.NotNull;

public abstract class RadarSource extends RadarLinkBehavior {

    protected abstract AbstractRadarLinkScreen getScreen(RadarLinkBlockEntity be);

    protected abstract void transferData(@NotNull RadarLinkContext context, @NotNull RadarTarget activeTarget);
}
