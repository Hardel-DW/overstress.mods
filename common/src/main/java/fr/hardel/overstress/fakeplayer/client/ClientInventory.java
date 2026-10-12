package fr.hardel.overstress.fakeplayer.client;

import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetCursorItemPacket;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.OptionalInt;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

public final class ClientInventory {
    private static final int SLOTS = InventoryMenu.SHIELD_SLOT + 1;
    private final ItemStack[] slots = new ItemStack[SLOTS];
    private ItemStack carried = ItemStack.EMPTY;
    private int stateId;
    private int selected;

    ClientInventory() {
        Arrays.fill(slots, ItemStack.EMPTY);
    }

    public ItemStack item(int menuSlot) {
        return slots[menuSlot];
    }

    public int selected() {
        return selected;
    }

    public ItemStack selectedItem() {
        return slots[InventoryMenu.USE_ROW_SLOT_START + selected];
    }

    public static int armorSlot(EquipmentSlot slot) {
        return InventoryMenu.ARMOR_SLOT_END - 1 - slot.getIndex();
    }

    public int best(ToDoubleFunction<ItemStack> score) {
        int best = selected;
        for (int index = 0; index < Inventory.SELECTION_SIZE; index++) {
            if (score.applyAsDouble(slots[InventoryMenu.USE_ROW_SLOT_START + index]) > score.applyAsDouble(slots[InventoryMenu.USE_ROW_SLOT_START + best])) {
                best = index;
            }
        }

        return best;
    }

    public OptionalInt hotbar(Predicate<ItemStack> wanted) {
        for (int index = 0; index < Inventory.SELECTION_SIZE; index++) {
            if (wanted.test(slots[InventoryMenu.USE_ROW_SLOT_START + index])) {
                return OptionalInt.of(index);
            }
        }

        return OptionalInt.empty();
    }

    int stateId() {
        return stateId;
    }

    ItemStack carried() {
        return carried;
    }

    void select(int index) {
        selected = index;
    }

    void set(int menuSlot, ItemStack item) {
        slots[menuSlot] = item;
    }

    void content(ClientboundContainerSetContentPacket packet) {
        if (packet.containerId() == InventoryMenu.CONTAINER_ID) {
            packet.items().toArray(slots);
            carried = packet.carriedItem();
            stateId = packet.stateId();
        }
    }

    void slot(ClientboundContainerSetSlotPacket packet) {
        if (packet.getContainerId() == InventoryMenu.CONTAINER_ID) {
            slots[packet.getSlot()] = packet.getItem();
            stateId = packet.getStateId();
        }
    }

    void cursor(ClientboundSetCursorItemPacket packet) {
        carried = packet.contents();
    }

    void held(ClientboundSetHeldSlotPacket packet) {
        if (Inventory.isHotbarSlot(packet.slot())) {
            selected = packet.slot();
        }
    }
}
