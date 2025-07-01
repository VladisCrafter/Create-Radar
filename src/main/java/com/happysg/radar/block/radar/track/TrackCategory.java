package com.happysg.radar.block.radar.track;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;

public enum TrackCategory {
    PLAYER,
    MOB,
    HOSTILE,
    ANIMAL,
    VS2,
    PROJECTILE,
    CONTRAPTION,
    MISC;


    public static TrackCategory get(Entity entity) {
        if (entity instanceof Player) {
            return PLAYER;
        } else if (entity instanceof Mob) {
            if (entity instanceof Animal) {
                return ANIMAL;
            }
            if (entity instanceof Enemy) {
                return HOSTILE;
            }
            return MOB;
        } else if (entity instanceof AbstractContraptionEntity) {
            return CONTRAPTION;
        } else if (entity instanceof Projectile) {
            return PROJECTILE;
        }
        return MISC;
    }
}