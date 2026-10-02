package fr.hardel.overstress.fakeplayer.scenario;

import fr.hardel.overstress.fakeplayer.BotState;
import fr.hardel.overstress.fakeplayer.client.BotPilot;
import fr.hardel.overstress.fakeplayer.client.SeenEntity;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Set;

final class Melee {
    private static final double REACH = Attributes.ENTITY_INTERACTION_RANGE.value().getDefaultValue();
    private static final int PATIENCE_TICKS = 60;
    private static final double PROGRESS = 0.5;
    private static final Set<EntityType<?>> AIRBORNE = Set.of(EntityTypes.ALLAY, EntityTypes.BAT, EntityTypes.BEE, EntityTypes.BLAZE, EntityTypes.BREEZE, EntityTypes.ENDER_DRAGON,
        EntityTypes.GHAST, EntityTypes.HAPPY_GHAST, EntityTypes.PARROT, EntityTypes.PHANTOM, EntityTypes.VEX, EntityTypes.WITHER);

    private Melee() {
    }

    static boolean pursuable(BotPilot pilot, BotState state, SeenEntity target) {
        return target.id() != state.shunned && !target.type().builtInRegistryHolder().is(EntityTypeTags.AQUATIC) && !AIRBORNE.contains(target.type());
    }

    static void wield(BotPilot pilot) {
        pilot.inventory().hotbar(item -> item.is(ItemTags.SWORDS)).ifPresent(pilot.hands()::select);
    }

    static void engage(BotPilot pilot, BotState state, SeenEntity target, double speed) {
        double distance = Math.sqrt(target.box().distanceToSqr(pilot.eye()));
        if (state.quarry != target.id()) {
            state.quarry = target.id();
            state.closest = distance;
            state.hits = target.hits();
            state.chase = 0;
        }

        if (distance < state.closest - PROGRESS || target.hits() != state.hits) {
            state.closest = Math.min(state.closest, distance);
            state.hits = target.hits();
            state.chase = 0;
        }

        if (++state.chase > PATIENCE_TICKS) {
            state.shunned = target.id();
            return;
        }

        pilot.look(target.box().getCenter());
        if (distance <= REACH) {
            pilot.hands().attack(target);
            return;
        }

        pilot.walkTo(target.position(), speed);
    }
}
