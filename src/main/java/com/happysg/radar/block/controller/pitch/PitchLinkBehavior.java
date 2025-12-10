package com.happysg.radar.block.controller.pitch;

import com.happysg.radar.block.datalink.DataController;
import com.happysg.radar.block.datalink.DataLinkBlockEntity;
import com.happysg.radar.block.datalink.DataLinkContext;
import com.happysg.radar.block.datalink.DataPeripheral;
import com.happysg.radar.block.datalink.screens.AbstractDataLinkScreen;
import com.happysg.radar.block.datalink.screens.AutoTargetScreen;
import com.happysg.radar.block.datalink.screens.TargetingConfig;
import com.happysg.radar.block.monitor.MonitorBlockEntity;
import com.happysg.radar.block.radar.track.RadarTrack;
import com.happysg.radar.config.RadarConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlockEntity;

public class PitchLinkBehavior extends DataPeripheral {

    @OnlyIn(value = Dist.CLIENT)
    @Override
    protected AbstractDataLinkScreen getScreen(DataLinkBlockEntity be) {
        return new AutoTargetScreen(be);
    }

    @Override
    protected void transferData(@NotNull DataLinkContext context, @NotNull DataController activeTarget) {
        if (!(context.getSourceBlockEntity() instanceof AutoPitchControllerBlockEntity controller))
            return;

        if (context.getMonitorBlockEntity() == null || context.level().isClientSide())
            return;

        MonitorBlockEntity monitor = context.getMonitorBlockEntity();
        TargetingConfig targetingConfig = TargetingConfig.fromTag(context.sourceConfig());

        BlockPos mountBlock = controller.getMount();
        Vec3 targetPos = monitor.getTargetPos(targetingConfig);
        RadarTrack track = monitor.getactivetrack();
        //todo better way to handle instead of passing nlul to stop firing
        monitor.getMount(mountBlock);
        controller.setTrack(track);
        controller.setSafeZones(monitor.safeZones);
        controller.setTarget(targetPos);
        controller.setFiringTarget(targetPos, targetingConfig);

    }
}
