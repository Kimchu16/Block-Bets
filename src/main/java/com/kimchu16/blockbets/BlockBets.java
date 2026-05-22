package com.kimchu16.blockbets;

import com.kimchu16.blockbets.block.ModBlocks;
import com.kimchu16.blockbets.block.entity.ModBlockEntities;
import com.kimchu16.blockbets.item.ModItemGroups;
import com.kimchu16.blockbets.screen.ModScreenHandlers;
import com.kimchu16.blockbets.slotmachine.SlotMachineConfig;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockBets implements ModInitializer {
	public static final String MOD_ID = "blockbets";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		SlotMachineConfig.load();
		ModItemGroups.registerItemGroups();
		ModBlocks.registerModBlocks();
		ModBlockEntities.registerBlockEntities();
		ModScreenHandlers.registerScreenHandlers();
	}
}
