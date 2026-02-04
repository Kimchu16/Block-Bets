package com.kimchu16.blockbets.block.custom;

import com.kimchu16.blockbets.block.entity.custom.SlotMachineBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.screen.slot.Slot;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class SlotMachineBlock extends BlockWithEntity {
    public static final MapCodec<SlotMachineBlock> CODEC = createCodec(SlotMachineBlock::new);
    public static final DirectionProperty FACING = FacingBlock.FACING;
    private static final VoxelShape SHAPE = Block.createCuboidShape(2.0, 0.0, 1.0, 15.0, 16.0, 15.0);

    public SlotMachineBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    protected MapCodec<? extends SlotMachineBlock> getCodec() {
        return CODEC;
    }

    // Allows the Front of the slot machine to always face player when placed
    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {

        return new SlotMachineBlockEntity(pos, state);
    }

    // If method override missing the model will appear invisible
    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;

    }

    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        ItemScatterer.onStateReplaced(state, newState, world, pos);
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof SlotMachineBlockEntity slotMachineBlockEntity) {
            if (!world.isClient()){
                player.openHandledScreen(slotMachineBlockEntity);
            }
        }

        return ActionResult.SUCCESS;
    }
}
