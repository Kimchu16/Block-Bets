package com.kimchu16.blockbets.screen.custom;

import com.kimchu16.blockbets.BlockBets;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
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

        int buttonWidth = 18;
        int x = this.x + (backgroundWidth / 2) - (buttonWidth / 2) - 10;
        int y = this.y + 50;

        ButtonWidget rollButton = ButtonWidget.builder(Text.of("Roll"), (btn) -> {
            this.client.getToastManager().add(
                    SystemToast.create(this.client, SystemToast.Type.NARRATOR_TOGGLE, Text.of("Test"), Text.of("Button works!"))
            );
        }).dimensions(x,y, 40, 20).build();

        this.addDrawableChild(rollButton);
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
