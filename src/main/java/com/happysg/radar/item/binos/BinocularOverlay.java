package com.happysg.radar.item.binos;

import com.happysg.radar.CreateRadar;
import com.happysg.radar.item.binos.Binoculars;
import com.happysg.radar.registry.ModGuiTextures;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.SpyglassItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = CreateRadar.MODID)
public class BinocularOverlay {

    private static final ResourceLocation OVERLAY = ModGuiTextures.BINOCULAR_OVERLAY.location;

    @SubscribeEvent
    public static void onRenderOverlayPre(RenderGuiOverlayEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;


        if (!player.isUsingItem()) return;
        if (!(player.getUseItem().getItem() instanceof Binoculars)) return;

        if (event.getOverlay().id().equals(VanillaGuiOverlay.SPYGLASS.id())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderOverlayPost(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.VIGNETTE.id()))
            return;
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        if (!player.isUsingItem()) return;
        if (!(player.getUseItem().getItem() instanceof Binoculars)) return;

        int screenW = event.getWindow().getGuiScaledWidth();
        int screenH = event.getWindow().getGuiScaledHeight();

        // i match this to the actual pixel size of my binocular overlay png
        final int TEX_W = 512;
        final int TEX_H = 256;

        // base scale: fit to screen height (2:1 binocular aspect)
        float baseScale = (float) screenH / (float) TEX_H;

        // i reduce the size by ~20%
        float scale = baseScale * 0.87f;

        int drawW = (int) (TEX_W * scale);
        int drawH = (int) (TEX_H * scale);

        int x0 = (screenW - drawW) / 2;
        int y0 = (screenH - drawH) / 2;

        // black bars around the binocular overlay
        if (x0 > 0) {
            event.getGuiGraphics().fill(0, 0, x0, screenH, 0xFF000000);
            event.getGuiGraphics().fill(x0 + drawW, 0, screenW, screenH, 0xFF000000);
        }
        if (y0 > 0) {
            event.getGuiGraphics().fill(0, 0, screenW, y0, 0xFF000000);
            event.getGuiGraphics().fill(0, y0 + drawH, screenW, screenH, 0xFF000000);
        }

        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        //RenderSystem.defaultBlendFunc();
       // RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        var pose = event.getGuiGraphics().pose();
        pose.pushPose();

        // translate to center, then scale uniformly
        pose.translate(x0, y0, 0);
        pose.scale(scale, scale, 1f);

        // draw at native texture resolution to avoid UV stretching or tiling
        event.getGuiGraphics().blit(
                OVERLAY,
                0, 0,
                0, 0,
                TEX_W, TEX_H,
                TEX_W, TEX_H
        );

        pose.popPose();
        RenderSystem.disableBlend();
    }





}
