package com.kimchu16.blockbets.screen;

import com.kimchu16.blockbets.BlockBets;
import com.kimchu16.blockbets.screen.custom.SlotMachineScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class ModScreenHandlers {
    public static final ScreenHandlerType<SlotMachineScreenHandler> SLOT_MACHINE_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(BlockBets.MOD_ID, "slot_screen_handler"),
                    new ExtendedScreenHandlerType<>(SlotMachineScreenHandler::new, BlockPos.PACKET_CODEC));

    public static void registerScreenHandlers() {
        BlockBets.LOGGER.info("Registering Screen Handlers for " + BlockBets.MOD_ID);
    }
}
