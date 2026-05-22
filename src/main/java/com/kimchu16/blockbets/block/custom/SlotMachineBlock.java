package com.kimchu16.blockbets.block.custom;

import com.kimchu16.blockbets.block.entity.custom.SlotMachineBlockEntity;
import com.kimchu16.blockbets.block.entity.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

public class SlotMachineBlock extends BlockWithEntity {
    public static final MapCodec<SlotMachineBlock> CODEC = createCodec(SlotMachineBlock::new);
    public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = Properties.DOUBLE_BLOCK_HALF;
    private static final VoxelShape LOWER_SHAPE = Block.createCuboidShape(2.0, 0.0, 1.0, 16.0, 16.0, 15.0);
    private static final VoxelShape UPPER_SHAPE = Block.createCuboidShape(2.0, 0.0, 1.0, 16.0, 13.0, 15.0);

    public SlotMachineBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState()
                .with(FACING, Direction.SOUTH)
                .with(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return state.get(HALF) == DoubleBlockHalf.UPPER ? UPPER_SHAPE : LOWER_SHAPE;
    }

    @Override
    protected MapCodec<? extends SlotMachineBlock> getCodec() {
        return CODEC;
    }

    // Allows the Front of the slot machine to always face player when placed
    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockPos upperPos = ctx.getBlockPos().up();
        if (!ctx.getWorld().isInBuildLimit(upperPos) || !ctx.getWorld().getBlockState(upperPos).canReplace(ctx)) {
            return null;
        }

        return getDefaultState()
                .with(FACING, ctx.getHorizontalPlayerFacing().getOpposite())
                .with(HALF, DoubleBlockHalf.LOWER);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        if (state.get(HALF) == DoubleBlockHalf.LOWER) {
            world.setBlockState(pos.up(), state.with(HALF, DoubleBlockHalf.UPPER), Block.NOTIFY_ALL);
        }
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                                   WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        DoubleBlockHalf half = state.get(HALF);
        if (isPairedHalfDirection(half, direction)
                && (!neighborState.isOf(this) || neighborState.get(HALF) == half)) {
            return Blocks.AIR.getDefaultState();
        }

        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        if (state.get(HALF) == DoubleBlockHalf.UPPER) {
            BlockState lowerState = world.getBlockState(pos.down());
            return lowerState.isOf(this) && lowerState.get(HALF) == DoubleBlockHalf.LOWER;
        }
        return true;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF);
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return state.get(HALF) == DoubleBlockHalf.LOWER ? new SlotMachineBlockEntity(pos, state) : null;
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return !world.isClient() && state.get(HALF) == DoubleBlockHalf.LOWER
                ? validateTicker(type, ModBlockEntities.SLOT_MACHINE_BE, SlotMachineBlockEntity::tick)
                : null;
    }

    // If method override missing the model will appear invisible
    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;

    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient()) {
            breakPairedHalf(world, pos, state, player);
        }

        return super.onBreak(world, pos, state, player);
    }

    @Override
    public void afterBreak(World world, PlayerEntity player, BlockPos pos, BlockState state,
                           @Nullable BlockEntity blockEntity, ItemStack tool) {
        if (player.isCreative() && state.get(HALF) == DoubleBlockHalf.LOWER) {
            return;
        }

        super.afterBreak(world, player, pos, state, blockEntity, tool);
    }

    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof SlotMachineBlockEntity slotMachineBlockEntity) {
                slotMachineBlockEntity.clearActiveUser();
                // Closing with an unspun bet keeps it in the block inventory; breaking drops it safely here.
                ItemScatterer.spawn(world, pos, slotMachineBlockEntity);
                world.updateComparators(pos, this);
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        BlockPos lowerPos = getLowerPos(state, pos);
        BlockState lowerState = world.getBlockState(lowerPos);
        if (!lowerState.isOf(this) || lowerState.get(HALF) != DoubleBlockHalf.LOWER) {
            return ActionResult.PASS;
        }

        BlockEntity blockEntity = world.getBlockEntity(lowerPos);
        if (blockEntity instanceof SlotMachineBlockEntity slotMachineBlockEntity) {
            if (!world.isClient()){
                if (slotMachineBlockEntity.tryUse(player)) {
                    player.openHandledScreen(slotMachineBlockEntity);
                } else {
                    player.sendMessage(Text.translatable("gui.blockbets.slot_machine.busy"), true);
                }
            }
        }

        return ActionResult.SUCCESS;
    }

    private static boolean isPairedHalfDirection(DoubleBlockHalf half, Direction direction) {
        return half == DoubleBlockHalf.LOWER ? direction == Direction.UP : direction == Direction.DOWN;
    }

    private static BlockPos getLowerPos(BlockState state, BlockPos pos) {
        return state.get(HALF) == DoubleBlockHalf.UPPER ? pos.down() : pos;
    }

    private void breakPairedHalf(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        DoubleBlockHalf half = state.get(HALF);
        BlockPos otherPos = half == DoubleBlockHalf.LOWER ? pos.up() : pos.down();
        BlockState otherState = world.getBlockState(otherPos);
        if (!otherState.isOf(this) || otherState.get(HALF) == half) {
            return;
        }

        if (half == DoubleBlockHalf.UPPER) {
            world.breakBlock(otherPos, !player.isCreative() && player.canHarvest(otherState), player);
        }
    }
}
