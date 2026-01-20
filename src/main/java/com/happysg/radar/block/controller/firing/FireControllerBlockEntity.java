package com.happysg.radar.block.controller.firing;


import com.happysg.radar.block.behavior.networks.NetworkContext;
import com.happysg.radar.block.behavior.networks.NetworkData;
import com.happysg.radar.block.behavior.networks.WeaponNetworkData;
import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

import java.util.List;

public class FireControllerBlockEntity extends SmartBlockEntity  {
    private static final Logger LOGGER = LogUtils.getLogger();
    boolean powered = false;

    // server-time when we last got a target update

    public FireControllerBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {

    }
    public boolean isPowered() {
        return powered;
    }

    public void setPowered(boolean powered) {
        if (level == null || level.isClientSide)
            return;

        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof FireControllerBlock))
            return;

        if (state.getValue(FireControllerBlock.POWERED) == powered)
            return;


        level.setBlock(worldPosition, state.setValue(FireControllerBlock.POWERED, powered), 3);

        // make sure redstone dust + comparators around it re-check
        level.updateNeighborsAt(worldPosition, state.getBlock());
        for (Direction d : Direction.values())
            level.updateNeighborsAt(worldPosition.relative(d), state.getBlock());

        level.updateNeighbourForOutputSignal(worldPosition, state.getBlock());

        setChanged();
        sendData();
    }





    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putBoolean("Powered", powered);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        powered = tag.getBoolean("Powered");
    }



}

