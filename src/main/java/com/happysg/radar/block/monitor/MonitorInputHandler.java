package com.happysg.radar.block.monitor;


import com.happysg.radar.block.radar.bearing.RadarBearingBlockEntity;
import com.happysg.radar.block.radar.bearing.RadarTrack;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;

public class MonitorInputHandler {


    public static void monitorPlayerHovering(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        Level level = event.player.level();
        Vec3 hit = player.pick(5, 0.0F, false).getLocation();
        if (player.pick(5, 0.0F, false) instanceof BlockHitResult result) {
            if (level.getBlockEntity(result.getBlockPos()) instanceof MonitorBlockEntity be && level.getBlockEntity(be.getControllerPos()) instanceof MonitorBlockEntity monitor) {
                Direction monitorFacing = level.getBlockState(monitor.getControllerPos())
                        .getValue(MonitorBlock.FACING);
                Vec3 relative = monitor.calculateRelativePosition(hit, monitorFacing);
                if (monitor.radarPos == null)
                    return;
                Vec3 RadarPos = monitor.radarPos.getCenter();
                float range = monitor.getRadar().map(RadarBearingBlockEntity::getRange).orElse(0f);
                Vec3 selected = RadarPos.add(relative.scale(range));
                monitor.getRadar().map(RadarBearingBlockEntity::getEntityPositions)
                        .ifPresent(entityPositions -> {
                            double distance = .1f * range;
                            for (RadarTrack track : entityPositions) {
                                Vec3 entityPos = track.position();
                                entityPos = entityPos.multiply(1, 0, 1);
                                Vec3 selectedNew = selected.multiply(1, 0, 1);
                                double newDistance = entityPos.distanceTo(selectedNew);
                                if (monitor.hoveredEntity != null && monitor.hoveredEntity.equals(track.entityId()) && newDistance > distance) {
                                    monitor.hoveredEntity = null;
                                    monitor.notifyUpdate();
                                }
                                if (newDistance < distance) {
                                    distance = newDistance;
                                    monitor.hoveredEntity = track.entityId();
                                    monitor.notifyUpdate();
                                }
                            }
                        });
            }
        }

    }
}
