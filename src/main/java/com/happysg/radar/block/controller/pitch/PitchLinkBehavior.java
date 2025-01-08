package com.happysg.radar.block.controller.pitch;

import com.happysg.radar.CreateRadar;
import com.happysg.radar.block.monitor.MonitorBlockEntity;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTarget;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

public class PitchLinkBehavior extends DisplaySource {

    @Override
    public List<MutableComponent> provideText(DisplayLinkContext context, DisplayTargetStats stats) {
        return List.of();
    }

    @Override
    public int getPassiveRefreshTicks() {
        return 1;
    }

    @Override
    public void transferData(DisplayLinkContext context, DisplayTarget activeTarget, int line) {
        super.transferData(context, activeTarget, line);
        if (!(context.getSourceBlockEntity() instanceof AutoPitchControllerBlockEntity controller))
            return;

        if (!(context.getTargetBlockEntity() instanceof MonitorBlockEntity))
            return;

        MonitorBlockEntity monitor = ((MonitorBlockEntity) context.getTargetBlockEntity()).getController();

        if (monitor == null)
            return;

        Vec3 targetPos = monitor.getTargetPos();
        controller.setTarget(targetPos);
        controller.chargeCount = context.blockEntity().getSourceConfig().getInt("charge");
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void initConfigurationWidgets(DisplayLinkContext context, ModularGuiLineBuilder builder, boolean isFirstLine) {
        if (isFirstLine)
            addFilterConfig(builder);
    }

    @OnlyIn(Dist.CLIENT)
    protected void addFilterConfig(ModularGuiLineBuilder builder) {
        builder.addSelectionScrollInput(0, 100,
                (si, l) -> si
                        .forOptions(List.of(Component.literal("0"), Component.literal("1"), Component.literal("2"), Component.literal("3"), Component.literal("4")))
                        .titled(Component.translatable(CreateRadar.MODID + ".pitch.powder_charge"))
                , "charge");
    }
}
