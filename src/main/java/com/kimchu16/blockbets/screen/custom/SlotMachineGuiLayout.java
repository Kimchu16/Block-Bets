package com.kimchu16.blockbets.screen.custom;

public final class SlotMachineGuiLayout {
    public static final int GUI_WIDTH = 256;
    public static final int GUI_HEIGHT = 236;
    public static final int PADDING = 8;
    public static final int SLOT_SPACING = 18;
    public static final int TITLE_Y = 10;
    public static final int SUBTITLE_Y = 23;
    public static final int MAIN_PANEL_X = PADDING + 2;
    public static final int MAIN_PANEL_Y = 38;
    public static final int MAIN_PANEL_WIDTH = GUI_WIDTH - (MAIN_PANEL_X * 2);
    public static final int MAIN_PANEL_HEIGHT = 74;
    public static final int BET_PANEL_X = 22;
    public static final int BET_PANEL_Y = 50;
    public static final int BET_PANEL_WIDTH = 64;
    public static final int BET_PANEL_HEIGHT = 50;
    public static final int INPUT_SLOT_X = BET_PANEL_X + (BET_PANEL_WIDTH - SLOT_SPACING) / 2;
    public static final int INPUT_SLOT_Y = BET_PANEL_Y + 20;
    public static final int RESULT_LABEL_Y = 47;
    public static final int RESULT_PANEL_X = 100;
    public static final int RESULT_PANEL_Y = 57;
    public static final int RESULT_PANEL_WIDTH = 132;
    public static final int RESULT_PANEL_HEIGHT = 42;
    public static final int BUTTON_Y = 120;
    public static final int SPIN_BUTTON_WIDTH = 72;
    public static final int CLOSE_BUTTON_WIDTH = 48;
    public static final int BUTTON_HEIGHT = 18;
    public static final int BUTTON_GAP = 12;
    public static final int BUTTON_GROUP_WIDTH = SPIN_BUTTON_WIDTH + BUTTON_GAP + CLOSE_BUTTON_WIDTH;
    public static final int BUTTON_GROUP_X = centerButtonGroup(BUTTON_GROUP_WIDTH);
    public static final int SPIN_BUTTON_X = BUTTON_GROUP_X;
    public static final int SPIN_BUTTON_Y = BUTTON_Y;
    public static final int CLOSE_BUTTON_X = SPIN_BUTTON_X + SPIN_BUTTON_WIDTH + BUTTON_GAP;
    public static final int CLOSE_BUTTON_Y = BUTTON_Y;
    public static final int INVENTORY_LABEL_Y = 142;
    public static final int INVENTORY_X = (GUI_WIDTH - SLOT_SPACING * 9) / 2;
    public static final int INVENTORY_Y = 154;
    public static final int HOTBAR_Y = INVENTORY_Y + SLOT_SPACING * 3 + 4;

    public static int centerButtonGroup(int totalWidth) {
        return (GUI_WIDTH - totalWidth) / 2;
    }

    private SlotMachineGuiLayout() {
    }
}
