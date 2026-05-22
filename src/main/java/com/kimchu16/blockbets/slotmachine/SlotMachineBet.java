package com.kimchu16.blockbets.slotmachine;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.stream.Collectors;

public final class SlotMachineBet {
    private SlotMachineBet() {
    }

    public static int getMinimumBetAmount() {
        return SlotMachineConfig.get().getMinimumBetAmount();
    }

    public static boolean isValidBet(ItemStack stack) {
        return !stack.isEmpty()
                && SlotMachineConfig.get().isAllowedBetItem(stack.getItem())
                && stack.getCount() >= getMinimumBetAmount()
                && stack.getComponentChanges().isEmpty();
    }

    public static boolean isCleanAllowedBetItem(ItemStack stack) {
        return !stack.isEmpty()
                && SlotMachineConfig.get().isAllowedBetItem(stack.getItem())
                && stack.getComponentChanges().isEmpty();
    }

    public static Text getAcceptedBetItemsText() {
        String itemNames = SlotMachineConfig.get().getBetItems().stream()
                .map(item -> item.getName().getString())
                .collect(Collectors.joining(", "));
        return Text.literal(itemNames);
    }

    public static ItemStack createPayoutStack(Item item, int count) {
        if (count <= 0) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, count);
    }
}
