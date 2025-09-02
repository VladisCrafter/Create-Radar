package com.happysg.radar.block.controller.networkfilter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class NetworkFiltererBlockEntity extends BlockEntity {
    // inventory
    // inside your BlockEntity
    private final ItemStackHandler inventory = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public int getSlotLimit(int slot) {
            // Enforce a single-item capacity per slot
            return 1;
        }

        // Optional: prevent inserting more than 1 even if insertItem is called with higher count
        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack == null || stack.isEmpty()) return stack;
            // create a single-item copy to try insert (this ensures method respects single-item behavior)
            ItemStack one = stack.copy();
            one.setCount(1);
            ItemStack remainder = super.insertItem(slot, one, simulate);
            if (remainder.isEmpty()) {
                // we consumed 1 from the incoming stack
                ItemStack out = stack.copy();
                out.shrink(1);
                return out; // remainder for the original stack (count decreased by 1)
            }
            // couldn't insert, return original stack unchanged
            return stack;
        }
    };


    // capability wrapper
    private LazyOptional<IItemHandler> handler = LazyOptional.of(() -> inventory);

    // Array storing a *copy* of each slot's item NBT (or null if slot has no tag / is empty)
    // Index 0..2
    private CompoundTag[] slotNbt = new CompoundTag[3];

    public NetworkFiltererBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state); // adjust to your BE registration
        // initialize array (nulls by default)
        for (int i = 0; i < slotNbt.length; i++) slotNbt[i] = null;
    }
    // inside your BlockEntity




    // Called after inventory changes: copy the item's tag into slotNbt[slot]
    private void updateSlotNbtFromInventory(int slot) {
        if (slot < 0 || slot >= inventory.getSlots()) return;
        ItemStack s = inventory.getStackInSlot(slot);
        if (s == null || s.isEmpty() || !s.hasTag()) {
            slotNbt[slot] = null;
        } else {
            CompoundTag tag = s.getTag();
            slotNbt[slot] = tag == null ? null : tag.copy(); // store copy to be safe
        }
    }

    // Public getter to access stored tag (may be null)
    @Nullable
    public CompoundTag getSlotNbt(int slot) {
        if (slot < 0 || slot >= slotNbt.length) return null;
        return slotNbt[slot];
    }


    public void setSlotNbt(int slot, @Nullable CompoundTag tag) { // DO NOT DELETE I BEG YOU
        if (slot < 0 || slot >= slotNbt.length) return;
        slotNbt[slot] = (tag == null) ? null : tag.copy();
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    // Serialization: save inventory (as before) AND save our slotNbt array
    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);

        // load inventory
        if (nbt.contains("inv")) {
            inventory.deserializeNBT(nbt.getCompound("inv"));
        } else {
            // fallback: try older format if needed
        }

        // after inventory deserialized, ensure our slotNbt matches the inventory
        for (int i = 0; i < inventory.getSlots(); i++) {
            updateSlotNbtFromInventory(i);
        }

        // load stored per-slot tags if present (prefer this so explicit stored tags survive even if item removed)
        if (nbt.contains("slotTags")) {
            CompoundTag tagsTag = nbt.getCompound("slotTags");
            for (int i = 0; i < slotNbt.length; i++) {
                if (tagsTag.contains("slot" + i)) {
                    CompoundTag t = tagsTag.getCompound("slot" + i);
                    if (t.isEmpty()) slotNbt[i] = null;
                    else slotNbt[i] = t.copy();
                } else {
                    slotNbt[i] = null;
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);

        // save inventory
        nbt.put("inv", inventory.serializeNBT());

        // save the per-slot tags as a compound under "slotTags"
        CompoundTag tagsTag = new CompoundTag();
        for (int i = 0; i < slotNbt.length; i++) {
            if (slotNbt[i] != null) {
                tagsTag.put("slot" + i, slotNbt[i].copy());
            } else {
                // keep an empty compound so keys are stable (optional)
                tagsTag.put("slot" + i, new CompoundTag());
            }
        }
        nbt.put("slotTags", tagsTag);
    }

    // Networking helpers: ensure clients get the slotNbt when block entity updates
    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // Capability plumbing
    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return handler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        handler = LazyOptional.of(() -> inventory);
        // make sure slotNbt synced with inventory on load
        for (int i = 0; i < inventory.getSlots(); i++) updateSlotNbtFromInventory(i);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        handler.invalidate();
    }

    // Convenience: expose the raw item handler if needed

    public IItemHandler getItemHandler() {
        return inventory;
    }
       /*
    public void sendFullNbtToPlayer(Player player) { (debug method for seeing item NBT)
        if (player == null || this.level == null) return;

        // Get the block entity's full save NBT (same data that is written to disk)
        CompoundTag tag = this.saveWithoutMetadata(); // includes inventory, our slotTags, mappings, etc.

        String full = tag == null ? "{}" : tag.toString();

        // Protect chat from exploding: split into chunks if too long
        final int MAX_CHARS = 1000; // tweak: how many chars per chat message
        if (full.length() <= MAX_CHARS) {
            player.sendSystemMessage(Component.literal(full).withStyle(ChatFormatting.GREEN));
            return;
        }

        // Split into multiple messages
        for (int i = 0; i < full.length(); i += MAX_CHARS) {
            String part = full.substring(i, Math.min(full.length(), i + MAX_CHARS));
            player.sendSystemMessage(Component.literal(part).withStyle(ChatFormatting.GREEN));
        }
    }

     */
}










