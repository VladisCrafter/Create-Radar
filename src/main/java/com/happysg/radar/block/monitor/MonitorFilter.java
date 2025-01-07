package com.happysg.radar.block.monitor;

import com.happysg.radar.block.radar.bearing.RadarTrack;

import java.util.function.Predicate;

public enum MonitorFilter {
    ALL_ENTITIES(entity -> true),
    NO_MOBS(entity -> !(entity == RadarTrack.EntityType.MOB)),
    ONLY_MOBS(entity -> entity == RadarTrack.EntityType.MOB),
    PLAYERS_ONLY(entity -> entity == RadarTrack.EntityType.PLAYER);
    //  PROJECTILES_ONLY(entity->entity instanceof Projectile),
    //  VS2_ONLY(entity->false),
    //  MOB_BOSSES_ONLY(entity->entity instanceof EnderDragon||entity instanceof WitherBoss);

    private final Predicate<RadarTrack.EntityType> predicate;

    MonitorFilter(Predicate<RadarTrack.EntityType> test) {
        this.predicate = test;
    }

    public boolean test(RadarTrack.EntityType entity) {
        return predicate.test(entity);
    }
}