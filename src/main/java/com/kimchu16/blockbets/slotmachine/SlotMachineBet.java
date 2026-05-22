package com.kimchu16.blockbets.slotmachine;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public final class SlotMachineBet {
    private static final Item DEFAULT_BET_ITEM = Items.DIAMOND;
    private static final int DEFAULT_BET_AMOUNT = 5;

    private SlotMachineBet() {
    }

    public static Item getBetItem() {
        return DEFAULT_BET_ITEM;
    }

    public static int getBetAmount() {
        return DEFAULT_BET_AMOUNT;
    }

    public static boolean isExactBet(ItemStack stack) {
        return !stack.isEmpty()
                && stack.isOf(DEFAULT_BET_ITEM)
                && stack.getCount() == DEFAULT_BET_AMOUNT
                && stack.getComponentChanges().isEmpty();
    }
}
