package com.happysg.radar.item.identfilter;

import com.happysg.radar.CreateRadar;
import com.happysg.radar.registry.ModGuiTextures;
import com.happysg.radar.utils.screenelements.DynamicIconButton;


import com.happysg.radar.utils.screenelements.TooltipIcon;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import dev.engine_room.flywheel.lib.transform.TransformStack;

import io.netty.buffer.Unpooled;
import net.createmod.catnip.gui.AbstractSimiScreen;


import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.checkerframework.checker.signature.qual.Identifier;
import org.jetbrains.annotations.NotNull;


import java.util.ArrayList;
import java.util.List;

import static com.happysg.radar.CreateRadar.MODID;


public class PlayerListScreen extends AbstractSimiScreen {


    protected ModGuiTextures background;
    protected DynamicIconButton friendfoe;
    protected DynamicIconButton remove;
    protected DynamicIconButton playeradd;
    protected EditBox playerentry;
    protected IconButton confirmButton;
    protected List<String> entries = new ArrayList<>();
    protected List<Boolean> friendorfoe =new ArrayList<>();
    private final List<DynamicIconButton> deleteButtons = new ArrayList<>();
    private final List<TooltipIcon> factionIndicators = new ArrayList<>();

    GuiGraphics heregraphics;
    public PlayerListScreen() {
        this.background = ModGuiTextures.PLAYER_LIST;
    }


    protected void renderWindow(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int x = guiLeft;
        int y = guiTop;
       heregraphics = graphics;
        //ClientPlayNetworking.send(new Identifier(MODID, "request_player_list"), new PacketByteBuf(Unpooled.buffer()));
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
        clearWidgets();


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
        playerentry.setTextColor(-1);
        playerentry.setBordered(false);
        addRenderableWidget(playerentry);
        confirmButton = new IconButton(guiLeft + 192, guiTop + 101, AllIcons.I_CONFIRM);
        confirmButton.withCallback(this::onClose);
        addRenderableWidget(confirmButton);
        loadEntriesFromNBT();
        rebuildList();
    }

    protected void addItem() {
        String inbox = playerentry.getValue();
        boolean faction = friendfoe.getState();
        friendorfoe.add(faction);
        entries.add(inbox);
        playerentry.setValue("");
        rebuildList();
        }
    protected void rebuildList() {
        deleteButtons.forEach(this::removeWidget);
        factionIndicators.forEach(this::removeWidget);
        deleteButtons.clear();
        factionIndicators.clear();
        for (int i = 0; i < entries.size(); i++) {
            int y = guiTop + 17 + i * 23;
            // faction icon
            TooltipIcon factionindicator = new TooltipIcon(
                    guiLeft + 172, y + 4,
                    friendorfoe.get(i) ? ModGuiTextures.ID_SMILE : ModGuiTextures.ID_FROWN,
                    Component.translatable(MODID +
                            (friendorfoe.get(i) ? ".faction.friendly" : ".faction.enemy"))
            );

            addRenderableWidget(factionindicator);
            factionIndicators.add(factionindicator);

            // delete button (capture ‘del’ itself, not index)
            DynamicIconButton del = new DynamicIconButton(
                    guiLeft + 182, y + 4,
                    ModGuiTextures.ID_X,
                    Component.translatable(MODID + ".card.remove_entry"),
                    11, 11
            );
            del.withCallback((mx, my) -> {
                int idx = deleteButtons.indexOf(del);
                if (idx != -1) removeEntry(idx);
            });
            addRenderableWidget(del);
            deleteButtons.add(del);
        }

    }
    protected void drawList(GuiGraphics graphics) {
        for (int i = 0; i < entries.size(); i++) {
            int y = guiTop + 17 + i * 23;
            ModGuiTextures.ID_CARD.render(graphics, guiLeft + 16, y);

            String label = (i + 1) + ": " + entries.get(i);
            graphics.drawString(font, label, guiLeft + 42, y + 6, 0, false);
        }
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
    private static final String NBT_TAG = MODID;
    private static final String NBT_LIST  = "playerList";

    private void saveEntriesToNBT() {
        // get the player’s persistent data
        assert Minecraft.getInstance().player != null;
        CompoundTag pData = Minecraft.getInstance().player.getPersistentData();
        // get (or create) our mod’s sub‑tag
        CompoundTag modData = pData.contains(NBT_TAG)
                ? pData.getCompound(NBT_TAG)
                : new CompoundTag();

        // build a ListTag of compounds { name: String, friend: Boolean }
        ListTag listTag = new ListTag();
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag elem = new CompoundTag();
            elem.putString("name", entries.get(i));
            elem.putBoolean("friend", friendorfoe.get(i));
            listTag.add(elem);
        }

        modData.put(NBT_LIST, listTag);
        pData.put(NBT_TAG, modData);
    }

    private void loadEntriesFromNBT() {
        assert Minecraft.getInstance().player != null;
        CompoundTag pData = Minecraft.getInstance().player.getPersistentData();
        if (!pData.contains(NBT_TAG)) return;
        CompoundTag modData = pData.getCompound(NBT_TAG);
        if (!modData.contains(NBT_LIST)) return;

        ListTag listTag = modData.getList(NBT_LIST, Tag.TAG_COMPOUND);
        entries.clear();
        friendorfoe.clear();
        for (Tag raw : listTag) {
            CompoundTag elem = (CompoundTag) raw;
            entries.add(elem.getString("name"));
            friendorfoe.add(elem.getBoolean("friend"));
        }
    }
    @Override
    public void removed() {
        super.removed();
        saveEntriesToNBT();
    }
    /*
    public static void saveToServerPlayer(ServerPlayerEntity player, List<String> entries, List<Boolean> friendOrFoe) {
        CompoundTag root = player.getPersistentData();
        CompoundTag modData = root.contains(MODID)
                ? root.getCompound(MODID)
                : new CompoundTag();

        ListTag listTag = new ListTag();
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag elem = new CompoundTag();
            elem.putString("name", entries.get(i));
            elem.putBoolean("friend", friendOrFoe.get(i));
            listTag.add(elem);
        }
        modData.put("playerList", listTag);
        root.put(MODID, modData);
    }

     */

}
