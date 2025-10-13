package com.happysg.radar.block.network;

import com.happysg.radar.block.controller.firing.FireControllerBlock;
import com.happysg.radar.block.controller.firing.FireControllerBlockEntity;
import com.happysg.radar.block.controller.pitch.AutoPitchControllerBlock;
import com.happysg.radar.block.controller.pitch.AutoPitchControllerBlockEntity;
import com.happysg.radar.block.controller.WIP.AutoYawControllerBlock;
import com.happysg.radar.block.controller.WIP.AutoYawControllerBlockEntity;
import com.happysg.radar.block.datalink.screens.TargetingConfig;
import com.happysg.radar.compat.cbc.CannonTargeting;
import com.happysg.radar.compat.cbc.CannonUtil;
import com.happysg.radar.compat.cbc.VS2CannonTargeting;
import com.happysg.radar.compat.vs2.PhysicsHandler;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlockEntity;
import rbasamoyai.createbigcannons.cannon_control.contraption.AbstractMountedCannonContraption;
import rbasamoyai.createbigcannons.cannon_control.contraption.PitchOrientedContraptionEntity;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.happysg.radar.compat.cbc.CannonTargeting.calculateProjectileYatX;
import static java.lang.Math.abs;

public class WeaponNetwork {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static boolean isController(Block block) {
        return block instanceof AutoPitchControllerBlock
                || block instanceof AutoYawControllerBlock
                || block instanceof FireControllerBlock;
    }
    public static boolean isControllerEntity(BlockEntity blockEntity) {
        return blockEntity instanceof AutoPitchControllerBlockEntity
                || blockEntity instanceof AutoYawControllerBlockEntity
                || blockEntity instanceof FireControllerBlockEntity;
    }

    private CannonMountBlockEntity cannonMount;
    private AutoPitchControllerBlockEntity autoPitchController;
    private AutoYawControllerBlockEntity autoYawController;
    private FireControllerBlockEntity fireController;
    private ResourceKey<Level> dimension;
    private ServerLevel level;
    private UUID uuid;

    private Vec3 targetPos;
    private Double targetPitch;
    private Double targetYaw;

    public WeaponNetwork(Level level) {
        if(level == null || level.isClientSide()) return;
        this.dimension = level.dimension();
        this.level = (ServerLevel) level;
        this.uuid = UUID.randomUUID();
        WeaponNetworkRegistry.register(this);
    }

    boolean firePrimed = false;
    public void tick(){

        if(fireController != null) fireController.turnOff();
        if(cannonMount == null) return;
        Double targetPitch = getTargetPitch();
        if(autoPitchController != null){
            if(targetPitch != null) autoPitchController.setTargetAngle(targetPitch.floatValue());
            autoPitchController.isRunning = targetPitch != null;
        }
        Double targetYaw = getTargetYaw();
        if(autoYawController != null){
            if(targetYaw != null) autoYawController.setTargetAngle(targetYaw.floatValue());
            autoYawController.isRunning = targetYaw != null;
        }
        if(fireController != null){
            if(fireController.turnedOn) fireController.turnOff();
            else if(isReadyToFire()) {
                if(firePrimed) {
                    firePrimed = false;
                    fireController.turnOn();
                }
                firePrimed = true;
            }
            else {
                firePrimed = false;
                fireController.turnOff();
            }
        }


    }

    public void checkNeighbors(BlockPos tempPos) {
        if ((cannonMount == null && tempPos == null) || level == null) return;
        BlockPos pos = tempPos == null ? cannonMount.getBlockPos() : tempPos;
        BlockPos[] neighbors = {
                pos.north(), pos.south(), pos.east(), pos.west(), pos.below()
        };
        for (BlockPos neighborPos : neighbors) {
            BlockEntity neighbor = level.getBlockEntity(neighborPos);
            if (neighbor != null) {
                if(neighbor instanceof CannonMountBlockEntity) {
                    if(setCannonMount((CannonMountBlockEntity) neighbor)){
                        checkNeighbors(neighborPos);
                    }
                }
                else {
                    if(setController(neighbor)){
                        checkNeighbors(neighborPos);
                    }
                }
            }
        }
    }
    public UUID getUuid() {
        return uuid;
    }

    public CannonMountBlockEntity getCannonMount() {
        return cannonMount;
    }
    public boolean setCannonMount(CannonMountBlockEntity cannonMount) {
        if(this.cannonMount != null) return false;
        this.cannonMount = cannonMount;
        return true;
    }


    public AutoPitchControllerBlockEntity getAutoPitchController() {
        return autoPitchController;
    }

    public void setAutoPitchController(AutoPitchControllerBlockEntity autoPitchController) {
        setAutoPitchController(autoPitchController, true);
    }
    public void setAutoPitchController(AutoPitchControllerBlockEntity autoPitchController, boolean makeDirty) {
        this.autoPitchController = autoPitchController;
    }

    public AutoYawControllerBlockEntity getAutoYawController() {
        return autoYawController;
    }

    public void setAutoYawController(AutoYawControllerBlockEntity autoYawController, boolean makeDirty) {
        this.autoYawController = autoYawController;
    }
    public void setAutoYawController(AutoYawControllerBlockEntity autoYawController) {
        setAutoYawController(autoYawController, true);
    }
    public FireControllerBlockEntity getFireController() {
        return fireController;
    }

    public void setFireController(FireControllerBlockEntity fireController, boolean makeDirty) {
        this.fireController = fireController;
    }
    public void setFireController(FireControllerBlockEntity fireController) {
        setFireController(fireController, true);
    }

    public boolean setController(BlockEntity controller) {
        if (controller instanceof AutoPitchControllerBlockEntity pitchController && this.autoPitchController == null) {
            setAutoPitchController(pitchController);
        } else if (controller instanceof AutoYawControllerBlockEntity yawController && this.autoYawController == null) {
            setAutoYawController(yawController);
        } else if (controller instanceof FireControllerBlockEntity fireCtrl && this.fireController == null) {
            setFireController(fireCtrl);
        } else if (controller instanceof CannonMountBlockEntity cannon && this.cannonMount == null) {
            setCannonMount(cannon);
        }
        else {
            return false;
        }
        return true;
    }
    public boolean isControllerFilled(BlockEntity controller) {
        if (controller instanceof AutoPitchControllerBlockEntity && this.autoPitchController != null) {
            if(level.getBlockEntity(this.autoPitchController.getBlockPos()) instanceof AutoPitchControllerBlockEntity) {
                return true;
            }
            removeController(controller);
        } else if (controller instanceof AutoYawControllerBlockEntity && this.autoYawController != null) {
            if(level.getBlockEntity(this.autoYawController.getBlockPos()) instanceof AutoYawControllerBlockEntity) {
                return true;
            }
            removeController(controller);
        } else if (controller instanceof FireControllerBlockEntity && this.fireController != null) {
            if(level.getBlockEntity(this.fireController.getBlockPos()) instanceof FireControllerBlockEntity) {
                return true;
            }
            removeController(controller);
        } else if (controller instanceof CannonMountBlockEntity && this.cannonMount != null) {
            if (level.getBlockEntity(this.cannonMount.getBlockPos()) instanceof CannonMountBlockEntity) {
                return true;
            }
            cannonMount = null;
        }
            return false;
    }
    public boolean removeController(BlockEntity controller) {
        if (controller instanceof AutoPitchControllerBlockEntity) {
            setAutoPitchController(null);
        } else if (controller instanceof AutoYawControllerBlockEntity) {
            setAutoYawController(null);
        } else if (controller instanceof FireControllerBlockEntity fireCtrl) {
            setFireController(null);
        } else if (controller instanceof CannonMountBlockEntity) {
            setCannonMount(null);
        }
        else {
            return false;
        }
        return true;
    }


    public boolean contains(BlockPos pos) {
        if (level == null || level.isClientSide()) {
            return false;
        }

        return (cannonMount != null && pos.equals(cannonMount.getBlockPos()))
                || (autoPitchController != null && pos.equals(autoPitchController.getBlockPos()))
                || (autoYawController != null && pos.equals(autoYawController.getBlockPos()))
                || (fireController != null && pos.equals(fireController.getBlockPos()));
    }


    public ResourceKey<Level> getDimension() {
        return dimension;
    }
    public boolean isEmpty(){
        return cannonMount == null && autoPitchController == null && autoYawController == null && fireController == null;
    }

    public void setTarget(Vec3 targetPos) {
        if (level == null || level.isClientSide()) {
            LOGGER.debug(" • bailing: client side or null target → isRunning=false");
            return;
        }
        setTargetPos(targetPos);
        setTargetPitch(null);
        setTargetYaw(null);
        if (targetPos == null) {
            return;
        }

        LOGGER.debug("→ setTarget called with {}", targetPos);

            if(PhysicsHandler.isBlockInShipyard(level, getCannonMount().getBlockPos())) {
                List<List<Double>> anglePairs = VS2CannonTargeting.calculatePitchAndYawVS2(cannonMount, targetPos, level);
                if(anglePairs == null) return;
                if(anglePairs.isEmpty()) return;
                if(anglePairs.get(0).isEmpty()) return;
                List<List<Double>> usableAngles = new ArrayList<>();
                for ( List<Double> angles : anglePairs) {
                    if (cannonMount.getContraption() == null) break;
                    if (angles.get(0) < cannonMount.getContraption().maximumElevation() && angles.get(0) > -cannonMount.getContraption().maximumDepression()) {
                        usableAngles.add(angles);
                    }
                }
                if (targetingConfig.artilleryMode() && usableAngles.size() == 2) {
                    setTargetPitch(usableAngles.get(1).get(0));
                    setTargetYaw(usableAngles.get(1).get(1));
                } else if (!usableAngles.isEmpty()) {
                    setTargetPitch(usableAngles.get(0).get(0));
                    setTargetYaw(usableAngles.get(0).get(1));
                }

                LOGGER.debug("  Computed targetPitch (CBC) = {} rad {}}", this.getTargetPitch(), Math.toDegrees(this.getTargetPitch()));

            } else{
                List<Double> pitches = CannonTargeting.calculatePitch(cannonMount, targetPos, level);
                Double targetYaw = CannonTargeting.calculateYaw(cannonMount, targetPos);
                if (pitches == null) {
                    LOGGER.debug("   • calculatePitch returned null → aborting");
                    return;
                }
                if (pitches.isEmpty()) {
                    LOGGER.debug("   • calculatePitch returned empty list → aborting");
                    return;
                }
                LOGGER.debug("   • raw pitches = {}", pitches);
                List<Double> usablePitches = new ArrayList<>();
                for ( Double pitch : pitches) {
                    if (cannonMount.getContraption() == null) break;
                    if (pitches.get(0) < cannonMount.getContraption().maximumElevation() && pitches.get(0) > -cannonMount.getContraption().maximumDepression()) {
                        usablePitches.add(pitch);
                    }
                }
                LOGGER.debug("   • usable pitches = {}", usablePitches);
                if (targetingConfig != null && targetingConfig.artilleryMode() && usablePitches.size() == 2) {
                    setTargetPitch(usablePitches.get(1));
                } else if (!usablePitches.isEmpty()) {
                    setTargetPitch(usablePitches.get(0));
                }
                setTargetYaw(targetYaw);

                LOGGER.debug("   • computed targetPitch={}° ({} rad) → isRunning=true", this.targetPitch, Math.toDegrees(this.targetPitch));
                LOGGER.debug("   • computed targetYaw={}° ({} rad) → isRunning=true", this.targetYaw, Math.toDegrees(this.targetYaw));
                LOGGER.debug(">>> pitch.setTarget() on SERVER at {} → target={}", cannonMount.getBlockPos(), targetPos);
            }
        }


    private boolean isReadyToFire() {
        boolean hasTarget = targetPos != null;
        boolean correctOrient = hasCorrectYawPitch();
        boolean safeZoneClear = !passesSafeZone();
        LOGGER.debug("isTargetInRange() check →, yawPitchOK={}, safeZoneClear={}", correctOrient, safeZoneClear);

        return correctOrient && safeZoneClear && hasTarget;
    }
    public void setSafeZones(List<AABB> safeZones) {
        LOGGER.debug("setSafeZones() → {} zones", safeZones.size());
        this.safeZones = safeZones;
    }

    private boolean passesSafeZone() {
        if(safeZones == null) return false;
        LOGGER.debug("passesSafeZone() → checking {} safe zones", safeZones.size());
        Vec3 target = this.getTargetPos();

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

            float speed = CannonUtil.getInitialVelocity(cc, level);
            double drag = CannonUtil.getProjectileDrag(cc, level);
            double grav = CannonUtil.getProjectileGravity(cc, level);
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
        float prevYaw;
        float prevPitch;
        float yaw;
        float pitch;
        if(targetYaw == null || targetPitch == null) return false;
        try {
            Field prevPitchField = CannonMountBlockEntity.class.getDeclaredField("prevPitch");
            prevPitchField.setAccessible(true);
            prevPitch = prevPitchField.getFloat(cannonMount);

            Field prevYawField = CannonMountBlockEntity.class.getDeclaredField("prevYaw");
            prevYawField.setAccessible(true);
            prevYaw = prevYawField.getFloat(cannonMount);

            Field yawField = CannonMountBlockEntity.class.getDeclaredField("cannonYaw");
            yawField.setAccessible(true);
            yaw = yawField.getFloat(cannonMount);

            Field pitchField = CannonMountBlockEntity.class.getDeclaredField("cannonPitch");
            pitchField.setAccessible(true);
            pitch = pitchField.getFloat(cannonMount);
        } catch (Exception e) {
            throw new RuntimeException("Failed to access cannonYaw", e);
        }
        return abs(yaw - targetYaw) < 0.01 && abs(pitch - targetPitch) < 0.01 && abs(prevYaw - targetYaw) < 0.01 && abs(prevPitch - targetPitch) < 0.01;
    }
    public Vec3 getTargetPos() {return targetPos;}
    public void setTargetPos(Vec3 targetPos) {this.targetPos = targetPos;}

    public Double getTargetPitch() {
        return targetPitch;
    }

    public void setTargetPitch(Double targetPitch) {
        this.targetPitch = targetPitch;
    }

    public Double getTargetYaw() {
        return targetYaw;
    }

    public void setTargetYaw(Double targetYaw) {
        this.targetYaw = targetYaw;
    }
}
