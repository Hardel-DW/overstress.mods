package fr.hardel.overstress.fakeplayer.scenario;

import fr.hardel.overstress.fakeplayer.client.BotPilot;
import fr.hardel.overstress.fakeplayer.client.SeenEntity;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.ai.attributes.Attributes;

final class Melee {
    private static final double REACH = Attributes.ENTITY_INTERACTION_RANGE.value().getDefaultValue();

    private Melee() {
    }

    static void wield(BotPilot pilot) {
        pilot.inventory().hotbar(item -> item.is(ItemTags.SWORDS)).ifPresent(pilot.hands()::select);
    }

    static void engage(BotPilot pilot, SeenEntity target, double speed) {
        pilot.look(target.box().getCenter());
        if (target.box().distanceToSqr(pilot.eye()) <= REACH * REACH) {
            pilot.hands().attack(target);
            return;
        }

        pilot.walkTo(target.position(), speed);
    }
}
