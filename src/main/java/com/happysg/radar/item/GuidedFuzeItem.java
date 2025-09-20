package com.happysg.radar.item;

import com.happysg.radar.CreateRadar;
import com.happysg.radar.block.datalink.screens.TargetingConfig;
import com.happysg.radar.block.monitor.MonitorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import rbasamoyai.createbigcannons.munitions.AbstractCannonProjectile;
import rbasamoyai.createbigcannons.munitions.fuzes.FuzeItem;

import java.util.List;

public class GuidedFuzeItem extends FuzeItem {

    public GuidedFuzeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        BlockPos clickedPos = pContext.getClickedPos();
        if (pContext.getLevel().getBlockEntity(clickedPos) instanceof MonitorBlockEntity blockEntity) {
            pContext.getItemInHand().getOrCreateTag().put("monitorPos", NbtUtils.writeBlockPos(blockEntity.getControllerPos()));
            return InteractionResult.SUCCESS;
        }
        return super.useOn(pContext);
    }

    @Override
    public boolean onProjectileTick(ItemStack stack, AbstractCannonProjectile projectile) {
        CompoundTag tag = stack.getOrCreateTag();
        Vec3 delta = projectile.getDeltaMovement();
        if (tag.contains("monitorPos")) {
            BlockPos monitorPos = NbtUtils.readBlockPos(tag.getCompound("monitorPos"));
            if (delta.y > 0)
                return false;
            if (projectile.level().getBlockEntity(monitorPos) instanceof MonitorBlockEntity monitor) {
                Vec3 target = monitor.getTargetPos(TargetingConfig.DEFAULT);
                if (target == null)
                    return false;
                double horizontalDistance = Math.sqrt(Math.pow(projectile.position().x - target.x, 2) + Math.pow(projectile.position().z - target.z, 2));
                if (Math.abs(projectile.position().y - target.y) > horizontalDistance / 2 || tag.contains("valid")) {
                    tag.putBoolean("valid", true);
                } else
                    return false;
                Vec3 direction = target.subtract(projectile.position());
                projectile.setDeltaMovement(direction.normalize().scale(3));

            }
        }
        return super.onProjectileTick(stack, projectile);
    }

    @Override
    public boolean onProjectileImpact(ItemStack stack, AbstractCannonProjectile projectile, HitResult hitResult, AbstractCannonProjectile.ImpactResult impactResult, boolean baseFuze) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
        if (pStack.getOrCreateTag().contains("monitorPos")) {
            BlockPos monitorPos = NbtUtils.readBlockPos(pStack.getOrCreateTag().getCompound("monitorPos"));
            pTooltipComponents.add(Component.translatable(CreateRadar.MODID + ".linked_monitor", Component.translatable(CreateRadar.MODID + ".monitor_coords", monitorPos.getX(), monitorPos.getY(), monitorPos.getZ())));
        } else
            pTooltipComponents.add(Component.translatable(CreateRadar.MODID + ".no_monitor"));
    }


}
