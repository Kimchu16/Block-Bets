package com.kimchu16.blockbets;

import com.kimchu16.blockbets.screen.ModScreenHandlers;
import com.kimchu16.blockbets.screen.custom.SlotMachineScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class BlockBetsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(ModScreenHandlers.SLOT_MACHINE_SCREEN_HANDLER, SlotMachineScreen::new);
    }
}
