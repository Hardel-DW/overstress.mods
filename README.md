# Overstress

Server load harness for Minecraft 26.2 / Fabric. It spawns headless players and makes them do the
expensive things real players do, so a server can be measured under a reproducible load instead of a
guess. Built to stress [Leafs](../leafs-template-26.2), but it depends on nothing except Fabric API.

The bots are real `ServerPlayer` instances placed through the real login path, on a connection with
no channel: packets are dropped, so network and serialisation cost is absent from any measurement
taken with them. They are ticked at the head of their level's tick, on whatever thread runs it, which
is what makes the load land on the region owning them when the server is regionised.

Each bot also runs vanilla's own player tick, held in place afterwards the way vanilla holds a
client-authoritative player. That is not decoration: measured on 200 idle bots, skipping it made a bot
cost 0.035 ms instead of 0.122 ms, so the harness read 3.5 times too cheap.

## Commands

`/fakeplayer` needs permission level 2 (gamemaster).

| Command | Effect |
| --- | --- |
| `/fakeplayer spawn <count> <spread> [scenario]` | Spawns `count` bots (1-500), each at its own draw within `spread` blocks of you on both axes (16-100000). Without `scenario`, each bot picks one at random. |
| `/fakeplayer scenario set <bot> <scenario>` | Reassigns one bot. Errors on a real player. |
| `/fakeplayer scenario random <percent> <scenario>` | Gives `scenario` to `percent` of the fleet (1-100) and idles everyone else. |
| `/fakeplayer scenario list` | Every bot and what it runs, grouped by scenario. |
| `/fakeplayer clear` | Disconnects every bot. |
| `/fakeplayer list` | How many are alive, without the roster. |

`scenario random` shuffles and takes a slice rather than rolling a die per bot, so 30% of 200 bots is
exactly 60 bots and not roughly 60. The remainder is set to `overstress:idle`, which makes the command
the whole picture of the fleet rather than a partial edit: run it twice with different scenarios and
the second run undoes the first.

Bots always land in the overworld on the surface of their column, the same placement rule
`/spreadplayers` uses, scattered around the horizontal position of whoever ran the command. From the
console that is the world spawn.

## Scenarios

| Scenario | Load it produces |
| --- | --- |
| `overstress:idle` | Nothing at all. The baseline: what a connected player costs before it moves. |
| `overstress:wander` | Walks one fixed diagonal and never turns. Chunk loading, cheap and steady. |
| `overstress:fly` | Flies a straight line 100 blocks over the surface at 36 blocks/s. Chunk generation, fast. |
| `overstress:mine` | Walks and breaks a block every 8 ticks. Block updates, drops, lighting. |
| `overstress:fight` | Hunts and hits the nearest mob within 16 blocks. Entity queries and combat. |
| `overstress:spawner` | Spawns zombies around itself up to 300 nearby. Entity tick and AI. |
| `overstress:dimensions` | Wanders, then crosses to the next dimension every 15 seconds. Cross-level transfer. |
| `overstress:random` | Runs another scenario for a minute, then draws a new one. Mixed, shifting load. |

`fly` is a plain fly and not an elytra: an elytra trades height for speed, so a bot on one sinks into
the first mountain it meets. Its altitude is recomputed every tick from the column it is entering, so
terrain is cleared before it is reached rather than after.

`random` draws per bot, never draws itself, and cuts each bot's first period short by a random amount,
so a fleet spawned in one tick drifts apart instead of switching in lockstep.

Bots are made invulnerable and given a 20 block step height on their first tick, so they walk over
terrain instead of dying to it.

## What it does not simulate

The mod depends on nothing but Fabric API, so it runs on any 26.2 server, modded or not. Bots join
through the real login path, which means mods see a genuine player join and their attachments,
scoreboards, teams and permission checks all behave. Four things are still absent, and a measurement
that ignores them will read too optimistic:

- **Network cost is zero.** The connection drops every packet, so serialisation, compression and
  encryption for N players never happen. On a real server that is a large share of the tick.
- **Movement is teleportation.** Bots are placed with `snapTo`, so the whole inbound movement path is
  skipped: no packet handling, no collision resolution, no fall damage. Anti-cheat and movement mods
  see nothing to work with.
- **There is no client.** Nothing arrives inbound, so no packet handler ever runs. A mod that sends a
  custom payload and waits for an answer waits forever, and the `ChannelFutureListener` overload of
  `send` drops its listener rather than firing it. A mod that blocks on that callback will hang.
- **Profiles are offline-mode.** UUIDs are derived from the bot name, so an online-mode server or an
  auth mod may refuse them.

So it is a good instrument for world, entity and chunk load, and the wrong instrument for anything
about networking or player input.

## Adding a scenario

Scenarios live in a Fabric registry, so another mod can add one without a fork. Implement
`BotScenario` and register it from your initializer, before registries freeze:

```java
Registry.register(BotScenarios.REGISTRY, Identifier.fromNamespaceAndPath("mymod", "afk_farm"), new AfkFarmScenario());
```

The command picks it up on its own: it suggests and resolves whatever is in the registry. The
registry key is `overstress:bot_scenario`. Entries are not synced to clients, because a scenario is
behavior that only ever runs server-side.

## Build

`./gradlew build`, then drop `build/libs/overstress-<version>.jar` in the server's `mods` folder.

## License

CC0-1.0.
