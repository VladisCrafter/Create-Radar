package com.happysg.radar.block.datalink.screens;

import com.happysg.radar.CreateRadar;
import com.happysg.radar.block.datalink.DataLinkBlockEntity;
import com.happysg.radar.block.monitor.MonitorFilter;
import com.happysg.radar.registry.ModGuiTextures;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Indicator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.List;

public class RadarFilterScreen extends AbstractDataLinkScreen {

    boolean player;
    boolean vs2;
    boolean contraption;
    boolean mob;
    boolean projectile;

    protected IconButton playerButton;
    protected Indicator playerIndicator;
    protected IconButton vs2Button;
    protected Indicator vs2Indicator;
    protected IconButton contraptionButton;
    protected Indicator contraptionIndicator;
    protected IconButton mobButton;
    protected Indicator mobIndicator;
    protected IconButton projectileButton;
    protected Indicator projectileIndicator;

    List<String> playerBlacklist;
    List<String> playerWhitelist;
    List<String> vs2Whitelist;

    public RadarFilterScreen(DataLinkBlockEntity be) {
        super(be);
        this.background = ModGuiTextures.RADAR_FILTER;
        MonitorFilter monitorFilter = MonitorFilter.DEFAULT;
        if (be.getSourceConfig().contains("filter")) {
            monitorFilter = MonitorFilter.fromTag(be.getSourceConfig().getCompound("filter"));
        }
        player = monitorFilter.player();
        vs2 = monitorFilter.vs2();
        contraption = monitorFilter.contraption();
        mob = monitorFilter.mob();
        projectile = monitorFilter.projectile();
        playerBlacklist = monitorFilter.blacklistPlayers();
        playerWhitelist = monitorFilter.whitelistPlayers();
        vs2Whitelist = monitorFilter.whitelistVS();
    }


    @Override
    protected void init() {
        super.init();
        playerButton = new IconButton(guiLeft + 42, guiTop + 32, ModGuiTextures.PLAYER_BUTTON);
        playerButton.setToolTip(Component.translatable(CreateRadar.MODID + ".radar_button.player"));
        playerIndicator = new Indicator(guiLeft + 42, guiTop + 25, Component.empty());
        playerIndicator.state = player ? Indicator.State.GREEN : Indicator.State.RED;
        playerButton.withCallback((x, y) -> {
            player = !player;
            playerIndicator.state = player ? Indicator.State.GREEN : Indicator.State.RED;
        });
        addRenderableWidget(playerButton);
        addRenderableWidget(playerIndicator);

        vs2Button = new IconButton(guiLeft + 70, guiTop + 32, ModGuiTextures.VS2_BUTTON);
        vs2Button.setToolTip(Component.translatable(CreateRadar.MODID + ".radar_button.vs2"));
        vs2Indicator = new Indicator(guiLeft + 70, guiTop + 25, Component.empty());
        vs2Indicator.state = vs2 ? Indicator.State.GREEN : Indicator.State.RED;
        vs2Button.withCallback((x, y) -> {
            vs2 = !vs2;
            vs2Indicator.state = vs2 ? Indicator.State.GREEN : Indicator.State.RED;
        });
        addRenderableWidget(vs2Button);
        addRenderableWidget(vs2Indicator);

        contraptionButton = new IconButton(guiLeft + 98, guiTop + 32, ModGuiTextures.CONTRAPTION_BUTTON);
        contraptionButton.setToolTip(Component.translatable(CreateRadar.MODID + ".radar_button.contraption"));
        contraptionIndicator = new Indicator(guiLeft + 98, guiTop + 25, Component.empty());
        contraptionIndicator.state = contraption ? Indicator.State.GREEN : Indicator.State.RED;
        contraptionButton.withCallback((x, y) -> {
            contraption = !contraption;
            contraptionIndicator.state = contraption ? Indicator.State.GREEN : Indicator.State.RED;
        });
        addRenderableWidget(contraptionButton);
        addRenderableWidget(contraptionIndicator);

        mobButton = new IconButton(guiLeft + 126, guiTop + 32, ModGuiTextures.MOB_BUTTON);
        mobButton.setToolTip(Component.translatable(CreateRadar.MODID + ".radar_button.mob"));
        mobIndicator = new Indicator(guiLeft + 126, guiTop + 25, Component.empty());
        mobIndicator.state = mob ? Indicator.State.GREEN : Indicator.State.RED;
        mobButton.withCallback((x, y) -> {
            mob = !mob;
            mobIndicator.state = mob ? Indicator.State.GREEN : Indicator.State.RED;
        });
        addRenderableWidget(mobButton);
        addRenderableWidget(mobIndicator);

        projectileButton = new IconButton(guiLeft + 154, guiTop + 32, ModGuiTextures.PROJECTILE_BUTTON);
        projectileButton.setToolTip(Component.translatable(CreateRadar.MODID + ".radar_button.projectile"));
        projectileIndicator = new Indicator(guiLeft + 154, guiTop + 25, Component.empty());
        projectileIndicator.state = projectile ? Indicator.State.GREEN : Indicator.State.RED;
        projectileButton.withCallback((x, y) -> {
            projectile = !projectile;
            projectileIndicator.state = projectile ? Indicator.State.GREEN : Indicator.State.RED;
        });
        addRenderableWidget(projectileButton);
        addRenderableWidget(projectileIndicator);

    }

    @Override
    protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.renderWindow(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void onClose(CompoundTag tag) {
        super.onClose(tag);
        MonitorFilter monitorFilter = new MonitorFilter(player, vs2, contraption, mob, projectile, playerBlacklist, playerWhitelist, List.of(), vs2Whitelist);
        tag.put("filter", monitorFilter.toTag());
    }
}
