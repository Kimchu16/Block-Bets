package com.kimchu16.blockbets.screen.custom;

import com.kimchu16.blockbets.block.entity.custom.SlotMachineBlockEntity;
import com.kimchu16.blockbets.screen.ModScreenHandlers;
import com.kimchu16.blockbets.slotmachine.SlotMachineBet;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;

public class SlotMachineScreenHandler extends ScreenHandler {
    public static final int BET_SLOT_ID = 0;
    public static final int SPIN_BUTTON_ID = 0;

    private final Inventory inventory;
    private final SlotMachineBlockEntity blockEntity;
    private final PropertyDelegate propertyDelegate;

    public SlotMachineScreenHandler(int syncId, PlayerInventory playerInventory, BlockPos pos) {
        this(syncId, playerInventory, playerInventory.player.getWorld().getBlockEntity(pos));
    }

    public SlotMachineScreenHandler(int syncId, PlayerInventory playerInventory, BlockEntity blockEntity) {
        super(ModScreenHandlers.SLOT_MACHINE_SCREEN_HANDLER, syncId);
        if (!(blockEntity instanceof SlotMachineBlockEntity slotMachineBlockEntity)) {
            throw new IllegalStateException("Slot machine screen opened without a slot machine block entity");
        }

        this.inventory = slotMachineBlockEntity;
        this.blockEntity = slotMachineBlockEntity;
        this.propertyDelegate = slotMachineBlockEntity.getPropertyDelegate();
        addProperties(this.propertyDelegate);

        this.addSlot(new BetInputSlot(
                inventory,
                slotMachineBlockEntity,
                SlotMachineBlockEntity.INPUT_SLOT,
                SlotMachineGuiLayout.INPUT_SLOT_X,
                SlotMachineGuiLayout.INPUT_SLOT_Y
        ));

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();
            if (invSlot < this.inventory.size()) {
                if (!this.insertItem(originalStack, this.inventory.size(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.insertItem(originalStack, 0, this.inventory.size(), false)) {
                return  ItemStack.EMPTY;
            }

            if (originalStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }
        return newStack;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id == SPIN_BUTTON_ID) {
            if (player.getWorld().isClient()) {
                return false;
            }

            return this.blockEntity.spin(player) != null;
        }

        return false;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        // Unspun bets intentionally remain stored in the block inventory and are dropped if the block is broken.
        this.blockEntity.releaseUser(player);
    }

    private void addPlayerInventory(PlayerInventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l <9; ++l) {
                this.addSlot(new Slot(
                        playerInventory,
                        l + i * 9 + 9,
                        SlotMachineGuiLayout.INVENTORY_X + l * SlotMachineGuiLayout.SLOT_SPACING,
                        SlotMachineGuiLayout.INVENTORY_Y + i * SlotMachineGuiLayout.SLOT_SPACING
                ));
            }
        }
    }

    private void addPlayerHotbar(PlayerInventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(
                    playerInventory,
                    i,
                    SlotMachineGuiLayout.INVENTORY_X + i * SlotMachineGuiLayout.SLOT_SPACING,
                    SlotMachineGuiLayout.HOTBAR_Y
            ));
        }
    }

    public int getLastOutcomeId() {
        return propertyDelegate.get(0);
    }

    private static class BetInputSlot extends Slot {
        private final SlotMachineBlockEntity blockEntity;

        public BetInputSlot(Inventory inventory, SlotMachineBlockEntity blockEntity, int index, int x, int y) {
            super(inventory, index, x, y);
            this.blockEntity = blockEntity;
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return !blockEntity.isRolling() && SlotMachineBet.isValidBet(stack);
        }

        @Override
        public ItemStack insertStack(ItemStack stack, int count) {
            int insertedCount = Math.min(stack.getCount(), count);
            if (!hasStack()
                    && insertedCount < SlotMachineBet.getMinimumBetAmount()
                    && SlotMachineBet.isCleanAllowedBetItem(stack)) {
                return stack;
            }
            return super.insertStack(stack, count);
        }

        @Override
        public boolean canTakeItems(PlayerEntity playerEntity) {
            return !blockEntity.isRolling() && super.canTakeItems(playerEntity);
        }

        @Override
        public int getMaxItemCount() {
            return super.getMaxItemCount();
        }

        @Override
        public int getMaxItemCount(ItemStack stack) {
            return stack.getMaxCount();
        }
    }
}
