package com.happysg.radar.block.controller.firing;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import com.happysg.radar.block.controller.pitch.AutoPitchControllerBlockEntity;
import com.happysg.radar.block.controller.yaw.AutoYawControllerBlockEntity;
import com.happysg.radar.block.datalink.screens.TargetingConfig;
import rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlock;
import com.happysg.radar.compat.cbc.CannonUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlockEntity;
import rbasamoyai.createbigcannons.cannon_control.contraption.AbstractMountedCannonContraption;
import rbasamoyai.createbigcannons.cannon_control.contraption.PitchOrientedContraptionEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.happysg.radar.compat.cbc.CannonTargeting.calculateProjectileYatX;

public class FiringControlBlockEntity {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int TARGET_TIMEOUT_TICKS = 20;   // 1s at 20 TPS

    private TargetingConfig targetingConfig = TargetingConfig.DEFAULT;
    private Vec3 target;
    private boolean firing;

    private boolean prevAssemblyPowered = false;
    private boolean prevFirePowered     = false;

    public final CannonMountBlockEntity cannonMount;
    public final AutoPitchControllerBlockEntity pitchController;
    public final Level level;

    public List<AABB> safeZones = new ArrayList<>();
    private long lastTargetTick = -1;   // server-time when we last got a target update

    public FiringControlBlockEntity(AutoPitchControllerBlockEntity controller, CannonMountBlockEntity cannonMount) {
        this.cannonMount = cannonMount;
        this.pitchController = controller;
        this.level = cannonMount.getLevel();
        LOGGER.debug("FiringControlBlockEntity.<init>() → controller={} mountPos={}", controller, cannonMount.getBlockPos());
    }

    public void setSafeZones(List<AABB> safeZones) {
        LOGGER.debug("setSafeZones() → {} zones", safeZones.size());
        this.safeZones = safeZones;
    }

    /**
     * Called every tick by the pitch controller.
     */
    public void tick() {
        LOGGER.debug("tick() start → target={} lastTargetTick={} safeZones={} autoFire={} firing={}", target, lastTargetTick, safeZones.size(), targetingConfig.autoFire(), firing);
        boolean pulsedThisTick = false;
        // invalidate stale target
        if (target != null && level != null && level.getGameTime() - lastTargetTick > TARGET_TIMEOUT_TICKS) {
            LOGGER.debug("  → target stale ({} ticks old), clearing",
                    level.getGameTime() - lastTargetTick);
            target = null;
        }


        boolean inRange  = isTargetInRange();
        boolean autoFire = targetingConfig.autoFire();
        LOGGER.debug("  → inRange={} autoFire={}", inRange, autoFire);

        boolean shouldFire = isTargetInRange() && targetingConfig.autoFire();

        if (inRange && autoFire) {
            LOGGER.debug("  → firing condition met");
            tryFireCannon();
        } else {
            LOGGER.debug("  → firing condition not met");
            stopFireCannon();
        }

        if (shouldFire) {
            // only fire once
            if (!pulsedThisTick) {
                tryFireCannon();
                pulsedThisTick = true;
            }
        } else {
            stopFireCannon();
        }
    }

    /**
     * Radar updates the gun’s target through this method.
     */
    public void setTarget(Vec3 target, TargetingConfig config) {
        LOGGER.debug("setTarget() → new target={} config={} atTick={}",
                target, config, level != null ? level.getGameTime() : -1L);

        if (target == null) {
            LOGGER.debug("  → target null, stopping fire");
            this.target = null;
            stopFireCannon();
            return;
        }

        this.target = target;
        this.targetingConfig = config;
        this.lastTargetTick  = level != null ? level.getGameTime() : 0L;
        LOGGER.debug("  → stored target & config; lastTargetTick={}", this.lastTargetTick);

        // — propagate to pitch controller so it can start adjusting elevation —
        if (this.pitchController != null) {
            LOGGER.debug("  → forwarding target to pitchController");
            this.pitchController.setTarget(target);
        }

        // — also propagate to the yaw controller so it can rotate horizontally —
        BlockPos yawPos = cannonMount.getBlockPos().below();
        if (!(level.getBlockEntity(yawPos) instanceof AutoYawControllerBlockEntity)) {
            yawPos = cannonMount.getBlockPos().above();
            LOGGER.debug("  → yawPos = {}", yawPos);
        }

        if (level.getBlockEntity(yawPos) instanceof AutoYawControllerBlockEntity yawCtrl) {
            LOGGER.debug("  → forwarding target to yawController at {}", yawPos);
            yawCtrl.setTarget(target);
        }
    }

    private boolean isTargetInRange() {
        boolean hasTarget     = target != null;
        boolean correctOrient = hasCorrectYawPitch();
        boolean safeZoneClear = !passesSafeZone();
        LOGGER.debug("isTargetInRange() check → hasTarget={}, yawPitchOK={}, safeZoneClear={}",
                hasTarget, correctOrient, safeZoneClear);

        return hasTarget && correctOrient && safeZoneClear;
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
        LOGGER.debug("hasCorrectYawPitch() start");
        BlockPos below = cannonMount.getBlockPos().below();
        if (!(level.getBlockEntity(below) instanceof AutoYawControllerBlockEntity)) {
            below = cannonMount.getBlockPos().above();
            LOGGER.debug("  → no yaw controller at {}", below);
        }

        if (!(level.getBlockEntity(below) instanceof AutoYawControllerBlockEntity yawCtrl)) {
            LOGGER.debug("  → no yaw controller at {}", below);
            return false;
        }
        boolean ok = yawCtrl.atTargetYaw() && pitchController.atTargetPitch();
        LOGGER.debug("  → yaw ok={}, pitch ok={}", yawCtrl.atTargetYaw(), pitchController.atTargetPitch());
        return ok;
    }

    private void stopFireCannon() {
        LOGGER.debug("stopFireCannon() → sending redstone OFF");

        // assembly stays on; fire goes low
        boolean assembly = true;
        cannonMount.onRedstoneUpdate(
                assembly,
                prevAssemblyPowered,
                false,
                prevFirePowered,
                0
        );

        // reset our trackers so next tick we start fresh
        prevAssemblyPowered = assembly;
        prevFirePowered     = false;
        firing              = false;
    }

    private void tryFireCannon() {
        LOGGER.debug("tryFireCannon() → pulsing redstone ON");

        // assume assembly line is always “on” once the cannon is assembled
        boolean assembly = true;

        // 1) drive fire line LOW
        cannonMount.onRedstoneUpdate(
                assembly,            // new assembly
                prevAssemblyPowered, // old assembly
                false,               // new fire = off
                prevFirePowered,     // old fire
                0                    // signal strength
        );
        prevAssemblyPowered = assembly;
        prevFirePowered     = false;

        // 2) drive fire line HIGH (rising edge)
        cannonMount.onRedstoneUpdate(assembly, prevAssemblyPowered, true, prevFirePowered, 15);
        prevAssemblyPowered = assembly;
        prevFirePowered     = true;

        firing = true;
    }
}
