package com.happysg.radar.item.radarilteritem.idfilterscreens;

import com.happysg.radar.CreateRadar;
import com.happysg.radar.registry.ModGuiTextures;
import com.happysg.radar.utils.screenelements.DynamicIconButton;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;

public class PlayerListScreen extends AbstractSimiScreen {


    protected ModGuiTextures background;
    protected DynamicIconButton friendfoe;
    protected DynamicIconButton remove;
    protected DynamicIconButton playeradd;
    protected EditBox playerentry;
    protected IconButton confirmButton;
    protected List<String> savedInputs = new ArrayList<>();

    public PlayerListScreen() {
        this.background = ModGuiTextures.PLAYER_LIST;
        }
    protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int x = guiLeft;
        int y = guiTop;

        background.render(graphics, x, y);
        MutableComponent header = Component.translatable(CreateRadar.MODID + ".player_list.title");
        graphics.drawString(font, header, x + background.width / 2 - font.width(header) / 2, y + 4, 0, false);

        PoseStack ms = graphics.pose();
        ms.pushPose();
        ms.translate(0, guiTop + 46, 0);
        ms.translate(0, 21, 0);
        ms.popPose();

        ms.pushPose();
        TransformStack.of(ms)
                .pushPose()
                .translate(x + background.width + 4, y + background.height + 4, 100)
                .scale(40)
                .rotateX(-22)
                .rotateY(63);
        ms.popPose();

    }

    protected void init() {
        setWindowSize(background.width, background.height);
        super.init();
        clearWidgets();


        friendfoe = new DynamicIconButton(guiLeft +156,guiTop+129,ModGuiTextures.ID_SMILE,ModGuiTextures.ID_FROWN,
                Component.translatable(CreateRadar.MODID + ".filter_isfriend"),
                Component.translatable(CreateRadar.MODID + ".filter_isfoe"),
                11,11);
        addRenderableWidget(friendfoe);
        remove =  new DynamicIconButton(guiLeft +168,guiTop+129,ModGuiTextures.ID_X,ModGuiTextures.ID_X,
                Component.translatable(CreateRadar.MODID + ".filter_remove"),
                Component.translatable(CreateRadar.MODID + ".filter_remove"),
                11,11);
        addRenderableWidget(remove);
        playeradd = new DynamicIconButton(guiLeft+188,guiTop+127, ModGuiTextures.ID_ADD,ModGuiTextures.ID_ADD,
                Component.translatable(CreateRadar.MODID + ".filter_add"),
                Component.translatable(CreateRadar.MODID + ".filter_add"),
                16,16);
        addRenderableWidget(playeradd);



    playerentry = new EditBox(font,guiLeft+22,guiTop+130,135,11,
                Component.translatable(CreateRadar.MODID + ".filter_insert_player_user"));
    playerentry.setMaxLength(16);
    playerentry.setFGColor(12895428);
    playerentry.setTextColor(16777215);
    playerentry.setAlpha(0xf0);
    playerentry.setBordered(false);
        addRenderableWidget(playerentry);


        confirmButton = new IconButton(guiLeft+192,guiTop+101, AllIcons.I_CONFIRM);
        confirmButton.withCallback(this::onClose);
        addRenderableWidget(confirmButton);


    }

    }

