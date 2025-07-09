package com.happysg.radar.block.radar.behavior;

import com.happysg.radar.block.datalink.DataController;
import com.happysg.radar.block.datalink.DataLinkBlockEntity;
import com.happysg.radar.block.datalink.DataLinkContext;
import com.happysg.radar.block.datalink.DataPeripheral;
import com.happysg.radar.block.datalink.screens.AbstractDataLinkScreen;
import com.happysg.radar.item.detectionfilter.screens.RadarFilterScreen;
import com.happysg.radar.block.monitor.MonitorBlockEntity;
import com.happysg.radar.block.monitor.MonitorFilter;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

public  class RadarScannerLinkBehavior extends DataPeripheral {

    public void transferData(@NotNull DataLinkContext context, @NotNull DataController activeTarget) {
        if (context.level().isClientSide())
            return;
        if (context.getSourceBlockEntity() instanceof SmartBlockEntity smartBlockEntity) {
            RadarScanningBlockBehavior behavior = smartBlockEntity.getBehaviour(RadarScanningBlockBehavior.TYPE);
            if (behavior != null && context.getMonitorBlockEntity() != null) {
                MonitorBlockEntity monitorBlockEntity = context.getMonitorBlockEntity();
                    monitorBlockEntity.getController().setRadarPos(context.getSourcePos());
                    if (context.sourceConfig().contains("filter")) {
                        monitorBlockEntity.setFilter(MonitorFilter.fromTag(context.sourceConfig().getCompound("filter")));
                    }
                    monitorBlockEntity.getController().updateCache();
            }
        }
    }


    @OnlyIn(value = Dist.CLIENT)
    @Override
    protected AbstractDataLinkScreen getScreen(DataLinkBlockEntity be) {
        return null;
        //return new RadarFilterScreen(be);
    }


}
