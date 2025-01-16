package com.happysg.radar.compat.vs2;

import com.happysg.radar.compat.Mods;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.LoadedShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

public class VS2Utils {

    public static BlockPos getWorldPos(Level level, BlockPos pos) {
        if (!Mods.VALKYRIENSKIES.isLoaded())
            return pos;
        if (VSGameUtilsKt.getShipObjectManagingPos(level, pos) != null) {
            final LoadedShip loadedShip = VSGameUtilsKt.getShipObjectManagingPos(level, pos);
            final Vector3d vec = loadedShip.getShipToWorld().transformPosition(new Vector3d(pos.getX(), pos.getY(), pos.getZ()));
            VectorConversionsMCKt.toMinecraft(vec);
            final BlockPos newPos = new BlockPos((int) vec.x(), (int) vec.y(), (int) vec.z());
            return newPos;
        }
        return pos;
    }

    public static BlockPos getWorldPos(BlockEntity blockEntity) {
        return getWorldPos(blockEntity.getLevel(), blockEntity.getBlockPos());
    }

    public static Iterable<Ship> getLoadedShips(Level level, AABB aabb) {
        if (!Mods.VALKYRIENSKIES.isLoaded())
            return null;
        return VSGameUtilsKt.getShipsIntersecting(level, aabb);
    }

    public static LoadedShip getShipManagingPos(Level level, BlockPos pos) {
        if (!Mods.VALKYRIENSKIES.isLoaded())
            return null;
        return VSGameUtilsKt.getShipObjectManagingPos(level, pos);
    }

    public static LoadedShip getShipManagingPos(BlockEntity blockEntity) {
        return getShipManagingPos(blockEntity.getLevel(), blockEntity.getBlockPos());
    }

    public static Vec3 getWorldVec(Level level, BlockPos pos) {
        if (!Mods.VALKYRIENSKIES.isLoaded())
            return new Vec3(pos.getX(), pos.getY(), pos.getZ());
        if (VSGameUtilsKt.getShipObjectManagingPos(level, pos) != null) {
            final LoadedShip loadedShip = VSGameUtilsKt.getShipObjectManagingPos(level, pos);
            final Vector3d vec = loadedShip.getShipToWorld().transformPosition(new Vector3d(pos.getX(), pos.getY(), pos.getZ()));
            VectorConversionsMCKt.toMinecraft(vec);
            return new Vec3(vec.x(), vec.y(), vec.z());
        }
        return new Vec3(pos.getX(), pos.getY(), pos.getZ());
    }

    public static Vec3 getWorldVec(BlockEntity blockEntity) {
        return getWorldVec(blockEntity.getLevel(), blockEntity.getBlockPos());
    }

}
