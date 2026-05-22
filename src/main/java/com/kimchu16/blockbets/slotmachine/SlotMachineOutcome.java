package com.kimchu16.blockbets.slotmachine;

import net.minecraft.text.Text;
import net.minecraft.util.math.random.Random;

public enum SlotMachineOutcome {
    JACKPOT(0, "jackpot", 5, 200),
    WIN(1, "win", 20, 140),
    PUSH(2, "push", 25, 100),
    LOSS(3, "loss", 30, 40),
    BUST(4, "bust", 20, 0);

    public static final int NO_OUTCOME_ID = -1;

    private final int id;
    private final String name;
    private final int weight;
    private final int payoutPercent;

    SlotMachineOutcome(int id, String name, int weight, int payoutPercent) {
        this.id = id;
        this.name = name;
        this.weight = weight;
        this.payoutPercent = payoutPercent;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getWeight() {
        return weight;
    }

    public int getPayoutPercent() {
        return payoutPercent;
    }

    public int calculatePayout(int betAmount) {
        long payout = (long) betAmount * payoutPercent / 100L;
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, payout));
    }

    public Text getDisplayText() {
        return Text.translatable("gui.blockbets.slot_machine.outcome." + name);
    }

    public static SlotMachineOutcome roll(Random random) {
        int roll = random.nextInt(100);
        int cumulativeWeight = 0;

        for (SlotMachineOutcome outcome : values()) {
            cumulativeWeight += outcome.weight;
            if (roll < cumulativeWeight) {
                return outcome;
            }
        }

        return BUST;
    }

    public static Text getDisplayText(int id) {
        SlotMachineOutcome outcome = fromId(id);
        if (outcome == null) {
            return Text.translatable("gui.blockbets.slot_machine.output_placeholder");
        }
        return outcome.getDisplayText();
    }

    public static SlotMachineOutcome fromId(int id) {
        for (SlotMachineOutcome outcome : values()) {
            if (outcome.id == id) {
                return outcome;
            }
        }
        return null;
    }
}
