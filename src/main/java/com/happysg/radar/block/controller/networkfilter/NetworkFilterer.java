package com.happysg.radar.block.controller.networkfilter;

import com.happysg.radar.CreateRadar;
import com.happysg.radar.registry.ModBlockEntityTypes;
import com.happysg.radar.registry.ModItems;
import com.simibubi.create.AllShapes;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;


public class NetworkFilterer extends WrenchableDirectionalBlock implements IBE<NetworkFiltererBlockEntity> {

    public NetworkFilterer(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
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
    public @NotNull InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            ItemStack held = player.getItemInHand(hand);

            if (held.isEmpty()) {
                // client: play hand animation and bail early (server does the work)
                if (world.isClientSide) return InteractionResult.SUCCESS;

                // local hit coordinates (0..1 inside the block)
                Vec3 hitVec = hit.getLocation();
                double dx = hitVec.x - pos.getX();
                double dy = hitVec.y - pos.getY();
                double dz = hitVec.z - pos.getZ();

                Direction face = hit.getDirection();

                // compute clicked UV in 0..16 depending on face (matches renderer mapping)
                double clickU = 0.0;
                double clickV = 0.0;
                switch (face) {
                    case NORTH -> { // -Z
                        clickU = dx * 16.0;
                        clickV = (1.0 - dy) * 16.0;
                    }
                    case SOUTH -> { // +Z
                        clickU = (1.0 - dx) * 16.0;
                        clickV = (1.0 - dy) * 16.0;
                    }
                    case WEST -> { // -X
                        clickU = (1.0 - dz) * 16.0;
                        clickV = (1.0 - dy) * 16.0;
                    }
                    case EAST -> { // +X
                        clickU = dz * 16.0;
                        clickV = (1.0 - dy) * 16.0;
                    }
                    case UP -> { // +Y
                        clickU = dx * 16.0;
                        clickV = (1.0 - dz) * 16.0;
                    }
                    case DOWN -> { // -Y
                        clickU = dx * 16.0;
                        clickV = dz * 16.0;
                    }
                }

                // renderer's UV positions — keep synced with renderer!
                final double[][] UVS = {{5.0, 11.0}, {11.0, 11.0}, {11.0, 5.0}};
                final double PIXEL_THRESHOLD = 2.5;// pixels: how close the click must be (tweakable)

                // find clicked slot (if any)
                int clickedSlot = -1;
                for (int i = 0; i < UVS.length; i++) {
                    double du = clickU - UVS[i][0];
                    double dv = clickV - UVS[i][1];
                    double distSq = du * du + dv * dv;
                    if (distSq <= (PIXEL_THRESHOLD * PIXEL_THRESHOLD)) {
                        clickedSlot = i;
                        break;
                    }
                }

                if (clickedSlot == -1) {
                    // not clicking near any configured UV -> do nothing
                    return InteractionResult.PASS;
                }

                // got a slot — try to extract a single item from that slot (server-side)
                BlockEntity be = world.getBlockEntity(pos);
                if (!(be instanceof NetworkFiltererBlockEntity threeBE)) return InteractionResult.PASS;

                IItemHandler inv = threeBE.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
                if (inv == null) return InteractionResult.PASS;
                // extract one item
                ItemStack extracted = inv.extractItem(clickedSlot, 1, false);

                // try give to player inventory, else drop it
                boolean added = player.addItem(extracted);
                if (!added) {
                    player.drop(extracted, false);
                }
                world.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6f, 1.0f);
                return InteractionResult.CONSUME;
            }

            // --- NON-EMPTY HAND: keep existing insertion logic (unchanged)
            if (world.isClientSide) {
                return InteractionResult.SUCCESS;
            }

            BlockEntity be = world.getBlockEntity(pos);
            if (!(be instanceof NetworkFiltererBlockEntity)) return InteractionResult.PASS;
            NetworkFiltererBlockEntity threeBE = (NetworkFiltererBlockEntity) be;

            // Allowed items mapping
            Item radarItem = ModItems.RADAR_FILTER_ITEM.get();
            Item identItem = ModItems.IDENT_FILTER_ITEM.get();
            Item targetItem = ModItems.TARGET_FILTER_ITEM.get();

            int targetSlot;
            Item heldItem = held.getItem();
            if (heldItem == radarItem) {
                targetSlot = 0;
            } else if (heldItem == identItem) {
                targetSlot = 1;
            } else if (heldItem == targetItem) {
                targetSlot = 2;
            } else {
                player.displayClientMessage(Component.translatable(CreateRadar.MODID + ".network_filter.invalid").withStyle(ChatFormatting.RED),true);
                return InteractionResult.PASS;
            }

            IItemHandler inv = threeBE.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);

            if (inv == null) return InteractionResult.PASS;


            ItemStack toInsert = held.copy();
            toInsert.setCount(1);
            ItemStack remainder = inv.insertItem(targetSlot, toInsert, false);

            if (remainder.isEmpty()) {
                held.shrink(1);
                if (held.getCount() <= 0) player.setItemInHand(hand, ItemStack.EMPTY);
                world.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6f, 1.0f);

                return InteractionResult.CONSUME;
            } else {
                return InteractionResult.PASS;
            }
    }
    /*
    private void sendSlotContentsWithNbt(Player player, IItemHandler inv) {
        for (int i = 0; i < 3; i++) {
            ItemStack s = inv.getStackInSlot(i);
            if (s == null || s.isEmpty()) {
       //         player.sendSystemMessage(Component.literal("Slot " + (i + 1) + ": (empty)"));
                continue;
            }

            String name = s.getHoverName().getString();
            int count = s.getCount();

            // don't create a tag if it doesn't exist — use hasTag() / getTag()
            String nbtString;
            if (s.hasTag()) {
                CompoundTag tag = s.getTag(); // may be non-null if hasTag() true
                // raw NBT as a string; could be long
                nbtString = tag == null ? "(null)" : tag.toString();
            } else {
                nbtString = "(none)";
            }

            // Compose and send. You can switch to displayClientMessage(..., true) if you prefer actionbar
            player.sendSystemMessage(Component.literal(
                    "Slot " + (i + 1) + ": " + count + "x " + name + " | NBT: " + nbtString
            ));
        }
    }
    */

}
