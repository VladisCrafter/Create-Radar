package com.happysg.radar.block.behavior.networks;

import com.happysg.radar.block.behavior.networks.config.DetectionConfig;
import com.happysg.radar.block.behavior.networks.config.IdentificationConfig;
import com.happysg.radar.block.behavior.networks.config.TargetingConfig;
import com.happysg.radar.block.monitor.MonitorBlockEntity;
import com.mojang.logging.LogUtils;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;

public class NetworkData extends SavedData {

    public enum RadarKind { BEARING, STATIONARY }
    public enum Mountkind { NORMAL, FIXED, COMPACT}


    public record FilterKey(ResourceKey<Level> dim, BlockPos filtererPos) {}
    public Set<BlockPos> getWeaponEndpoints(Group group) {
        return Collections.unmodifiableSet(group.weaponEndpoints);
    }
    public static class Group {
        public final FilterKey key;
        public @Nullable String selectedTargetId;
        public @Nullable BlockPos monitorPos;

        public @Nullable BlockPos radarPos;
        public @Nullable RadarKind radarKind;

        /** Controllers linked into this filter group. */
        /** Controllers linked into this filter group. */
        public final Set<BlockPos> weaponEndpoints = new HashSet<>();

        /** Used weapon mounts (constraint set). */
        public final Set<BlockPos> usedWeaponMounts = new HashSet<>();

        /** DataLink blocks belonging to this group. */
        public final Set<BlockPos> dataLinks = new HashSet<>();

        /** Filter tags */
        public CompoundTag targetingTag = defaultTargetingTag();
        public CompoundTag identificationTag = defaultIdentificationTag();
        public CompoundTag detectionTag = defaultDetectionTag();

        public Group(FilterKey key) {
            this.key = key;
        }
    }


    // filtererKey -> group
    private final Map<String, Group> groupsByFilterer = new HashMap<>();

    // endpointPos -> filtererKey
    private final Map<String, String> endpointToFilterer = new HashMap<>();

    // weaponMountPos -> filtererKey (enforces uniqueness)
    private final Map<String, String> weaponMountToFilterer = new HashMap<>();

    // datalinkPos -> filtererKey
    private final Map<String, String> dataLinkToFilterer = new HashMap<>();

    // datalinkPos -> endpointPos
    private final Map<String, String> dataLinkToEndpoint = new HashMap<>();

    // controllerPos -> weaponMountPos  (PERSISTED so cleanup works)
    private final Map<String, String> controllerToWeaponMount = new HashMap<>();


    public static NetworkData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                NetworkData::load,
                NetworkData::new,
                "network_data"
        );
    }
    public void setSelectedTargetId(Group g, @Nullable String id) {
        g.selectedTargetId = id;
        setDirty();
    }

    @Nullable
    public String getSelectedTargetId(Group g) {
        return g.selectedTargetId;
    }

    public NetworkData() {}
    public void dissolveNetworkForBrokenController(ServerLevel level, BlockPos brokenPos) {
        ResourceKey<Level> dim = level.dimension();

        // 1) If the broken block is the filterer/controller itself, dissolve that group
        String filtererKey = filtererKey(new FilterKey(dim, brokenPos));
        if (groupsByFilterer.containsKey(filtererKey)) {
            dissolveGroup(level, filtererKey);
            setDirty();
            return;
        }

        // 2) If the broken block is a weapon controller, look up its mount and dissolve its group
        String controllerKey = posKey(dim, brokenPos);
        String mountKey = controllerToWeaponMount.remove(controllerKey);
        if (mountKey != null) {
            // mountKey -> filtererKey
            String owningFiltererKey = weaponMountToFilterer.get(mountKey);
            if (owningFiltererKey != null && groupsByFilterer.containsKey(owningFiltererKey)) {
                dissolveGroup(level, owningFiltererKey);
                setDirty();
            }
        }
    }

    /**
     * Fully deletes a group and all of its reverse-map references.
     */
    private void dissolveGroup(ServerLevel level, String filtererKey) {
        Group group = groupsByFilterer.remove(filtererKey);
        if (group == null) return;

        // tell loaded nodes they are no longer linked (optional but nice)
        notifyNodeDisconnected(level, group.monitorPos);
        notifyNodeDisconnected(level, group.radarPos);
        //notifyNodeDisconnected(level, group.);

        for (BlockPos endpointPos : group.weaponEndpoints) {
            notifyNodeDisconnected(level, endpointPos);
            endpointToFilterer.remove(posKey(level.dimension(), endpointPos));
        }


        for (BlockPos mountPos : group.usedWeaponMounts) {
            String mountKey = posKey(level.dimension(), mountPos);
            weaponMountToFilterer.remove(mountKey);

            // remove any controller->mount mappings that point at this mount
            controllerToWeaponMount.entrySet().removeIf(e -> mountKey.equals(e.getValue()));
        }

        for (BlockPos dlPos : group.dataLinks) {
            String dlKey = posKey(level.dimension(), dlPos);
            dataLinkToFilterer.remove(dlKey);
            dataLinkToEndpoint.remove(dlKey);
            notifyNodeDisconnected(level, dlPos);
        }

        // remove the group key itself from any other indexes if needed
        // (most of your indexes are removed above; this is just a safety sweep)
        endpointToFilterer.values().removeIf(filtererKey::equals);
        weaponMountToFilterer.values().removeIf(filtererKey::equals);
        dataLinkToFilterer.values().removeIf(filtererKey::equals);
    }

    /**
     * Soft callback for anything that cares about network disconnect.
     * Safe even if the BE doesn't implement anything.
     */
    private void notifyNodeDisconnected(ServerLevel level, @Nullable BlockPos pos) {
        if (pos == null) return;

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof INetworkNode node) {
            node.onNetworkDisconnected();
        }
    }

// ------------------------------------------------------------
// Key helpers (string keys used by your maps)
// ------------------------------------------------------------
@Nullable
public static BlockPos getFiltererPosFromGroupKey(@Nullable String filtererKey) {
    if (filtererKey == null || filtererKey.isEmpty()) return null;
    return posFromKey(filtererKey);
}
    public Map<String, Group> getGroups() {
        return Collections.unmodifiableMap(groupsByFilterer);
    }

    private static String filtererKey(FilterKey key) {
        return key.dim().location() + "|" + key.filtererPos().asLong();
    }

    private static String posKey(ResourceKey<Level> dim, BlockPos pos) {
        return dim.location() + "|" + pos.asLong();
    }
    // ------------------------------------------------------------
    // Save / Load
    // ------------------------------------------------------------

    public static NetworkData load(CompoundTag root) {
        NetworkData data = new NetworkData();

        // Groups
        ListTag groupsTag = root.getList("Groups", Tag.TAG_COMPOUND);
        for (int i = 0; i < groupsTag.size(); i++) {
            CompoundTag g = groupsTag.getCompound(i);

            ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(g.getString("Dim")));
            BlockPos filtererPos = NbtUtils.readBlockPos(g.getCompound("FiltererPos"));
            String groupKey = key(dim, filtererPos);

            Group group = new Group(new FilterKey(dim, filtererPos));

            group.targetingTag = g.contains("Targeting", Tag.TAG_COMPOUND) ? g.getCompound("Targeting") : defaultTargetingTag();
            group.identificationTag = g.contains("Identification", Tag.TAG_COMPOUND) ? g.getCompound("Identification") : defaultIdentificationTag();
            group.detectionTag = g.contains("Detection", Tag.TAG_COMPOUND) ? g.getCompound("Detection") : defaultDetectionTag();
            group.selectedTargetId = g.contains("SelectedTargetId", Tag.TAG_STRING) ? g.getString("SelectedTargetId") : null;


            if (g.contains("MonitorPos", Tag.TAG_COMPOUND)) {
                group.monitorPos = NbtUtils.readBlockPos(g.getCompound("MonitorPos"));
                data.endpointToFilterer.put(key(dim, group.monitorPos), groupKey);
            }

            if (g.contains("RadarPos", Tag.TAG_COMPOUND)) {
                group.radarPos = NbtUtils.readBlockPos(g.getCompound("RadarPos"));
                group.radarKind = RadarKind.valueOf(g.getString("RadarKind"));
                data.endpointToFilterer.put(key(dim, group.radarPos), groupKey);
            }

            // weapon endpoints
            ListTag weapons = g.getList("WeaponEndpoints", Tag.TAG_COMPOUND);
            for (int w = 0; w < weapons.size(); w++) {
                BlockPos ep = NbtUtils.readBlockPos(weapons.getCompound(w));
                group.weaponEndpoints.add(ep);
                data.endpointToFilterer.put(key(dim, ep), groupKey);
            }

            // used mounts
            ListTag usedMounts = g.getList("UsedWeaponMounts", Tag.TAG_COMPOUND);
            for (int m = 0; m < usedMounts.size(); m++) {
                BlockPos mp = NbtUtils.readBlockPos(usedMounts.getCompound(m));
                group.usedWeaponMounts.add(mp);
                data.weaponMountToFilterer.put(key(dim, mp), groupKey);
            }

            // datalinks
            ListTag links = g.getList("DataLinks", Tag.TAG_COMPOUND);
            for (int d = 0; d < links.size(); d++) {
                BlockPos lp = NbtUtils.readBlockPos(links.getCompound(d));
                group.dataLinks.add(lp);
                data.dataLinkToFilterer.put(key(dim, lp), groupKey);
            }

            data.groupsByFilterer.put(groupKey, group);
        }

        // dataLinkToEndpoint
        if (root.contains("DataLinkToEndpoint", Tag.TAG_LIST)) {
            ListTag list = root.getList("DataLinkToEndpoint", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag e = list.getCompound(i);
                String dl = e.getString("DL");
                String ep = e.getString("EP");
                if (!dl.isEmpty() && !ep.isEmpty()) data.dataLinkToEndpoint.put(dl, ep);
            }
        }

        // controllerToWeaponMount
        if (root.contains("ControllerToWeaponMount", Tag.TAG_LIST)) {
            ListTag list = root.getList("ControllerToWeaponMount", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag e = list.getCompound(i);
                String c = e.getString("C");
                String m = e.getString("M");
                if (!c.isEmpty() && !m.isEmpty()) data.controllerToWeaponMount.put(c, m);
            }
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag root) {
        ListTag groupsTag = new ListTag();

        for (Group group : groupsByFilterer.values()) {

            CompoundTag g = new CompoundTag();
            g.putString("Dim", group.key.dim().location().toString());
            g.put("FiltererPos", NbtUtils.writeBlockPos(group.key.filtererPos()));

            g.put("Targeting", group.targetingTag);
            g.put("Identification", group.identificationTag);
            g.put("Detection", group.detectionTag);
            if (group.selectedTargetId != null)
                g.putString("SelectedTargetId", group.selectedTargetId);

            if (group.monitorPos != null) g.put("MonitorPos", NbtUtils.writeBlockPos(group.monitorPos));

            if (group.radarPos != null && group.radarKind != null) {
                g.put("RadarPos", NbtUtils.writeBlockPos(group.radarPos));
                g.putString("RadarKind", group.radarKind.name());
            }

            ListTag weapons = new ListTag();
            for (BlockPos ep : group.weaponEndpoints) weapons.add(NbtUtils.writeBlockPos(ep));
            g.put("WeaponEndpoints", weapons);

            ListTag usedMounts = new ListTag();
            for (BlockPos mp : group.usedWeaponMounts) usedMounts.add(NbtUtils.writeBlockPos(mp));
            g.put("UsedWeaponMounts", usedMounts);

            ListTag links = new ListTag();
            for (BlockPos lp : group.dataLinks) links.add(NbtUtils.writeBlockPos(lp));
            g.put("DataLinks", links);

            groupsTag.add(g);
        }

        root.put("Groups", groupsTag);

        // Persist dataLinkToEndpoint
        ListTag dl2ep = new ListTag();
        for (var e : dataLinkToEndpoint.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putString("DL", e.getKey());
            t.putString("EP", e.getValue());
            dl2ep.add(t);
        }
        root.put("DataLinkToEndpoint", dl2ep);

        // Persist controllerToWeaponMount
        ListTag c2m = new ListTag();
        for (var e : controllerToWeaponMount.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putString("C", e.getKey());
            t.putString("M", e.getValue());
            c2m.add(t);
        }
        root.put("ControllerToWeaponMount", c2m);

        return root;
    }

    // ------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------

    public @Nullable Group getGroup(ResourceKey<Level> dim, BlockPos filtererPos) {
        return groupsByFilterer.get(key(dim, filtererPos));
    }

    public Group getOrCreateGroup(ResourceKey<Level> dim, BlockPos filtererPos) {
        String k = key(dim, filtererPos);
        return groupsByFilterer.computeIfAbsent(k, _k -> {
            setDirty();
            return new Group(new FilterKey(dim, filtererPos));
        });
    }

    public @Nullable BlockPos getFiltererForEndpoint(ResourceKey<Level> dim, BlockPos endpointPos) {
        String fk = endpointToFilterer.get(key(dim, endpointPos));
        return fk == null ? null : posFromKey(fk);
    }

    public @Nullable BlockPos getFiltererForWeaponMount(ResourceKey<Level> dim, BlockPos weaponMountPos) {
        String fk = weaponMountToFilterer.get(key(dim, weaponMountPos));
        return fk == null ? null : posFromKey(fk);
    }

    public @Nullable BlockPos getFiltererForDataLink(ResourceKey<Level> dim, BlockPos dataLinkPos) {
        String fk = dataLinkToFilterer.get(key(dim, dataLinkPos));
        return fk == null ? null : posFromKey(fk);
    }

    // ------------------------------------------------------------
    // Validation
    // ------------------------------------------------------------

    public boolean canAttachMonitor(Group group, BlockPos monitorPos) {
        if (group.monitorPos != null && !group.monitorPos.equals(monitorPos)) return false;
        String endpointKey = key(group.key.dim(), monitorPos);
        String existing = endpointToFilterer.get(endpointKey);
        String myKey = key(group.key.dim(), group.key.filtererPos());
        return existing == null || existing.equals(myKey);
    }

    public boolean canAttachRadar(Group group, BlockPos radarPos, RadarKind kind) {
        if (group.radarPos != null && !group.radarPos.equals(radarPos)) return false;
        if (group.radarKind != null && group.radarKind != kind) return false;
        String endpointKey = key(group.key.dim(), radarPos);
        String existing = endpointToFilterer.get(endpointKey);
        String myKey = key(group.key.dim(), group.key.filtererPos());
        return existing == null || existing.equals(myKey);
    }

    public boolean canAttachWeaponEndpoint(Group group, BlockPos controllerPos, BlockPos weaponMountPos) {
        String myKey = key(group.key.dim(), group.key.filtererPos());

        // controller already owned by other group?
        String endpointK = key(group.key.dim(), controllerPos);
        String existingEndpointOwner = endpointToFilterer.get(endpointK);
        if (existingEndpointOwner != null && !existingEndpointOwner.equals(myKey)) return false;

        // mount already owned by other group?
        String mountK = key(group.key.dim(), weaponMountPos);
        String existingMountOwner = weaponMountToFilterer.get(mountK);
        if (existingMountOwner != null && !existingMountOwner.equals(myKey)) return false;

        // in-group uniqueness
        return !group.usedWeaponMounts.contains(weaponMountPos);
    }

    // ------------------------------------------------------------
    // Mutations (commit)
    // ------------------------------------------------------------

    public void attachMonitor(ServerLevel level,Group group, BlockPos monitorPos) {
        ResourceKey<Level> dim = group.key.dim();
        String filtererKey = key(dim, group.key.filtererPos());

        group.monitorPos = monitorPos;

        // index the clicked monitor pos
        endpointToFilterer.put(key(dim, monitorPos), filtererKey);


        if (level != null) {
            var be = level.getBlockEntity(monitorPos);
            if (be instanceof MonitorBlockEntity m) {
                BlockPos controllerPos = m.getControllerPos();
                endpointToFilterer.put(key(dim, controllerPos), filtererKey);
            }
        }

        setDirty();
    }


    public void attachRadar(Group group, BlockPos radarPos, RadarKind kind) {
        ResourceKey<Level> dim = group.key.dim();
        String filtererKey = key(dim, group.key.filtererPos());

        group.radarPos = radarPos;
        group.radarKind = kind;

        endpointToFilterer.put(key(dim, radarPos), filtererKey);

        setDirty();
    }


    public void attachWeaponEndpoint(Group group, BlockPos controllerPos, BlockPos weaponMountPos) {
        group.weaponEndpoints.add(controllerPos);
        group.usedWeaponMounts.add(weaponMountPos);

        String dimKey = group.key.dim().location().toString();
        endpointToFilterer.put(key(group.key.dim(), controllerPos), key(group.key.dim(), group.key.filtererPos()));
        weaponMountToFilterer.put(key(group.key.dim(), weaponMountPos), key(group.key.dim(), group.key.filtererPos()));

        // controller -> mount mapping
        controllerToWeaponMount.put(key(group.key.dim(), controllerPos), key(group.key.dim(), weaponMountPos));

        setDirty();
    }

    public void addDataLinkToGroup(Group group, BlockPos dataLinkPos, BlockPos endpointPos) {
        String gk = key(group.key.dim(), group.key.filtererPos());
        group.dataLinks.add(dataLinkPos);
        dataLinkToFilterer.put(key(group.key.dim(), dataLinkPos), gk);
        dataLinkToEndpoint.put(key(group.key.dim(), dataLinkPos), key(group.key.dim(), endpointPos));
        setDirty();
    }
    public void retargetEndpoint(ResourceKey<Level> dim, BlockPos oldEndpoint, BlockPos newEndpoint) {
        if (oldEndpoint == null || newEndpoint == null || oldEndpoint.equals(newEndpoint))
            return;

        String oldK = key(dim, oldEndpoint);
        String newK = key(dim, newEndpoint);


        String filtererKey = endpointToFilterer.remove(oldK);
        if (filtererKey == null) return;


        endpointToFilterer.put(newK, filtererKey);

        Group group = groupsByFilterer.get(filtererKey);
        if (group != null) {
            if (oldEndpoint.equals(group.monitorPos)) group.monitorPos = newEndpoint;
            if (oldEndpoint.equals(group.radarPos)) group.radarPos = newEndpoint;

            if (group.weaponEndpoints.remove(oldEndpoint)) {
                group.weaponEndpoints.add(newEndpoint);
            }
        }

        for (Map.Entry<String, String> e : dataLinkToEndpoint.entrySet()) {
            if (oldK.equals(e.getValue())) {
                e.setValue(newK);
            }
        }

        setDirty();
    }

    private static final Logger LOGGER = LogUtils.getLogger();

// In NetworkData
    @Nullable
    public BlockPos peekEndpointForDataLink(ResourceKey<Level> dim, BlockPos dataLinkPos) {
        String endpointKey = dataLinkToEndpoint.get(key(dim, dataLinkPos));
        return endpointKey == null ? null : posFromKey(endpointKey);
    }


    public void removeDataLinkAndCleanup(ResourceKey<Level> dim, BlockPos dataLinkPos, @Nullable ServerLevel level) {
        String dlKey = key(dim, dataLinkPos);

        String filtererKey = dataLinkToFilterer.remove(dlKey);
        String endpointKey = dataLinkToEndpoint.remove(dlKey);

        if (filtererKey == null) {
            setDirty();
            return;
        }

        Group group = groupsByFilterer.get(filtererKey);
        if (group != null) {
            group.dataLinks.remove(dataLinkPos);
        }

        if (endpointKey != null && group != null) {
            BlockPos endpointPos = posFromKey(endpointKey);
            LOGGER.warn("Pos = " +endpointPos.toString());
            if (group != null && level != null && group.monitorPos != null) {

                BlockPos normalizedEndpoint = endpointPos;

                BlockEntity endpointBe = level.getBlockEntity(endpointPos);
                if (endpointBe instanceof MonitorBlockEntity endpointMonitor) {
                    BlockPos controllerPos = endpointMonitor.getControllerPos();
                    if (controllerPos != null) {
                        normalizedEndpoint = controllerPos;
                    }
                }


                boolean isMonitorEndpoint =
                        normalizedEndpoint.equals(group.monitorPos) || endpointPos.equals(group.monitorPos);

                if (isMonitorEndpoint) {
                    BlockEntity controllerBe = level.getBlockEntity(group.monitorPos);

                    // if the controller BE isn't loaded for some reason, fall back
                    if (!(controllerBe instanceof MonitorBlockEntity) && endpointBe instanceof MonitorBlockEntity) {
                        controllerBe = endpointBe;
                    }

                    if (controllerBe instanceof MonitorBlockEntity monitor) {
                        monitor.onNetworkDisconnected();
                    }

                    group.monitorPos = null;
                    endpointToFilterer.remove(endpointKey);
                }






        } else if (endpointPos.equals(group.radarPos)) {
                group.radarPos = null;
                group.radarKind = null;
                endpointToFilterer.remove(endpointKey);

            } else if (group.weaponEndpoints.remove(endpointPos)) {
                endpointToFilterer.remove(endpointKey);

                // If you use controllerToWeaponMount, free it deterministically
                String mountKey = controllerToWeaponMount.remove(endpointKey);
                if (mountKey != null) {
                    BlockPos mp = posFromKey(mountKey);
                    group.usedWeaponMounts.remove(mp);
                    weaponMountToFilterer.remove(mountKey);
                }
            }
        }

        // autodelete empty group
        cleanupIfEmpty(filtererKey);

        setDirty();
    }


    private void cleanupIfEmpty(String filtererKey) {
        Group group = groupsByFilterer.get(filtererKey);
        if (group == null) return;

        boolean empty =
                group.monitorPos == null &&
                        group.radarPos == null &&
                        group.weaponEndpoints.isEmpty() &&
                        group.dataLinks.isEmpty();

        if (!empty) return;

        ResourceKey<Level> dim = group.key.dim();

        if (group.monitorPos != null) endpointToFilterer.remove(key(dim, group.monitorPos));
        if (group.radarPos != null) endpointToFilterer.remove(key(dim, group.radarPos));

        for (BlockPos ep : group.weaponEndpoints) endpointToFilterer.remove(key(dim, ep));
        for (BlockPos mp : group.usedWeaponMounts) weaponMountToFilterer.remove(key(dim, mp));
        for (BlockPos dl : group.dataLinks) {
            dataLinkToFilterer.remove(key(dim, dl));
            dataLinkToEndpoint.remove(key(dim, dl));
        }

        groupsByFilterer.remove(filtererKey);
    }


    public void onEndpointRemoved(ResourceKey<Level> dim, BlockPos endpointPos) {
        if (endpointPos == null) return;

        String endpointKey = key(dim, endpointPos);
        String filtererKey = endpointToFilterer.get(endpointKey);
        if (filtererKey == null) return;

        Group group = groupsByFilterer.get(filtererKey);
        if (group == null) {
            endpointToFilterer.remove(endpointKey);
            return;
        }

        if (endpointPos.equals(group.monitorPos)) {
            group.monitorPos = null;
            endpointToFilterer.remove(endpointKey);
        } else if (endpointPos.equals(group.radarPos)) {
            group.radarPos = null;
            group.radarKind = null;
            endpointToFilterer.remove(endpointKey);
        } else if (group.weaponEndpoints.remove(endpointPos)) {

            endpointToFilterer.remove(endpointKey);

            String mountKey = controllerToWeaponMount.remove(endpointKey);
            if (mountKey != null) {
                BlockPos mp = posFromKey(mountKey);
                group.usedWeaponMounts.remove(mp);
                weaponMountToFilterer.remove(mountKey);
            }
        } else {
            endpointToFilterer.remove(endpointKey);
        }


        List<String> dlKeysToRemove = new ArrayList<>();
        for (Map.Entry<String, String> e : dataLinkToEndpoint.entrySet()) {
            if (endpointKey.equals(e.getValue())) {
                dlKeysToRemove.add(e.getKey());
            }
        }

        for (String dlKey : dlKeysToRemove) {
            dataLinkToEndpoint.remove(dlKey);

            String owner = dataLinkToFilterer.get(dlKey);
            if (filtererKey.equals(owner)) {
                dataLinkToFilterer.remove(dlKey);

                BlockPos dlPos = posFromKey(dlKey);
                group.dataLinks.remove(dlPos);
            }
        }

        cleanupIfEmpty(filtererKey);

        setDirty();
    }


    public void setTargetingConfig(Group group, TargetingConfig cfg) {
        CompoundTag root = new CompoundTag();
        root.put("targeting", cfg.toTag());
        group.targetingTag = root;
        setDirty();
    }

    public void setAllFilters(Group group, TargetingConfig t, IdentificationConfig i, DetectionConfig d) {
        CompoundTag root = new CompoundTag();
        root.put("targeting", t.toTag());
        group.targetingTag = root;

        group.identificationTag = i.toTag();
        group.detectionTag = d.toTag();

        setDirty();
    }


    private static CompoundTag defaultTargetingTag() {
        CompoundTag root = new CompoundTag();
        root.put("targeting", TargetingConfig.DEFAULT.toTag());
        return root;
    }

    private static CompoundTag defaultIdentificationTag() {
        return IdentificationConfig.DEFAULT.toTag();
    }

    private static CompoundTag defaultDetectionTag() {
        return DetectionConfig.DEFAULT.toTag();
    }

    private static String key(ResourceKey<Level> dim, BlockPos pos) {
        return dim.location() + "|" + pos.asLong();
    }

    private static BlockPos posFromKey(String key) {
        int idx = key.indexOf('|');
        long packed = Long.parseLong(key.substring(idx + 1));
        return BlockPos.of(packed);
    }

    public Map<String, Group> getGroupsByFiltererView() {
        return Collections.unmodifiableMap(groupsByFilterer);
    }
    @Nullable
    public Group getGroupForEndpoint(ResourceKey<Level> dim, BlockPos endpointPos) {
        BlockPos filterer = getFiltererForEndpoint(dim, endpointPos);
        if (filterer == null) return null;
        return getGroup(dim, filterer);
    }


}
