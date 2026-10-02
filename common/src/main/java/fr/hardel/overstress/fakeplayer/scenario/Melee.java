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
    private static final int PATIENCE_TICKS = 100;
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
        if (state.quarry != target.id()) {
            state.quarry = target.id();
            state.chase = 0;
        }

        pilot.look(target.box().getCenter());
        if (target.box().distanceToSqr(pilot.eye()) <= REACH * REACH) {
            state.chase = 0;
            pilot.hands().attack(target);
            return;
        }

        if (++state.chase > PATIENCE_TICKS) {
            state.shunned = target.id();
            return;
        }

        pilot.walkTo(target.position(), speed);
    }
}
