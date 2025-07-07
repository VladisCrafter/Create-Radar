package com.happysg.radar.block.datalink.screens.idfilterscreens;

import com.happysg.radar.CreateRadar;
import com.happysg.radar.registry.ModGuiTextures;
import com.simibubi.create.foundation.gui.widget.Indicator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Map;

//TODO make more dymamic for more usecases
//TODO fix tooltips
public class WhitelistStateIndicator extends Indicator {

    private boolean isOn;
    private final Map<State, ModGuiTextures> stateTextures;
    private final Map<State, Component> tooltips;


    /**
     * @param x         screen X
     * @param y         screen Y
     * @param redTex  the ModGuiTextures entry to draw when ON
     * @param greenTex the ModGuiTextures entry to draw when OFF
     */
    public WhitelistStateIndicator(int x, int y,
                                   ModGuiTextures greenTex,
                                   ModGuiTextures redTex) {
        super(x, y, Component.empty());

        // Map each state to a texture
        this.stateTextures = Map.of(
                State.GREEN, greenTex,
                State.RED, redTex
        );

        // Optional: state-based tooltips
        this.tooltips = Map.of(
                State.ON,Component.translatable(CreateRadar.MODID + ".radar_tooltip.whitelist"),
                State.OFF,Component.translatable(CreateRadar.MODID + ".radar_tooltip.blacklist")
        );

        this.state = State.RED; // default
        this.setWidth(redTex.width);
        this.setHeight(redTex.height);
    }

    @Override
    public List<Component> getToolTip() {
        return List.of(tooltips.getOrDefault(state, Component.empty()));
    }
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks ) {
        if (!visible)
            return;
        ModGuiTextures toDraw;
        switch (state) {
            case ON: toDraw = ModGuiTextures.FILTER_ON; break;
            case OFF: toDraw = ModGuiTextures.FILTER_OFF; break;
            default: toDraw = ModGuiTextures.FILTER_ON; break;
        }
        toDraw.render(graphics, getX(), getY());
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        ModGuiTextures tex = stateTextures.getOrDefault(state, stateTextures.get(State.RED));
        graphics.blit(
                tex.location,
                getX(), getY(),
                tex.startX, tex.startY,
                tex.width, tex.height,
                tex.textureWidth, tex.textureHeight
        );
    }
}
