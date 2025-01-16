package com.happysg.radar.block.radar.bearing;

import com.happysg.radar.block.monitor.MonitorBlockEntity;
import com.happysg.radar.block.monitor.MonitorFilter;
import com.happysg.radar.block.radar.link.RadarLinkBlockEntity;
import com.happysg.radar.block.radar.link.RadarLinkContext;
import com.happysg.radar.block.radar.link.RadarSource;
import com.happysg.radar.block.radar.link.RadarTarget;
import com.happysg.radar.block.radar.link.screens.AbstractRadarLinkScreen;
import com.happysg.radar.block.radar.link.screens.RadarFilterScreen;
import org.jetbrains.annotations.NotNull;

public class RadarBearingLinkBehavior extends RadarSource {

    public void transferData(@NotNull RadarLinkContext context, @NotNull RadarTarget activeTarget) {
        if (context.getTargetBlockEntity() instanceof MonitorBlockEntity monitorBlockEntity) {
            if (context.sourceConfig().contains("filter")) {
                monitorBlockEntity.setFilter(MonitorFilter.fromTag(context.sourceConfig().getCompound("filter")));
            }
            monitorBlockEntity.setRadarPos(context.getSourcePos());
        }
    }

    @Override
    protected AbstractRadarLinkScreen getScreen(RadarLinkBlockEntity be) {
        return new RadarFilterScreen(be);
    }
}
