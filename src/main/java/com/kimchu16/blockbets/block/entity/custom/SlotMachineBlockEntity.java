package com.kimchu16.blockbets.block.entity.custom;

import com.kimchu16.blockbets.block.entity.ImplementedInventory;
import com.kimchu16.blockbets.block.entity.ModBlockEntities;
import com.kimchu16.blockbets.screen.custom.SlotMachineScreenHandler;
import com.kimchu16.blockbets.slotmachine.SlotMachineBet;
import com.kimchu16.blockbets.slotmachine.SlotMachineOutcome;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class SlotMachineBlockEntity extends BlockEntity implements ImplementedInventory, ExtendedScreenHandlerFactory<BlockPos> {
    public static final int INPUT_SLOT = 0;
    public static final int INVENTORY_SIZE = 1;
    private static final String LAST_OUTCOME_KEY = "LastOutcome";

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return index == 0 ? lastOutcomeId : 0;
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                lastOutcomeId = value;
            }
        }

        @Override
        public int size() {
            return 1;
        }
    };
    @Nullable
    private UUID activeUser;
    private boolean rolling;
    private int lastOutcomeId = SlotMachineOutcome.NO_OUTCOME_ID;

    public SlotMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SLOT_MACHINE_BE, pos, state);
    }

    @Override
    public DefaultedList<ItemStack> getItems() {
        return inventory;
    }

    public boolean tryUse(PlayerEntity player) {
        if (rolling) {
            return false;
        }

        UUID playerUuid = player.getUuid();
        if (activeUser == null || activeUser.equals(playerUuid)) {
            activeUser = playerUuid;
            markDirty();
            return true;
        }

        return false;
    }

    public boolean isActiveUser(PlayerEntity player) {
        return activeUser != null && activeUser.equals(player.getUuid());
    }

    public void releaseUser(PlayerEntity player) {
        if (isActiveUser(player)) {
            activeUser = null;
            markDirty();
        }
    }

    public void clearActiveUser() {
        activeUser = null;
        rolling = false;
        markDirty();
    }

    public boolean tryStartRoll(PlayerEntity player) {
        if (!isActiveUser(player) || rolling) {
            return false;
        }

        rolling = true;
        markDirty();
        return true;
    }

    public void finishRoll() {
        rolling = false;
        markDirty();
    }

    public boolean isRolling() {
        return rolling;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        inventory.set(slot, stack);
        if (stack.getCount() > getMaxCountPerStack()) {
            stack.setCount(getMaxCountPerStack());
        }
        markDirty();
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack stack = Inventories.removeStack(inventory, slot);
        if (!stack.isEmpty()) {
            markDirty();
        }
        return stack;
    }

    @Override
    public void clear() {
        inventory.clear();
        markDirty();
    }

    @Override
    public void markDirty() {
        super.markDirty();
        if (world != null && !world.isClient()) {
            world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_ALL);
        }
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        if (world == null || world.getBlockEntity(pos) != this) {
            return false;
        }
        return player.squaredDistanceTo(
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5
        ) <= 64.0 && (activeUser == null || isActiveUser(player));
    }

    public PropertyDelegate getPropertyDelegate() {
        return propertyDelegate;
    }

    @Nullable
    public SlotMachineOutcome spin(PlayerEntity player) {
        if (world == null || world.isClient() || !isActiveUser(player) || rolling) {
            return null;
        }

        ItemStack betStack = getStack(INPUT_SLOT);
        if (!SlotMachineBet.isExactBet(betStack)) {
            return null;
        }

        rolling = true;
        markDirty();

        try {
            removeStack(INPUT_SLOT, SlotMachineBet.getBetAmount());

            SlotMachineOutcome outcome = SlotMachineOutcome.roll(world.getRandom());
            lastOutcomeId = outcome.getId();
            int payoutCount = outcome.calculatePayout(SlotMachineBet.getBetAmount());
            giveOrDropPayout(player, SlotMachineBet.createPayoutStack(payoutCount));
            return outcome;
        } finally {
            finishRoll();
        }
    }

    private void giveOrDropPayout(PlayerEntity player, ItemStack payoutStack) {
        if (payoutStack.isEmpty()) {
            return;
        }

        if (!player.isRemoved()) {
            player.getInventory().insertStack(payoutStack);
        }

        if (!payoutStack.isEmpty() && world != null) {
            ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, payoutStack);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        Inventories.writeNbt(nbt, inventory, registryLookup);
        nbt.putInt(LAST_OUTCOME_KEY, lastOutcomeId);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        Inventories.readNbt(nbt, inventory, registryLookup);
        lastOutcomeId = nbt.contains(LAST_OUTCOME_KEY) ? nbt.getInt(LAST_OUTCOME_KEY) : SlotMachineOutcome.NO_OUTCOME_ID;
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayerEntity serverPlayerEntity) {
        return this.pos;
    }

    @Override
    public Text getDisplayName() {
        return Text.literal("Slot Machine");
    }

    @Override
    public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new SlotMachineScreenHandler(syncId, playerInventory, this);
    }

    // These 2 methods are needed in order for the client and server to properly synchronize
    @Override
    public @Nullable Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        return createNbt(registryLookup);
    }
}
