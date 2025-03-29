package com.happysg.radar.compat.computercraft;

import com.happysg.radar.CreateRadar;
import com.happysg.radar.block.controller.yaw.AutoYawControllerBlockEntity;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.GenericPeripheral;
import net.minecraft.world.phys.Vec3;

public class YawControllerPeripheral implements GenericPeripheral {
    @Override
    public String id() {
        return CreateRadar.asResource("yaw_controller").toString();
    }

    @LuaFunction(mainThread = true)
    public void setTargetPosition(AutoYawControllerBlockEntity entity, double x, double y, double z) {
        entity.setTarget(new Vec3(x, y, z));
    }

    @LuaFunction(mainThread = true)
    public void setTargetAngle(AutoYawControllerBlockEntity entity, float angle) {
        entity.setTargetAngle(angle);
    }

    @LuaFunction(mainThread = true)
    public double getTargetAngle(AutoYawControllerBlockEntity entity){
        return entity.getTargetAngle();
    }
}
