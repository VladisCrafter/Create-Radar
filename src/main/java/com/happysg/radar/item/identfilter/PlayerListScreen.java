package com.happysg.radar.item.identfilter;

import com.happysg.radar.networking.NetworkHandler;
import com.happysg.radar.networking.networkhandlers.ListNBTHandler;

import com.happysg.radar.networking.packets.SaveListsPacket;
import com.happysg.radar.registry.ModGuiTextures;
import com.happysg.radar.utils.screenelements.DynamicIconButton;


import com.happysg.radar.utils.screenelements.TooltipIcon;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;

import dev.engine_room.flywheel.lib.transform.TransformStack;

import net.createmod.catnip.gui.AbstractSimiScreen;


import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;


import java.util.ArrayList;
import java.util.List;

import static com.happysg.radar.CreateRadar.MODID;


public class PlayerListScreen extends AbstractSimiScreen {


    protected ModGuiTextures background;
    protected DynamicIconButton friendfoe;
    protected DynamicIconButton remove;
    protected DynamicIconButton playeradd;
    protected DynamicIconButton add;
    protected EditBox playerentry;
    protected IconButton confirmButton;
    protected List<String> entries = new ArrayList<>();
    protected List<Boolean> friendorfoe =new ArrayList<>();
    private final List<DynamicIconButton> deleteButtons = new ArrayList<>();
    private final List<TooltipIcon> factionIndicators = new ArrayList<>();
    private static final int MAX_VISIBLE = 3;
    private int startIndex = 0;
    private boolean isAddingNewSlot = false;
    private int currentAddSlotY = -1;
    public PlayerListScreen() {
        this.background = ModGuiTextures.PLAYER_LIST;
    }


    protected void renderWindow(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int x = guiLeft;
        int y = guiTop;

        background.render(graphics, x, y);
        MutableComponent header = Component.translatable(MODID + ".player_list.title");
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

        drawList(graphics);

    }



    protected void init() {
        setWindowSize(background.width, background.height);
        super.init();

        ListNBTHandler.LoadedLists loaded = ListNBTHandler.loadFromHeldItem(minecraft.player);
        this.entries      = loaded.entries;
        this.friendorfoe  = loaded.friendOrFoe;

        confirmButton = new IconButton(guiLeft + 192, guiTop + 101, AllIcons.I_CONFIRM);
        confirmButton.withCallback(this::onClose);
        addRenderableWidget(confirmButton);
        minecraft.player.getMainHandItem().setTag(null);
/*
        friendfoe = new DynamicIconButton(guiLeft + 156, guiTop + 129, ModGuiTextures.ID_SMILE, ModGuiTextures.ID_FROWN,
                Component.translatable(MODID + ".filter_isfriend"),
                Component.translatable(MODID + ".filter_isfoe"),
                11, 11);
        addRenderableWidget(friendfoe);
        remove = new DynamicIconButton(guiLeft + 168, guiTop + 129, ModGuiTextures.ID_X, ModGuiTextures.ID_X,
                Component.translatable(MODID + ".filter_remove"),
                Component.translatable(MODID + ".filter_remove"),
                11, 11);
        addRenderableWidget(remove);
        playeradd = new DynamicIconButton(guiLeft + 188, guiTop + 127, ModGuiTextures.ID_ADD, ModGuiTextures.ID_ADD,
                Component.translatable(MODID + ".filter_add"),
                Component.translatable(MODID + ".filter_add"),
                16, 16);
        playeradd.withCallback((mx, my) -> addItem());
        addRenderableWidget(playeradd);

        playerentry = new EditBox(font, guiLeft + 22, guiTop + 130, 135, 11,
                Component.translatable(MODID + ".filter_insert_player_user"));
        playerentry.setMaxLength(16);
        playerentry.setTextColor(-1w);
        playerentry.setBordered(false);
        addRenderableWidget(playerentry);
        confirmButton = new IconButton(guiLeft + 192, guiTop + 101, AllIcons.I_CONFIRM);
        confirmButton.withCallback(this::onClose);
        addRenderableWidget(confirmButton);

        rebuildList();

 */
        rebuildList();
    }



    private void rebuildList() {
        deleteButtons.forEach(this::removeWidget);
        factionIndicators.forEach(this::removeWidget);
        deleteButtons.clear();
        factionIndicators.clear();

        int endIndex = Math.min(entries.size(), startIndex + MAX_VISIBLE);
        for (int idx = startIndex; idx < endIndex; idx++) {
            int displayPos = idx - startIndex;            // 0,1,2
            int y = guiTop + 17 + displayPos * 23;

            // faction icon
            TooltipIcon factionIndicator = new TooltipIcon(
                    guiLeft + 172, y + 4,
                    friendorfoe.get(idx) ? ModGuiTextures.ID_SMILE : ModGuiTextures.ID_FROWN,
                    Component.translatable(MODID +
                            (friendorfoe.get(idx) ? ".faction.friendly" : ".faction.enemy"))
            );
            addRenderableWidget(factionIndicator);
            factionIndicators.add(factionIndicator);

            // delete button
            DynamicIconButton del = new DynamicIconButton(
                    guiLeft + 182, y + 4,
                    ModGuiTextures.ID_X,
                    Component.translatable(MODID + ".card.remove_entry"),
                    11, 11
            );
            int finalIdx = idx;
            del.withCallback((mx, my) -> {
                // capture the actual entry index
                removeEntry(finalIdx);
                // optionally adjust startIndex if you’re at the end
            });
            addRenderableWidget(del);
            deleteButtons.add(del);
        }
    }

    private void addPlusButton(int slotIndex) {
        if (add != null) return;  // already exists
        int y = guiTop + 17 + slotIndex * 23;
        add = new DynamicIconButton(guiLeft + 21, y + 2, ModGuiTextures.ID_ADD,
                Component.translatable(MODID + ".filter_add_new"), 16, 16);
        add.withCallback((mx, my) -> startNewSlot(y));
        //add.withCallback((mx, my) ->removeWidget(add));

        addRenderableWidget(add);
    }


    private void startNewSlot(int y) {
        removeWidget(add);
        if (add != null) {
            add = null;
        }

        isAddingNewSlot = true;
        currentAddSlotY = y;

        playeradd = new DynamicIconButton(guiLeft + 185, y, ModGuiTextures.ID_ADD, ModGuiTextures.ID_ADD,
                Component.translatable(MODID + ".filter_add"),
                Component.translatable(MODID + ".filter_add"),
                16, 16);
        playeradd.withCallback((mx, my) -> addItem());
        addRenderableWidget(playeradd);

        playerentry = new EditBox(font, guiLeft + 26 , y + 4, 135, 11,
                Component.translatable(MODID + ".filter_insert_player_user"));
        playerentry.setMaxLength(16);
        playerentry.setTextColor(-1);
        playerentry.setBordered(false);
        addRenderableWidget(playerentry);

        friendfoe = new DynamicIconButton(guiLeft + 159, y + 4, ModGuiTextures.ID_SMILE, ModGuiTextures.ID_FROWN,
                Component.translatable(MODID + ".filter_isfriend"),
                Component.translatable(MODID + ".filter_isfoe"),
                11, 11);
        addRenderableWidget(friendfoe);
    }


    private void addItem() {
        String input = playerentry.getValue();
        boolean faction = friendfoe.getState();
        removeWidget(playerentry);
        removeWidget(playeradd);
        removeWidget(friendfoe);
        friendorfoe.add(faction);
        entries.add(input);
        isAddingNewSlot = false;
        currentAddSlotY = -1;
        rebuildList();
    }


    protected void drawList(GuiGraphics graphics) {
        int available = Math.max(0, entries.size() - startIndex);
        int drawCount = Math.min(available, MAX_VISIBLE);

        // 1) Draw existing entries
        for (int i = 0; i < drawCount; i++) {
            int idx = startIndex + i;
            int y   = guiTop + 17 + i * 23;

            ModGuiTextures.ID_CARD.render(graphics, guiLeft + 16, y);
            String label = (idx + 1) + ": " + entries.get(idx);
            graphics.drawString(font, label, guiLeft + 42, y + 6, 0, false);
        }

        // 2) If there’s room for one more slot...
        if (drawCount < MAX_VISIBLE && !isAddingNewSlot) {
            addPlusButton(drawCount);
        }

        // 3) If we ARE adding, draw the add‑card at its chosen Y
        if (isAddingNewSlot && currentAddSlotY >= 0) {
            ModGuiTextures.ID_CARD.render(graphics, guiLeft + 4, currentAddSlotY);
        }
    }



    private void scrollUp() {
        startIndex = Math.max(0, startIndex - 1);
        rebuildList();
    }
    private void scrollDown() {
        startIndex = Math.min(entries.size() - MAX_VISIBLE, startIndex + 1);
        rebuildList();
    }

    protected void removeEntry(int entry){
        DynamicIconButton removeMe =  deleteButtons.remove(entry);
        removeWidget(removeMe);
        TooltipIcon removeicon = factionIndicators.remove(entry);
        removeWidget(removeicon);
        entries.remove(entry);
        friendorfoe.remove(entry);
        rebuildList();

    }
    @Override
    public void removed() {
        super.removed();
        if (minecraft.player != null && minecraft.level.isClientSide) {
            NetworkHandler.CHANNEL.sendToServer(
                    new SaveListsPacket(this.entries, this.friendorfoe)
            );
        }
    }


}
