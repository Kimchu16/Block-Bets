package com.kimchu16.blockbets.block.entity;

import com.kimchu16.blockbets.BlockBets;
import com.kimchu16.blockbets.block.ModBlocks;
import com.kimchu16.blockbets.block.entity.custom.SlotMachineBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntities {
    public static final BlockEntityType<SlotMachineBlockEntity> SLOT_MACHINE_BE =
            Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(BlockBets.MOD_ID, "slot_machine_be"),
                    BlockEntityType.Builder.create(SlotMachineBlockEntity::new, ModBlocks.SLOT_MACHINE_BLOCK).build(null));

    public static void registerBlockEntities(){
        BlockBets.LOGGER.info("Registering Block Entities for " + BlockBets.MOD_ID);
    }
}
