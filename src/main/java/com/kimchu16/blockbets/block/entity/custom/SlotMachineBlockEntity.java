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
import net.minecraft.registry.Registries;
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
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class SlotMachineBlockEntity extends BlockEntity implements ImplementedInventory, ExtendedScreenHandlerFactory<BlockPos> {
    public static final int INPUT_SLOT = 0;
    public static final int INVENTORY_SIZE = 1;
    private static final int[] NO_AUTOMATION_SLOTS = new int[0];
    private static final String LAST_OUTCOME_KEY = "LastOutcome";
    private static final String ROLLING_KEY = "Rolling";
    private static final String ROLL_TICKS_REMAINING_KEY = "RollTicksRemaining";
    private static final String ROLLING_PLAYER_KEY = "RollingPlayer";
    private static final String PENDING_BET_ITEM_KEY = "PendingBetItem";
    private static final String PENDING_BET_AMOUNT_KEY = "PendingBetAmount";
    private static final int MIN_ROLL_DELAY_TICKS = 60;
    private static final int MAX_ROLL_DELAY_TICKS = 100;

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> lastOutcomeId;
                case 1 -> rolling ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                lastOutcomeId = value;
            } else if (index == 1) {
                rolling = value != 0;
            }
        }

        @Override
        public int size() {
            return 2;
        }
    };
    @Nullable
    private UUID activeUser;
    private boolean rolling;
    private int rollTicksRemaining;
    private int lastOutcomeId = SlotMachineOutcome.NO_OUTCOME_ID;
    @Nullable
    private UUID rollingPlayerUuid;
    @Nullable
    private Item pendingBetItem;
    private int pendingBetAmount;

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
        if (!rolling
                && activeUser != null
                && world instanceof ServerWorld serverWorld
                && serverWorld.getServer().getPlayerManager().getPlayer(activeUser) == null) {
            activeUser = null;
            markDirty();
        }
    }

    public boolean isActiveUser(PlayerEntity player) {
        return activeUser != null && activeUser.equals(player.getUuid());
    }

    public void releaseUser(PlayerEntity player) {
        if (!rolling && isActiveUser(player)) {
            activeUser = null;
            markDirty();
        }
    }

    public void clearActiveUser() {
        activeUser = null;
        clearPendingRoll();
        markDirty();
    }

    public boolean isRolling() {
        return rolling;
    }

    public static void tick(World world, BlockPos pos, BlockState state, SlotMachineBlockEntity blockEntity) {
        if (world.isClient() || !blockEntity.rolling) {
            return;
        }

        blockEntity.rollTicksRemaining--;
        if (blockEntity.rollTicksRemaining <= 0) {
            blockEntity.resolveRoll();
        }
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

    public boolean spin(PlayerEntity player) {
        if (world == null || world.isClient() || !isActiveUser(player) || rolling) {
            return false;
        }

        ItemStack betStack = getStack(INPUT_SLOT);
        if (!SlotMachineBet.isValidBet(betStack)) {
            return false;
        }

        pendingBetItem = betStack.getItem();
        pendingBetAmount = betStack.getCount();
        rollingPlayerUuid = player.getUuid();
        rollTicksRemaining = getRollDelayTicks();
        rolling = true;
        lastOutcomeId = SlotMachineOutcome.NO_OUTCOME_ID;
        removeStack(INPUT_SLOT);
        markDirty();
        return true;
    }

    private int getRollDelayTicks() {
        if (world == null) {
            return MIN_ROLL_DELAY_TICKS;
        }
        return MIN_ROLL_DELAY_TICKS + world.getRandom().nextInt(MAX_ROLL_DELAY_TICKS - MIN_ROLL_DELAY_TICKS + 1);
    }

    private void resolveRoll() {
        if (world == null || world.isClient()) {
            return;
        }

        if (pendingBetItem == null || pendingBetAmount <= 0) {
            clearPendingRoll();
            markDirty();
            return;
        }

        Item betItem = pendingBetItem;
        int betAmount = pendingBetAmount;
        SlotMachineOutcome outcome = SlotMachineOutcome.roll(world.getRandom());
        lastOutcomeId = outcome.getId();

        ServerPlayerEntity player = getRollingPlayer();
        if (player != null && !player.isRemoved()) {
            int payoutCount = outcome.calculatePayout(betAmount);
            giveOrDropPayout(player, SlotMachineBet.createPayoutStack(betItem, payoutCount));
            sendOutcomeMessage(player, outcome, payoutCount, betItem);
            playOutcomeSound(outcome);
            if (outcome == SlotMachineOutcome.JACKPOT && SlotMachineConfig.get().isJackpotFireworksEnabled()) {
                launchJackpotFirework();
            }
        }

        clearPendingRoll();
        markDirty();
    }

    @Nullable
    private ServerPlayerEntity getRollingPlayer() {
        if (rollingPlayerUuid == null || !(world instanceof ServerWorld serverWorld)) {
            return null;
        }
        return serverWorld.getServer().getPlayerManager().getPlayer(rollingPlayerUuid);
    }

    private void clearPendingRoll() {
        rolling = false;
        rollTicksRemaining = 0;
        rollingPlayerUuid = null;
        pendingBetItem = null;
        pendingBetAmount = 0;
        activeUser = null;
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
        nbt.putBoolean(ROLLING_KEY, rolling);
        nbt.putInt(ROLL_TICKS_REMAINING_KEY, rollTicksRemaining);
        nbt.putInt(PENDING_BET_AMOUNT_KEY, pendingBetAmount);
        if (rollingPlayerUuid != null) {
            nbt.putUuid(ROLLING_PLAYER_KEY, rollingPlayerUuid);
        }
        if (pendingBetItem != null) {
            nbt.putString(PENDING_BET_ITEM_KEY, Registries.ITEM.getId(pendingBetItem).toString());
        }
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        Inventories.readNbt(nbt, inventory, registryLookup);
        lastOutcomeId = nbt.contains(LAST_OUTCOME_KEY) ? nbt.getInt(LAST_OUTCOME_KEY) : SlotMachineOutcome.NO_OUTCOME_ID;
        rolling = nbt.getBoolean(ROLLING_KEY);
        rollTicksRemaining = nbt.contains(ROLL_TICKS_REMAINING_KEY)
                ? Math.max(1, nbt.getInt(ROLL_TICKS_REMAINING_KEY))
                : 0;
        pendingBetAmount = nbt.getInt(PENDING_BET_AMOUNT_KEY);
        rollingPlayerUuid = nbt.containsUuid(ROLLING_PLAYER_KEY) ? nbt.getUuid(ROLLING_PLAYER_KEY) : null;
        pendingBetItem = readPendingBetItem(nbt);

        if (rolling && (pendingBetItem == null || pendingBetAmount <= 0 || rollingPlayerUuid == null)) {
            clearPendingRoll();
        }
    }

    @Nullable
    private Item readPendingBetItem(NbtCompound nbt) {
        if (!nbt.contains(PENDING_BET_ITEM_KEY)) {
            return null;
        }

        Identifier itemId = Identifier.tryParse(nbt.getString(PENDING_BET_ITEM_KEY));
        if (itemId == null || !Registries.ITEM.containsId(itemId)) {
            return null;
        }
        return Registries.ITEM.get(itemId);
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
