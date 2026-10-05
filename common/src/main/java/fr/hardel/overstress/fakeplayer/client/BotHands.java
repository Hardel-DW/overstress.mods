package fr.hardel.overstress.fakeplayer.client;

import com.google.common.hash.HashCode;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.HashedPatchMap;
import net.minecraft.network.HashedStack;
import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.Crypt;
import net.minecraft.util.HashOps;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.time.Instant;
import java.util.BitSet;
import java.util.Optional;
import java.util.function.Consumer;

public final class BotHands {
    private static final int DESTROY_DELAY_TICKS = 5;
    private static final int RIGHT_CLICK_DELAY_TICKS = 4;
    private static final float AIRBORNE_MINING_DIVISOR = 5;
    private static final float CORRECT_TOOL_DIVISOR = 30;
    private static final float WRONG_TOOL_DIVISOR = 100;
    private static final float TICKS_PER_SECOND = 20;
    private static final float CHARGE_PARTIAL_TICK = 0.5F;
    private static final LastSeenMessages.Update NOTHING_SEEN = new LastSeenMessages.Update(0, new BitSet(), LastSeenMessages.EMPTY.computeChecksum());
    private final Consumer<Packet<?>> send;
    private final BotBody body;
    private final ClientTerrain terrain;
    private final ClientInventory inventory;
    private final HashedPatchMap.HashGenerator hasher;
    private int sequence;
    private int carriedIndex;
    private int attackStrengthTicker;
    private ItemStack lastMainHand = ItemStack.EMPTY;
    private boolean destroying;
    private BlockPos destroyPos = BlockPos.ZERO;
    private Direction destroyDirection = Direction.DOWN;
    private ItemStack destroyingItem = ItemStack.EMPTY;
    private float destroyProgress;
    private int destroyDelay;
    private boolean attackHeld;
    private boolean attackWasHeld;
    private boolean useHeld;
    private int rightClickDelay;
    private int useTicks;

    BotHands(Consumer<Packet<?>> send, BotBody body, ClientTerrain terrain, ClientInventory inventory, RegistryAccess registries) {
        this.send = send;
        this.body = body;
        this.terrain = terrain;
        this.inventory = inventory;
        RegistryOps<HashCode> hashOps = registries.createSerializationContext(HashOps.CRC32C_INSTANCE);
        this.hasher = component -> component.encodeValue(hashOps).getOrThrow(IllegalArgumentException::new).asInt();
    }

    public boolean destroying() {
        return destroying;
    }

    public boolean using() {
        return useTicks > 0;
    }

    public boolean charged() {
        double attackSpeed = inventory.selectedItem().getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)
            .compute(Attributes.ATTACK_SPEED, Attributes.ATTACK_SPEED.value().getDefaultValue(), EquipmentSlot.MAINHAND);
        return (attackStrengthTicker + CHARGE_PARTIAL_TICK) * attackSpeed >= TICKS_PER_SECOND;
    }

    public float progress(BlockState state) {
        float hardness = state.getDestroySpeed(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        ItemStack tool = inventory.selectedItem();
        float speed = tool.getDestroySpeed(state) / (body.onGround() ? 1 : AIRBORNE_MINING_DIVISOR);
        boolean correctTool = !state.requiresCorrectToolForDrops() || tool.isCorrectToolForDrops(state);
        return hardness < 0 ? 0 : speed / hardness / (correctTool ? CORRECT_TOOL_DIVISOR : WRONG_TOOL_DIVISOR);
    }

    public void select(int hotbarIndex) {
        inventory.select(hotbarIndex);
    }

    /** True once the block is broken for the client, which predicts the break as a real one does. */
    public boolean mine(BlockPos pos, Direction face, BlockState state) {
        attackHeld = true;
        if (using()) {
            return false;
        }

        if (!attackWasHeld) {
            boolean instant = destroy(pos, face, state);
            send.accept(ServerboundPunchPacket.INSTANCE);
            if (instant) {
                return true;
            }
        }

        boolean broken = continueDestroying(pos, face, state);
        send.accept(ServerboundPunchPacket.INSTANCE);
        return broken;
    }

    public boolean attack(SeenEntity target) {
        if (using() || !charged()) {
            return false;
        }

        sendCarriedItem();
        send.accept(new ServerboundAttackPacket(target.id()));
        attackStrengthTicker = 0;
        send.accept(ServerboundPunchPacket.INSTANCE);
        return true;
    }

    public void use() {
        useHeld = true;
        if (using() || rightClickDelay > 0 || destroying) {
            return;
        }

        rightClickDelay = RIGHT_CLICK_DELAY_TICKS;
        sendCarriedItem();
        send.accept(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, ++sequence, body.yRot(), body.xRot()));
        Consumable consumable = inventory.selectedItem().get(DataComponents.CONSUMABLE);
        useTicks = consumable == null ? 0 : consumable.consumeTicks();
    }

    public void useOn(BlockPos pos, Direction face) {
        if (using() || destroying) {
            return;
        }

        rightClickDelay = RIGHT_CLICK_DELAY_TICKS;
        sendCarriedItem();
        Vec3 hit = Vec3.atCenterOf(pos).add(face.getUnitVec3().scale(0.5));
        send.accept(new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, pos, false), ++sequence));
    }

    public void equip(int menuSlot) {
        ItemStack item = inventory.item(menuSlot);
        int armorSlot = ClientInventory.armorSlot(item.get(DataComponents.EQUIPPABLE).slot());
        Int2ObjectMap<HashedStack> changed = new Int2ObjectOpenHashMap<>();
        changed.put(menuSlot, HashedStack.EMPTY);
        changed.put(armorSlot, HashedStack.create(item, hasher));
        send.accept(new ServerboundContainerClickPacket(InventoryMenu.CONTAINER_ID, inventory.stateId(), (short) menuSlot, (byte) 0, ContainerInput.QUICK_MOVE, changed,
            HashedStack.create(inventory.carried(), hasher)));
        inventory.set(menuSlot, ItemStack.EMPTY);
        inventory.set(armorSlot, item);
    }

    public void say(String message) {
        send.accept(new ServerboundChatPacket(message, Instant.now(), Crypt.SaltSupplier.getLong(), Optional.empty(), NOTHING_SEEN));
    }

    public void command(String command) {
        send.accept(new ServerboundChatCommandPacket(command));
    }

    void begin() {
        sendCarriedItem();
        rightClickDelay = Math.max(0, rightClickDelay - 1);
        useTicks = Math.max(0, useTicks - 1);
        attackStrengthTicker = ItemStack.isSameItem(lastMainHand, inventory.selectedItem()) ? attackStrengthTicker + 1 : 0;
        lastMainHand = inventory.selectedItem();
        attackWasHeld = attackHeld;
        attackHeld = false;
        useHeld = false;
    }

    void end() {
        if (!attackHeld) {
            stopDestroying();
        }

        if (using() && !useHeld) {
            sendCarriedItem();
            send.accept(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM, BlockPos.ZERO, Direction.DOWN));
            useTicks = 0;
        }
    }

    void stopDestroying() {
        if (destroying) {
            send.accept(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, destroyPos, Direction.DOWN));
            destroying = false;
            destroyProgress = 0;
            attackStrengthTicker = 0;
        }
    }

    void forget() {
        destroying = false;
        destroyProgress = 0;
        useTicks = 0;
    }

    private boolean destroy(BlockPos pos, Direction face, BlockState state) {
        if (destroying && sameTarget(pos)) {
            return false;
        }

        if (destroying) {
            send.accept(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, destroyPos, face));
        }

        int started = ++sequence;
        boolean instant = progress(state) >= 1;
        destroying = !instant;
        destroyPos = pos.immutable();
        destroyDirection = face;
        destroyingItem = inventory.selectedItem();
        destroyProgress = 0;
        if (instant) {
            terrain.update(pos, Blocks.AIR.defaultBlockState());
        }

        send.accept(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, face, started));
        return instant;
    }

    private boolean continueDestroying(BlockPos pos, Direction face, BlockState state) {
        sendCarriedItem();
        if (destroyDelay > 0) {
            destroyDelay--;
            return false;
        }

        if (!sameTarget(pos)) {
            return destroy(pos, face, state);
        }

        destroyProgress += progress(state);
        if (destroyProgress >= 1) {
            destroying = false;
            destroyProgress = 0;
            destroyDelay = DESTROY_DELAY_TICKS;
            terrain.update(pos, Blocks.AIR.defaultBlockState());
            send.accept(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, face, ++sequence));
            return true;
        }

        if (destroyDirection != face) {
            destroyDirection = face;
            send.accept(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.CHANGE_DESTROY_DIRECTION, pos, face));
        }

        return false;
    }

    private boolean sameTarget(BlockPos pos) {
        return pos.equals(destroyPos) && ItemStack.isSameItemSameComponents(inventory.selectedItem(), destroyingItem);
    }

    private void sendCarriedItem() {
        if (inventory.selected() != carriedIndex) {
            carriedIndex = inventory.selected();
            send.accept(new ServerboundSetCarriedItemPacket(carriedIndex));
        }
    }
}
