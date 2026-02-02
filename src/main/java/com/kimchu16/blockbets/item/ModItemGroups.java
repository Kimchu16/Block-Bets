package com.kimchu16.blockbets.item;

import com.kimchu16.blockbets.BlockBets;
import com.kimchu16.blockbets.block.ModBlocks;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

// Creates a custom Item Group tab in the Creative inventory which includes the mods items/blocks
public class ModItemGroups {
    public static final ItemGroup BLOCK_BETS_ITEMS_GROUP = Registry.register(Registries.ITEM_GROUP,
            Identifier.of(BlockBets.MOD_ID, "block_bets_items"),
            FabricItemGroup.builder()
                    .icon(()-> new ItemStack(ModBlocks.SLOT_MACHINE_BLOCK))
                    .displayName(Text.translatable("itemgroup.blockbets.block_bets_items"))
                    .entries((displayContext, entries) -> {
                        entries.add(ModBlocks.SLOT_MACHINE_BLOCK);
                    }).build());

    public static void registerItemGroups() {
        BlockBets.LOGGER.info("Registering Item Groups for " + BlockBets.MOD_ID);
    }
}
