package com.happysg.radar.mixin;

import com.happysg.radar.CreateRadar;
import com.happysg.radar.block.controller.pitch.AutoPitchControllerBlockEntity;
import com.happysg.radar.block.controller.yaw.AutoYawControllerBlockEntity;
import com.happysg.radar.block.monitor.MonitorBlockEntity;
import com.happysg.radar.block.radar.bearing.RadarBearingBlockEntity;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockItem;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;


@Mixin(DisplayLinkBlockItem.class)
public abstract class DisplayLinkBlockItemMixin extends BlockItem {

    public DisplayLinkBlockItemMixin(Block pBlock, Properties pProperties) {
        super(pBlock, pProperties);
    }

    /**
     * @author happysg
     * @reason Add the ability to link to MonitorBlockEntity regardless of the distance
     * and links source without opening the screen
     */
    //TODO use proper Mixin instead of Overwrite
    @Overwrite
    public InteractionResult useOn(UseOnContext pContext) {
        ItemStack stack = pContext.getItemInHand();
        BlockPos pos = pContext.getClickedPos();
        Level level = pContext.getLevel();
        BlockState state = level.getBlockState(pos);
        Player player = pContext.getPlayer();

        if (player == null)
            return InteractionResult.FAIL;

        if (player.isShiftKeyDown() && stack.hasTag()) {
            if (level.isClientSide)
                return InteractionResult.SUCCESS;
            player.displayClientMessage(Lang.translateDirect("display_link.clear"), true);
            stack.setTag(null);
            return InteractionResult.SUCCESS;
        }

        if (!stack.hasTag()) {
            if (level.isClientSide)
                return InteractionResult.SUCCESS;
            CompoundTag stackTag = stack.getOrCreateTag();
            stackTag.put("SelectedPos", NbtUtils.writeBlockPos(pos));
            player.displayClientMessage(Lang.translateDirect("display_link.set"), true);
            stack.setTag(stackTag);
            return InteractionResult.SUCCESS;
        }

        CompoundTag tag = stack.getTag();
        CompoundTag teTag = new CompoundTag();

        BlockPos selectedPos = NbtUtils.readBlockPos(tag.getCompound("SelectedPos"));
        BlockPos placedPos = pos.relative(pContext.getClickedFace(), state.canBeReplaced() ? 0 : 1);

        if (!selectedPos.closerThan(placedPos, AllConfigs.server().logistics.displayLinkRange.get()) && !create_Radar$isMonitor(level, selectedPos)) {
            player.displayClientMessage(Lang.translateDirect("display_link.too_far")
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }


        //fixme poor design
        //no need open screen to link
        CompoundTag data = new CompoundTag();
        if (level.getBlockEntity(pos) instanceof RadarBearingBlockEntity) {
            data.putString("Id", CreateRadar.asResource("radar").toString());
            data.putInt("Filter", 0);
            teTag.put("Source", data);
        }
        if (level.getBlockEntity(pos) instanceof AutoYawControllerBlockEntity) {
            data.putString("Id", CreateRadar.asResource("yaw_controller").toString());
            teTag.put("Source", data);
        }
        if (level.getBlockEntity(pos) instanceof AutoPitchControllerBlockEntity) {
            data.putString("Id", CreateRadar.asResource("pitch_controller").toString());
            teTag.put("Source", data);
        }
        teTag.put("TargetOffset", NbtUtils.writeBlockPos(selectedPos.subtract(placedPos)));
        tag.put("BlockEntityTag", teTag);

        InteractionResult useOn = super.useOn(pContext);
        if (level.isClientSide || useOn == InteractionResult.FAIL)
            return useOn;

        ItemStack itemInHand = player.getItemInHand(pContext.getHand());
        if (!itemInHand.isEmpty())
            itemInHand.setTag(null);
        player.displayClientMessage(Lang.translateDirect("display_link.success")
                .withStyle(ChatFormatting.GREEN), true);
        return useOn;
    }

    @Unique
    private boolean create_Radar$isMonitor(Level level, BlockPos selectedPos) {
        return level.getBlockEntity(selectedPos) instanceof MonitorBlockEntity;
    }
}
