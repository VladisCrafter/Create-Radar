package com.happysg.radar.block.controller.yaw;

import com.happysg.radar.block.datalink.DataController;
import com.happysg.radar.block.datalink.DataLinkBlockEntity;
import com.happysg.radar.block.datalink.DataLinkContext;
import com.happysg.radar.block.datalink.DataPeripheral;
import com.happysg.radar.block.datalink.screens.AbstractDataLinkScreen;
import com.happysg.radar.block.datalink.screens.TargetingConfig;
import com.happysg.radar.block.monitor.MonitorBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

public class YawLinkBehavior extends DataPeripheral {

    public void transferData(DataLinkContext context, @NotNull DataController activeTarget) {
        if (!(context.getSourceBlockEntity() instanceof AutoYawControllerBlockEntity controller))
            return;

        MonitorBlockEntity monitor = context.getMonitorBlockEntity();
        if (monitor == null)
            return;
        TargetingConfig targetingConfig = TargetingConfig.fromTag(context.sourceConfig());
        Vec3 targetPos = monitor.getTargetPos(targetingConfig);
        if (targetPos == null)
            return;
        controller.setTarget(targetPos);
    }

    @OnlyIn(value = Dist.CLIENT)
    @Override
    protected AbstractDataLinkScreen getScreen(DataLinkBlockEntity be) {
        return null;
    }
}
