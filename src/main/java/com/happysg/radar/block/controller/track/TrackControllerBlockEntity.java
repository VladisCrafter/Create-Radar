package com.happysg.radar.block.controller.track;

import com.happysg.radar.compat.Mods;
import com.happysg.radar.compat.vs2.VS2Utils;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniondc;

public class TrackControllerBlockEntity extends KineticBlockEntity {

    public Vec3 target;

    public TrackControllerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void tick() {
        super.tick();
        if (!Mods.TRACKWORK.isLoaded())
            return;
        // System.out.println(VS2Utils.getWorldVec(this));
        getAngleToTarget();

    }

    private double getAngleToTarget() {
        Vec3 target = getTarget();
        Vec3 pos = VS2Utils.getWorldVec(this);
        Quaterniondc worldRotation = VS2Utils.getShipManagingPos(this).getTransform().getShipToWorldRotation();

        return 0;
    }

    public Vec3 getTarget() {
        return level.getNearestPlayer(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), 100, false).position();
    }
}
