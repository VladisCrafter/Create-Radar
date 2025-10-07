package com.happysg.radar.block.controller.yaw;

import com.happysg.radar.block.network.WeaponNetwork;
import com.happysg.radar.block.network.WeaponNetworkUnit;

import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlockEntity;
import rbasamoyai.createbigcannons.cannon_control.contraption.PitchOrientedContraptionEntity;

import java.util.Vector;




public class AutoYawControllerBlockEntity extends SplitShaftBlockEntity implements WeaponNetworkUnit {
    private static final double TOLERANCE = 0.1;
    private double targetAngle;
    private double currentAngle = 0.0;
    public boolean isRunning;
    private WeaponNetwork weaponNetwork;
    Vector<YawInstruct> instructions;
    int currentInstruction;
    int currentInstructionDuration;
    float currentInstructionProgress;
    int timer;
    boolean poweredPreviously;
    ContSequenceContext contsequenceContext;

    public AutoYawControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        instructions = YawInstruct.createDefault();
        currentInstruction = -1;
        currentInstructionDuration = -1;
        currentInstructionProgress = 0;
        timer = 0;
        poweredPreviously = false;

    }

    public record ContSequenceContext(ControllerInst instruction, double relativeValue) {
        public static ContSequenceContext Fromcontroller(ControllerInst instruction, double kineticSpeed,
                                                         int absoluteValue) {
                return instruction.needsPropagation()
                        ? new ContSequenceContext(instruction, kineticSpeed == 0 ? 0 : absoluteValue / kineticSpeed)
                        : null;
            }

            public double getEffectiveValue(double speedAtTarget) {
                return Math.abs(relativeValue * speedAtTarget);
            }

            public CompoundTag serializeNBT() {
                CompoundTag nbt = new CompoundTag();
                NBTHelper.writeEnum(nbt, "Mode", instruction);
                nbt.putDouble("Value", relativeValue);
                return nbt;
            }

            public static ContSequenceContext fromNBT(CompoundTag nbt) {
                if (nbt.isEmpty())
                    return null;
                return new ContSequenceContext(NBTHelper.readEnum(nbt, "Mode", ControllerInst.class),
                        nbt.getDouble("Value"));
            }

        }



        @Override
        public void tick() {
            super.tick();
            instructionsForTargetAngle(targetAngle);

            if (isIdle())
                return;
            if (level.isClientSide)
                return;
            if (currentInstructionDuration < 0)
                return;
            if (timer < currentInstructionDuration) {
                timer++;
                currentInstructionProgress += getInstruction(currentInstruction).getTickProgress(speed);
                return;
            }
            run(currentInstruction + 1);
        }


    private static int mapAngleToValue(double normalizedAngle, int maxValue) {
        // raw mapping: 0 -> 0, 359.6 -> 360 etc. Round to nearest.
        int v = (int) Math.round(normalizedAngle);
        // GUI uses range 1..maxValue (see screen code using withRange(1, max+1)), so treat 0 as maxValue.
        if (v == 0)
            v = Math.max(1, maxValue);
        if (v < 1)
            v = 1;
        if (v > maxValue)
            v = maxValue;
        return v;
    }
    public static Vector<YawInstruct> instructionsForTargetAngle(double targetAngleDeg) {
        Vector<YawInstruct> seq = new Vector<>();
        double normalized = ((targetAngleDeg % 360.0) + 360.0) % 360.0;
        int mapped = mapAngleToValue(normalized, ControllerInst.TURN_ANGLE.maxValue);
        if (mapped <= 0)
            return seq;
        InstSpeedMod speed = InstSpeedMod.FORWARD;
        seq.add(new YawInstruct(ControllerInst.TURN_ANGLE, speed, mapped));
        return seq;
    }


    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        if (isIdle())
            return;
        float currentSpeed = Math.abs(speed);
        if (Math.abs(previousSpeed) == currentSpeed)
            return;
        YawInstruct instruction = getInstruction(currentInstruction);
        if (instruction == null)
            return;
        if (getSpeed() == 0)
            run(-1);

            // Update instruction time with regards to new speed
        currentInstructionDuration = instruction.getDuration(currentInstructionProgress, getTheoreticalSpeed());
        timer = 0;
        }

        public boolean isIdle() {
            return currentInstruction == -1;
        }

        public void update(boolean isPowered, boolean isRunning) {
            if (!isIdle())
                return;
            if (isPowered == isRunning)
                return;
            if (!level.hasNeighborSignal(worldPosition)) {
                level.setBlock(worldPosition, getBlockState().setValue(AutoYawControllerBlock.STATE, 0), 3);
                return;
            }
            if (getSpeed() == 0)
                return;
            run(0);
        }

        public void run(int instructionIndex) {
            YawInstruct instruction = getInstruction(instructionIndex);
            if (instruction == null || instruction.continstruction == ControllerInst.END) {
                if (getModifier() != 0)
                    detachKinetics();
                currentInstruction = -1;
                currentInstructionDuration = -1;
                currentInstructionProgress = 0;
                contsequenceContext = null;
                timer = 0;
                    sendData();
                return;
            }

            detachKinetics();
            currentInstructionDuration = instruction.getDuration(0, getTheoreticalSpeed());
            currentInstruction = instructionIndex;
            currentInstructionProgress = 0;
            contsequenceContext = ContSequenceContext.Fromcontroller(
                    instruction.continstruction, // ← if that’s the field name in YawInstruct
                    getTheoreticalSpeed() * getModifier(),
                    instruction.value
            );
            timer = 0;
        }

        public YawInstruct getInstruction(int instructionIndex) {
            return instructionIndex >= 0 && instructionIndex < instructions.size() ? instructions.get(instructionIndex)
                    : null;
        }

        protected void copySequenceContextFrom(AutoYawControllerBlockEntity sourceBE) {}

        @Override
        public void write(CompoundTag compound, boolean clientPacket) {
            compound.putInt("InstructionIndex", currentInstruction);
            compound.putInt("InstructionDuration", currentInstructionDuration);
            compound.putFloat("InstructionProgress", currentInstructionProgress);
            compound.putInt("Timer", timer);
            compound.putBoolean("PrevPowered", poweredPreviously);
            compound.put("Instructions", YawInstruct.serializeAll(instructions));
            super.write(compound, clientPacket);
        }

        @Override
        protected void read(CompoundTag compound, boolean clientPacket) {
            currentInstruction = compound.getInt("InstructionIndex");
            currentInstructionDuration = compound.getInt("InstructionDuration");
            currentInstructionProgress = compound.getFloat("InstructionProgress");
            poweredPreviously = compound.getBoolean("PrevPowered");
            timer = compound.getInt("Timer");
            instructions = YawInstruct.deserializeAll(compound.getList("Instructions", Tag.TAG_COMPOUND));
            super.read(compound, clientPacket);
        }



        @Override
        public float getRotationSpeedModifier(Direction face) {
            if (isVirtual())
                return 1;
            return (!hasSource() || face == getSourceFacing()) ? 1 : getModifier();
        }

        public int getModifier() {
          if (atTargetYaw() == true){
              return 0;
          }else{
              return 1;
          }
        }

        public Vector<YawInstruct> getInstructions() {
            return this.instructions;
        }
    /*
    public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        for(Direction direction : Direction.values()) {
            pLevel.updateNeighborsAt(pPos.relative(direction), this);
        }
        BlockEntity be = pLevel.getBlockEntity(pPos);
        if (be instanceof archive AutoyawControllerBlockEntity) {
            AutoyawControllerBlockEntity.onPlaced();
        }
    }


    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (!pIsMoving) {
            for(Direction direction : Direction.values()) {
                pLevel.updateNeighborsAt(pPos.relative(direction), getBlockPos());
            }
        }
        BlockEntity be = pLevel.getBlockEntity(pPos);
        if (be instanceof archive AutoyawControllerBlockEntity) {
            AutoyawControllerBlockEntity.onRemoved();
        }
    }

     */
    public double getTargetAngle() {
        return targetAngle;
    }
    public boolean atTargetYaw() {
        BlockPos turretPos = getBlockPos().above();
        if (level == null || !(level.getBlockEntity(turretPos) instanceof CannonMountBlockEntity mount))
            return false;
        PitchOrientedContraptionEntity contraption = mount.getContraption();
        if (contraption == null)
            return false;
        return Math.abs(contraption.yaw - targetAngle) < TOLERANCE;
    }
    public void setTargetAngle(float targetAngle) {
        this.targetAngle = targetAngle;
        notifyUpdate();
    }
    public WeaponNetwork getWeaponNetwork() {
        return weaponNetwork;
    }

    public void setWeaponNetwork(WeaponNetwork weaponNetwork) {
        this.weaponNetwork = weaponNetwork;
    }
    public BlockEntity getBlockEntity() {return this;}

}
