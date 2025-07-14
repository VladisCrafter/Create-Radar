package com.happysg.radar.item.identfilter;

import com.happysg.radar.networking.packets.ListNBTHandler;

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
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
        loadListsFromHeldItem();
        assert minecraft != null;
        assert minecraft.player != null;
        ListNBTHandler.LoadedLists loaded = ListNBTHandler.loadFromHeldItem(minecraft.player);
        this.entries = loaded.entries;
        this.friendorfoe = loaded.friendOrFoe;



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
    @Override
    public void removed() {
        super.removed();
        assert minecraft != null;
        Player player = minecraft.player;
        saveListsToHeldItem(entries,friendorfoe);
        assert player != null;
        ListNBTHandler.saveToHeldItem(player,entries,friendorfoe);
    }

    public void saveListsToHeldItem( List<String> entries, List<Boolean> friendOrFoe) {
        assert minecraft != null;
        assert minecraft.player != null;
        ItemStack stack = minecraft.player.getMainHandItem();
        if (stack.isEmpty()) return;

        CompoundTag tag = stack.getOrCreateTag();

        // Serialize entries (strings)
        ListTag entriesTag = new ListTag();
        for (String s : entries) {
            entriesTag.add(StringTag.valueOf(s));
        }
        tag.put("EntriesList", entriesTag);

        // Serialize friendOrFoe (booleans as bytes)
        ListTag foeTag = new ListTag();
        for (Boolean b : friendOrFoe) {
            foeTag.add(ByteTag.valueOf(b ? (byte)1 : (byte)0));
        }
        tag.put("FriendOrFoeList", foeTag);

        stack.setTag(tag);
    }

    /**
     * Read the two lists back from the ItemStack in the player's main hand into this instance's fields.
     */
    public void loadListsFromHeldItem() {
        assert minecraft != null;
        assert minecraft.player != null;
        ItemStack stack = minecraft.player.getMainHandItem();
        if (stack.isEmpty() || !stack.hasTag()) return;

        CompoundTag tag = stack.getTag();

        // Deserialize entries
        assert tag != null;
        ListTag entriesTag = tag.getList("EntriesList", 8); // 8 = String NBT type
        this.entries.clear();
        for (int i = 0; i < entriesTag.size(); i++) {
            this.entries.add(entriesTag.getString(i));
        }

        // Deserialize friendOrFoe
        ListTag foeTag = tag.getList("FriendOrFoeList", 1); // 1 = Byte NBT type
        this.friendorfoe.clear();
        for (Tag value : foeTag) {
            byte val = value.getId();
            this.friendorfoe.add(val != 0);
        }
    }
}
