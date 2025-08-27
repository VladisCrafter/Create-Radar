package com.happysg.radar.block.controller.networkfilter;

import com.happysg.radar.CreateRadar;
import com.happysg.radar.registry.ModBlockEntityTypes;
import com.happysg.radar.registry.ModItems;
import com.simibubi.create.AllShapes;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;



public class NetworkFilterer extends WrenchableDirectionalBlock implements IBE<NetworkFiltererBlockEntity> {

    public NetworkFilterer(Properties properties) {
        super(properties);
    }
    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return AllShapes.DATA_GATHERER.get(pState.getValue(FACING));
    }
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState placed = super.getStateForPlacement(context);
        return placed.setValue(FACING, context.getClickedFace());
    }
    public Class<NetworkFiltererBlockEntity> getBlockEntityClass() {
        return NetworkFiltererBlockEntity.class;
    }

     @Override
    public BlockEntityType<? extends NetworkFiltererBlockEntity> getBlockEntityType() {
        return ModBlockEntityTypes.NETWORK_FILTER_BLOCK_ENTITY.get();
    }
    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);

        // CLIENT: animate hand quickly
        if (world.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof NetworkFiltererBlockEntity)) return InteractionResult.PASS;
        NetworkFiltererBlockEntity threeBE = (NetworkFiltererBlockEntity) be;
        Item radarItem = ModItems.RADAR_FILTER_ITEM.get();   // <- replace/remove .get() if needed
        Item identItem = ModItems.IDENT_FILTER_ITEM.get();
        Item targetItem = ModItems.TARGET_FILTER_ITEM.get();

        int targetSlot = -1;
        Item heldItem = held.getItem();
        if (heldItem == radarItem) {
            targetSlot = 0;
        } else if (heldItem == identItem) {
            targetSlot = 1;
        } else if (heldItem == targetItem) {
            targetSlot = 2;
        } else if (held.isEmpty()) {
            return InteractionResult.PASS;
        }else{
            // invalid item
            player.displayClientMessage(Component.translatable(CreateRadar.MODID + ".network_filter.invalid").withStyle(ChatFormatting.RED),true);
            return InteractionResult.FAIL;
        }

        // get the item handler capability from the block entity
        IItemHandler inv = threeBE.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
        if (inv == null) {
            return InteractionResult.PASS;
        }

        // Try to insert a single item into the targeted slot
        ItemStack toInsert = held.copy();
        toInsert.setCount(1);

        ItemStack remainder = inv.insertItem(targetSlot, toInsert, false);

        if (remainder.isEmpty()) {
            // insertion succeeded: remove one from player and play sound
            held.shrink(1);
            if (held.getCount() <= 0) player.setItemInHand(hand, ItemStack.EMPTY);

            world.playSound(null, pos, net.minecraft.sounds.SoundEvents.ITEM_PICKUP, net.minecraft.sounds.SoundSource.BLOCKS, 0.6f, 1.0f);

            // Now list contents of the block to the player (Slot 1..3)
            for (int i = 0; i < 3; i++) {
                ItemStack slotStack = inv.getStackInSlot(i);
                String line;
                if (slotStack.isEmpty()) {
                    line = "Slot " + (i + 1) + ": (empty)";
                } else {
                    line = "Slot " + (i + 1) + ": " + slotStack.getCount() + "x " + slotStack.getHoverName().getString();
                }
                // send as system chat so it persists (not actionbar)
                player.sendSystemMessage(Component.literal(line));
            }

            return InteractionResult.SUCCESS;
        } else {
            // couldn't insert into mapped slot (full, or stack limits)
            player.sendSystemMessage(Component.literal("Could not insert item (no space in mapped slot)."));
            return InteractionResult.PASS;
        }
    }
}


