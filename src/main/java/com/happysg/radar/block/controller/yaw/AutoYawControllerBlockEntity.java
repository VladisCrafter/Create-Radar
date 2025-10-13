package com.happysg.radar.block.controller.yaw;

import com.happysg.radar.block.controller.kineticstuff.YawInstruct;
import com.happysg.radar.block.network.WeaponNetwork;
import com.happysg.radar.block.network.WeaponNetworkUnit;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import com.simibubi.create.content.kinetics.transmission.sequencer.Instruction;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencedGearshiftBlock;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencerInstructions;
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
    Vector<Instruction> instructions;
    int currentInstruction;
    int currentInstructionDuration;
    float currentInstructionProgress;
    int timer;
    boolean poweredPreviously;

    public AutoYawControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        instructions = YawInstruct.createDefault();
        currentInstruction = -1;
        currentInstructionDuration = -1;
        currentInstructionProgress = 0;
        timer = 0;
        poweredPreviously = false;

    }
    public record SequenceContext(SequencerInstructions instruction, double relativeValue) {
        public static com.simibubi.create.content.kinetics.transmission.sequencer.SequencedGearshiftBlockEntity.SequenceContext fromGearshift(SequencerInstructions instruction, double kineticSpeed,
                                                                                                                                                  int absoluteValue) {
                return instruction.needsPropagation()
                        ? new com.simibubi.create.content.kinetics.transmission.sequencer.SequencedGearshiftBlockEntity.SequenceContext(instruction, kineticSpeed == 0 ? 0 : absoluteValue / kineticSpeed)
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

            public static com.simibubi.create.content.kinetics.transmission.sequencer.SequencedGearshiftBlockEntity.SequenceContext fromNBT(CompoundTag nbt) {
                if (nbt.isEmpty())
                    return null;
                return new com.simibubi.create.content.kinetics.transmission.sequencer.SequencedGearshiftBlockEntity.SequenceContext(NBTHelper.readEnum(nbt, "Mode", SequencerInstructions.class),
                        nbt.getDouble("Value"));
            }

        }



        @Override
        public void tick() {
            super.tick();

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
                level.setBlock(worldPosition, getBlockState().setValue(SequencedGearshiftBlock.STATE, 0), 3);
                return;
            }
            if (getSpeed() == 0)
                return;
            run(0);
        }

        public void risingFlank() {
            YawInstruct instruction = getInstruction(currentInstruction);
            if (instruction == null)
                return;
            if (poweredPreviously)
                return;
            poweredPreviously = true;

            switch (instruction.onRedstonePulse()) {
                case CONTINUE:
                    run(currentInstruction + 1);
                    break;
                default:
                    break;
            }
        }

        public void run(int instructionIndex) {
            YawInstruct instruction = getInstruction(instructionIndex);
            if (instruction == null || instruction.instruction == SequencerInstructions.END) {
                if (getModifier() != 0)
                    detachKinetics();
                currentInstruction = -1;
                currentInstructionDuration = -1;
                currentInstructionProgress = 0;
                sequenceContext = null;
                timer = 0;
                if (!level.hasNeighborSignal(worldPosition))
                    level.setBlock(worldPosition, getBlockState().setValue(SequencedGearshiftBlock.STATE, 0), 3);
                else
                    sendData();
                return;
            }

            detachKinetics();
            currentInstructionDuration = instruction.getDuration(0, getTheoreticalSpeed());
            currentInstruction = instructionIndex;
            currentInstructionProgress = 0;
            sequenceContext = com.simibubi.create.content.kinetics.transmission.sequencer.SequencedGearshiftBlockEntity.SequenceContext.fromGearshift(instruction.instruction, getTheoreticalSpeed() * getModifier(),
                    instruction.value);
            timer = 0;
            level.setBlock(worldPosition, getBlockState().setValue(SequencedGearshiftBlock.STATE, instructionIndex + 1), 3);
        }

        public YawInstruct getInstruction(int instructionIndex) {
            return instructionIndex >= 0 && instructionIndex < instructions.size() ? instructions.get(instructionIndex)
                    : null;
        }

        @Override
        protected void copySequenceContextFrom(KineticBlockEntity sourceBE) {}

        @Override
        public void write(CompoundTag compound, boolean clientPacket) {
            compound.putInt("InstructionIndex", currentInstruction);
            compound.putInt("InstructionDuration", currentInstructionDuration);
            compound.putFloat("InstructionProgress", currentInstructionProgress);
            compound.putInt("Timer", timer);
            compound.putBoolean("PrevPowered", poweredPreviously);
            compound.put("Instructions", Instruction.serializeAll(instructions));
            super.write(compound, clientPacket);
        }

        @Override
        protected void read(CompoundTag compound, boolean clientPacket) {
            currentInstruction = compound.getInt("InstructionIndex");
            currentInstructionDuration = compound.getInt("InstructionDuration");
            currentInstructionProgress = compound.getFloat("InstructionProgress");
            poweredPreviously = compound.getBoolean("PrevPowered");
            timer = compound.getInt("Timer");
            instructions = Instruction.deserializeAll(compound.getList("Instructions", Tag.TAG_COMPOUND));
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

        public Vector<Instruction> getInstructions() {
            return this.instructions;
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
    public WeaponNetwork getWeaponNetwork() {
        return weaponNetwork;
    }

    public void setWeaponNetwork(WeaponNetwork weaponNetwork) {
        this.weaponNetwork = weaponNetwork;
    }
    public BlockEntity getBlockEntity() {return this;}

}
