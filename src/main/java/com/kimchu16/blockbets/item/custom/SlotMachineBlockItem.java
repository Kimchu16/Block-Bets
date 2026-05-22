package com.kimchu16.blockbets.item.custom;

import com.kimchu16.blockbets.slotmachine.SlotMachineBet;
import com.kimchu16.blockbets.slotmachine.SlotMachineConfig;
import com.kimchu16.blockbets.slotmachine.SlotMachineOutcome;
import net.minecraft.block.Block;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class SlotMachineBlockItem extends BlockItem {
    public SlotMachineBlockItem(Block block, Item.Settings settings) {
        super(block, settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        tooltip.add(Text.translatable("tooltip.blockbets.slot_machine.description").formatted(Formatting.GRAY));

        if (!Screen.hasShiftDown()) {
            tooltip.add(Text.translatable("tooltip.blockbets.slot_machine.hold_shift").formatted(Formatting.YELLOW));
            return;
        }

        SlotMachineConfig config = SlotMachineConfig.get();
        tooltip.add(Text.translatable(
                "tooltip.blockbets.slot_machine.bet",
                SlotMachineBet.getMinimumBetAmount(),
                SlotMachineBet.getAcceptedBetItemsText()
        ).formatted(Formatting.GRAY));

        for (SlotMachineOutcome outcome : SlotMachineOutcome.values()) {
            tooltip.add(Text.translatable(
                    "tooltip.blockbets.slot_machine.outcome",
                    outcome.getDisplayText(),
                    config.getWeight(outcome),
                    config.getPayoutMultiplier(outcome).stripTrailingZeros().toPlainString()
            ).formatted(Formatting.DARK_GRAY));
        }
    }
}
