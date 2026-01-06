package com.happysg.radar.compat.computercraft;

import com.happysg.radar.CreateRadar;
import com.happysg.radar.block.controller.pitch.AutoPitchControllerBlockEntity;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.GenericPeripheral;
import net.minecraft.world.phys.Vec3;

public class PitchControllerPeripheral implements GenericPeripheral {
    @Override
    public String id() {
        return CreateRadar.asResource("pitch_controller").toString();
    }

//    @LuaFunction(mainThread = true)
//    public void setTargetPosition(AutoPitchControllerBlockEntity entity, double x, double y, double z) {
//        entity.setTarget(new Vec3(x, y, z)); //TODO
//    }

    @LuaFunction(mainThread = true)
    public void setTargetAngle(AutoPitchControllerBlockEntity entity, float angle) {
        entity.setTargetAngle(angle);
    }

    @LuaFunction(mainThread = true)
    public double getTargetAngle(AutoPitchControllerBlockEntity entity){
        return entity.getTargetAngle();
    }
}
