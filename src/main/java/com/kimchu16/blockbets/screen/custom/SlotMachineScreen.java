package com.kimchu16.blockbets.screen.custom;

import com.kimchu16.blockbets.BlockBets;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class SlotMachineScreen extends HandledScreen<SlotMachineScreenHandler> {
    public static final Identifier  GUI_TEXTURE =
            Identifier.of(BlockBets.MOD_ID, "textures/gui/slot_machine/slot_machine_gui.png");

    public SlotMachineScreen(SlotMachineScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Override
    protected void init() {
        super.init(); // Need this to offset inventory slots instead of defaulting to top left of screen (this.x, this.y)

        ButtonWidget spinButton = ButtonWidget.builder(Text.translatable("gui.blockbets.slot_machine.spin"), (btn) -> {
            if (this.client != null && this.client.interactionManager != null) {
                this.client.interactionManager.clickButton(this.handler.syncId, SlotMachineScreenHandler.SPIN_BUTTON_ID);
            }
        }).dimensions(this.x + 86, this.y + 50, 46, 20).build();

        ButtonWidget closeButton = ButtonWidget.builder(Text.translatable("gui.blockbets.slot_machine.close"), (btn) -> this.close())
                .dimensions(this.x + 134, this.y + 50, 34, 20)
                .build();

        this.addDrawableChild(spinButton);
        this.addDrawableChild(closeButton);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, this.title, 8, 6, 0x404040, false);
        context.drawText(this.textRenderer, Text.translatable("gui.blockbets.slot_machine.bet_input"), 24, 22, 0x404040, false);
        context.drawText(this.textRenderer, Text.translatable("gui.blockbets.slot_machine.output"), 86, 22, 0x404040, false);
        context.drawText(this.textRenderer, Text.translatable("gui.blockbets.slot_machine.output_placeholder"), 86, 34, 0x606060, false);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);

        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;

        context.drawTexture(GUI_TEXTURE, x, y, 0, 0, backgroundWidth, backgroundHeight);
    }

}
