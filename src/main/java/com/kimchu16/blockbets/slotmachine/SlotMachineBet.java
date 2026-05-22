package com.kimchu16.blockbets.slotmachine;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public final class SlotMachineBet {
    private SlotMachineBet() {
    }

    public static Item getBetItem() {
        return SlotMachineConfig.get().getBetItem();
    }

    public static int getBetAmount() {
        return SlotMachineConfig.get().getBetAmount();
    }

    public static boolean isExactBet(ItemStack stack) {
        return !stack.isEmpty()
                && stack.isOf(getBetItem())
                && stack.getCount() == getBetAmount()
                && stack.getComponentChanges().isEmpty();
    }

    public static ItemStack createPayoutStack(int count) {
        if (count <= 0) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(getBetItem(), count);
    }
}
