package com.happysg.radar.block.behavior.networks;

import com.happysg.radar.block.controller.firing.FireControllerBlockEntity;
import com.happysg.radar.block.radar.track.RadarTrack;
import com.happysg.radar.config.RadarConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import org.joml.Vector3f;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import com.happysg.radar.block.controller.pitch.AutoPitchControllerBlockEntity;
import com.happysg.radar.block.controller.yaw.AutoYawControllerBlockEntity;
import com.happysg.radar.block.behavior.networks.config.TargetingConfig;
import com.happysg.radar.compat.cbc.CannonUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.valkyrienskies.core.impl.shadow.En;
import rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlockEntity;
import rbasamoyai.createbigcannons.cannon_control.cannon_mount.YawControllerBlockEntity;
import rbasamoyai.createbigcannons.cannon_control.contraption.AbstractMountedCannonContraption;
import rbasamoyai.createbigcannons.cannon_control.contraption.PitchOrientedContraptionEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.happysg.radar.compat.cbc.CannonTargeting.calculateProjectileYatX;

public class WeaponFiringControl {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int TARGET_TIMEOUT_TICKS = 20;   // 1s at 20 TPS

    private TargetingConfig targetingConfig = TargetingConfig.DEFAULT;
    private Vec3 target;
    private boolean firing;
    private float offset;

    private boolean prevAssemblyPowered = false;
    private boolean prevFirePowered     = false;

    public final CannonMountBlockEntity cannonMount;
    public AutoPitchControllerBlockEntity pitchController;
    public AutoYawControllerBlockEntity yawController;
    public FireControllerBlockEntity fireController;
    public WeaponNetworkData.WeaponGroupView view;
    public final Level level;
    private RadarTrack activetrack;
    private Entity targetEntity;

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

                Optional<Vec3> entry = zone.clip(start, end);
                Optional<Vec3> exit  = zone.clip(end, start);

                if (entry.isPresent() && exit.isPresent()) {
                    result = RayResult.BLOCKED_SAFEZONE;
                    break;
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



    private boolean checkLineOfSight(Vec3 target) {
        if (activetrack == null || target == null) {

            return false;
        }
        if(!targetingConfig.lineOfSight()){
            return true;
        }
        float height = activetrack.getEnityHeight();
        int blocksHigh = (int) Math.ceil(height);
        Vec3 start = cannonMount.getBlockPos().getCenter().add(0, 2, 0);
        for (int h = blocksHigh - 1; h >= 0; h--) {
            // center of each block, top-first
            Vec3 end = target.add(0, h + 0.5, 0);
            if (rayClear(start, end).isClear()) {
                offset = h + 0.5f; // highest valid clear point
                return true;
            }
        }

        return false; // nothing was clear
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





    public void setSafeZones(List<AABB> safeZones) {
        LOGGER.debug("setSafeZones() → {} zones", safeZones.size());
        this.safeZones = safeZones;
    }
    public static Entity getEntityByUUID(ServerLevel level, UUID uuid) {
        return level.getEntity(uuid); // returns Entity or null
    }

    /**
     * Called every tick by the pitch controller.
     */
    public void refreshControllers(){
        if(!(level instanceof ServerLevel serverLevel)) return;
        if(view.yawPos() != null && level.getBlockEntity(view.yawPos()) instanceof AutoYawControllerBlockEntity autoyaw){
            this.yawController = autoyaw;
        }
        if(view.pitchPos() != null && level.getBlockEntity(view.pitchPos()) instanceof AutoPitchControllerBlockEntity autopitch){
            this.pitchController = autopitch;
        }
        if(view.firingPos() != null && level.getBlockEntity(view.firingPos()) instanceof FireControllerBlockEntity firecont){
            this.fireController = firecont;
        }
    }
    public void tick() {
        LOGGER.debug("tick() start → target={} lastTargetTick={} safeZones={} autoFire={} firing={}", target, lastTargetTick, safeZones.size(), targetingConfig.autoFire(), firing);
        if (level.getGameTime() % 10 == 0) {
            refreshControllers();
        }
        boolean pulsedThisTick = false;
        //LOGGER.warn("rotating");
        // invalidate stale target
        if (target != null && level != null && level.getGameTime() - lastTargetTick > TARGET_TIMEOUT_TICKS) {
            LOGGER.debug("  → target stale ({} ticks old), clearing",
                    level.getGameTime() - lastTargetTick);
            target = null;
        }
        if(targetEntity == null && level instanceof ServerLevel sl){
            targetEntity = getEntityByUUID(sl, activetrack.getUuid());
        }
        target = targetEntity.position();
        boolean inRange  = isTargetInRange();
        boolean autoFire = targetingConfig.autoFire();
        LOGGER.debug("  → inRange={} autoFire={}", inRange, autoFire);

        boolean shouldFire = isTargetInRange();
        if(fireController != null){
            LOGGER.warn("check");
            if (inRange && autoFire) {
                LOGGER.warn("  → firing condition met");
                tryFireCannon();
            } else {
                LOGGER.debug("  → firing condition not met");
                stopFireCannon();
            }

            if (shouldFire) {
                tryFireCannon();
            } else {
                stopFireCannon();
            }
        }
        // — propagate to pitch controller so it can start adjusting elevation —
        Vec3 offsettarget = target.add(0,offset,0);
        if (this.pitchController != null) {
            LOGGER.debug("  → forwarding target to pitchController");
            this.pitchController.setTarget(offsettarget);
        }

        // — also propagate to the yaw controller so it can rotate horizontally —
        if(view.yawPos() !=null) {
            if (level.getBlockEntity(view.yawPos()) instanceof AutoYawControllerBlockEntity yawCtrl) {
                LOGGER.debug("  → forwarding target to yawController at {}", view.yawPos());
                yawCtrl.setTarget(offsettarget);
            }
        }
    }

    /**
     * Radar updates the gun’s target through this method.
     */
    public void resetTarget(){
        this.target =null;
        this.targetEntity = null;
    }
    public void setTarget(Vec3 target, TargetingConfig config, RadarTrack track, WeaponNetworkData.WeaponGroupView view, Entity targetEntity)     {
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
//        if(!(level instanceof ServerLevel serverLevel)){
//            return;
//        }
//        if(view.yawPos() != null && serverLevel.getBlockEntity(view.yawPos()) instanceof AutoYawControllerBlockEntity yCtrl) {
//            this.yawController = yCtrl;
//        }
//        if(view.firingPos() != null && serverLevel.getBlockEntity(view.firingPos()) instanceof FireControllerBlockEntity fCtrl){
//            this.fireController = fCtrl;
//        }
        this.target = target;//.add(0,offset,0);
        this.targetingConfig = config;
        this.targetEntity = targetEntity;
        this.lastTargetTick  = level.getGameTime();
        this.view = view;
        this.activetrack = track;




    }


    private boolean isTargetInRange() {

        boolean hasTarget     = target != null;
        boolean correctOrient = hasCorrectYawPitch();
        boolean safeZoneClear = !passesSafeZone();
        boolean lineOfSight   = checkLineOfSight(target);
        LOGGER.debug("isTargetInRange() check → hasTarget={}, yawPitchOK={}, safeZoneClear={}, LOS = {}",
                hasTarget, correctOrient, safeZoneClear, lineOfSight);

        return hasTarget && correctOrient && safeZoneClear && lineOfSight;
    }

    private boolean passesSafeZone() {
        LOGGER.debug("passesSafeZone() → checking {} safe zones", safeZones.size());
        if(safeZones.isEmpty()){
            return false;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            LOGGER.debug("  → not server level, skip safe-zone check");
            return false;
        }

        if (target == null) {
            LOGGER.debug("  → target is null, skipping safe zone check");
            return false;
        }

        Vec3 cannonPos = cannonMount.getBlockPos().getCenter();

        for (AABB zone : safeZones) {
            if (zone == null) continue;

            if (zone.contains(target)) {
                LOGGER.debug("  → target {} inside zone {}", target, zone);
                return true;
            }

            Vec3 baseCannon = new Vec3(cannonPos.x, zone.minY, cannonPos.z);
            Vec3 baseTarget = new Vec3(target.x, zone.minY, target.z);
            Optional<Vec3> oMin = zone.clip(baseCannon, baseTarget);
            Optional<Vec3> oMax = zone.clip(baseTarget, baseCannon);

            if (oMin.isEmpty() || oMax.isEmpty()) {
                LOGGER.debug("  → no intersection segment for zone {}", zone);
                continue;
            }

            Vec3 minX = oMin.get();
            Vec3 maxX = oMax.get();

            double yMin = zone.minY - cannonMount.getBlockPos().getY();
            double yMax = zone.maxY - cannonMount.getBlockPos().getY();

            LOGGER.debug("  yMin: {}, yMax: {}", yMin, yMax);

            PitchOrientedContraptionEntity contraption = cannonMount.getContraption();
            if (contraption == null || !(contraption.getContraption() instanceof AbstractMountedCannonContraption cc)) {
                LOGGER.warn("  → cannon contraption missing or invalid, skipping");
                continue;
            }

            float speed = CannonUtil.getInitialVelocity(cc, serverLevel);
            double drag = CannonUtil.getProjectileDrag(cc, serverLevel);
            double grav = CannonUtil.getProjectileGravity(cc, serverLevel);
            int length  = CannonUtil.getBarrelLength(cc);

            LOGGER.debug("  Speed: {}, Drag: {}, Grav: {}, Length: {}", speed, drag, grav, length);

            double theta = Math.toRadians(cannonMount.getDisplayPitch());
            double distMin = cannonPos.distanceTo(minX) - length * Math.cos(theta);
            double distMax = cannonPos.distanceTo(maxX) - length * Math.cos(theta);
            double yAtMin  = calculateProjectileYatX(speed, distMin, theta, drag, grav);
            double yAtMax  = calculateProjectileYatX(speed, distMax, theta, drag, grav);

            LOGGER.debug("  → zone {}: yAtMin={}, yAtMax={} vs yMin={}, yMax={}",
                    zone, yAtMin, yAtMax, yMin, yMax);

            if ((yAtMin >= yMin && yAtMax <= yMax) || (yAtMin <= yMin && yAtMax >= yMin)) {
                LOGGER.debug("  → trajectory passes through zone {}", zone);
                return true;
            }
        }

        LOGGER.debug("passesSafeZone() → false");
        return false;
    }

    private boolean hasCorrectYawPitch() {
        boolean yaw = yawController.atTargetYaw();
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
        LOGGER.warn("firing!");

    }
}
