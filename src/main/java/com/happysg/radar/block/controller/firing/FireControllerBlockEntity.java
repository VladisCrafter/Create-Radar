package com.happysg.radar.block.controller.firing;

import com.happysg.radar.block.controller.yaw.AutoYawControllerBlockEntity;
import com.happysg.radar.block.datalink.screens.TargetingConfig;
import com.happysg.radar.block.network.WeaponNetwork;
import com.happysg.radar.block.network.WeaponNetworkRegistry;
import com.happysg.radar.block.network.WeaponNetworkUnit;
import com.happysg.radar.compat.cbc.CannonUtil;
import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlockEntity;
import rbasamoyai.createbigcannons.cannon_control.contraption.AbstractMountedCannonContraption;
import rbasamoyai.createbigcannons.cannon_control.contraption.PitchOrientedContraptionEntity;

import java.util.List;
import java.util.Optional;

import static com.happysg.radar.compat.cbc.CannonTargeting.calculateProjectileYatX;

public class FireControllerBlockEntity extends SmartBlockEntity implements WeaponNetworkUnit {
    private static final Logger LOGGER = LogUtils.getLogger();



    //private TargetingConfig targetingConfig = TargetingConfig.DEFAULT;
    //private Vec3 target;
    //public List<AABB> safeZones = new ArrayList<>();
    public boolean turnedOn = false;
    private WeaponNetwork weaponNetwork;

      // server-time when we last got a target update

    public FireControllerBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
    }

    public void onPlaced() {
        if (!(level instanceof ServerLevel serverLevel)) return;

        for (Direction direction : Direction.values()) {
            BlockEntity neighborBE = level.getBlockEntity(worldPosition.relative(direction));
            if (neighborBE instanceof CannonMountBlockEntity cannon) {
                WeaponNetwork weaponNetwork = WeaponNetworkRegistry.networkContains(worldPosition);
                WeaponNetwork cannonWeaponNetwork = WeaponNetworkRegistry.networkContains(cannon.getBlockPos());

                if (weaponNetwork != null) { // Shouldn't happen normally
                    setWeaponNetwork(weaponNetwork);
                } else if (cannonWeaponNetwork != null && cannonWeaponNetwork.getFireController() == null) {
                    cannonWeaponNetwork.setFireController(this);
                    setWeaponNetwork(cannonWeaponNetwork);
                } else if (WeaponNetworkRegistry.networkContains(cannon.getBlockPos()) == null) {
                    WeaponNetwork newNetwork = new WeaponNetwork(level);
                    newNetwork.setCannonMount(cannon);
                    newNetwork.setFireController(this);
                    setWeaponNetwork(newNetwork);
                }
            }
        }
    }

    public void onRemoved() {
        if (weaponNetwork != null && weaponNetwork.getFireController() == this) {
            weaponNetwork.setFireController(null);
            setWeaponNetwork(null);
        }
    }
    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {

    }

    public void turnOff() {
        BlockState state = getBlockState();
        level.setBlockAndUpdate(this.getBlockPos(), state.setValue(BlockStateProperties.POWERED, false));
        turnedOn = false;
    }

    public void turnOn() {
        BlockState state = getBlockState();
        level.setBlockAndUpdate(this.getBlockPos(), state.setValue(BlockStateProperties.POWERED, true));
        turnedOn = true;
    }

    public WeaponNetwork  getWeaponNetwork() {
        return weaponNetwork;
    }

    public void setWeaponNetwork(WeaponNetwork weaponNetwork) {
        this.weaponNetwork = weaponNetwork;
    }
    public BlockEntity getBlockEntity() {return this;}
}
