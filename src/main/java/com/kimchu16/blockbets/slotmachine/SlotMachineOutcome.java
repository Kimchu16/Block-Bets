package com.kimchu16.blockbets.slotmachine;

import java.math.BigDecimal;

import net.minecraft.text.Text;
import net.minecraft.util.math.random.Random;

public enum SlotMachineOutcome {
    JACKPOT(0, "jackpot", 5, "2.0"),
    WIN(1, "win", 20, "1.4"),
    PUSH(2, "push", 25, "1.0"),
    LOSS(3, "loss", 30, "0.4"),
    BUST(4, "bust", 20, "0");

    public static final int NO_OUTCOME_ID = -1;

    private final int id;
    private final String name;
    private final int defaultWeight;
    private final BigDecimal defaultPayoutMultiplier;

    SlotMachineOutcome(int id, String name, int defaultWeight, String defaultPayoutMultiplier) {
        this.id = id;
        this.name = name;
        this.defaultWeight = defaultWeight;
        this.defaultPayoutMultiplier = new BigDecimal(defaultPayoutMultiplier);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getDefaultWeight() {
        return defaultWeight;
    }

    public BigDecimal getDefaultPayoutMultiplier() {
        return defaultPayoutMultiplier;
    }

    public int calculatePayout(int betAmount) {
        return SlotMachineConfig.get().calculatePayout(this, betAmount);
    }

    public Text getDisplayText() {
        return Text.translatable("gui.blockbets.slot_machine.outcome." + name);
    }

    public static SlotMachineOutcome roll(Random random) {
        SlotMachineConfig config = SlotMachineConfig.get();
        int roll = random.nextInt(config.getTotalWeight());
        int cumulativeWeight = 0;

        for (SlotMachineOutcome outcome : values()) {
            cumulativeWeight += config.getWeight(outcome);
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
