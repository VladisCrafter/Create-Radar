package com.happysg.radar.mixin;

import com.happysg.radar.CreateRadar;
import com.happysg.radar.block.controller.pitch.AutoPitchControllerBlockEntity;
import com.happysg.radar.block.controller.yaw.AutoYawControllerBlockEntity;
import com.happysg.radar.block.radar.bearing.RadarBearingBlockEntity;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockItem;
import com.simibubi.create.foundation.utility.Lang;
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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(DisplayLinkBlockItem.class)
public abstract class DisplayLinkBlockItemMixin extends BlockItem {

    public DisplayLinkBlockItemMixin(Block pBlock, Properties pProperties) {
        super(pBlock, pProperties);
    }


    //todo fix this
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true, remap = true)
    public void injectUseOn(UseOnContext pContext, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack stack = pContext.getItemInHand();
        BlockPos pos = pContext.getClickedPos();
        Level level = pContext.getLevel();
        BlockState state = level.getBlockState(pos);
        Player player = pContext.getPlayer();
        BlockPos placedPos = pos.relative(pContext.getClickedFace(), state.canBeReplaced() ? 0 : 1);
        if (create_Radar$isRadarComponent(pos, level)) {
            if (player == null || player.isShiftKeyDown()) {
                return;
            }
            if (!stack.hasTag()) {
                return;
            }

            CompoundTag tag = stack.getTag();
            CompoundTag teTag = new CompoundTag();

            BlockPos selectedPos = NbtUtils.readBlockPos(tag.getCompound("SelectedPos"));
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
                return;

            ItemStack itemInHand = player.getItemInHand(pContext.getHand());
            if (!itemInHand.isEmpty())
                itemInHand.setTag(null);
            player.displayClientMessage(Lang.translateDirect("display_link.success")
                    .withStyle(ChatFormatting.GREEN), true);
            cir.setReturnValue(InteractionResult.SUCCESS);
            cir.cancel();
        }
    }

    @Unique
    private boolean create_Radar$isRadarComponent(BlockPos placedPos, Level level) {
        return level.getBlockEntity(placedPos) instanceof RadarBearingBlockEntity || level.getBlockEntity(placedPos) instanceof AutoYawControllerBlockEntity || level.getBlockEntity(placedPos) instanceof AutoPitchControllerBlockEntity;
    }
}
