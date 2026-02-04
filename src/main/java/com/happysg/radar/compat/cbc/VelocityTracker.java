package com.happysg.radar.compat.cbc;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;

public class VelocityTracker {
    private static final Map<UUID, Vec3> LAST_POS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<UUID, Vec3> LAST_VEL = new java.util.concurrent.ConcurrentHashMap<>();

    public static Vec3 getEstimatedVelocityPerTick(Entity e) {
        if (e == null) return Vec3.ZERO;

        UUID id = e.getUUID();
        Vec3 now = e.position();

        Vec3 last = LAST_POS.put(id, now);
        if (last == null) {
            LAST_VEL.put(id, Vec3.ZERO);
            return Vec3.ZERO;
        }

        Vec3 vel = now.subtract(last); // blocks/tick
        LAST_VEL.put(id, vel);
        return vel;
    }

    public static Vec3 getLastVelocityPerTick(UUID id) {
        return LAST_VEL.getOrDefault(id, Vec3.ZERO);
    }

    public static void clear(UUID id) {
        LAST_POS.remove(id);
        LAST_VEL.remove(id);
    }
}
