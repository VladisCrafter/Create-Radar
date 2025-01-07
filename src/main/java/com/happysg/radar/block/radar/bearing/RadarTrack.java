package com.happysg.radar.block.radar.bearing;

import com.jozufozu.flywheel.util.Color;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public record RadarTrack(String entityId, Vec3 position, long scannedTime, Color color, boolean contraption, int id,
                         EntityType entityType) {

    public static int BLUE = 255;
    public static int WHITE = 16777215;
    public static int RED = 16711680;
    public static int GREEN = 65280;
    public static int YELLOW = 16776960;

    public RadarTrack(Entity entity) {
        this(entity.getStringUUID(), getPosition(entity), entity.level().getGameTime(), getColor(entity), isContraption(entity), entity.getId(), getEntityType(entity));
    }

    private static EntityType getEntityType(Entity entity) {
        if (entity instanceof Player)
            return EntityType.PLAYER;
        if (entity instanceof Projectile)
            return EntityType.PROJECTILE;
        if (entity instanceof Mob)
            return EntityType.MOB;
        if (entity instanceof AbstractContraptionEntity)
            return EntityType.CONTRAPTION;
        return EntityType.MISC;
    }

    private static Vec3 getPosition(Entity entity) {
        if (entity instanceof AbstractContraptionEntity entity1)
            return entity1.getContraption().anchor.getCenter();
        return entity.getEyePosition();
    }

    private static Color getColor(Entity entity) {
        if (entity instanceof Player)
            return new Color(BLUE);
        if (entity instanceof Animal)
            return new Color(GREEN);
        if (entity instanceof Enemy)
            return new Color(RED);
        return new Color(WHITE);
    }

    private static boolean isContraption(Entity entity) {
        return entity instanceof AbstractContraptionEntity;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("entityId", entityId);
        tag.putDouble("x", position.x);
        tag.putDouble("y", position.y);
        tag.putDouble("z", position.z);
        tag.putLong("scannedTime", scannedTime);
        tag.putInt("color", color.getRGB());
        tag.putBoolean("contraption", contraption);
        tag.putInt("id", id);
        tag.putInt("entityType", entityType.ordinal());
        return tag;
    }

    public static RadarTrack deserializeNBT(CompoundTag tag) {
        return new RadarTrack(tag.getString("entityId"), new Vec3(tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z")), tag.getLong("scannedTime"), new Color(tag.getInt("color")), tag.getBoolean("contraption"), tag.getInt("id"), EntityType.values()[tag.getInt("entityType")]);
    }

    public static CompoundTag serializeNBTList(Collection<RadarTrack> tracks) {
        ListTag list = new ListTag();
        for (RadarTrack track : tracks) {
            list.add(track.serializeNBT());
        }
        CompoundTag tag = new CompoundTag();
        tag.put("tracks", list);
        return tag;
    }

    public static List<RadarTrack> deserializeListNBT(CompoundTag tag) {
        List<RadarTrack> tracks = new ArrayList<>();
        ListTag list = tag.getList("tracks", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            tracks.add(RadarTrack.deserializeNBT(list.getCompound(i)));
        }
        return tracks;
    }

    public enum EntityType {
        MISC,
        PLAYER,
        PROJECTILE,
        MOB,
        CONTRAPTION,
        VS2,
    }
}
