package com.happysg.radar.block.datalink.screens.idfilterscreens;

import com.happysg.radar.CreateRadar;
import com.happysg.radar.block.datalink.DataController;
import com.happysg.radar.block.datalink.DataLinkBlock;
import com.happysg.radar.block.datalink.DataLinkBlockEntity;
import com.happysg.radar.block.datalink.DataPeripheral;
import com.happysg.radar.block.datalink.screens.AbstractDataLinkScreen;
import com.happysg.radar.block.monitor.MonitorFilter;
import com.happysg.radar.registry.ModGuiTextures;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

public class PlayerListScreen extends AbstractSimiScreen {
    private static final ItemStack FALLBACK = new ItemStack(Items.BARRIER);

    protected ModGuiTextures background;

    private IconButton confirmButton;

    BlockState sourceState;
    BlockState targetState;
    DataPeripheral source;
    DataController target;
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
        /*
        GuiGameElement.of(blockEntity.getBlockState()
                        .setValue(DataLinkBlock.FACING, Direction.DOWN)) //UP
                .;
        ms.popPose();

         */
    }
    protected void init() {
        setWindowSize(background.width, background.height);
        super.init();
        clearWidgets();

        int x = guiLeft;
        int y = guiTop;

        confirmButton = new IconButton(x + background.width - 33, y + background.height - 24, AllIcons.I_CONFIRM);
        confirmButton.withCallback(this::onClose);
        addRenderableWidget(confirmButton);


    }

    }

