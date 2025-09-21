package com.happysg.radar.block.datalink;

import com.happysg.radar.block.network.*;
import com.happysg.radar.block.radar.bearing.RadarBearingBlockEntity;
import com.happysg.radar.registry.AllDataBehaviors;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlockEntity;

import java.util.ArrayList;
import java.util.List;

public class DataLinkBlockEntity extends SmartBlockEntity {

    protected BlockPos targetOffset = BlockPos.ZERO;

    private CompoundTag sourceConfig;
    boolean ledState = false;

    private BlockPos linkedMonitorPos;

    public DataLinkBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {

    }
    public void onDestroyed(){
        if(level == null || level.isClientSide())return;
        ServerLevel serverLevel = (ServerLevel) level;
        BlockEntity targetBlockEntity = level.getBlockEntity(getTargetPosition());
        BlockEntity sourceBlockEntity = level.getBlockEntity(getSourcePosition());
        if((targetBlockEntity instanceof CannonMountBlockEntity) && sourceBlockEntity instanceof WeaponNetworkUnit weaponNetworkUnit){
            weaponNetworkUnit.getWeaponNetwork().removeController(sourceBlockEntity);
            weaponNetworkUnit.setWeaponNetwork(null);
        } else if ((sourceBlockEntity instanceof CannonMountBlockEntity) && targetBlockEntity instanceof WeaponNetworkUnit weaponNetworkUnit) {
            weaponNetworkUnit.getWeaponNetwork().removeController(sourceBlockEntity);
            weaponNetworkUnit.setWeaponNetwork(null);
        }
        if ((sourceBlockEntity instanceof RadarBearingBlockEntity && targetBlockEntity != null) ||
                (targetBlockEntity instanceof RadarBearingBlockEntity && sourceBlockEntity != null)) {
            BlockEntity networkBlockEntity = (sourceBlockEntity instanceof RadarBearingBlockEntity) ? targetBlockEntity: sourceBlockEntity;

            Network network = NetworkRegistry.networkThatContainsPos(networkBlockEntity.getBlockPos(), serverLevel);
            if(network != null) network.setRadarPos(null);
        }
        removeWeaponNetwork(serverLevel, sourceBlockEntity, targetBlockEntity);
        removeWeaponNetwork(serverLevel, targetBlockEntity, sourceBlockEntity);
        removeFromNetwork(serverLevel, sourceBlockEntity, targetBlockEntity);
        removeFromNetwork(serverLevel, targetBlockEntity, sourceBlockEntity);

    }

    private void removeWeaponNetwork(ServerLevel serverLevel, BlockEntity targetBlockEntity, BlockEntity sourceBlockEntity) {
        if((targetBlockEntity instanceof CannonMountBlockEntity || WeaponNetwork.isControllerEntity(targetBlockEntity)) && sourceBlockEntity != null){
            Network network = NetworkRegistry.networkThatContainsPos(sourceBlockEntity.getBlockPos(), serverLevel);
            if(network != null){
                network.removeWeaponNetwork(WeaponNetworkRegistry.networkContains(targetBlockEntity.getBlockPos()));
            }
        }
    }

    private void removeFromNetwork(ServerLevel serverLevel, BlockEntity targetBlockEntity, BlockEntity sourceBlockEntity) {
        if(sourceBlockEntity != null){
            BlockPos sourcePos = sourceBlockEntity.getBlockPos();
            Network network = NetworkRegistry.networkThatContainsPos(sourcePos, serverLevel);
            if(network != null) {
            network.removeNetworkBlock(sourcePos);
                if (targetBlockEntity != null) {
                    network.removeNetworkBlock(targetBlockEntity.getBlockPos());
                }
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        updateGatheredData();
    }

    public void updateGatheredData() {
        BlockPos sourcePosition = getSourcePosition();
        BlockPos targetPosition = getTargetPosition();

        if (!level.isLoaded(targetPosition) || !level.isLoaded(sourcePosition))
            return;
        ArrayList<DataLinkBehavior> behaviors = AllDataBehaviors.getBehavioursForBlockPoses(sourcePosition, targetPosition, level);
        if(behaviors.isEmpty()){
            ledState = false;
            return;
        }
        ledState = true;
        for(DataLinkBehavior behavior : behaviors) {
            if (behavior == null) continue;
            behavior.transferData(new DataLinkContext(level, this));
        }
        sendData();
    }

    @Override
    public void writeSafe(CompoundTag tag) {
        super.writeSafe(tag);
        writeGatheredData(tag);
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        writeGatheredData(tag);
        tag.putBoolean("LedState", ledState);
    }

    private void writeGatheredData(CompoundTag tag) {
        tag.put("TargetOffset", NbtUtils.writeBlockPos(targetOffset));
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        targetOffset = NbtUtils.readBlockPos(tag.getCompound("TargetOffset"));
        ledState = tag.getBoolean("LedState");
    }

    public void target(BlockPos targetPosition) {
        this.targetOffset = targetPosition.subtract(worldPosition);
    }

    public BlockPos getSourcePosition() {
        return worldPosition.relative(getDirection());
    }

    public CompoundTag getSourceConfig() {
        return sourceConfig;
    }

    public void setSourceConfig(CompoundTag sourceConfig) {
        this.sourceConfig = sourceConfig;
    }

    public Direction getDirection() {
        return getBlockState().getOptionalValue(DataLinkBlock.FACING)
                .orElse(Direction.UP)
                .getOpposite();
    }

    public BlockPos getTargetPosition() {
        return worldPosition.offset(targetOffset);
    }


}
