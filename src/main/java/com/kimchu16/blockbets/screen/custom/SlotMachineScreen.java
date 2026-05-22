package com.kimchu16.blockbets.screen.custom;

import com.kimchu16.blockbets.slotmachine.SlotMachineBet;
import com.kimchu16.blockbets.slotmachine.SlotMachineConfig;
import com.kimchu16.blockbets.slotmachine.SlotMachineOutcome;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

import static com.kimchu16.blockbets.screen.custom.SlotMachineGuiLayout.CLOSE_BUTTON_X;
import static com.kimchu16.blockbets.screen.custom.SlotMachineGuiLayout.CLOSE_BUTTON_Y;
import static com.kimchu16.blockbets.screen.custom.SlotMachineGuiLayout.GUI_HEIGHT;
import static com.kimchu16.blockbets.screen.custom.SlotMachineGuiLayout.GUI_WIDTH;
import static com.kimchu16.blockbets.screen.custom.SlotMachineGuiLayout.HOTBAR_Y;
import static com.kimchu16.blockbets.screen.custom.SlotMachineGuiLayout.INPUT_SLOT_X;
import static com.kimchu16.blockbets.screen.custom.SlotMachineGuiLayout.INPUT_SLOT_Y;
import static com.kimchu16.blockbets.screen.custom.SlotMachineGuiLayout.INVENTORY_X;
import static com.kimchu16.blockbets.screen.custom.SlotMachineGuiLayout.INVENTORY_Y;
import static com.kimchu16.blockbets.screen.custom.SlotMachineGuiLayout.PADDING;
import static com.kimchu16.blockbets.screen.custom.SlotMachineGuiLayout.RESULT_PANEL_HEIGHT;
import static com.kimchu16.blockbets.screen.custom.SlotMachineGuiLayout.RESULT_PANEL_WIDTH;
import static com.kimchu16.blockbets.screen.custom.SlotMachineGuiLayout.RESULT_PANEL_X;
import static com.kimchu16.blockbets.screen.custom.SlotMachineGuiLayout.RESULT_PANEL_Y;
import static com.kimchu16.blockbets.screen.custom.SlotMachineGuiLayout.SPIN_BUTTON_X;
import static com.kimchu16.blockbets.screen.custom.SlotMachineGuiLayout.SPIN_BUTTON_Y;
import static com.kimchu16.blockbets.screen.custom.SlotMachineGuiLayout.TITLE_Y;

public class SlotMachineScreen extends HandledScreen<SlotMachineScreenHandler> {
    private static final int COLOR_OUTER_FRAME = 0xFF7A4A12;
    private static final int COLOR_GOLD_TRIM = 0xFFE3AF36;
    private static final int COLOR_GOLD_LIGHT = 0xFFFFD96A;
    private static final int COLOR_BACKGROUND = 0xFF16100D;
    private static final int COLOR_PANEL_RED = 0xFF5A1718;
    private static final int COLOR_PANEL_RED_DARK = 0xFF351010;
    private static final int COLOR_DISPLAY = 0xFF070707;
    private static final int COLOR_SLOT = 0xFF2E2E2E;
    private static final int COLOR_SLOT_DARK = 0xFF111111;
    private static final int COLOR_SLOT_LIGHT = 0xFF686868;
    private static final int COLOR_TEXT = 0xFFFFF0C8;
    private static final int COLOR_MUTED_TEXT = 0xFFC8AD79;
    private static final int COLOR_ERROR_TEXT = 0xFFFF7777;
    private static final int BUTTON_WIDTH = 64;
    private static final int CLOSE_BUTTON_WIDTH = 38;
    private static final int BUTTON_HEIGHT = 18;
    private static final int SLOT_SIZE = 18;
    private static final int PLAYER_SLOT_ROWS = 3;
    private static final int PLAYER_SLOT_COLUMNS = 9;

    private boolean invalidBetShown;
    private int observedOutcomeId = SlotMachineOutcome.NO_OUTCOME_ID;

    public SlotMachineScreen(SlotMachineScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = GUI_WIDTH;
        this.backgroundHeight = GUI_HEIGHT;
        this.playerInventoryTitleX = INVENTORY_X;
        this.playerInventoryTitleY = INVENTORY_Y - 11;
    }

    @Override
    protected void init() {
        super.init();

        ThemedButtonWidget spinButton = new ThemedButtonWidget(
                this.x + SPIN_BUTTON_X,
                this.y + SPIN_BUTTON_Y,
                BUTTON_WIDTH,
                BUTTON_HEIGHT,
                Text.translatable("gui.blockbets.slot_machine.spin"),
                () -> {
                    ItemStack betStack = this.handler.getSlot(SlotMachineScreenHandler.BET_SLOT_ID).getStack();
                    this.invalidBetShown = !SlotMachineBet.isValidBet(betStack);
                    if (this.client != null && this.client.interactionManager != null) {
                        this.client.interactionManager.clickButton(this.handler.syncId, SlotMachineScreenHandler.SPIN_BUTTON_ID);
                    }
                });
        spinButton.setTooltip(Tooltip.of(Text.translatable("tooltip.blockbets.slot_machine.gui.spin")));

        ThemedButtonWidget closeButton = new ThemedButtonWidget(
                this.x + CLOSE_BUTTON_X,
                this.y + CLOSE_BUTTON_Y,
                CLOSE_BUTTON_WIDTH,
                BUTTON_HEIGHT,
                Text.translatable("gui.blockbets.slot_machine.close"),
                this::close
        );

        this.addDrawableChild(spinButton);
        this.addDrawableChild(closeButton);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        updateResultState();
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
        drawGuiTooltips(context, mouseX, mouseY);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, GUI_WIDTH / 2, TITLE_Y, COLOR_GOLD_LIGHT);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("gui.blockbets.slot_machine.subtitle"),
                GUI_WIDTH / 2, TITLE_Y + 11, COLOR_MUTED_TEXT);

        context.drawText(this.textRenderer, Text.translatable("gui.blockbets.slot_machine.bet_input"), INPUT_SLOT_X - 2, 38, COLOR_TEXT, false);
        context.drawText(this.textRenderer, Text.translatable("gui.blockbets.slot_machine.minimum_bet",
                SlotMachineBet.getMinimumBetAmount()), PADDING + 12, 72, COLOR_MUTED_TEXT, false);

        context.drawText(this.textRenderer, Text.translatable("gui.blockbets.slot_machine.output"), RESULT_PANEL_X, 30, COLOR_TEXT, false);
        Text resultText = getResultText();
        int resultColor = invalidBetShown ? COLOR_ERROR_TEXT : COLOR_GOLD_LIGHT;
        context.drawCenteredTextWithShadow(this.textRenderer, resultText,
                RESULT_PANEL_X + RESULT_PANEL_WIDTH / 2, RESULT_PANEL_Y + 8, resultColor);
        context.drawCenteredTextWithShadow(this.textRenderer, getPayoutText(),
                RESULT_PANEL_X + RESULT_PANEL_WIDTH / 2, RESULT_PANEL_Y + 22, COLOR_MUTED_TEXT);

        context.drawText(this.textRenderer, this.playerInventoryTitle, INVENTORY_X, INVENTORY_Y - 11, COLOR_MUTED_TEXT, false);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        drawFrame(context);
        drawMachinePanel(context);
        drawInventoryPanel(context);
    }

    private void drawFrame(DrawContext context) {
        context.fill(this.x, this.y, this.x + GUI_WIDTH, this.y + GUI_HEIGHT, COLOR_OUTER_FRAME);
        context.fill(this.x + 2, this.y + 2, this.x + GUI_WIDTH - 2, this.y + GUI_HEIGHT - 2, COLOR_GOLD_TRIM);
        context.fill(this.x + 4, this.y + 4, this.x + GUI_WIDTH - 4, this.y + GUI_HEIGHT - 4, COLOR_BACKGROUND);
        context.drawBorder(this.x + 4, this.y + 4, GUI_WIDTH - 8, GUI_HEIGHT - 8, COLOR_PANEL_RED_DARK);
    }

    private void drawMachinePanel(DrawContext context) {
        drawPanel(context, PADDING, 27, GUI_WIDTH - PADDING * 2, 70, COLOR_PANEL_RED, COLOR_GOLD_TRIM);
        drawPanel(context, PADDING + 9, 37, 50, 48, COLOR_PANEL_RED_DARK, COLOR_GOLD_TRIM);
        drawMachineSlot(context, INPUT_SLOT_X, INPUT_SLOT_Y);

        drawPanel(context, RESULT_PANEL_X - 4, RESULT_PANEL_Y - 4,
                RESULT_PANEL_WIDTH + 8, RESULT_PANEL_HEIGHT + 8, COLOR_DISPLAY, COLOR_GOLD_TRIM);
        context.drawBorder(this.x + RESULT_PANEL_X - 1, this.y + RESULT_PANEL_Y - 1,
                RESULT_PANEL_WIDTH + 2, RESULT_PANEL_HEIGHT + 2, COLOR_PANEL_RED);
    }

    private void drawInventoryPanel(DrawContext context) {
        drawPanel(context, PADDING, 99, GUI_WIDTH - PADDING * 2, 84, 0xFF211C19, 0xFF4A3A27);
        for (int row = 0; row < PLAYER_SLOT_ROWS; row++) {
            for (int column = 0; column < PLAYER_SLOT_COLUMNS; column++) {
                drawPlayerSlot(context, INVENTORY_X + column * SLOT_SIZE, INVENTORY_Y + row * SLOT_SIZE);
            }
        }

        for (int column = 0; column < PLAYER_SLOT_COLUMNS; column++) {
            drawPlayerSlot(context, INVENTORY_X + column * SLOT_SIZE, HOTBAR_Y);
        }
    }

    private void drawPanel(DrawContext context, int relativeX, int relativeY, int width, int height, int fillColor, int borderColor) {
        context.fill(this.x + relativeX, this.y + relativeY, this.x + relativeX + width, this.y + relativeY + height, fillColor);
        context.drawBorder(this.x + relativeX, this.y + relativeY, width, height, borderColor);
    }

    private void drawMachineSlot(DrawContext context, int relativeX, int relativeY) {
        context.fill(this.x + relativeX - 2, this.y + relativeY - 2, this.x + relativeX + 18, this.y + relativeY + 18, COLOR_GOLD_TRIM);
        context.fill(this.x + relativeX - 1, this.y + relativeY - 1, this.x + relativeX + 17, this.y + relativeY + 17, COLOR_PANEL_RED);
        context.fill(this.x + relativeX, this.y + relativeY, this.x + relativeX + 16, this.y + relativeY + 16, COLOR_DISPLAY);
    }

    private void drawPlayerSlot(DrawContext context, int relativeX, int relativeY) {
        context.fill(this.x + relativeX - 1, this.y + relativeY - 1, this.x + relativeX + 17, this.y + relativeY + 17, COLOR_SLOT_DARK);
        context.fill(this.x + relativeX, this.y + relativeY, this.x + relativeX + 16, this.y + relativeY + 16, COLOR_SLOT);
        context.drawHorizontalLine(this.x + relativeX, this.x + relativeX + 15, this.y + relativeY, COLOR_SLOT_LIGHT);
        context.drawVerticalLine(this.x + relativeX, this.y + relativeY, this.y + relativeY + 15, COLOR_SLOT_LIGHT);
        context.drawHorizontalLine(this.x + relativeX, this.x + relativeX + 15, this.y + relativeY + 15, COLOR_SLOT_DARK);
        context.drawVerticalLine(this.x + relativeX + 15, this.y + relativeY, this.y + relativeY + 15, COLOR_SLOT_DARK);
    }

    private void drawGuiTooltips(DrawContext context, int mouseX, int mouseY) {
        if (this.focusedSlot != null && this.focusedSlot.hasStack()) {
            return;
        }

        if (this.focusedSlot != null && this.focusedSlot.id == SlotMachineScreenHandler.BET_SLOT_ID) {
            context.drawTooltip(this.textRenderer, getBetTooltip(), mouseX, mouseY);
            return;
        }

        if (isMouseOver(mouseX, mouseY, RESULT_PANEL_X - 4, RESULT_PANEL_Y - 4,
                RESULT_PANEL_WIDTH + 8, RESULT_PANEL_HEIGHT + 8)) {
            context.drawTooltip(this.textRenderer, getResultTooltip(), mouseX, mouseY);
        }
    }

    private boolean isMouseOver(int mouseX, int mouseY, int relativeX, int relativeY, int width, int height) {
        return mouseX >= this.x + relativeX
                && mouseX < this.x + relativeX + width
                && mouseY >= this.y + relativeY
                && mouseY < this.y + relativeY + height;
    }

    private void updateResultState() {
        int currentOutcomeId = this.handler.getLastOutcomeId();
        if (currentOutcomeId != observedOutcomeId) {
            observedOutcomeId = currentOutcomeId;
            invalidBetShown = false;
        }
    }

    private Text getResultText() {
        if (invalidBetShown) {
            return Text.translatable("gui.blockbets.slot_machine.result.invalid");
        }

        SlotMachineOutcome outcome = SlotMachineOutcome.fromId(this.handler.getLastOutcomeId());
        if (outcome == null) {
            return Text.translatable("gui.blockbets.slot_machine.result.ready");
        }
        return outcome.getDisplayText();
    }

    private Text getPayoutText() {
        if (invalidBetShown) {
            return Text.translatable("gui.blockbets.slot_machine.result.payout_zero");
        }

        SlotMachineOutcome outcome = SlotMachineOutcome.fromId(this.handler.getLastOutcomeId());
        if (outcome == null) {
            return Text.translatable("gui.blockbets.slot_machine.result.payout_empty");
        }

        return Text.translatable("gui.blockbets.slot_machine.result.payout_multiplier",
                SlotMachineConfig.get().getPayoutMultiplier(outcome).stripTrailingZeros().toPlainString());
    }

    private List<Text> getBetTooltip() {
        return List.of(
                Text.translatable("tooltip.blockbets.slot_machine.gui.bet",
                        SlotMachineBet.getMinimumBetAmount(),
                        SlotMachineBet.getAcceptedBetItemsText()).formatted(Formatting.GOLD),
                Text.translatable("tooltip.blockbets.slot_machine.gui.clean").formatted(Formatting.GRAY)
        );
    }

    private List<Text> getResultTooltip() {
        List<Text> tooltip = new ArrayList<>();
        tooltip.add(Text.translatable("tooltip.blockbets.slot_machine.gui.result", getResultText()).formatted(Formatting.GOLD));
        tooltip.add(getPayoutText().copy().formatted(Formatting.GRAY));
        tooltip.add(Text.empty());
        tooltip.add(Text.translatable("tooltip.blockbets.slot_machine.gui.odds").formatted(Formatting.YELLOW));

        SlotMachineConfig config = SlotMachineConfig.get();
        for (SlotMachineOutcome outcome : SlotMachineOutcome.values()) {
            tooltip.add(Text.translatable(
                    "tooltip.blockbets.slot_machine.outcome",
                    outcome.getDisplayText(),
                    config.getWeight(outcome),
                    config.getPayoutMultiplier(outcome).stripTrailingZeros().toPlainString()
            ).formatted(Formatting.DARK_GRAY));
        }

        return tooltip;
    }

    private static class ThemedButtonWidget extends PressableWidget {
        private final Runnable onPress;

        private ThemedButtonWidget(int x, int y, int width, int height, Text message, Runnable onPress) {
            super(x, y, width, height, message);
            this.onPress = onPress;
        }

        @Override
        public void onPress() {
            onPress.run();
        }

        @Override
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            int fillColor = this.active
                    ? (this.isHovered() ? 0xFF8A2422 : 0xFF6E1B1E)
                    : 0xFF352020;
            int textColor = this.active ? COLOR_GOLD_LIGHT : COLOR_MUTED_TEXT;

            context.fill(getX(), getY(), getRight(), getBottom(), fillColor);
            context.drawBorder(getX(), getY(), getWidth(), getHeight(), COLOR_GOLD_TRIM);
            context.drawBorder(getX() + 1, getY() + 1, getWidth() - 2, getHeight() - 2, COLOR_PANEL_RED_DARK);
            context.drawCenteredTextWithShadow(
                    MinecraftClient.getInstance().textRenderer,
                    getMessage(),
                    getX() + getWidth() / 2,
                    getY() + (getHeight() - 8) / 2,
                    textColor
            );
        }

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) {
            appendDefaultNarrations(builder);
        }
    }
}
