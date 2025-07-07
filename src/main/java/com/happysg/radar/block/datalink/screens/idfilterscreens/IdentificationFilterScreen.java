package com.happysg.radar.block.datalink.screens.idfilterscreens;
import com.happysg.radar.CreateRadar;
import com.happysg.radar.block.datalink.DataLinkBlockEntity;
import com.happysg.radar.block.datalink.screens.AbstractDataLinkScreen;
import com.happysg.radar.registry.ModGuiTextures;
import com.simibubi.create.foundation.gui.widget.IconButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import static com.happysg.radar.registry.ModGuiTextures.FILTER_OFF;
import static com.happysg.radar.registry.ModGuiTextures.FILTER_ON;
//TODO move away form AbstractDataLinkScreen. abstractsimiscreen?
public class IdentificationFilterScreen extends AbstractDataLinkScreen {
    boolean whitelist_player;
    boolean whitelist_ship;



    protected IconButton playerlistbutton;
    protected IconButton  playerFilter;
    protected IconButton shipbutton;
    protected IconButton shipFilter;
    protected WhitelistStateIndicator shiplistIndicator;
    protected WhitelistStateIndicator playerwhitelistIndicator;
    private final DataLinkBlockEntity be;
    public IdentificationFilterScreen(DataLinkBlockEntity be) {
        super(be);
        this.be = be;
        this.background = ModGuiTextures.IDENT_FILTER;
    }

    @Override
    protected void init() {
        super.init();
        playerFilter = new IconButton(guiLeft + 41, guiTop + 25, ModGuiTextures.FILTER_BUTTON);
        playerFilter.setToolTip(Component.translatable(CreateRadar.MODID + ".radar_button.player"));
        playerFilter.withCallback((x, y) -> {
            Minecraft.getInstance().setScreen(new PlayerListScreen());

        });
        addRenderableWidget(playerFilter);

        playerlistbutton = new IconButton(guiLeft + 64,guiTop +26,ModGuiTextures.INVIS);
        playerwhitelistIndicator = new WhitelistStateIndicator(guiLeft + 64, guiTop + 26,FILTER_OFF,FILTER_ON);
        playerwhitelistIndicator.state = whitelist_player ? WhitelistStateIndicator.State.ON: WhitelistStateIndicator.State.OFF;
        playerwhitelistIndicator.getToolTip();
        playerwhitelistIndicator.withCallback((x, y) -> {
            whitelist_player = !whitelist_player;
            playerwhitelistIndicator.state = whitelist_player ? WhitelistStateIndicator.State.ON : WhitelistStateIndicator.State.OFF;
        });
        addRenderableWidget(playerwhitelistIndicator);





        shipFilter = new IconButton(guiLeft + 124, guiTop + 25, ModGuiTextures.FILTER_BUTTON );
        shipFilter.setToolTip(Component.translatable(CreateRadar.MODID + ".radar_button.player"));
        shipFilter.withCallback((x, y) -> {
            Minecraft.getInstance().setScreen(new ShipListScreen());

        });
        addRenderableWidget(shipFilter);
//TODO fix tool tips
        shipbutton = new IconButton(guiLeft + 147,guiTop +25,ModGuiTextures.INVIS);
        shiplistIndicator = new WhitelistStateIndicator(guiLeft + 147, guiTop + 26,FILTER_OFF,FILTER_ON);
        shiplistIndicator.state = whitelist_ship ? WhitelistStateIndicator.State.ON: WhitelistStateIndicator.State.OFF;
        shiplistIndicator.getToolTip();
        shiplistIndicator.withCallback((x, y) -> {
            whitelist_ship = !whitelist_ship;
            shiplistIndicator.state = whitelist_ship ? WhitelistStateIndicator.State.ON : WhitelistStateIndicator.State.OFF;
        });
        addRenderableWidget(shiplistIndicator);
    }

    @Override
    protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.renderWindow(graphics, mouseX, mouseY, partialTicks);
    }
    //TODO add state save


}
