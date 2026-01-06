package com.happysg.radar.block.Test;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class TestBlockEntity extends SplitShaftBlockEntity {

    private boolean outputAttached = true;

    public TestBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        if (state.hasProperty(TestBlock.OUTPUT_ATTACHED)) {
            outputAttached = state.getValue(TestBlock.OUTPUT_ATTACHED);
        }
    }

    @Override
    public float getRotationSpeedModifier(Direction face) {
        if (face == Direction.UP) {
            if (!outputAttached) return 0f;
            return outputMultiplier;
        }
        return 1f; // bottom input always passes through unchanged
    }


    public boolean isOutputAttached() {
        return outputAttached;
    }

    /** Call from your command */
    public void setOutputAttached(boolean attached) {
        if (this.outputAttached == attached)
            return;

        this.outputAttached = attached;

        if (level == null)
            return;

        BlockPos pos = getBlockPos();
        BlockState state = getBlockState();

        // 1) Flip blockstate so Create re-queries hasShaftTowards() correctly
        if (state.hasProperty(TestBlock.OUTPUT_ATTACHED)) {
            level.setBlock(pos, state.setValue(TestBlock.OUTPUT_ATTACHED, attached), 3);
        }

        // 2) Nudge updates (vanilla + Create sync)
        level.sendBlockUpdated(pos, state, getBlockState(), 3);
        level.updateNeighborsAt(pos, getBlockState().getBlock());

        // 3) Tell Create's SyncedBlockEntity pipeline to sync + re-evaluate visuals/network
        // (exists in Create 6.x; mentioned directly in changelogs)
        notifyUpdate();

        setChanged();
    }
    // ---- fields ----
    private float outputMultiplier = 1.0f; // 1x = same RPM as input

    public float getOutputMultiplier() {
        return outputMultiplier;
    }

    /**
     * Sets the ratio applied to the TOP output only.
     * Example: 2.0 -> doubles RPM; 0.5 -> halves RPM; -1.0 -> reverses.
     */
    public void setOutputMultiplier(float mult) {
        // avoid NaN/inf weirdness
        if (Float.isNaN(mult) || Float.isInfinite(mult))
            return;

        if (this.outputMultiplier == mult)
            return;

        this.outputMultiplier = mult;

        if (level != null) {
            // Recompute + sync on Create 6.x
            notifyUpdate();
            setChanged();
        }
    }
    @Override
    public void onLoad(){
        if (outputAttached){
            attachKinetics();
        }else {
            detachKinetics();
        }
    }

    public void setOutputTargetRPM(float targetRpm) {
        // Create uses "speed" in RPM units for Kinetics.
        float inputRpm = getSpeed();

        if (Math.abs(inputRpm) < 0.0001f) {
            setOutputMultiplier(0f);
            return;
        }

        setOutputMultiplier(targetRpm / inputRpm);
    }

    public void toggleOutputAttached() {
        if (!outputAttached){
            setOutputAttached(true);
            attachKinetics();

        }else{
            setOutputAttached(false);
            detachKinetics();
        }

    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putBoolean("OutputAttached", outputAttached);
        tag.putFloat("OutputMultiplier", outputMultiplier);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        if (tag.contains("OutputAttached")) outputAttached = tag.getBoolean("OutputAttached");
        if (tag.contains("OutputMultiplier")) outputMultiplier = tag.getFloat("OutputMultiplier");
    }


}