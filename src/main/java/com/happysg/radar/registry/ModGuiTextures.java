package com.happysg.radar.registry;

import com.happysg.radar.CreateRadar;
import com.mojang.blaze3d.systems.RenderSystem;
import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.gui.element.ScreenElement;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public enum ModGuiTextures implements ScreenElement {


    TARGETING_FILTER("targeting_filter",224,107),
    PLAYER_BUTTON("targeting_filter",23,44,16,16),
    VS2_BUTTON("targeting_filter", 43,44,16,16),
    MOB_BUTTON("targeting_filter",63,44,16,16),
    ANIMAL_BUTTON("targeting_filter",83,44,16,16),
    PROJECTILE_BUTTON("targeting_filter",103,44,16,16),
    LOS_BUTTON("targeting_filter",123,44,16,16),
    AUTO_TARGET("targeting_filter",171,43,16,16),
    CHECK("targeting_filter",192,84,16,16),
    DETECTION_FILTER("detection_filter",256,96),
    CONTRAPTION_BUTTON("detection_filter",89,39,16,16),
    MISSILE_BUTTON("detection_filter", 173,39,16,16),
    ITEM_BUTTON("detection_filter",201,39,16,16)
    ;

    public static final int FONT_COLOR = 0x575F7A;

    public final ResourceLocation location;
    public final int width, height;
    public final int startX, startY;
    public final int textureWidth, textureHeight;

    ModGuiTextures(String location, int width, int height) {
        this(location, 0, 0, width, height);
    }

    ModGuiTextures(int startX, int startY) {
        this("icons", startX * 16, startY * 16, 16, 16);
    }

    ModGuiTextures(String location, int startX, int startY, int width, int height) {
        this(CreateRadar.MODID, location, startX, startY, width, height, 256, 256);
    }

    ModGuiTextures(String namespace, String location, int startX, int startY, int width, int height, int textureWidth, int textureHeight) {
        this.location = new ResourceLocation(namespace, "textures/gui/" + location + ".png");
        this.width = width;
        this.height = height;
        this.startX = startX;
        this.startY = startY;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
    }

    @OnlyIn(Dist.CLIENT)
    public void bind() {
        RenderSystem.setShaderTexture(0, location);
    }

    @OnlyIn(Dist.CLIENT)
    public void render(GuiGraphics graphics, int x, int y) {
        graphics.blit(location, x, y, startX, startY, width, height);
    }

    @OnlyIn(Dist.CLIENT)
    public void render(GuiGraphics graphics, int x, int y, Color c) {
        bind();
        UIRenderHelper.drawColoredTexture(graphics, c, x, y, startX, startY, width, height);
    }
}
