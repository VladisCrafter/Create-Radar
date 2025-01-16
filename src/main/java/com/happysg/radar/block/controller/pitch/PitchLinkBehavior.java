package com.happysg.radar.block.controller.pitch;

import com.happysg.radar.block.monitor.MonitorBlockEntity;
import com.happysg.radar.block.radar.link.RadarLinkBlockEntity;
import com.happysg.radar.block.radar.link.RadarLinkContext;
import com.happysg.radar.block.radar.link.RadarSource;
import com.happysg.radar.block.radar.link.RadarTarget;
import com.happysg.radar.block.radar.link.screens.AbstractRadarLinkScreen;
import com.happysg.radar.block.radar.link.screens.RadarTargetScreen;
import com.happysg.radar.block.radar.link.screens.TargetingConfig;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class PitchLinkBehavior extends RadarSource {

    @Override
    protected AbstractRadarLinkScreen getScreen(RadarLinkBlockEntity be) {
        return new RadarTargetScreen(be);
    }

    @Override
    protected void transferData(@NotNull RadarLinkContext context, @NotNull RadarTarget activeTarget) {
        if (!(context.getSourceBlockEntity() instanceof AutoPitchControllerBlockEntity controller))
            return;

        if (!(context.getTargetBlockEntity() instanceof MonitorBlockEntity))
            return;

        MonitorBlockEntity monitor = ((MonitorBlockEntity) context.getTargetBlockEntity()).getController();

        if (monitor == null)
            return;

        TargetingConfig targetingConfig = TargetingConfig.fromTag(context.sourceConfig().getCompound("targeting"));

        Vec3 targetPos = monitor.getTargetPos(targetingConfig);
        controller.setTarget(targetPos);
        controller.setTargetingConfig(targetingConfig);

    }
}
