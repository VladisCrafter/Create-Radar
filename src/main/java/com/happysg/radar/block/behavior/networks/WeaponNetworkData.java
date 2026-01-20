package com.happysg.radar.block.behavior.networks;

import com.happysg.radar.block.behavior.networks.config.TargetingConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class WeaponNetworkData extends SavedData {

    /** A group is uniquely identified by its mount location (dim + pos). */
    public record MountKey(ResourceKey<Level> dim, BlockPos mountPos) {}

    public static class Group {
        public final MountKey key;

        public @Nullable BlockPos yawPos;
        public @Nullable BlockPos pitchPos;
        public @Nullable BlockPos firingPos;

        public CompoundTag targetingTag;
        public final Set<BlockPos> dataLinks = new HashSet<>();

        public Group(MountKey key) {
            this.key = key;
            this.targetingTag = defaultTargetingTag();
        }

        public int controllerCount() {
            int c = 0;
            if (yawPos != null) c++;
            if (pitchPos != null) c++;
            if (firingPos != null) c++;
            return c;
        }

        public boolean isFull() {
            return controllerCount() >= 3;
        }
    }
    public record WeaponGroupView(BlockPos mountPos,
                                  @Nullable BlockPos yawPos,
                                  @Nullable BlockPos pitchPos,
                                  @Nullable BlockPos firingPos) {

        /**
         * All endpoints that exist (yaw/pitch/firing).
         */
        public Set<BlockPos> endpoints() {
            Set<BlockPos> out = new HashSet<>();
            if (yawPos != null) out.add(yawPos);
            if (pitchPos != null) out.add(pitchPos);
            if (firingPos != null) out.add(firingPos);
            return out;
        }

        /**
         * All endpoints except the one you started from.
         */
        public Set<BlockPos> otherEndpoints(BlockPos exclude) {
            Set<BlockPos> out = endpoints();
            out.remove(exclude);
            return out;
        }
    }
    @Nullable
    private Group findGroupByEndpointSlow(ResourceKey<Level> dim, BlockPos endpointPos) {
        for (Group g : groupsByMount.values()) {
            if (!g.key.dim().equals(dim))
                continue;

            if (endpointPos.equals(g.yawPos) || endpointPos.equals(g.pitchPos) || endpointPos.equals(g.firingPos)) {
                return g;
            }
        }
        return null;
    }

    @Nullable
    public WeaponGroupView getWeaponGroupViewFromEndpoint(ResourceKey<Level> dim, BlockPos endpointPos) {
        // Fast path
        String mountKey = controllerToMount.get(key(dim, endpointPos));

        Group g = null;

        if (mountKey != null) {
            g = groupsByMount.get(mountKey);
        }

        // Self-heal path (index missing or stale)
        if (g == null) {
            g = findGroupByEndpointSlow(dim, endpointPos);
            if (g == null)
                return null;

            // rebuild the index for next time
            String mk = key(dim, g.key.mountPos());
            if (g.yawPos != null)    controllerToMount.put(key(dim, g.yawPos), mk);
            if (g.pitchPos != null)  controllerToMount.put(key(dim, g.pitchPos), mk);
            if (g.firingPos != null) controllerToMount.put(key(dim, g.firingPos), mk);

            // also ensure groupsByMount is keyed correctly (just in case)
            groupsByMount.put(mk, g);

            setDirty();
        }

        return new WeaponGroupView(g.key.mountPos(), g.yawPos, g.pitchPos, g.firingPos);
    }


    // "dim|posLong" -> Group
    private final Map<String, Group> groupsByMount = new HashMap<>();

    // index datalink position -> mount key string
    private final Map<String, String> dataLinkToMount = new HashMap<>();

    // index controller position -> mount key string (fast lookup)
    private final Map<String, String> controllerToMount = new HashMap<>();

    // -------------------------
    // SavedData plumbing
    // -------------------------

    public static WeaponNetworkData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                WeaponNetworkData::load,
                WeaponNetworkData::new,
                "radar_mount_links"
        );
    }

    public WeaponNetworkData() {}

    // -------------------------
    // Load / Save
    // -------------------------

    public static WeaponNetworkData load(CompoundTag tag) {
        WeaponNetworkData data = new WeaponNetworkData();

        ListTag groups = tag.getList("Groups", Tag.TAG_COMPOUND);
        for (int i = 0; i < groups.size(); i++) {
            CompoundTag g = groups.getCompound(i);

            ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(g.getString("Dim")));
            BlockPos mountPos = NbtUtils.readBlockPos(g.getCompound("MountPos"));

            String mountKey = key(dim, mountPos);
            Group group = new Group(new MountKey(dim, mountPos));

            if (g.contains("YawPos", Tag.TAG_COMPOUND))   group.yawPos = NbtUtils.readBlockPos(g.getCompound("YawPos"));
            if (g.contains("PitchPos", Tag.TAG_COMPOUND)) group.pitchPos = NbtUtils.readBlockPos(g.getCompound("PitchPos"));
            if (g.contains("FiringPos", Tag.TAG_COMPOUND))group.firingPos = NbtUtils.readBlockPos(g.getCompound("FiringPos"));

            // Targeting tag (optional)
            if (g.contains("Targeting", Tag.TAG_COMPOUND)) {
                group.targetingTag = g.getCompound("Targeting");
            } else {
                group.targetingTag = defaultTargetingTag();
            }

            // Populate controller -> mount index
            if (group.yawPos != null)   data.controllerToMount.put(key(dim, group.yawPos), mountKey);
            if (group.pitchPos != null) data.controllerToMount.put(key(dim, group.pitchPos), mountKey);
            if (group.firingPos != null)data.controllerToMount.put(key(dim, group.firingPos), mountKey);

            // Datalinks
            ListTag links = g.getList("DataLinks", Tag.TAG_COMPOUND);
            for (int j = 0; j < links.size(); j++) {
                BlockPos lp = NbtUtils.readBlockPos(links.getCompound(j));
                group.dataLinks.add(lp);
                data.dataLinkToMount.put(key(dim, lp), mountKey);
            }

            data.groupsByMount.put(mountKey, group);
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag groups = new ListTag();

        for (Group group : groupsByMount.values()) {
            CompoundTag g = new CompoundTag();
            g.putString("Dim", group.key.dim().location().toString());
            g.put("MountPos", NbtUtils.writeBlockPos(group.key.mountPos()));

            if (group.yawPos != null) g.put("YawPos", NbtUtils.writeBlockPos(group.yawPos));
            if (group.pitchPos != null) g.put("PitchPos", NbtUtils.writeBlockPos(group.pitchPos));
            if (group.firingPos != null) g.put("FiringPos", NbtUtils.writeBlockPos(group.firingPos));

            g.put("Targeting", group.targetingTag);

            ListTag links = new ListTag();
            for (BlockPos p : group.dataLinks) {
                links.add(NbtUtils.writeBlockPos(p));
            }
            g.put("DataLinks", links);

            groups.add(g);
        }

        tag.put("Groups", groups);
        return tag;
    }

    // -------------------------
    // Accessors
    // -------------------------

    public @Nullable Group getGroup(ResourceKey<Level> dim, BlockPos mountPos) {
        return groupsByMount.get(key(dim, mountPos));
    }

    public Group getOrCreateGroup(ResourceKey<Level> dim, BlockPos mountPos) {
        String k = key(dim, mountPos);
        return groupsByMount.computeIfAbsent(k, _k -> {
            setDirty();
            return new Group(new MountKey(dim, mountPos));
        });
    }

    public Map<String, Group> getGroups() {
        return Collections.unmodifiableMap(groupsByMount);
    }

    /** Fast lookup: which mount group owns this controller? */
    public @Nullable BlockPos getMountForController(ResourceKey<Level> dim, BlockPos controllerPos) {
        String mk = controllerToMount.get(key(dim, controllerPos));
        if (mk == null) return null;
        return posFromKey(mk);
    }

    /** Fast lookup: get the Group for a controller pos (or null). */
    public @Nullable Group getGroupForController(ResourceKey<Level> dim, BlockPos controllerPos) {
        BlockPos mount = getMountForController(dim, controllerPos);
        return mount == null ? null : getGroup(dim, mount);
    }

    // -------------------------
    // Mutation helpers
    // -------------------------

    /** Used at placement-time: merge new selections into the mount's group. */
    public boolean tryMergeIntoGroup(Group group,
                                     @Nullable BlockPos yaw,
                                     @Nullable BlockPos pitch,
                                     @Nullable BlockPos firing) {

        // Cannot add a different controller of an existing type
        if (yaw != null && group.yawPos != null && !group.yawPos.equals(yaw)) return false;
        if (pitch != null && group.pitchPos != null && !group.pitchPos.equals(pitch)) return false;
        if (firing != null && group.firingPos != null && !group.firingPos.equals(firing)) return false;

        ResourceKey<Level> dim = group.key.dim();
        String mountKey = key(dim, group.key.mountPos());

        // Fill empty slots + update controller index
        if (yaw != null && group.yawPos == null) {
            group.yawPos = yaw;
            controllerToMount.put(key(dim, yaw), mountKey);
        }
        if (pitch != null && group.pitchPos == null) {
            group.pitchPos = pitch;
            controllerToMount.put(key(dim, pitch), mountKey);
        }
        if (firing != null && group.firingPos == null) {
            group.firingPos = firing;
            controllerToMount.put(key(dim, firing), mountKey);
        }

        setDirty();
        return true;
    }

    public void addDataLinkToGroup(Group group, BlockPos dataLinkPos) {
        ResourceKey<Level> dim = group.key.dim();
        String mountKey = key(dim, group.key.mountPos());

        group.dataLinks.add(dataLinkPos);
        dataLinkToMount.put(key(dim, dataLinkPos), mountKey);
        setDirty();
    }

    /** Optional helper: remove a specific controller and keep group if it still has links. */
    public boolean removeController(ResourceKey<Level> dim, BlockPos controllerPos) {
        String mountKey = controllerToMount.remove(key(dim, controllerPos));
        if (mountKey == null) return false;

        Group group = groupsByMount.get(mountKey);
        if (group == null) return true;

        if (controllerPos.equals(group.yawPos)) group.yawPos = null;
        if (controllerPos.equals(group.pitchPos)) group.pitchPos = null;
        if (controllerPos.equals(group.firingPos)) group.firingPos = null;

        // Auto-delete if empty (no links + no controllers)
        cleanupIfEmpty(dim, mountKey, group);

        setDirty();
        return true;
    }

    // -------------------------
    // DataLink removal + cleanup
    // -------------------------

    /**
     * Removes only the DataLink position from the group.
     * Auto-deletes the group if it becomes empty (no links + no controllers).
     */
    public boolean removeDataLink(ResourceKey<Level> dim, BlockPos dataLinkPos) {
        String dlKey = key(dim, dataLinkPos);
        String mountKey = dataLinkToMount.remove(dlKey);
        if (mountKey == null) return false;

        Group group = groupsByMount.get(mountKey);
        if (group == null) {
            setDirty();
            return true;
        }

        group.dataLinks.remove(dataLinkPos);

        // If group is now empty, delete it
        cleanupIfEmpty(dim, mountKey, group);

        setDirty();
        return true;
    }

    /**
     * Strong auto-delete behavior:
     * - removes the DataLink
     * - if that was the last DataLink, clears controllers and deletes the whole group
     */
    public void removeDataLinkAndCleanup(ResourceKey<Level> dim, BlockPos dataLinkPos) {
        String dlKey = key(dim, dataLinkPos);
        String mountKey = dataLinkToMount.remove(dlKey);
        if (mountKey == null) return;

        Group group = groupsByMount.get(mountKey);
        if (group == null) {
            setDirty();
            return;
        }

        group.dataLinks.remove(dataLinkPos);

        // If no DataLinks remain, remove all controllers + delete group
        if (group.dataLinks.isEmpty()) {
            if (group.yawPos != null) {
                controllerToMount.remove(key(dim, group.yawPos));
                group.yawPos = null;
            }
            if (group.pitchPos != null) {
                controllerToMount.remove(key(dim, group.pitchPos));
                group.pitchPos = null;
            }
            if (group.firingPos != null) {
                controllerToMount.remove(key(dim, group.firingPos));
                group.firingPos = null;
            }

            groupsByMount.remove(mountKey);
        } else {
            // Otherwise just delete group if it's totally empty (shouldn't happen here)
            cleanupIfEmpty(dim, mountKey, group);
        }

        setDirty();
    }

    private void cleanupIfEmpty(ResourceKey<Level> dim, String mountKey, Group group) {
        if (!group.dataLinks.isEmpty()) return;
        if (group.controllerCount() != 0) return;

        // no links, no controllers => delete group
        groupsByMount.remove(mountKey);
    }

    // -------------------------
    // Validation (no mutation)
    // -------------------------

    public boolean canMergeIntoGroup(Group group,
                                     @Nullable BlockPos yaw,
                                     @Nullable BlockPos pitch,
                                     @Nullable BlockPos firing) {

        if (group.isFull()) return false;

        if (yaw != null && group.yawPos != null && !group.yawPos.equals(yaw)) return false;
        if (pitch != null && group.pitchPos != null && !group.pitchPos.equals(pitch)) return false;
        if (firing != null && group.firingPos != null && !group.firingPos.equals(firing)) return false;

        return true;
    }

    // -------------------------
    // Key helpers
    // -------------------------

    private static String key(ResourceKey<Level> dim, BlockPos pos) {
        return dim.location() + "|" + pos.asLong();
    }

    /** Extract BlockPos from "dim|posLong". Dimension is ignored here since caller already knows it. */
    private static BlockPos posFromKey(String key) {
        int idx = key.indexOf('|');
        long packed = Long.parseLong(key.substring(idx + 1));
        return BlockPos.of(packed);
    }

    private static CompoundTag defaultTargetingTag() {
        CompoundTag root = new CompoundTag();
        root.put("targeting", TargetingConfig.DEFAULT.toTag());
        return root;
    }
}
