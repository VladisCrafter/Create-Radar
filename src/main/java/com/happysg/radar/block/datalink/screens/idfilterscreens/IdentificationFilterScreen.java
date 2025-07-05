package com.happysg.radar.block.datalink.screens.idfilterscreens;

import com.happysg.radar.CreateRadar;
import com.happysg.radar.block.datalink.DataLinkBlockEntity;
import com.happysg.radar.block.datalink.screens.AbstractDataLinkScreen;
import com.happysg.radar.block.monitor.MonitorFilter;
import com.happysg.radar.registry.ModGuiTextures;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Indicator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class IdentificationFilterScreen extends AbstractDataLinkScreen {
    protected IconButton playerFilter;
    protected WhitelistStateIndicator playerwhitelistIndicator;




    public IdentificationFilterScreen(DataLinkBlockEntity be) {
        super(be);
        this.background = ModGuiTextures.DETECTION_FILTER;
        MonitorFilter monitorFilter = MonitorFilter.DEFAULT;
        if (be.getSourceConfig().contains("filter")) {
            monitorFilter = MonitorFilter.fromTag(be.getSourceConfig().getCompound("filter"));
        }
    }

    @Override
    protected void init(){
      /*  super.init();
        playerFilter = new IconButton(guiLeft + 32, guiTop + 38, ModGuiTextures.PLAYER_BUTTON);
        playerFilter.setToolTip(Component.translatable(CreateRadar.MODID + ".radar_button.player"));
        playerwhitelistIndicator = new WhitelistStateIndicator(guiLeft + 32, guiTop + 31, Component.empty());
        playerwhitelistIndicator.state = player ? Indicator.State.GREEN : Indicator.State.RED;
        playerFilter.withCallback((x, y) -> {
            Minecraft.getInstance().setScreen(new IdentificationFilterScreen(be));
            player = !player;
            playerwhitelistIndicator.state = player ? Indicator.State.GREEN : Indicator.State.RED;
        });
        addRenderableWidget(playerButton);
        addRenderableWidget(playerwhitelistIndicator);


       */

    }
    @Override
    protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.renderWindow(graphics, mouseX, mouseY, partialTicks);
    }

}
