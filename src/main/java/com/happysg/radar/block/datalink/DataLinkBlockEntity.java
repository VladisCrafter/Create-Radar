package com.happysg.radar.block.datalink;

import com.happysg.radar.block.datalink.screens.AbstractDataLinkScreen;
import com.happysg.radar.block.network.Network;
import com.happysg.radar.block.network.NetworkSavedData;
import com.happysg.radar.block.network.WeaponNetworkSavedData;
import com.happysg.radar.block.network.WeaponNetworkUnit;
import com.happysg.radar.block.radar.bearing.RadarBearingBlockEntity;
import com.happysg.radar.registry.AllDataBehaviors;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlockEntity;

import java.util.List;
import java.util.Optional;

public class DataLinkBlockEntity extends SmartBlockEntity {

    protected BlockPos targetOffset = BlockPos.ZERO;

    public DataPeripheral activeSource;
    public DataController activeTarget;

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
        if(activeSource != null && activeTarget != null && targetBlockEntity instanceof CannonMountBlockEntity && sourceBlockEntity instanceof WeaponNetworkUnit weaponNetworkUnit){
            weaponNetworkUnit.getWeaponNetwork().removeController(sourceBlockEntity);
            weaponNetworkUnit.setWeaponNetwork(null);
        }
        if ((sourceBlockEntity instanceof RadarBearingBlockEntity && targetBlockEntity != null) ||
                (targetBlockEntity instanceof RadarBearingBlockEntity && sourceBlockEntity != null)) {
            BlockEntity networkBlockEntity = (sourceBlockEntity instanceof RadarBearingBlockEntity) ? targetBlockEntity: sourceBlockEntity;

            Network network = NetworkSavedData.get(serverLevel).networkThatContainsPos(networkBlockEntity.getBlockPos(), level);
            network.setRadarPos(null);
        }
        if(targetBlockEntity != null){
            BlockPos targetPos = targetBlockEntity.getBlockPos();
            Network network = NetworkSavedData.get(serverLevel).networkThatContainsPos(targetPos, level);
            network.removeNetworkBlock(targetPos);
            if(sourceBlockEntity != null){
                network.removeNetworkBlock(sourceBlockEntity.getBlockPos());
            }
        }
        if(sourceBlockEntity != null){ //should never do anything cuz they are in the same network but just in case
            BlockPos sourcePos = sourceBlockEntity.getBlockPos();
            Network network = NetworkSavedData.get(serverLevel).networkThatContainsPos(sourcePos, level);
            network.removeNetworkBlock(sourcePos);
            if(targetBlockEntity != null){
                network.removeNetworkBlock(targetBlockEntity.getBlockPos());
            }
        }
        if(sourceBlockEntity instanceof CannonMountBlockEntity && targetBlockEntity != null){
            Network network = NetworkSavedData.get(serverLevel).networkThatContainsPos(targetBlockEntity.getBlockPos(), level);
            if(network != null){
                WeaponNetworkSavedData weaponNetworkSavedData = WeaponNetworkSavedData.get(serverLevel);
                network.removeWeaponNetwork(weaponNetworkSavedData.networkContains(sourceBlockEntity.getBlockPos()));
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

        DataController target = AllDataBehaviors.targetOf(level, targetPosition);
        DataPeripheral source = AllDataBehaviors.sourcesOf(level, sourcePosition);
        boolean notify = false;

        if (activeTarget != target) {
            activeTarget = target;
            notify = true;
        }

        if (activeSource != source) {
            activeSource = source;
            sourceConfig = new CompoundTag();
            notify = true;
        }

        if (notify)
            notifyUpdate();
        if (activeSource == null || activeTarget == null) {
            ledState = false;
            return;
        }

        ledState = true;
        activeSource.transferData(new DataLinkContext(level, this), activeTarget);
        sendData();
        //TODO implement advancement
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
        if (clientPacket && activeTarget != null)
            tag.putString("TargetType", activeTarget.id.toString());
        tag.putBoolean("LedState", ledState);
    }

    private void writeGatheredData(CompoundTag tag) {
        tag.put("TargetOffset", NbtUtils.writeBlockPos(targetOffset));

        if (activeSource != null) {
            CompoundTag data = sourceConfig.copy();
            data.putString("Id", activeSource.id.toString());
            tag.put("Source", data);
        }
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        targetOffset = NbtUtils.readBlockPos(tag.getCompound("TargetOffset"));
        ledState = tag.getBoolean("LedState");
        if (clientPacket && tag.contains("TargetType"))
            activeTarget = AllDataBehaviors.getTarget(new ResourceLocation(tag.getString("TargetType")));


        if (!tag.contains("Source"))
            return;

        CompoundTag data = tag.getCompound("Source");
        activeSource = AllDataBehaviors.getSource(new ResourceLocation(data.getString("Id")));
        sourceConfig = new CompoundTag();
        if (activeSource != null)
            sourceConfig = data.copy();
    }

    Optional<AbstractDataLinkScreen> getScreen() {
        return activeSource == null ? Optional.empty() : Optional.ofNullable(activeSource.getScreen(this));
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
