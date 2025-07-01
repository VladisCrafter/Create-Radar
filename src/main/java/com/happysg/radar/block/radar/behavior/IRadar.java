package com.happysg.radar.block.radar.behavior;

import com.happysg.radar.block.radar.track.RadarTrack;
import net.minecraft.core.BlockPos;

import java.util.Collection;

public interface IRadar {
    Collection<RadarTrack> getTracks();

    float getRange();

    boolean isRunning();

    BlockPos getWorldPos();

    float getGlobalAngle();

    //todo better name and/or plan to handle different types of radars
    default boolean renderRelativeToMonitor() {
        return true;
    }

}