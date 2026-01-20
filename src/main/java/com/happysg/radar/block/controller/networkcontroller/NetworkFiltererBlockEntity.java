package com.happysg.radar.block.controller.networkcontroller;

import com.happysg.radar.block.behavior.networks.NetworkData;
import com.happysg.radar.block.behavior.networks.config.DetectionConfig;
import com.happysg.radar.block.behavior.networks.config.IdentificationConfig;
import com.happysg.radar.block.behavior.networks.config.TargetingConfig;
import com.happysg.radar.block.controller.pitch.AutoPitchControllerBlockEntity;
import com.happysg.radar.block.radar.track.RadarTrack;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.slf4j.Logger;
import com.happysg.radar.block.radar.behavior.RadarScanningBlockBehavior;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import rbasamoyai.createbigcannons.cannon_control.contraption.PitchOrientedContraptionEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class NetworkFiltererBlockEntity extends BlockEntity {
    private static final String NBT_INVENTORY = "Inventory";
    private static final String NBT_SLOT_NBT  = "SlotNbt";
    private static final Logger LOGGER = LogUtils.getLogger();

    // Legacy keys (for backwards compatibility with old saves)
    private static final String LEGACY_INV = "inv";
    private static final String LEGACY_SLOT_TAGS = "slotTags";
    private static final int SLOT_DETECTION = 0;
    private static final int SLOT_IDENT     = 1;
    private static final int SLOT_TARGETING = 2;

    private  TargetingConfig targeting = TargetingConfig.DEFAULT;

    private final ItemStackHandler inventory = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);

            // Keep slotNbt in sync with actual inserted item tags
            updateSlotNbtFromInventory(slot);
            if (level != null && !level.isClientSide) {
                applyFiltersToNetwork();
            }


            setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack == null || stack.isEmpty()) return stack;

            ItemStack one = stack.copy();
            one.setCount(1);

            ItemStack remainder = super.insertItem(slot, one, simulate);
            if (remainder.isEmpty()) {
                ItemStack out = stack.copy();
                out.shrink(1);
                return out;
            }

            return stack;
        }
    };

    private LazyOptional<IItemHandler> handler = LazyOptional.of(() -> inventory);

    private final CompoundTag[] slotNbt = new CompoundTag[3];

    public NetworkFiltererBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        Arrays.fill(slotNbt, null);
    }

    public void receiveSelectedTargetFromMonitor(@Nullable RadarTrack track, List<AABB> safeZones) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        TargetingConfig cfg = targeting != null ? targeting : TargetingConfig.DEFAULT;
        List<AutoPitchControllerBlockEntity> entities  = getWeaponEndpointBlockEntities();
        for(AutoPitchControllerBlockEntity pitch: entities) {
            pitch.setAndAcquireTrack(track, targeting);
            pitch.setSafeZones(safeZones);
        }
    }

    private void updateSlotNbtFromInventory(int slot) {
        if (slot < 0 || slot >= inventory.getSlots()) return;

        ItemStack s = inventory.getStackInSlot(slot);
        if (s == null || s.isEmpty() || !s.hasTag()) {
            slotNbt[slot] = null;
        } else {
            CompoundTag tag = s.getTag();
            slotNbt[slot] = tag == null ? null : tag.copy();
        }
    }

    // Compact format: SlotNbt: [ {Slot:0, Tag:{...}}, ... ]
    private void loadSlotNbt(CompoundTag nbt) {
        Arrays.fill(slotNbt, null);

        if (!nbt.contains(NBT_SLOT_NBT, Tag.TAG_LIST))
            return;

        ListTag list = nbt.getList(NBT_SLOT_NBT, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int slot = entry.getInt("Slot");
            if (slot < 0 || slot >= slotNbt.length) continue;

            CompoundTag t = entry.getCompound("Tag");
            slotNbt[slot] = t.isEmpty() ? null : t.copy();
        }
    }
    public List<AutoPitchControllerBlockEntity> getWeaponEndpointBlockEntities() {
        if (!(level instanceof ServerLevel serverLevel))
            return List.of();
        NetworkData data = NetworkData.get(serverLevel);
        NetworkData.Group group = data.getGroup(serverLevel.dimension(), worldPosition);
        if (group == null)
            return List.of();
        Set<BlockPos> pos = data.getWeaponEndpoints(group);
        List<AutoPitchControllerBlockEntity> entities = new ArrayList<>();
        for(BlockPos pos1: pos){
            if(serverLevel.getBlockEntity(pos1) instanceof AutoPitchControllerBlockEntity pitch){
                entities.add(pitch);
            }
        }

        return entities;
    }

    private void saveSlotNbt(CompoundTag nbt) {
        ListTag list = new ListTag();

        for (int i = 0; i < slotNbt.length; i++) {
            CompoundTag t = slotNbt[i];
            if (t == null || t.isEmpty()) continue;

            CompoundTag entry = new CompoundTag();
            entry.putInt("Slot", i);
            entry.put("Tag", t.copy());
            list.add(entry);
        }

        if (!list.isEmpty()) {
            nbt.put(NBT_SLOT_NBT, list);
        }
    }

    // Legacy loader: old worlds had { slotTags: {slot0:{}, slot1:{}, ... } }
    private void loadLegacySlotTags(CompoundTag nbt) {
        if (!nbt.contains(LEGACY_SLOT_TAGS, Tag.TAG_COMPOUND))
            return;

        CompoundTag tagsTag = nbt.getCompound(LEGACY_SLOT_TAGS);
        for (int i = 0; i < slotNbt.length; i++) {
            String k = "slot" + i;
            if (tagsTag.contains(k, Tag.TAG_COMPOUND)) {
                CompoundTag t = tagsTag.getCompound(k);
                slotNbt[i] = t.isEmpty() ? null : t.copy();
            }
        }
    }



    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);

        nbt.put(NBT_INVENTORY, inventory.serializeNBT());
        saveSlotNbt(nbt);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);

        if (nbt.contains(NBT_INVENTORY, Tag.TAG_COMPOUND)) {
            inventory.deserializeNBT(nbt.getCompound(NBT_INVENTORY));
        } else if (nbt.contains(LEGACY_INV, Tag.TAG_COMPOUND)) {
            inventory.deserializeNBT(nbt.getCompound(LEGACY_INV));
        }

        if (nbt.contains(NBT_SLOT_NBT, Tag.TAG_LIST)) {
            loadSlotNbt(nbt);
        } else {
            loadLegacySlotTags(nbt);
        }
        for (int i = 0; i < inventory.getSlots(); i++) {
            updateSlotNbtFromInventory(i);
        }

        handler = LazyOptional.of(() -> inventory);
    }

    // Sync to client
    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // Capabilities
    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return handler.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        handler = LazyOptional.of(() -> inventory);
        for (int i = 0; i < inventory.getSlots(); i++) updateSlotNbtFromInventory(i);

        if (level instanceof ServerLevel sl) {
            applyFiltersToNetwork();
        }
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        handler.invalidate();
    }

    public IItemHandler getItemHandler() {
        return inventory;
    }
    public void applyFiltersToNetwork() {
        if (level == null || level.isClientSide) return;
        if (!(level instanceof ServerLevel sl)) return;

        DetectionConfig detection = readDetectionFromSlot();
        IdentificationConfig ident = readIdentificationFromSlot();
        targeting = readTargetingFromSlot();

        NetworkData data = NetworkData.get(sl);
        NetworkData.Group group = data.getOrCreateGroup(sl.dimension(), worldPosition);

        data.setAllFilters(group, targeting, ident, detection);
        applyDetectionToRadar(sl, group, detection);
    }

    private void applyDetectionToRadar(ServerLevel sl, NetworkData.Group group, DetectionConfig detection) {
        // group needs to know where the radar is
        if (group.radarPos == null) return;

        BlockEntity be = sl.getBlockEntity(group.radarPos);
        if (!(be instanceof SmartBlockEntity sbe)) return;

        RadarScanningBlockBehavior scan = BlockEntityBehaviour.get(sbe, RadarScanningBlockBehavior.TYPE);
        if (scan == null) return;

        scan.applyDetectionConfig(detection);
    }

    private DetectionConfig readDetectionFromSlot() {
        return readDetectionFromItem(inventory.getStackInSlot(SLOT_DETECTION));
    }

    private static DetectionConfig readDetectionFromItem(ItemStack stack) {
        CompoundTag det = extractConfigCompound(stack, "detection");
        if (det == null) return DetectionConfig.DEFAULT;

        boolean player     = det.contains("player", Tag.TAG_BYTE) ? det.getBoolean("player") : DetectionConfig.DEFAULT.player();
        boolean vs2        = det.contains("vs2", Tag.TAG_BYTE) ? det.getBoolean("vs2") : DetectionConfig.DEFAULT.vs2();
        boolean contraption= det.contains("contraption", Tag.TAG_BYTE) ? det.getBoolean("contraption") : DetectionConfig.DEFAULT.contraption();
        boolean mob        = det.contains("mob", Tag.TAG_BYTE) ? det.getBoolean("mob") : DetectionConfig.DEFAULT.mob();
        boolean projectile = det.contains("projectile", Tag.TAG_BYTE) ? det.getBoolean("projectile") : DetectionConfig.DEFAULT.projectile();
        boolean animal     = det.contains("animal", Tag.TAG_BYTE) ? det.getBoolean("animal") : DetectionConfig.DEFAULT.animal();
        boolean item       = det.contains("item", Tag.TAG_BYTE) ? det.getBoolean("item") : DetectionConfig.DEFAULT.item();

        return new DetectionConfig(player, vs2, contraption, mob, projectile, animal, item);
    }
    private IdentificationConfig readIdentificationFromSlot() {
        return readIdentificationFromItem(inventory.getStackInSlot(SLOT_IDENT));
    }
    public void dissolveNetwork(ServerLevel level) {
        NetworkData data = NetworkData.get(level);
        if (data == null) return;

        data.dissolveNetworkForBrokenController(level, worldPosition);

        data.setDirty();
    }

    private static IdentificationConfig readIdentificationFromItem(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTag()) return IdentificationConfig.DEFAULT;
        CompoundTag root = stack.getTag();
        if (root == null) return IdentificationConfig.DEFAULT;

        if (root.contains("Filters", Tag.TAG_COMPOUND)) {
            CompoundTag filters = root.getCompound("Filters");
            if (filters.contains("identification", Tag.TAG_COMPOUND)) {
                return IdentificationConfig.fromTag(filters.getCompound("identification"));
            }
        }
        if (root.contains("identification", Tag.TAG_COMPOUND)) {
            return IdentificationConfig.fromTag(root.getCompound("identification"));
        }

        ListTag entriesTag = root.getList("EntriesList", Tag.TAG_STRING);
        ListTag foeTag = root.getList("FriendOrFoeList", Tag.TAG_BYTE);

        int n = Math.min(entriesTag.size(), foeTag.size());
        if (n <= 0 && !root.contains("IDSTRING", Tag.TAG_STRING))
            return IdentificationConfig.DEFAULT;

        List<String> names = new ArrayList<>(n);
        List<Boolean> flags = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            names.add(entriesTag.getString(i));
            flags.add(foeTag.getInt(i) != 0);

        }

        String label = root.getString("IDSTRING");
        return new IdentificationConfig(names, flags, label);
    }
    private TargetingConfig readTargetingFromSlot() {
        return readTargetingFromItem(inventory.getStackInSlot(SLOT_TARGETING));
    }

    private static TargetingConfig readTargetingFromItem(ItemStack stack) {
        CompoundTag inner = extractConfigCompound(stack, "targeting");
        if (inner == null) return TargetingConfig.DEFAULT;

        CompoundTag root = new CompoundTag();
        root.put("targeting", inner);
        return TargetingConfig.fromTag(root);
    }
    @Nullable
    private static CompoundTag extractConfigCompound(ItemStack stack, String key) {
        if (stack == null || stack.isEmpty() || !stack.hasTag()) return null;
        CompoundTag root = stack.getTag();
        if (root == null) return null;

        if (root.contains("Filters", Tag.TAG_COMPOUND)) {
            CompoundTag filters = root.getCompound("Filters");
            if (filters.contains(key, Tag.TAG_COMPOUND)) {
                return filters.getCompound(key);
            }
        }
        if (root.contains(key, Tag.TAG_COMPOUND)) {
            return root.getCompound(key);
        }


        return null;
    }






}
