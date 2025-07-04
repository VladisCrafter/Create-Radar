package com.happysg.radar.block.datalink.screens;

import com.happysg.radar.CreateRadar;
import com.happysg.radar.block.datalink.DataLinkBlockEntity;
import com.happysg.radar.registry.ModGuiTextures;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Indicator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

public class AutoTargetScreen extends AbstractDataLinkScreen {

    boolean player;
    boolean contraption;
    boolean mob;
    boolean animal;
    boolean projectile;
    boolean autoTarget;
    boolean autoFire;
    boolean artilleryMode;

    protected IconButton playerButton;
    protected Indicator playerIndicator;
    protected IconButton contraptionButton;
    protected Indicator contraptionIndicator;
    protected IconButton mobButton;
    protected Indicator mobIndicator;
    protected IconButton animalButton;
    protected Indicator animalIndicator;
    protected IconButton projectileButton;
    protected Indicator projectileIndicator;
    protected IconButton autoTargetButton;
    protected Indicator autoTargetIndicator;
    protected IconButton autoFireButton;
    protected IconButton artilleryModeButton;


    public AutoTargetScreen(DataLinkBlockEntity be) {
        super(be);
        this.background = ModGuiTextures.CANNON_TARGETING;
        TargetingConfig targetingConfig = TargetingConfig.DEFAULT;
        if (be.getSourceConfig().contains("targeting")) {
            targetingConfig = TargetingConfig.fromTag(be.getSourceConfig());
        }
        player = targetingConfig.player();
        contraption = targetingConfig.contraption();
        mob = targetingConfig.mob();
        animal = targetingConfig.animal();
        projectile = targetingConfig.projectile();
        autoTarget = targetingConfig.autoTarget();
        autoFire = targetingConfig.autoFire();
        artilleryMode = targetingConfig.artilleryMode();
    }


    @Override
    protected void init() {
        super.init();
        playerButton = new IconButton(guiLeft + 42, guiTop + 26, ModGuiTextures.PLAYER_BUTTON);
        playerButton.setToolTip(Component.translatable(CreateRadar.MODID + ".radar_button.player"));
        playerIndicator = new Indicator(guiLeft + 42, guiTop + 19, Component.empty());
        playerIndicator.state = player ? Indicator.State.GREEN : Indicator.State.RED;
        playerButton.withCallback((x, y) -> {
            player = !player;
            playerIndicator.state = player ? Indicator.State.GREEN : Indicator.State.RED;
        });
        addRenderableWidget(playerButton);
        addRenderableWidget(playerIndicator);
        contraptionButton = new IconButton(guiLeft + 70, guiTop + 26, ModGuiTextures.CONTRAPTION_BUTTON);
        contraptionButton.setToolTip(Component.translatable(CreateRadar.MODID + ".radar_button.contraption"));
        contraptionIndicator = new Indicator(guiLeft + 70, guiTop + 19, Component.empty());
        contraptionIndicator.state = contraption ? Indicator.State.GREEN : Indicator.State.RED;
        contraptionButton.withCallback((x, y) -> {
            contraption = !contraption;
            contraptionIndicator.state = contraption ? Indicator.State.GREEN : Indicator.State.RED;
        });
        addRenderableWidget(contraptionButton);
        addRenderableWidget(contraptionIndicator);

        mobButton = new IconButton(guiLeft + 98, guiTop + 26, ModGuiTextures.MOB_BUTTON);
        mobButton.setToolTip(Component.translatable(CreateRadar.MODID + ".radar_button.hostile"));
        mobIndicator = new Indicator(guiLeft + 98, guiTop + 19, Component.empty());
        mobIndicator.state = mob ? Indicator.State.GREEN : Indicator.State.RED;
        mobButton.withCallback((x, y) -> {
            mob = !mob;
            mobIndicator.state = mob ? Indicator.State.GREEN : Indicator.State.RED;
        });
        addRenderableWidget(mobButton);
        addRenderableWidget(mobIndicator);

        animalButton = new IconButton(guiLeft + 126, guiTop + 26, ModGuiTextures.ANIMAL_BUTTON);
        animalButton.setToolTip(Component.translatable(CreateRadar.MODID + ".radar_button.animal"));
        animalIndicator = new Indicator(guiLeft + 126, guiTop + 19, Component.empty());
        animalIndicator.state = animal ? Indicator.State.GREEN : Indicator.State.RED;
        animalButton.withCallback((x, y) -> {
            animal = !animal;
            animalIndicator.state = animal ? Indicator.State.GREEN : Indicator.State.RED;
        });
        addRenderableWidget(animalButton);
        addRenderableWidget(animalIndicator);

        projectileButton = new IconButton(guiLeft + 154, guiTop + 26, ModGuiTextures.PROJECTILE_BUTTON);
        projectileButton.setToolTip(Component.translatable(CreateRadar.MODID + ".radar_button.projectile"));
        projectileIndicator = new Indicator(guiLeft + 154, guiTop + 19, Component.empty());
        projectileIndicator.state = projectile ? Indicator.State.GREEN : Indicator.State.RED;
        projectileButton.withCallback((x, y) -> {
            projectile = !projectile;
            projectileIndicator.state = projectile ? Indicator.State.GREEN : Indicator.State.RED;
        });
        addRenderableWidget(projectileButton);
        addRenderableWidget(projectileIndicator);

        autoTargetButton = new IconButton(guiLeft + 202, guiTop + 47, ModGuiTextures.AUTO_TARGET);
        autoTargetButton.setToolTip(Component.translatable(CreateRadar.MODID + ".radar_button.auto_target"));
        autoTargetIndicator = new Indicator(guiLeft + 202, guiTop + 40, Component.empty());
        autoTargetIndicator.state = autoTarget ? Indicator.State.GREEN : Indicator.State.RED;
        autoTargetButton.withCallback((x, y) -> {
            autoTarget = !autoTarget;
            autoTargetIndicator.state = autoTarget ? Indicator.State.GREEN : Indicator.State.RED;
        });
        addRenderableWidget(autoTargetButton);
        addRenderableWidget(autoTargetIndicator);

        autoFireButton = new IconButton(guiLeft + 98, guiTop + 69, autoFire ? ModGuiTextures.AUTO_FIRE : ModGuiTextures.MANUAL_FIRE);
        autoFireButton.setToolTip(Component.translatable(CreateRadar.MODID + ".radar_button.auto_fire"));
        autoFireButton.withCallback((x, y) -> {
            autoFire = !autoFire;
            autoFireButton.setIcon(autoFire ? ModGuiTextures.AUTO_FIRE : ModGuiTextures.MANUAL_FIRE);
        });
        addRenderableWidget(autoFireButton);

        artilleryModeButton = new IconButton(guiLeft + 126, guiTop + 69, artilleryMode ? ModGuiTextures.ARTILLERY_MODE : ModGuiTextures.SHALLOW_MODE);
        artilleryModeButton.setToolTip(Component.translatable(CreateRadar.MODID + ".radar_button.artillery_mode"));
        artilleryModeButton.withCallback((x, y) -> {
            artilleryMode = !artilleryMode;
            artilleryModeButton.setIcon(artilleryMode ? ModGuiTextures.ARTILLERY_MODE : ModGuiTextures.SHALLOW_MODE);
        });
        addRenderableWidget(artilleryModeButton);

    }

    @Override
    public void onClose(CompoundTag tag) {
        super.onClose(tag);
        TargetingConfig targetingConfig = new TargetingConfig(player, contraption, mob, animal, projectile, autoTarget, autoFire, artilleryMode);
        tag.put("targeting", targetingConfig.toTag());
    }
}
