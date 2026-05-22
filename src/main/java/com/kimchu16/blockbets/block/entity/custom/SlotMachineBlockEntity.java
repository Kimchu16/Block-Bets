package com.kimchu16.blockbets.block.entity.custom;

import com.kimchu16.blockbets.block.entity.ImplementedInventory;
import com.kimchu16.blockbets.block.entity.ModBlockEntities;
import com.kimchu16.blockbets.screen.custom.SlotMachineScreenHandler;
import com.kimchu16.blockbets.slotmachine.SlotMachineBet;
import com.kimchu16.blockbets.slotmachine.SlotMachineConfig;
import com.kimchu16.blockbets.slotmachine.SlotMachineOutcome;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.component.type.FireworksComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class SlotMachineBlockEntity extends BlockEntity implements ImplementedInventory, ExtendedScreenHandlerFactory<BlockPos> {
    public static final int INPUT_SLOT = 0;
    public static final int INVENTORY_SIZE = 1;
    private static final int[] NO_AUTOMATION_SLOTS = new int[0];
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
        clearStaleActiveUser();
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

    private void clearStaleActiveUser() {
        if (activeUser != null
                && world instanceof ServerWorld serverWorld
                && serverWorld.getServer().getPlayerManager().getPlayer(activeUser) == null) {
            activeUser = null;
            rolling = false;
            markDirty();
        }
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
    public int[] getAvailableSlots(Direction side) {
        return NO_AUTOMATION_SLOTS;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction side) {
        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction side) {
        return false;
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
        if (!SlotMachineBet.isValidBet(betStack)) {
            return null;
        }

        if (!tryStartRoll(player)) {
            return null;
        }

        try {
            Item betItem = betStack.getItem();
            int betAmount = betStack.getCount();
            removeStack(INPUT_SLOT);

            SlotMachineOutcome outcome = SlotMachineOutcome.roll(world.getRandom());
            lastOutcomeId = outcome.getId();
            if (!player.isRemoved()) {
                int payoutCount = outcome.calculatePayout(betAmount);
                giveOrDropPayout(player, SlotMachineBet.createPayoutStack(betItem, payoutCount));
                sendOutcomeMessage(player, outcome, payoutCount, betItem);
                playOutcomeSound(outcome);
                if (outcome == SlotMachineOutcome.JACKPOT && SlotMachineConfig.get().isJackpotFireworksEnabled()) {
                    launchJackpotFirework();
                }
            }
            return outcome;
        } finally {
            finishRoll();
        }
    }

    private void giveOrDropPayout(PlayerEntity player, ItemStack payoutStack) {
        if (payoutStack.isEmpty()) {
            return;
        }

        player.getInventory().insertStack(payoutStack);

        if (!payoutStack.isEmpty() && world != null) {
            ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, payoutStack);
        }
    }

    private void sendOutcomeMessage(PlayerEntity player, SlotMachineOutcome outcome, int payoutCount, Item betItem) {
        player.sendMessage(Text.translatable(
                "message.blockbets.slot_machine.outcome",
                outcome.getDisplayText(),
                payoutCount,
                betItem.getName()
        ), false);
    }

    private void playOutcomeSound(SlotMachineOutcome outcome) {
        if (world == null) {
            return;
        }

        SoundEvent soundEvent = switch (outcome) {
            case JACKPOT -> SoundEvents.ENTITY_PLAYER_LEVELUP;
            case WIN -> SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP;
            case PUSH -> SoundEvents.BLOCK_NOTE_BLOCK_PLING.value();
            case LOSS -> SoundEvents.BLOCK_NOTE_BLOCK_BASS.value();
            case BUST -> SoundEvents.ENTITY_VILLAGER_NO;
        };
        float pitch = switch (outcome) {
            case JACKPOT -> 1.2f;
            case WIN -> 1.0f;
            case PUSH -> 0.9f;
            case LOSS -> 0.75f;
            case BUST -> 0.6f;
        };

        world.playSound(null, pos, soundEvent, SoundCategory.BLOCKS, 1.0f, pitch);
    }

    private void launchJackpotFirework() {
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }

        ItemStack fireworkStack = new ItemStack(Items.FIREWORK_ROCKET);
        fireworkStack.set(DataComponentTypes.FIREWORKS, new FireworksComponent(1, List.of(
                new FireworkExplosionComponent(
                        FireworkExplosionComponent.Type.LARGE_BALL,
                        IntList.of(0xFFD700, 0xFFFFFF),
                        IntList.of(0xFF5555),
                        true,
                        true
                )
        )));

        FireworkRocketEntity firework = new FireworkRocketEntity(
                serverWorld,
                pos.getX() + 0.5,
                pos.getY() + 1.0,
                pos.getZ() + 0.5,
                fireworkStack
        );
        serverWorld.spawnEntity(firework);
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
        return Text.translatable("block.blockbets.slot_machine_block");
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
