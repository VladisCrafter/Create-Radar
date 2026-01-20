package com.happysg.radar.block.monitor;

import com.happysg.radar.block.behavior.networks.INetworkNode;
import com.happysg.radar.block.behavior.networks.NetworkData;
import com.happysg.radar.block.behavior.networks.config.DetectionConfig;
import com.happysg.radar.block.behavior.networks.config.TargetingConfig;
import com.happysg.radar.block.controller.networkcontroller.NetworkFiltererBlockEntity;
import com.happysg.radar.block.datalink.DataLinkBlock;
import com.happysg.radar.block.radar.behavior.IRadar;
import com.happysg.radar.block.radar.track.RadarTrack;
import com.happysg.radar.block.radar.track.RadarTrackUtil;
import com.happysg.radar.block.radar.track.TrackCategory;
import com.happysg.radar.compat.vs2.PhysicsHandler;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.Ping;
import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.api.equipment.goggles.IHaveHoveringInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class MonitorBlockEntity extends SmartBlockEntity implements IHaveHoveringInformation, INetworkNode  {

    protected BlockPos controller;
    protected int radius = 1;

    /** Client must rely on this field (synced). Server also keeps it as cache. */
    protected @Nullable BlockPos radarPos;

    protected @Nullable IRadar radar;
    protected String hoveredEntity;
    protected String selectedEntity;
    protected RadarTrack activetrack;

    protected BlockPos mountBlock;

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Client renders from this list (synced via packet). */
    protected Collection<RadarTrack> cachedTracks = new ArrayList<>();

    /** Keep as field because renderer uses it (coloring). */
    protected DetectionConfig filter = DetectionConfig.DEFAULT;

    public final List<AABB> safeZones = new ArrayList<>();

    public MonitorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void initialize() {
        super.initialize();
        updateCacheServerOrClient();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    // -------------------------------------------------
    // Tick / sync
    // -------------------------------------------------

    @Override
    public void tick() {
        super.tick();

        if (level == null)
            return;
        if(activetrack != null){
            setSelectedTargetServer(activetrack);
        }
        if (!level.isClientSide && level instanceof ServerLevel sl) {
            if (level.getGameTime() % 5 == 0) {
                syncFromNetwork(sl);
                updateCacheServerOrClient();
                sendData();
            }
        }
        this.getRadar().ifPresent(radar -> {
            if (!radar.isRunning()) {
                setSelectedTargetServer(null);
            }
        });
    }
    public void onDataLinkRemoved() {
        // clear any cached network state
        this.activetrack = null;
        this.radarPos = null;
        this.radar = null;
        this.controller = null;


        LOGGER.warn("Reset " + controller +" " +radar + radarPos);
        // force client + server refresh
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
    public void onNetworkDisconnected(){
        onDataLinkRemoved();
    }



    private void syncFromNetwork(ServerLevel sl) {
        // network group may be null if not linked yet
        NetworkData.Group g = getNetworkGroup(sl);
        if (g == null) {
            // no network group: keep legacy radarPos/filter as-is
            return;
        }

        // Pull network radar pos (client cannot do this)
        BlockPos netRadar = g.radarPos;
        if (!Objects.equals(netRadar, radarPos)) {
            radarPos = netRadar;
            radar = null; // force re-resolve
        }

        // Pull network detection filter so client uses correct colors + server filters tracks correctly
        filter = DetectionConfig.fromTag(g.detectionTag);
    }
    public void setSelectedTargetServer(@Nullable RadarTrack track) {
        if (level == null || level.isClientSide)
            return;
        if (!(level instanceof ServerLevel sl))
            return;
        //LOGGER.warn("ping");
        MonitorBlockEntity controllerBe = getController();
        if (controllerBe == null)
            return;

        NetworkData.Group g = controllerBe.getNetworkGroup(sl);
        if (g == null)
            return;

        // // Update monitor-side state
        if (track == null) {
            controllerBe.selectedEntity = null;
            controllerBe.activetrack = null;
            LOGGER.warn("null");
        } else {
            controllerBe.selectedEntity = track.getId();
            controllerBe.activetrack = track;
        }

        // // Forward selection to the filterer BE at the group’s filterer position
        BlockPos filterpos = g.key.filtererPos();
        if (level.getBlockEntity(filterpos) instanceof NetworkFiltererBlockEntity filtererBe) {
           // LOGGER.warn("Ping");
            filtererBe.receiveSelectedTargetFromMonitor(track, safeZones);
        }
        controllerBe.setChanged();
        controllerBe.sendData();
    }


    // -------------------------------------------------
    // Network helpers (server only)
    // -------------------------------------------------

    @Nullable
    private NetworkData.Group getNetworkGroup(ServerLevel sl) {
        NetworkData data = NetworkData.get(sl);

        BlockPos endpointPos = getControllerPos();
        BlockPos filtererPos = data.getFiltererForEndpoint(sl.dimension(), endpointPos);
        if (filtererPos == null)
            return null;

        NetworkData.Group g = data.getGroup(sl.dimension(), filtererPos);
        if (g == null)
            return null;

        // If the group doesn't currently have a monitor endpoint, i'm not linked.
        if (g.monitorPos == null)
            return null;

        // Multiblock-safe: i only count as linked if my controller matches the group monitorPos
        if (!endpointPos.equals(g.monitorPos))
            return null;

        return g;
    }


    // -------------------------------------------------
    // Cache / radar resolve
    // -------------------------------------------------

    /** Updates cachedTracks. Server uses real radar tracks; client uses packet-populated cachedTracks. */
    public void updateCacheServerOrClient() {
        if (level == null) return;

        // Client: DO NOT rebuild cachedTracks; it should come from packets.
        if (level.isClientSide) {
            // If radarPos got cleared, clean up selection state.
            if (radarPos == null) {
                cachedTracks = List.of();
                activetrack = null;
                selectedEntity = null;
            }
            return;
        }

        // Server: rebuild and apply filter
        Optional<IRadar> r = getRadar();
        if (r.isEmpty()) {
            cachedTracks = List.of();
            activetrack = null;
            selectedEntity = null;
            return;
        }

        IRadar radar = r.get();
        DetectionConfig det = this.filter; // already synced from network (or legacy)

        cachedTracks = radar.getTracks().stream()
                .filter(det::test)
                .toList();

        // If the active track is no longer in the cache, handle it
        if (activetrack != null && cachedTracks.stream().noneMatch(t -> t.getId().equals(activetrack.getId()))) {
            setSelectedTargetServer(null);
        }
    }

    public boolean isLinked() {
        return getRadarCenterPos() != null;
    }


    public Optional<IRadar> getRadar() {
        if (level == null) return Optional.empty();
        if (!isLinked()) return Optional.empty();

        // Client: can't read SavedData, so use synced radarPos
        if (level.isClientSide) {
            if (radarPos == null) return Optional.empty();

            if (radar instanceof net.minecraft.world.level.block.entity.BlockEntity be
                    && be.getBlockPos().equals(radarPos)) {
                return Optional.of(radar);
            }

            radar = null;
            if (level.getBlockEntity(radarPos) instanceof IRadar r) radar = r;
            return Optional.ofNullable(radar);
        }

        // Server: radarPos will be set from network (or legacy) already
        if (radarPos == null) {
            radar = null;
            return Optional.empty();
        }

        if (radar instanceof net.minecraft.world.level.block.entity.BlockEntity be
                && be.getBlockPos().equals(radarPos)) {
            return Optional.of(radar);
        }

        radar = null;
        if (level.getBlockEntity(radarPos) instanceof IRadar r) {
            radar = r;
        }
        return Optional.ofNullable(radar);
    }

    // Basics

    public BlockPos getControllerPos() {

        if (controller == null) return worldPosition;
        return controller;
    }


    public int getSize() {
        return radius;
    }

    public void setControllerPos(BlockPos newController, int size) {
        if (level instanceof ServerLevel sl) {
            BlockPos oldController = this.controller == null ? worldPosition : this.controller;
            NetworkData data = NetworkData.get(sl);
            data.retargetEndpoint(sl.dimension(), oldController, newController);
        }

        this.controller = newController;
        this.radius = size;
        setChanged();
        sendData();
    }


    public boolean isController() {
        return worldPosition.equals(getControllerPos());
    }


    public MonitorBlockEntity getController() {
        if (isController()) return this;
        if (level != null && level.getBlockEntity(controller) instanceof MonitorBlockEntity controllerBe)
            return controllerBe;
        return this;
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return super.createRenderBoundingBox().inflate(10);
    }

    public Collection<RadarTrack> getTracks() {
        return cachedTracks;
    }

    public float getRange() {
        return getRadar().map(IRadar::getRange).orElse(0f);
    }

    @Nullable
    public Vec3 getRadarCenterPos() {
        if (radarPos == null || level == null) return null;
        return PhysicsHandler.getWorldVec(level, radarPos);
    }


    // Targeting


    public Vec3 getTargetPos(TargetingConfig targetingConfig) {
        AtomicReference<Vec3> targetPos = new AtomicReference<>();

        getRadar().ifPresent(radar -> {
            if (selectedEntity == null)
                tryFindAutoTarget(targetingConfig);
            if (selectedEntity == null)
                return;

            for (RadarTrack track : getController().cachedTracks) {
                if (track.id().equals(selectedEntity))
                    targetPos.set(track.position());
            }
        });

        if (targetPos.get() == null)
            selectedEntity = null;
        else if (isInSafeZone(targetPos.get()))
            return null;

        return targetPos.get();
    }

    private boolean projectileApproaching(RadarTrack track) {
        if (track.trackCategory() != TrackCategory.PROJECTILE) return true;

        Vec3 trackVel = track.velocity();
        if (trackVel == null || trackVel.lengthSqr() == 0) return false;

        Vec3 trackPos = track.position();
        Vec3 cannonPos = Vec3.atCenterOf(getControllerPos());

        Vec3 toCannon = cannonPos.subtract(trackPos).normalize();
        double dot = trackVel.normalize().dot(toCannon);

        return dot > 0;
    }

    private void tryFindAutoTarget(TargetingConfig targetingConfig) {
        if (!targetingConfig.autoTarget())
            return;

        final double[] distance = {Double.MAX_VALUE};

        getRadar().ifPresent(radar -> {
            for (RadarTrack track : getController().cachedTracks) {
                if (targetingConfig.test(track.trackCategory())
                        && track.position().distanceTo(Vec3.atCenterOf(getControllerPos())) < distance[0]
                        && !isInSafeZone(track.position())) {

                    if (projectileApproaching(track)) {
                        selectedEntity = track.id();
                        activetrack = track;
                        distance[0] = track.position().distanceTo(Vec3.atCenterOf(getControllerPos()));
                    }
                }
            }
        });

        if (selectedEntity != null)
            notifyUpdate();
    }

    public RadarTrack getactivetrack() {
        return activetrack;
    }


    // Safe zones


    public boolean isInSafeZone(Vec3 pos) {
        for (AABB safeZone : safeZones) {
            if (safeZone.contains(pos))
                return true;
        }
        return false;
    }

    public void addSafeZone(BlockPos startPos, BlockPos endPos) {
        double minX = Math.min(startPos.getX(), endPos.getX());
        double minY = Math.min(startPos.getY(), endPos.getY());
        double minZ = Math.min(startPos.getZ(), endPos.getZ());
        double maxX = Math.max(startPos.getX(), endPos.getX()) + 1;
        double maxY = Math.max(startPos.getY(), endPos.getY()) + 1;
        double maxZ = Math.max(startPos.getZ(), endPos.getZ()) + 1;

        getController().safeZones.add(new AABB(minX, minY, minZ, maxX, maxY, maxZ));
    }

    @OnlyIn(Dist.CLIENT)
    public void showSafeZone() {
        for (AABB safeZone : safeZones) {
            Outliner.getInstance().showAABB(safeZone, safeZone)
                    .colored(0x383b42)
                    .withFaceTextures(AllSpecialTextures.CHECKERED, AllSpecialTextures.HIGHLIGHT_CHECKERED)
                    .lineWidth(1 / 16f);
        }
    }

    public boolean tryRemoveAABB(BlockPos pos) {
        return safeZones.removeIf(safeZone -> safeZone.contains(Vec3.atCenterOf(pos)));
    }

    // -------------------------------------------------
    // NBT sync

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);

        if (tag.contains("Controller", Tag.TAG_COMPOUND))
            controller = NbtUtils.readBlockPos(tag.getCompound("Controller"));

        // if the packet explicitly says "no radar", i clear the cached radarPos
        if (clientPacket && tag.contains("HasRadarPos", Tag.TAG_BYTE) && !tag.getBoolean("HasRadarPos")) {
            radarPos = null;
            radar = null;
        } else if (tag.contains("radarPos", Tag.TAG_COMPOUND)) {
            radarPos = NbtUtils.readBlockPos(tag.getCompound("radarPos"));
        }

        selectedEntity = tag.contains("SelectedEntity", Tag.TAG_STRING) ? tag.getString("SelectedEntity") : null;
        hoveredEntity  = tag.contains("HoveredEntity", Tag.TAG_STRING) ? tag.getString("HoveredEntity") : null;

        if (tag.contains("Filter", Tag.TAG_COMPOUND))
            filter = DetectionConfig.fromTag(tag.getCompound("Filter"));
        else
            filter = DetectionConfig.DEFAULT;

        radius = tag.contains("Size", Tag.TAG_INT) ? tag.getInt("Size") : 1;

        if (clientPacket && tag.contains("tracks", Tag.TAG_COMPOUND))
            cachedTracks = RadarTrackUtil.deserializeListNBT(tag.getCompound("tracks"));

        readSafeZones(tag);
    }


    private void readSafeZones(CompoundTag tag) {
        safeZones.clear(); // IMPORTANT: avoid duplicates on every packet
        ListTag safeZonesTag = tag.getList("SafeZones", Tag.TAG_COMPOUND);

        for (int i = 0; i < safeZonesTag.size(); i++) {
            CompoundTag safeZoneTag = safeZonesTag.getCompound(i);
            AABB safeZone = new AABB(
                    safeZoneTag.getDouble("minX"),
                    safeZoneTag.getDouble("minY"),
                    safeZoneTag.getDouble("minZ"),
                    safeZoneTag.getDouble("maxX"),
                    safeZoneTag.getDouble("maxY"),
                    safeZoneTag.getDouble("maxZ")
            );
            safeZones.add(safeZone);
        }
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);

        if (controller != null)
            tag.put("Controller", NbtUtils.writeBlockPos(controller));

        if (selectedEntity != null) tag.putString("SelectedEntity", selectedEntity);
        if (hoveredEntity != null) tag.putString("HoveredEntity", hoveredEntity);

        tag.putInt("Size", radius);

        if (clientPacket) {
            tag.putBoolean("HasRadarPos", radarPos != null);

            if (radarPos != null)
                tag.put("radarPos", NbtUtils.writeBlockPos(radarPos));

            tag.put("Filter", filter.toTag());
            tag.put("tracks", RadarTrackUtil.serializeNBTList(cachedTracks));
        } else {
            if (level instanceof ServerLevel slevel) {
                if (getNetworkGroup(slevel) == null) {
                    if (radarPos != null)
                        tag.put("radarPos", NbtUtils.writeBlockPos(radarPos));
                    tag.put("Filter", filter.toTag());
                }
            }
        }

        tag.put("SafeZones", saveSafeZones());
    }



    private @NotNull ListTag saveSafeZones() {
        ListTag safeZonesTag = new ListTag();
        for (AABB safeZone : safeZones) {
            CompoundTag safeZoneTag = new CompoundTag();
            safeZoneTag.putDouble("minX", safeZone.minX);
            safeZoneTag.putDouble("minY", safeZone.minY);
            safeZoneTag.putDouble("minZ", safeZone.minZ);
            safeZoneTag.putDouble("maxX", safeZone.maxX);
            safeZoneTag.putDouble("maxY", safeZone.maxY);
            safeZoneTag.putDouble("maxZ", safeZone.maxZ);
            safeZonesTag.add(safeZoneTag);
        }
        return safeZonesTag;
    }

    // -------------------------------------------------
    // UI getters
    // -------------------------------------------------

    public String getHoveredEntity() { return hoveredEntity; }
    public String getSelectedEntity() { return selectedEntity; }
}
