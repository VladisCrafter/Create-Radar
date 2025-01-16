package com.happysg.radar.block.radar.link.screens;

import com.happysg.radar.block.radar.bearing.RadarTrack;
import net.minecraft.nbt.CompoundTag;

public record TargetingConfig(boolean player, boolean contraption, boolean mob, boolean animal, boolean projectile,
                              boolean autoTarget, boolean autoFire) {

    public static final TargetingConfig DEFAULT = new TargetingConfig(false, false, true, true, false, false, true);

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("player", player);
        tag.putBoolean("contraption", contraption);
        tag.putBoolean("mob", mob);
        tag.putBoolean("animal", animal);
        tag.putBoolean("projectile", projectile);
        tag.putBoolean("autoTarget", autoTarget);
        tag.putBoolean("autoFire", autoFire);
        return tag;
    }

    public static TargetingConfig fromTag(CompoundTag tag) {
        return new TargetingConfig(
                tag.getBoolean("player"),
                tag.getBoolean("contraption"),
                tag.getBoolean("mob"),
                tag.getBoolean("animal"),
                tag.getBoolean("projectile"),
                tag.getBoolean("autoTarget"),
                tag.getBoolean("autoFire")
        );
    }

    public boolean test(RadarTrack.EntityType entityType) {
        return switch (entityType) {
            case PLAYER -> player;
            case CONTRAPTION -> contraption;
            case MOB -> mob;
            case ANIMAL -> animal;
            case PROJECTILE -> projectile;
            default -> false;
        };
    }
}
