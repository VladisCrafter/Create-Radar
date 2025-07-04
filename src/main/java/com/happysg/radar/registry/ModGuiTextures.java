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

    RADAR_FILTER("filter", 219, 113),
    CANNON_TARGETING("targeting", 256, 120),
    PLAYER_BUTTON("filter", 43, 33, 18, 18),
    VS2_BUTTON("filter", 71, 33, 18, 18),
    CONTRAPTION_BUTTON("filter", 99, 33, 18, 18),
    MOB_BUTTON("filter", 127, 33, 18, 18),
    PROJECTILE_BUTTON("filter", 155, 33, 18, 18),
    ANIMAL_BUTTON("targeting", 127, 27, 18, 18),
    X("targeting", 23, 123, 18, 18),
    CHECKMARK("targeting", 3, 123, 18, 18),
    AUTO_TARGET("targeting", 203, 48, 18, 18),
    AUTO_FIRE("targeting", 44, 124, 18, 18),
    MANUAL_FIRE("targeting", 64, 124, 18, 18),
    ID_SCREEN("id_block", 225, 95),
    PLAYER_INPUT("radar_iff_list", 0, 136, 225, 120),
    VS2_INPUT("radar_iff_list", 0, 0, 225, 120),
    FRIENDLY("radar_iff_list", 238, 18, 18, 18),
    HOSTILE("radar_iff_list", 238, 0, 18, 18),
    SHALLOW_MODE("targeting", 104, 124, 18, 18),
    ARTILLERY_MODE("targeting", 84, 124, 18, 18);

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
