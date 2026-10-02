package fr.hardel.overstress.fakeplayer.scenario;

import fr.hardel.overstress.fakeplayer.BotScenario;
import fr.hardel.overstress.fakeplayer.BotStanding;
import fr.hardel.overstress.fakeplayer.BotState;
import fr.hardel.overstress.fakeplayer.client.BotHands;
import fr.hardel.overstress.fakeplayer.client.BotPilot;
import fr.hardel.overstress.fakeplayer.client.ClientInventory;
import fr.hardel.overstress.fakeplayer.client.SeenEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;

import java.util.OptionalInt;

public final class PvpScenario implements BotScenario {
    private static final BotStanding MORTAL = new BotStanding(GameType.SURVIVAL, false, false);
    private static final String DEFEAT = "gg";
    private static final int HUNGRY = 14;
    private static final double SPEED = 0.3;
    private static final int TURN_TICKS = 40;
    private static final double SIGHT_WIDTH = 64;
    private static final double SIGHT_HEIGHT = 32;

    @Override
    public void steer(BotPilot pilot, BotState state, RandomSource random) {
        BotHands hands = pilot.hands();
        if (pilot.status().deaths() > state.deaths) {
            state.deaths = pilot.status().deaths();
            hands.say(DEFEAT);
        }

        if (hands.using()) {
            hands.use();
            return;
        }

        OptionalInt armor = armorToWear(pilot.inventory());
        if (armor.isPresent()) {
            hands.equip(armor.getAsInt());
            return;
        }

        OptionalInt food = pilot.inventory().hotbar(item -> item.has(DataComponents.FOOD));
        if (food.isPresent() && pilot.status().food() <= HUNGRY) {
            hands.select(food.getAsInt());
            hands.use();
            return;
        }

        Melee.wield(pilot);
        SeenEntity foe = pilot.entities().nearest(pilot.position(), AABB.ofSize(pilot.position(), SIGHT_WIDTH, SIGHT_HEIGHT, SIGHT_WIDTH),
            entity -> pilot.entities().hurtable(entity) && (entity.type() == EntityTypes.PLAYER || entity.type().getCategory() == MobCategory.MONSTER));
        if (foe != null) {
            Melee.engage(pilot, foe, SPEED);
            return;
        }

        pilot.walk(state.heading, SPEED);
        if (--state.cooldown > 0) {
            return;
        }

        state.cooldown = TURN_TICKS;
        state.heading = random.nextDouble() * Math.PI * 2;
    }

    @Override
    public BotStanding standing() {
        return MORTAL;
    }

    private static OptionalInt armorToWear(ClientInventory inventory) {
        for (int slot = InventoryMenu.INV_SLOT_START; slot < InventoryMenu.USE_ROW_SLOT_END; slot++) {
            Equippable equippable = inventory.item(slot).get(DataComponents.EQUIPPABLE);
            if (equippable != null && equippable.slot().getType() == EquipmentSlot.Type.HUMANOID_ARMOR && inventory.item(ClientInventory.armorSlot(equippable.slot())).isEmpty()) {
                return OptionalInt.of(slot);
            }
        }

        return OptionalInt.empty();
    }
}
