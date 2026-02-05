package com.happysg.radar.block.behavior.networks;

import com.happysg.radar.block.behavior.networks.config.TargetingConfig;
import com.happysg.radar.block.controller.firing.FireControllerBlockEntity;
import com.happysg.radar.block.controller.pitch.AutoPitchControllerBlockEntity;
import com.happysg.radar.block.controller.yaw.AutoYawControllerBlock;
import com.happysg.radar.block.controller.yaw.AutoYawControllerBlockEntity;
import com.happysg.radar.block.radar.track.RadarTrack;
import com.happysg.radar.block.radar.track.RadarTrackUtil;
import com.happysg.radar.compat.Mods;
import com.happysg.radar.compat.cbc.AccelerationTracker;
import com.happysg.radar.compat.cbc.CannonLead;
import com.happysg.radar.compat.cbc.VelocityTracker;
import com.happysg.radar.compat.vs2.VS2ShipVelocityTracker;
import com.happysg.radar.compat.vs2.VS2Utils;
import com.happysg.radar.config.RadarConfig;
import com.happysg.radar.compat.cbc.CBCMuzzleUtil;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlockEntity;
import rbasamoyai.createbigcannons.cannon_control.contraption.AbstractMountedCannonContraption;
import rbasamoyai.createbigcannons.cannon_control.contraption.PitchOrientedContraptionEntity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WeaponFiringControl {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int TARGET_TIMEOUT_TICKS = 20;

    public TargetingConfig targetingConfig = TargetingConfig.DEFAULT;
    private Vec3 target;
    private boolean firing;
    private float offset;

    private Vec3 lastOffsetAim = null;
    private int aimStableTicks = 0;
    private static final int AIM_STABLE_REQUIRED = 1;
    private static final double AIM_STABLE_EPS = 0.5; // blocks


    public final CannonMountBlockEntity cannonMount;
    public AutoPitchControllerBlockEntity pitchController;
    public AutoYawControllerBlockEntity yawController;
    public FireControllerBlockEntity fireController;
    public WeaponNetworkData.WeaponGroupView view;
    public final Level level;
    private RadarTrack activetrack;
    private Entity targetEntity;
    private Ship targetShip;
    private BlockPos binoTargetPos;
    private boolean binoMode;
    private long targetShipId = -1;
    @Nullable private Vec3 lastAimPoint = null;

    private static final int VIS_REFRESH_TICKS = 3; // recompute every N ticks per entity
    private static final int MAX_POINTS_PER_REFRESH = 10; // ray budget per refresh
    private static final double ENTITY_INFLATE = 0.0; // grow AABB a bit for modded hitboxes
    private static final double FACE_EPS = 0.01; // tiny offset outside faces
    private final java.util.Map<Integer, VisCache> visCache = new java.util.HashMap<>();


    private static final class VisCache {
        Vec3 visiblePointOnTarget; // point we can see on the entity
        long lastTick;
    }

    public List<AABB> safeZones = new ArrayList<>();
    private long lastTargetTick = -1;   // server-time when we last got a target update
    private enum RayResult {
        CLEAR,
        BLOCKED_BLOCK,
        BLOCKED_SAFEZONE;

        public boolean isClear() {
            return this == CLEAR;
        }
    }

    public WeaponFiringControl(AutoPitchControllerBlockEntity controller, CannonMountBlockEntity cannonMount, AutoYawControllerBlockEntity yawController) {
        this.cannonMount = cannonMount;
        this.pitchController = controller;
        this.yawController = yawController;
        this.level = cannonMount.getLevel();

        LOGGER.debug("FiringControlBlockEntity.<init>() → controller={} mountPos={}", controller, cannonMount.getBlockPos());
    }
    private RayResult rayClear(Vec3 start, Vec3 end) {

        RayResult result = RayResult.CLEAR;


        if (!safeZones.isEmpty()) {
            for (AABB zone : safeZones) {
                if (zone == null) continue;
                if (zone.contains(start) || zone.contains(end) || zone.clip(start, end).isPresent()) {
                    return RayResult.BLOCKED_SAFEZONE;
                }
            }
        }


        if (result == RayResult.CLEAR) {
            ClipContext ctx = new ClipContext(
                    start, end,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    null
            );

            HitResult hit = level.clip(ctx);

            if (hit.getType() != HitResult.Type.MISS) {
                double hitDist = hit.getLocation().distanceTo(start);
                double targetDist = end.distanceTo(start);

                if (hitDist < targetDist) {
                    result = RayResult.BLOCKED_BLOCK;
                }
            }
        }


        if (RadarConfig.DEBUG_BEAMS && level instanceof ServerLevel server) {
            debugRay(server, start, end, result);
        }

        return result;
    }

    public Vec3 getCannonMuzzlePos() {
        if(yawController != null && yawController.isUpsideDown()){
            return cannonMount.getBlockPos().getCenter().add(0, -2.0, 0);
        }else{
            return cannonMount.getBlockPos().getCenter().add(0, 2.0, 0);
        }

    }

    /** Raycasts should start slightly forward to avoid self-hit on contraption blocks. */
    public Vec3 getCannonRayStart() {
        if(yawController != null && yawController.isUpsideDown()){
            return cannonMount.getBlockPos().getCenter().add(0, -2.0, 0);
        }else{
            return cannonMount.getBlockPos().getCenter().add(0, 2.0, 0);
        }
    }


    private static BlockPos findMuzzleAirLocal(AbstractMountedCannonContraption cc) {
        BlockPos p = cc.getStartPos().immutable();

        for (int i = 0; i < 512; i++) { // hard cap safety
            if (!cc.presentBlockEntities.containsKey(p)) {
                return p;
            }
            p = p.relative(cc.initialOrientation());
        }

        return cc.getStartPos();
    }


    private AABB inflatedAabb(Entity e) {
        AABB bb = e.getBoundingBox();
        return bb.inflate(ENTITY_INFLATE);
    }


    /**
     * Find a visible point on an entity by raycasting to points on its AABB "surface".
     * This does NOT depend on vertical probing points; it tries to shoot whatever part is visible.
     *
     * Returns null if no visible point found within budget.
     */
    @Nullable
    private Vec3 findVisiblePointOnEntity(Entity e, Vec3 start, int budget) {
        AABB bb = inflatedAabb(e);


        Vec3 center = bb.getCenter();
        Vec3 toCannon = start.subtract(center);


        double ax = Math.abs(toCannon.x);
        double ay = Math.abs(toCannon.y);
        double az = Math.abs(toCannon.z);


        ArrayList<Vec3> candidates = new ArrayList<>(24);


        double xMin = bb.minX, xMax = bb.maxX;
        double yMin = bb.minY, yMax = bb.maxY;
        double zMin = bb.minZ, zMax = bb.maxZ;


        double yMid = (yMin + yMax) * 0.5;
        double yChest = yMin + (yMax - yMin) * 0.45;


        candidates.add(new Vec3(center.x, yChest, center.z));
        candidates.add(new Vec3(center.x, yMid, center.z));


        // Choose dominant face
        boolean useX = ax >= ay && ax >= az;
        boolean useY = ay > ax && ay >= az;
        boolean useZ = !useX && !useY;


        if (useX) {
            boolean maxFace = (toCannon.x >= 0);
            double x = maxFace ? xMax : xMin;
            addFaceCandidates(candidates, x, yMin, yMax, zMin, zMax, 'x', maxFace);
        } else if (useY) {
            boolean maxFace = (toCannon.y >= 0);
            double y = maxFace ? yMax : yMin;
            addFaceCandidates(candidates, y, xMin, xMax, zMin, zMax, 'y', maxFace);
        } else { // useZ
            boolean maxFace = (toCannon.z >= 0);
            double z = maxFace ? zMax : zMin;
            addFaceCandidates(candidates, z, xMin, xMax, yMin, yMax, 'z', maxFace);
        }

        if (ax >= az) {
            boolean maxFace = (toCannon.x >= 0);
            double x = maxFace ? xMax : xMin;
            addFaceCandidates(candidates, x, yMin, yMax, zMin, zMax, 'x', maxFace);
        } else {
            boolean maxFace = (toCannon.z >= 0);
            double z = maxFace ? zMax : zMin;
            addFaceCandidates(candidates, z, xMin, xMax, yMin, yMax, 'z', maxFace);
        }

        candidates.add(center);

        int tries = 0;
        for (Vec3 end : candidates) {
            if (tries++ >= budget) break;

            if (!isPointInShootableRange(end)) continue;
            if (isOutOfKnownRange(end)) continue;
            if (rayClear(start, end).isClear()) return end;
        }

        return null;
    }


    /**
     * Adds a set of points on a face of the AABB.
     * axis 'x' => plane at x=planeVal, varying y/z.
     * axis 'y' => plane at y=planeVal, varying x/z.
     * axis 'z' => plane at z=planeVal, varying x/y.
     */
    private void addFaceCandidates(List<Vec3> out,
                                   double planeVal,
                                   double uMin, double uMax,
                                   double vMin, double vMax,
                                   char axis,
                                   boolean maxFace) {


        double uMid = (uMin + uMax) * 0.5;
        double vMid = (vMin + vMax) * 0.5;


        // Face center
        out.add(facePoint(axis, planeVal, uMid, vMid, maxFace));


        // 4 corners
        out.add(facePoint(axis, planeVal, uMin, vMin, maxFace));
        out.add(facePoint(axis, planeVal, uMin, vMax, maxFace));
        out.add(facePoint(axis, planeVal, uMax, vMin, maxFace));
        out.add(facePoint(axis, planeVal, uMax, vMax, maxFace));


        // 4 edge midpoints
        out.add(facePoint(axis, planeVal, uMin, vMid, maxFace));
        out.add(facePoint(axis, planeVal, uMax, vMid, maxFace));
        out.add(facePoint(axis, planeVal, uMid, vMin, maxFace));
        out.add(facePoint(axis, planeVal, uMid, vMax, maxFace));
    }


    private Vec3 facePoint(char axis, double planeVal, double u, double v, boolean maxFace) {
        double eps = maxFace ? -FACE_EPS : +FACE_EPS;


        return switch (axis) {
            case 'x' -> new Vec3(planeVal + eps, u, v);
            case 'y' -> new Vec3(u, planeVal + eps, v);
            default -> new Vec3(u, v, planeVal + eps); // 'z'
        };
    }


    /**
     * Cached visible point query for entities.
     * Returns null if we couldn't find a visible point within budget.
     */
    @Nullable
    private Vec3 getCachedVisiblePoint(Entity f) {
        int id = f.getId();
        long now = level.getGameTime();


        VisCache c = visCache.get(id);
        if (c != null && (now - c.lastTick) < VIS_REFRESH_TICKS) {
            return c.visiblePointOnTarget;
        }


        Vec3 start = getCannonRayStart();
        Vec3 vis = findVisiblePointOnEntity(f, start, MAX_POINTS_PER_REFRESH);


        if (c == null) c = new VisCache();
        c.visiblePointOnTarget = vis;
        c.lastTick = now;
        visCache.put(id, c);


        return vis;
    }

    private boolean checkLineOfSight(Vec3 target) {
        if (!binoMode && (activetrack == null || target == null)) {
            return false;
        }

        float height;

        if (!binoMode) {
            height = (targetEntity != null) ? targetEntity.getBbHeight()
                    : (activetrack != null ? activetrack.getEnityHeight() : 1f);
        } else {
            height = 1f;
        }

        int blocksHigh = (int) Math.ceil(height);
        Vec3 start = getCannonRayStart();
        if (isOutOfKnownRange(target)) return false;
        if (!isPointInShootableRange(target)) return false;

        LOGGER.debug("LOS DBG: trackCat={} entityType={} height={} blocksHigh={} target={}", activetrack != null ? activetrack.trackCategory() : "null", activetrack != null ? activetrack.entityType() : "null", height, blocksHigh, target);
        for (int h = blocksHigh - 1; h >= 0; h--) {
            // center of each block, top-first
            Vec3 end = target.add(0, h + 0.5, 0);
            if (rayClear(start, end).isClear()) {
                offset = h + 0.5f; // highest valid clear point
                return true;
            }
        }

        return false;
    }

    private boolean isPointInShootableRange(@Nullable Vec3 point) {
        if (point == null) return false;

        double max = pitchController != null ? pitchController.getMaxEngagementRangeBlocks() : 0.0;


        if (max <= 0.0) return true;

        Vec3 start = getCannonMuzzlePos();
        double dx = point.x - start.x;
        double dz = point.z - start.z;
        double horiz2 = dx * dx + dz * dz;

        return horiz2 <= (max * max);
    }

    // LOS query for network controller
    public boolean hasLineOfSightTo(@Nullable RadarTrack track) {
        if (!isMountStateOk()) return false;
        if (!targetingConfig.lineOfSight()) return true;

        if (track == null) return false;
        Vec3 p = track.position();
        if (p == null) return false;

        // Don’t waste rays on out-of-range
        if (!isPointInShootableRange(p)) return false;

        Vec3 start = getCannonRayStart();

        // If we can resolve an entity, use multi-point visible-surface probing
        if (level instanceof ServerLevel sl) {
            try {
                UUID uuid = UUID.fromString(track.getId());
                Entity e = sl.getEntity(uuid);
                if (e != null && e.isAlive()) {
                    return getCachedVisiblePoint(e) != null;
                }
            } catch (Throwable ignored) {}
        }

        for (int i = 0; i < 4; i++) {
            Vec3 end = p.add(0, 0.25 + i * 0.5, 0);
            if (!isPointInShootableRange(end)) continue;
            if (rayClear(start, end).isClear()) return true;
        }

        return false;
    }

    private void debugRay(ServerLevel server, Vec3 start, Vec3 end, RayResult result) {

        float r, g, b;

        switch (result) {
            case CLEAR -> {  // GREEN
                r = 0.0f; g = 1.0f; b = 0.0f;
            }
            case BLOCKED_BLOCK -> { // RED
                r = 1.0f; g = 0.0f; b = 0.0f;
            }
            case BLOCKED_SAFEZONE -> { // YELLOW
                r = 1.0f; g = 1.0f; b = 0.0f;
            }
            default -> {
                r = 1.0f; g = 1.0f; b = 1.0f;
            }
        }

        double dist = start.distanceTo(end);
        Vec3 dir = end.subtract(start).normalize();

        for (double d = 0; d < dist; d += 0.25) {
            Vec3 p = start.add(dir.scale(d));

            server.sendParticles(
                    new DustParticleOptions(new Vector3f(r, g, b), 1.0f),
                    p.x, p.y, p.z,
                    1, 0, 0, 0, 0
            );
        }
    }


    public void clearBinoTarget() {
        visCache.clear();
        this.binoMode = false;
        this.binoTargetPos = null;
        this.target = null;
        this.activetrack = null;

        lastAimPoint = null;
        lastOffsetAim = null;
        aimStableTicks = 0;

        stopFireCannon();
    }

    public void setSafeZones(List<AABB> safeZones) {
        LOGGER.debug("setSafeZones() → {} zones", safeZones.size());
        this.safeZones = safeZones;
    }
    public  Entity getEntityByUUID(ServerLevel level, UUID uuid) {
        return level.getEntity(uuid);
    }
    public Ship getShipByUUID(ServerLevel level, String uuid){
        return VSGameUtilsKt.getShipObjectWorld(level).getLoadedShips().getById(Long.parseLong(uuid));
    }

    /**
     * Called every tick by the pitch controller.
     */
    public void refreshControllers() {

        if (!(level instanceof ServerLevel serverLevel)) return;
        this.view = WeaponNetworkData.get(serverLevel).getWeaponGroupViewFromEndpoint(level.dimension(), pitchController.getBlockPos());
        if (view.yawPos() != null && level.getBlockEntity(view.yawPos()) instanceof AutoYawControllerBlockEntity autoyaw) {
            this.yawController = autoyaw;
        } else {
            this.yawController = null;
        }
        if (view.pitchPos() != null && level.getBlockEntity(view.pitchPos()) instanceof AutoPitchControllerBlockEntity autopitch) {
            this.pitchController = autopitch;
        } else {
            this.pitchController = null;
        }
        if (view.firingPos() != null && level.getBlockEntity(view.firingPos()) instanceof FireControllerBlockEntity firecont) {
            this.fireController = firecont;
        } else {
            this.fireController = null;
        }
    }

    private boolean isOutOfKnownRange(@Nullable Vec3 point) {
        if (point == null) return true; // no point to test

        double max = pitchController != null ? pitchController.getMaxEngagementRangeBlocks() : 0.0;

        if (max <= 0.0) return false;

        Vec3 start = getCannonRayStart();
        return point.distanceToSqr(start) > (max * max);
    }

    public void tick() {
        if (!isMountStateOk()) {
            stopFireCannon();
            return;
        }

        if (binoMode || activetrack != null) {
            lastTargetTick = level.getGameTime();
        }

        if (!binoMode && activetrack == null && target != null
                && level.getGameTime() - lastTargetTick > TARGET_TIMEOUT_TICKS) {
            LOGGER.debug("TARGET TIMEOUT: now={} last={} age={} target={} track={}",
                    level.getGameTime(), lastTargetTick, (level.getGameTime()-lastTargetTick),
                    target, activetrack != null ? activetrack.getId() : "null");
            resetTarget();
            stopFireCannon();
            return;
        }

        LOGGER.debug("tick() start → target={} lastTargetTick={} safeZones={} autoFire={} firing={}", target, lastTargetTick, safeZones.size(), targetingConfig.autoFire(), firing);


        if (!binoMode && activetrack != null && level instanceof ServerLevel sl) {

            boolean isVsShip = Mods.VALKYRIENSKIES.isLoaded() && "VS2:ship".equals(activetrack.entityType());

            if (isVsShip) {
                long id;
                try {
                    id = Long.parseLong(activetrack.id());
                } catch (NumberFormatException ignored) {
                    resetTarget();
                    return;
                }
                if (targetShip == null || targetShipId != id) {
                    targetShip = getShipByUUID(sl, activetrack.id());
                    targetShipId = id;
                    if (targetShip == null) {
                        resetTarget();
                        targetEntity = null;
                        return;
                    }
                }
                targetEntity = null;
            } else {
                if (targetEntity == null) {
                    targetEntity = getEntityByUUID(sl, UUID.fromString(activetrack.id()));
                    if (targetEntity == null || !targetEntity.isAlive()) {
                        resetTarget();
                        targetShip = null;
                        return;
                    }
                }
                targetShip = null;
            }
        }



        if (!binoMode) {
            if (targetShip != null) {
                target = RadarTrackUtil.getPosition(targetShip);
            } else if (targetEntity != null) {
                target = targetEntity.position();
            }
        }else {
            target = binoTargetPos.getCenter();
        }




        AbstractMountedCannonContraption cannonContraption;
        if (cannonMount.getContraption() == null) return;
        if (cannonMount.getContraption().getContraption() instanceof AbstractMountedCannonContraption cannon) {
            cannonContraption = cannon;
        } else return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        if (targetEntity != null) {
            if (!targetEntity.isAlive()) {
                resetTarget();
                return;
            }
        }
        if (targetShip != null) {
            long id;
            try {
                id = Long.parseLong(activetrack.id());
            } catch (NumberFormatException ignored) {
                resetTarget();
                return;
            }

            Ship live = VSGameUtilsKt.getShipObjectWorld(serverLevel).getLoadedShips().getById(id);
            if (live == null) {
                resetTarget();
                return;
            }

            targetShip = live;
        }



        Vec3 shooterVel;
        Vec3 shooterAccel;
        Vec3 targetVel;
        Vec3 targetAccel;
        if(VS2Utils.isBlockInShipyard(level,cannonMount.getBlockPos())){
            Ship mountship = VSGameUtilsKt.getShipManagingPos(level,cannonMount.getBlockPos());
            if(mountship ==null){
                shooterVel = Vec3.ZERO;
                shooterAccel = Vec3.ZERO;
            } else{
                shooterVel = VS2ShipVelocityTracker.getShipVelocityPerTick(mountship);
                shooterAccel = AccelerationTracker.getAccelerationPerTick2(mountship.getId(),shooterVel);
            }
        }else{
            shooterVel =Vec3.ZERO;
            shooterAccel = Vec3.ZERO;
        }
        if(targetShip != null){
            target = RadarTrackUtil.getPosition(targetShip);
            targetVel = VS2ShipVelocityTracker.getShipVelocityPerTick(targetShip);
            targetAccel =AccelerationTracker.getAccelerationPerTick2(targetShip.getId(),targetVel);
        }else if(!binoMode && targetEntity != null){
            target = targetEntity.position();
            targetVel = VelocityTracker.getEstimatedVelocityPerTick(targetEntity);
            targetAccel = AccelerationTracker.getAccelerationPerTick2(targetEntity.getUUID(),targetVel);
        }else if(binoMode && binoTargetPos != null){
            target = binoTargetPos.getCenter();
            targetVel = Vec3.ZERO;
            targetAccel = Vec3.ZERO;
        }else{
            return;
        }
        double dist = getCannonMuzzlePos().distanceTo(target);
        double noLeadDist = 8.0; // tune this

        Vec3 solvePos = target;

        if (!binoMode && targetEntity != null) {
            Vec3 vis = getCachedVisiblePoint(targetEntity);
            solvePos = (vis != null)
                    ? vis
                    : targetEntity.position().add(0.0, (targetEntity.getBbHeight() * 0.5)+.5, 0.0); // aim center mass
        }

        CannonLead.LeadSolution lead = null;
        if (dist > noLeadDist) {
            lead = CannonLead.solveLeadPerTickWithAcceleration(
                    cannonMount, cannonContraption, serverLevel,
                    shooterVel, shooterAccel,
                    solvePos, // use solvePos, not feet
                    targetVel, targetAccel,
                    RadarConfig.server().leadFiringDelay.get());
        }

        boolean hasLeadSolution = (lead != null && lead.aimPoint != null);
        Vec3 offsetAim = hasLeadSolution ? lead.aimPoint : solvePos;
        lastAimPoint = offsetAim;


        if (lastOffsetAim == null || lastOffsetAim.distanceTo(offsetAim) > AIM_STABLE_EPS) {
            aimStableTicks = 0;
            lastOffsetAim = offsetAim;
        } else {
            aimStableTicks++;
        }

        // drive controllers using offsetAim
        if (this.pitchController != null) {

            this.pitchController.setTarget(offsetAim);
        }

        if (view != null && view.yawPos() != null) {
            BlockEntity be = level.getBlockEntity(view.yawPos());
            if (be instanceof AutoYawControllerBlockEntity yawCtrl) {
                yawCtrl.setTarget(offsetAim);
            }
        }

        // Debug
        boolean auto = targetingConfig.autoFire();
        boolean yawPitchOk = hasCorrectYawPitch();
        boolean safeOk = !passesSafeZone();

        if (level.getGameTime() % 20 == 0) {
            LOGGER.debug("WFC FIREGATES: auto={} lead={} yawPitchOk={} safeOk={} firingBE={} target={} aim={} offset={} stable={}/{}", auto, hasLeadSolution, yawPitchOk, safeOk, fireController != null, target, offsetAim, offset, aimStableTicks, AIM_STABLE_REQUIRED);
            if (!yawPitchOk) {
                LOGGER.debug("WFC AIMCHK: yawCtrl={} pitchCtrl={} atYaw={} atPitch={} targYaw={} targPitch={}", yawController != null ? yawController.getBlockPos() : null, pitchController != null ? pitchController.getBlockPos() : null, yawController != null && yawController.atTargetYaw(), pitchController != null && pitchController.atTargetPitch(), yawController != null ? yawController.getTargetAngle() : null, pitchController != null ? pitchController.getTargetAngle() : null);
            }
        }
        // Debug End

        boolean shouldFire =
                targetingConfig.autoFire()
                        && hasLeadSolution
                        && hasCorrectYawPitch()
                        && !passesSafeZone()
                        && aimStableTicks == AIM_STABLE_REQUIRED;

        if (fireController != null) {
            if (shouldFire) tryFireCannon();
            else stopFireCannon();
        }
    }

    public void resetTarget(){
        visCache.clear();
        this.target =null;
        this.activetrack =null;
        this.targetEntity = null;
        this.targetShip   = null;
        this.targetShipId = -1;

        lastAimPoint = null;
        lastOffsetAim = null;
        aimStableTicks = 0;

        pitchController.setTrack(null);
        stopFireCannon();
    }

    public void setTarget(Vec3 target, TargetingConfig config, RadarTrack track, WeaponNetworkData.WeaponGroupView view){
        LOGGER.debug("setTarget() → new target={} config={} atTick={}",
                target, config, level != null ? level.getGameTime() : -1L);
        if (target == null) {
            LOGGER.debug("  → target null, stopping fire");
            this.target = null;
            stopFireCannon();
            pitchController.setTargetAngle(0);
            yawController.setTargetAngle(0);

            return;
        }
        this.binoMode =false;
        this.target = target;//.add(0,offset,0);

        lastOffsetAim = null;
        aimStableTicks = 0;

        this.targetingConfig = config;
        if (level != null) this.lastTargetTick  = level.getGameTime();
        this.view = view;
        this.activetrack = track;
        this.targetEntity = null;
        this.targetShip = null;
    }

    public void setBinoTarget(@Nullable BlockPos binoTarget, TargetingConfig config,
                              WeaponNetworkData.WeaponGroupView view, boolean reset) {

        this.view = view;
        this.targetingConfig = config;
        this.activetrack = null;

        if (reset || binoTarget == null) {
            this.binoMode = false;
            this.binoTargetPos = null;
            this.target = null;
            stopFireCannon();
            return;
        }

        this.binoMode = true;
        this.binoTargetPos = binoTarget.immutable();
        if (level != null) this.lastTargetTick = level.getGameTime();
    }

    private boolean isTargetInRange() {
        if (binoMode) {
            target = binoTargetPos.getCenter();
        }

        boolean hasTarget     = target != null;
        boolean correctOrient = hasCorrectYawPitch();
        boolean safeZoneClear = !passesSafeZone();

        LOGGER.debug("isTargetInRange() check → hasTarget={}, yawPitchOK={}, safeZoneClear={}",
                hasTarget, correctOrient, safeZoneClear);

        return hasTarget && correctOrient && safeZoneClear;
    }

    private boolean passesSafeZone() {
        if (safeZones == null || safeZones.isEmpty()) return false;

        Vec3 aim = (lastAimPoint != null) ? lastAimPoint : target;
        if (aim == null) return false;

        Vec3 start = getCannonRayStart();
        for (AABB zone : safeZones) {
            if (zone == null) continue;

            if (zone.contains(start) || zone.contains(aim)) {
                return true;
            }

            if (zone.clip(start, aim).isPresent()) {
                return true;
            }
        }

        return false;
    }

    private boolean hasCorrectYawPitch() {
        if(yawController == null && pitchController == null)return false;
        boolean yaw =true;
        if(yawController !=null) {
            yaw = yawController.atTargetYaw();
        }
        boolean pitch = pitchController.atTargetPitch();

        return yaw && pitch;
    }

    private void stopFireCannon() {
        if(this.fireController == null) return;
        fireController.setPowered(false);
    }

    private void tryFireCannon() {
        if(this.fireController == null) return;
        fireController.setPowered(true);
        LOGGER.debug("firing!");

    }

    public boolean canEngageTrack(@Nullable RadarTrack track, boolean requireLos) {
        if (!isMountStateOk()) return false;
        if (track == null) return false;
        Vec3 p = track.position();
        if (p == null) return false;
        if (!(level instanceof ServerLevel sl)) return false;

        if (passesSafeZoneForPoint(p)) return false;

        // 2) Range gate (PER CANNON)
        if (!isPointInShootableRange(p)) return false;

        // 3) LOS (PER CANNON, only if required by config)
        if (requireLos) return hasLineOfSightTo(track);

        return true;
    }

    private boolean passesSafeZoneForPoint(Vec3 aim) {
        if (safeZones == null || safeZones.isEmpty()) return false;
        if (aim == null) return false;

        Vec3 start = getCannonRayStart();
        for (AABB zone : safeZones) {
            if (zone == null) continue;
            if (zone.contains(start) || zone.contains(aim)) return true;
            if (zone.clip(start, aim).isPresent()) return true;
        }
        return false;
    }


    private boolean isMountStateOk() {
        if (level == null || cannonMount == null) return false;
        if (cannonMount.isRemoved()) return false;

        PitchOrientedContraptionEntity ce = cannonMount.getContraption();
        if (ce == null) return false;
        if (!ce.isAlive()) return false;

        if (!(ce.getContraption() instanceof AbstractMountedCannonContraption)) return false;

        return true;
    }
}
