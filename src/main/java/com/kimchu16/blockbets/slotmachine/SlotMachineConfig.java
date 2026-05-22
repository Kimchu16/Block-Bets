package com.kimchu16.blockbets.slotmachine;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kimchu16.blockbets.BlockBets;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class SlotMachineConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE_NAME = "blockbets-slot-machine.json";
    private static final List<String> DEFAULT_BET_ITEM_IDS = List.of(
            "minecraft:diamond",
            "minecraft:gold_ingot",
            "minecraft:iron_ingot",
            "minecraft:emerald"
    );
    private static SlotMachineConfig instance = createDefault();

    private final List<Item> betItems;
    private final List<Identifier> betItemIds;
    private final int minimumBetAmount;
    private final EnumMap<SlotMachineOutcome, OutcomeSettings> outcomes;
    private final boolean jackpotFireworks;

    private SlotMachineConfig(
            List<Item> betItems,
            List<Identifier> betItemIds,
            int minimumBetAmount,
            EnumMap<SlotMachineOutcome, OutcomeSettings> outcomes,
            boolean jackpotFireworks
    ) {
        this.betItems = List.copyOf(betItems);
        this.betItemIds = List.copyOf(betItemIds);
        this.minimumBetAmount = minimumBetAmount;
        this.outcomes = outcomes;
        this.jackpotFireworks = jackpotFireworks;
    }

    public static void load() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME);

        try {
            if (Files.notExists(configPath)) {
                writeDefaultConfig(configPath);
                instance = createDefault();
                return;
            }

            try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
                instance = parse(JsonParser.parseReader(reader).getAsJsonObject());
            }
        } catch (Exception exception) {
            BlockBets.LOGGER.warn("Failed to load slot machine config; using defaults.", exception);
            instance = createDefault();
        }
    }

    public static SlotMachineConfig get() {
        return instance;
    }

    public List<Item> getBetItems() {
        return betItems;
    }

    public List<Identifier> getBetItemIds() {
        return betItemIds;
    }

    public int getMinimumBetAmount() {
        return minimumBetAmount;
    }

    public boolean isAllowedBetItem(Item item) {
        return betItems.contains(item);
    }

    public int getWeight(SlotMachineOutcome outcome) {
        return outcomes.get(outcome).weight();
    }

    public BigDecimal getPayoutMultiplier(SlotMachineOutcome outcome) {
        return outcomes.get(outcome).payoutMultiplier();
    }

    public int getTotalWeight() {
        int totalWeight = 0;
        for (OutcomeSettings settings : outcomes.values()) {
            totalWeight += settings.weight();
        }
        return totalWeight;
    }

    public int calculatePayout(SlotMachineOutcome outcome, int betAmount) {
        BigDecimal payout = BigDecimal.valueOf(betAmount).multiply(outcomes.get(outcome).payoutMultiplier());
        BigDecimal roundedDown = payout.setScale(0, RoundingMode.DOWN);
        if (roundedDown.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) {
            return Integer.MAX_VALUE;
        }
        return Math.max(0, roundedDown.intValue());
    }

    public boolean isJackpotFireworksEnabled() {
        return jackpotFireworks;
    }

    private static SlotMachineConfig parse(JsonObject root) {
        boolean valid = true;

        List<Identifier> betItemIds = new ArrayList<>();
        List<Item> betItems = new ArrayList<>();
        for (String rawBetItemId : getBetItemStrings(root)) {
            Identifier betItemId = Identifier.tryParse(rawBetItemId);
            if (betItemId == null || !Registries.ITEM.containsId(betItemId)) {
                BlockBets.LOGGER.warn("Invalid slot machine bet item '{}'; using default config.", rawBetItemId);
                valid = false;
                continue;
            }

            Item betItem = Registries.ITEM.get(betItemId);
            if (betItem == Items.AIR) {
                BlockBets.LOGGER.warn("Slot machine bet item cannot be air; using default config.");
                valid = false;
                continue;
            }

            if (!betItems.contains(betItem)) {
                betItemIds.add(betItemId);
                betItems.add(betItem);
            }
        }

        if (betItems.isEmpty()) {
            BlockBets.LOGGER.warn("Slot machine must have at least one valid bet item; using default config.");
            valid = false;
        }

        int minimumBetAmount = getInt(root, "minimumBetAmount", getInt(root, "betAmount", 5));
        if (minimumBetAmount <= 0) {
            BlockBets.LOGGER.warn("Slot machine minimum bet amount must be positive; using default config.");
            valid = false;
        }

        boolean jackpotFireworks = getBoolean(root, "jackpotFireworks", true);
        EnumMap<SlotMachineOutcome, OutcomeSettings> outcomes = new EnumMap<>(SlotMachineOutcome.class);
        JsonObject outcomeRoot = getObject(root, "outcomes");
        int totalWeight = 0;

        for (SlotMachineOutcome outcome : SlotMachineOutcome.values()) {
            JsonObject outcomeObject = outcomeRoot == null ? null : getObject(outcomeRoot, outcome.getName());
            int weight = getInt(outcomeObject, "weight", outcome.getDefaultWeight());
            BigDecimal multiplier = getBigDecimal(outcomeObject, "payoutMultiplier", outcome.getDefaultPayoutMultiplier());

            if (weight < 0) {
                BlockBets.LOGGER.warn("Slot machine outcome '{}' has a negative weight; using default config.", outcome.getName());
                valid = false;
            }

            if (multiplier.signum() < 0) {
                BlockBets.LOGGER.warn("Slot machine outcome '{}' has a negative payout multiplier; using default config.", outcome.getName());
                valid = false;
            }

            outcomes.put(outcome, new OutcomeSettings(weight, multiplier));
            totalWeight += weight;
        }

        if (totalWeight != 100) {
            BlockBets.LOGGER.warn("Slot machine outcome weights must total 100; found {}. Using default config.", totalWeight);
            valid = false;
        }

        if (!valid) {
            return createDefault();
        }

        return new SlotMachineConfig(betItems, betItemIds, minimumBetAmount, outcomes, jackpotFireworks);
    }

    private static SlotMachineConfig createDefault() {
        EnumMap<SlotMachineOutcome, OutcomeSettings> outcomes = new EnumMap<>(SlotMachineOutcome.class);
        for (SlotMachineOutcome outcome : SlotMachineOutcome.values()) {
            outcomes.put(outcome, new OutcomeSettings(outcome.getDefaultWeight(), outcome.getDefaultPayoutMultiplier()));
        }

        List<Identifier> betItemIds = new ArrayList<>();
        List<Item> betItems = new ArrayList<>();
        for (String rawBetItemId : DEFAULT_BET_ITEM_IDS) {
            Identifier betItemId = Identifier.of(rawBetItemId);
            betItemIds.add(betItemId);
            betItems.add(Registries.ITEM.get(betItemId));
        }

        return new SlotMachineConfig(betItems, betItemIds, 5, outcomes, true);
    }

    private static void writeDefaultConfig(Path configPath) throws IOException {
        Files.createDirectories(configPath.getParent());
        Files.writeString(configPath, GSON.toJson(toJson(createDefault())), StandardCharsets.UTF_8);
    }

    private static JsonObject toJson(SlotMachineConfig config) {
        JsonObject root = new JsonObject();
        JsonArray betItems = new JsonArray();
        for (Identifier betItemId : config.betItemIds) {
            betItems.add(betItemId.toString());
        }
        root.add("betItems", betItems);
        root.addProperty("minimumBetAmount", config.minimumBetAmount);
        root.addProperty("jackpotFireworks", config.jackpotFireworks);

        JsonObject outcomes = new JsonObject();
        for (Map.Entry<SlotMachineOutcome, OutcomeSettings> entry : config.outcomes.entrySet()) {
            JsonObject outcome = new JsonObject();
            outcome.addProperty("weight", entry.getValue().weight());
            outcome.addProperty("payoutMultiplier", entry.getValue().payoutMultiplier());
            outcomes.add(entry.getKey().getName(), outcome);
        }
        root.add("outcomes", outcomes);

        return root;
    }

    private static List<String> getBetItemStrings(JsonObject root) {
        JsonArray betItems = getArray(root, "betItems");
        if (betItems == null) {
            return DEFAULT_BET_ITEM_IDS;
        }

        List<String> result = new ArrayList<>();
        for (JsonElement element : betItems) {
            result.add(element.getAsString());
        }
        return result;
    }

    private static String getString(JsonObject object, String key, String fallback) {
        if (object == null || !object.has(key)) {
            return fallback;
        }
        return object.get(key).getAsString();
    }

    private static int getInt(JsonObject object, String key, int fallback) {
        if (object == null || !object.has(key)) {
            return fallback;
        }
        return object.get(key).getAsInt();
    }

    private static boolean getBoolean(JsonObject object, String key, boolean fallback) {
        if (object == null || !object.has(key)) {
            return fallback;
        }
        return object.get(key).getAsBoolean();
    }

    private static BigDecimal getBigDecimal(JsonObject object, String key, BigDecimal fallback) {
        if (object == null || !object.has(key)) {
            return fallback;
        }
        return new BigDecimal(object.get(key).getAsString());
    }

    private static JsonObject getObject(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            return null;
        }

        JsonElement element = object.get(key);
        if (!element.isJsonObject()) {
            return null;
        }

        return element.getAsJsonObject();
    }

    private static JsonArray getArray(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            return null;
        }

        JsonElement element = object.get(key);
        if (!element.isJsonArray()) {
            return null;
        }

        return element.getAsJsonArray();
    }

    private record OutcomeSettings(int weight, BigDecimal payoutMultiplier) {
    }
}
