package com.happysg.radar.block.controller.yaw;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.Vector;

public class YawInstruct {

    public ControllerInst continstruction;
    InstSpeedMod speedModifier;
    public int value;

    public YawInstruct(ControllerInst instruction) {
        this(instruction, 1);
    }

    public YawInstruct(ControllerInst instruction, int value) {
        this(instruction, InstSpeedMod.FORWARD, value);
    }

    public YawInstruct(ControllerInst instruction, InstSpeedMod speedModifier, int value) {
        this.continstruction = instruction;
        this.speedModifier = speedModifier;
        this.value = value;
    }

    public int getDuration(float currentProgress, float speed) {
        speed *= speedModifier.value;
        speed = Math.abs(speed);
        double target = value - currentProgress;

        switch (continstruction) {

            // Always overshoot, target will stop early
            case TURN_ANGLE:
                double degreesPerTick = KineticBlockEntity.convertToAngular(speed);
                return (int) Math.ceil(target / degreesPerTick) + 2;
            case TURN_DISTANCE:
                double metersPerTick = KineticBlockEntity.convertToLinear(speed);
                return (int) Math.ceil(target / metersPerTick) + 2;

            // Timing instructions
            case DELAY:
                return (int) target;
            case AWAIT:
                return -1;
            case END:
            default:
                break;

        }
        return 0;
    }

    public float getTickProgress(float speed) {
        switch (continstruction) {

            case TURN_ANGLE:
                return KineticBlockEntity.convertToAngular(speed);

            case TURN_DISTANCE:
                return KineticBlockEntity.convertToLinear(speed);

            case DELAY:
                return 1;

            case AWAIT:
            case END:
            default:
                break;

        }
        return 0;
    }

    int getSpeedModifier() {
        switch (continstruction) {

            case TURN_ANGLE:
            case TURN_DISTANCE:
                return speedModifier.value;

            case END:
            case DELAY:
            case AWAIT:
            default:
                break;

        }
        return 0;
    }



    public static ListTag serializeAll(Vector<YawInstruct> instructions) {
        ListTag list = new ListTag();
        instructions.forEach(i -> list.add(i.serialize()));
        return list;
    }

    public static Vector<YawInstruct> deserializeAll(ListTag list) {
        if (list.isEmpty())
            return createDefault();
        Vector<YawInstruct> instructions = new Vector<>(5);
        list.forEach(inbt -> instructions.add(deserialize((CompoundTag) inbt)));
        return instructions;
    }

    public static Vector<YawInstruct> createDefault() {
        Vector<YawInstruct> instructions = new Vector<>(5);
        instructions.add(new YawInstruct(ControllerInst.TURN_ANGLE, 90));
        instructions.add(new YawInstruct(ControllerInst.END));
        return instructions;
    }

    CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        NBTHelper.writeEnum(tag, "Type", continstruction);
        NBTHelper.writeEnum(tag, "Modifier", speedModifier);
        tag.putInt("Value", value);
        return tag;
    }

    static YawInstruct deserialize(CompoundTag tag) {
        YawInstruct instruction = new YawInstruct(NBTHelper.readEnum(tag, "Type", ControllerInst.class));
        instruction.speedModifier = NBTHelper.readEnum(tag, "Modifier", InstSpeedMod.class);
        instruction.value = tag.getInt("Value");
        return instruction;
    }

}
