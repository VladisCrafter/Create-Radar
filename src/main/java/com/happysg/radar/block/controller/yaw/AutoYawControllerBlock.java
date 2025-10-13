package com.happysg.radar.block.controller.yaw;

import com.happysg.radar.registry.ModBlockEntityTypes;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencedGearshiftBlockEntity;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencedGearshiftScreen;
import com.simibubi.create.foundation.block.IBE;
import net.createmod.catnip.gui.ScreenOpener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import static com.simibubi.create.content.kinetics.transmission.sequencer.SequencedGearshiftBlock.STATE;
import static com.simibubi.create.content.kinetics.transmission.sequencer.SequencedGearshiftBlock.VERTICAL;

public class AutoYawControllerBlock extends HorizontalAxisKineticBlock implements IBE<AutoYawControllerBlockEntity> {


    public AutoYawControllerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(STATE, VERTICAL));
    }

    @Override
    public boolean shouldCheckWeakPower(BlockState state, SignalGetter level, BlockPos pos, Direction side) {
        return false;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos,
                                boolean isMoving) {
        if (level.isClientSide)
            return;
        if (!level.getBlockTicks()
                .willTickThisTick(pos, this))
            level.scheduleTick(pos, this, 1);
    }

    @Override
    public void tick(BlockState state, ServerLevel worldIn, BlockPos pos, RandomSource r) {
        withBlockEntityDo(worldIn, pos, sgte -> sgte.update(true, true));
    }

    @Override
    protected boolean areStatesKineticallyEquivalent(BlockState oldState, BlockState newState) {
        return false;
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        if (state.getValue(VERTICAL))
            return face.getAxis()
                    .isVertical();
        return super.hasShaftTowards(world, pos, state, face);
    }



    @OnlyIn(value = Dist.CLIENT)
    protected void displayScreen(SequencedGearshiftBlockEntity be, Player player) {
        if (player instanceof LocalPlayer)
            ScreenOpener.open(new SequencedGearshiftScreen(be));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction.Axis preferredAxis = RotatedPillarKineticBlock.getPreferredAxis(context);
        if (preferredAxis != null && (context.getPlayer() == null || !context.getPlayer()
                .isShiftKeyDown()))
            return withAxis(preferredAxis, context);
        return withAxis(context.getNearestLookingDirection()
                .getAxis(), context);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        BlockState newState = state;

        if (context.getClickedFace()
                .getAxis() != Direction.Axis.Y)
            if (newState.getValue(HORIZONTAL_AXIS) != context.getClickedFace()
                    .getAxis())
                newState = newState.cycle(VERTICAL);

        return super.onWrenched(newState, context);
    }

    private BlockState withAxis(Direction.Axis axis, BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(VERTICAL, axis.isVertical());
        if (axis.isVertical())
            return state.setValue(HORIZONTAL_AXIS, context.getHorizontalDirection()
                    .getAxis());
        return state.setValue(HORIZONTAL_AXIS, axis);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        if (state.getValue(VERTICAL))
            return Direction.Axis.Y;
        return super.getRotationAxis(state);
    }


    @Override
    public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {

        for(Direction direction : Direction.values()) {
            pLevel.updateNeighborsAt(pPos.relative(direction), this);
        }
        BlockEntity be = pLevel.getBlockEntity(pPos);
        if (be instanceof AutoYawControllerBlockEntity AutoyawControllerBlockEntity) {
            AutoyawControllerBlockEntity.onPlaced();
        }
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (!pIsMoving) {
            for(Direction direction : Direction.values()) {
                pLevel.updateNeighborsAt(pPos.relative(direction), this);
            }
        }
        BlockEntity be = pLevel.getBlockEntity(pPos);
        if (be instanceof AutoYawControllerBlockEntity AutoyawControllerBlockEntity) {
            AutoyawControllerBlockEntity.onRemoved();
        }
    }

    @Override
    public Class<AutoYawControllerBlockEntity> getBlockEntityClass() {
        return AutoYawControllerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AutoYawControllerBlockEntity> getBlockEntityType() {
        return ModBlockEntityTypes.AUTO_YAW_CONTROLLER.get();
    }
}
