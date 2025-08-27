package com.happysg.radar.block.controller.networkfilter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class NetworkFiltererBlockEntity extends BlockEntity {
    // example storage fields (replace with your real fields)
    public final NonNullList<ItemStack> stacks = NonNullList.withSize(3, ItemStack.EMPTY);
    private CompoundTag storedConfig; // store raw NBT, or parse into typed fields
    public String face;
    public ResourceLocation texture;
    private final ItemStackHandler inventory = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            // mark dirty and notify block update so clients re-render/get the updated data
            setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
        };

        // capability wrappe
        private LazyOptional<IItemHandler> handler = LazyOptional.of(() -> inventory);
    private int selectedSlot = 0;
    public NetworkFiltererBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
    }
    private final Map<ResourceLocation, Integer> mappings = new HashMap<>();


    public ItemStack insertWithMapping(ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;

        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        Integer mapped = mappings.get(key);

        ItemStack toInsert = stack.copy();

        if (mapped != null && mapped >= 0 && mapped < inventory.getSlots()) {
            // Try mapped slot first
            toInsert = inventory.insertItem(mapped, toInsert, false);
            if (toInsert.isEmpty()) return ItemStack.EMPTY;
            // if remainder exists, try to place into other slots
        }

        // fallback: insert into first available / merge as normal
        for (int i = 0; i < inventory.getSlots(); i++) {
            toInsert = inventory.insertItem(i, toInsert, false);
            if (toInsert.isEmpty()) break;
        }

        return toInsert;
    }

    // Bind an item type to selectedSlot
    public void bindItemToSelectedSlot(ResourceLocation itemKey) {
        // remove any previous mapping that points to selectedSlot (ensure uniqueness)
        Iterator<Map.Entry<ResourceLocation, Integer>> it = mappings.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ResourceLocation, Integer> e = it.next();
            if (e.getValue() == selectedSlot) it.remove();
        }
        mappings.put(itemKey, selectedSlot);
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    // Unbind selectedSlot (remove mapping that had this slot)
    public void unbindSelectedSlot() {
        Iterator<Map.Entry<ResourceLocation, Integer>> it = mappings.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ResourceLocation, Integer> e = it.next();
            if (e.getValue() == selectedSlot) it.remove();
        }
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    // Cycle selected slot: 0 -> 1 -> 2 -> 0
    public void cycleSelectedSlot() {
        selectedSlot = (selectedSlot + 1) % inventory.getSlots();
        setChanged();
    }

    public int getSelectedSlot() {
        return selectedSlot;
    }

    // Serialization (save mappings + selected slot + inventory)
    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        if (nbt.contains("inv")) {
            inventory.deserializeNBT(nbt.getCompound("inv"));
        }
        mappings.clear();
        if (nbt.contains("mappings")) {
            CompoundTag mapTag = nbt.getCompound("mappings");
            for (String key : mapTag.getAllKeys()) {
                int slot = mapTag.getInt(key);
                mappings.put(new ResourceLocation(key), slot);
            }
        }
        if (nbt.contains("selectedSlot")) {
            selectedSlot = nbt.getInt("selectedSlot");
        }
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.put("inv", inventory.serializeNBT());
        CompoundTag mapTag = new CompoundTag();
        for (Map.Entry<ResourceLocation, Integer> e : mappings.entrySet()) {
            mapTag.putInt(e.getKey().toString(), e.getValue());
        }
        nbt.put("mappings", mapTag);
        nbt.putInt("selectedSlot", selectedSlot);
    }

    // Networking helpers
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
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        handler.invalidate();
    }

    // Utility: expose a copy of mapping for client render/UI if you want:
    public Map<ResourceLocation, Integer> getMappings() {
        return new HashMap<>(mappings);
    }
}









