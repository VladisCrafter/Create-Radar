package com.happysg.radar.block.datalink.screens.idfilterscreens;

import com.happysg.radar.CreateRadar;
import com.simibubi.create.foundation.gui.widget.Indicator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class WhitelistStateIndicator extends Indicator {
    private final Component filterOn;
    private final Component fiterOff;

    public WhitelistStateIndicator(int x, int y, Component on, Component off) {
        super(x, y, Component.empty());
        this.filterOn = on;
        this.fiterOff = off;

        this.state = State.OFF;

    }
    @Override
    public List<Component> getToolTip() {
        Component.translatable(CreateRadar.MODID + ".radar_indicator.whitelistoff");
        Component.translatable(CreateRadar.MODID + ".radar_indicator.whiteliston");
        return List.of(state == State.OFF ? filterOn : fiterOff);


    }
    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int x = getX();
        int y = getY();
        ResourceLocation texture = (state == State.OFF)
                ? new ResourceLocation("radar", "textures/gui/indicator_on.png")
                : new ResourceLocation("radar", "textures/gui/indicator_off.png");

        graphics.blit(texture, x, y, 0, 0, 16, 16, 16, 16);
    }

}
