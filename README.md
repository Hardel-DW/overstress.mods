# Overstress
Overstress is a server mod for NeoForge and Fabric from 26.1 onwards that simulates players using scenarios, to run benchmarks, test performance with many players, or anything else.

Each bot is a headless client, built so that its cost is close to a real player. It logs in through the server's own login and configuration, the server ticks it through its connection like any player, and everything it does goes through the packets a vanilla client sends, which the server checks.

A few things to note:
- **Two links.** `direct` hands the packets over as objects: the server pays no encoding, no compression and no socket. `network` connects over TCP to the server's own port with the vanilla pipeline, so the server pays all of it. The bot drops the bytes it receives, a real client decodes them on its own machine.
- **Movement is a simple client physics.** The bot follows the surface of the chunks it received, falls with the vanilla gravity, glides with an elytra, and climbs up to 4 blocks per tick where a player would jump.
- **Every action is a packet.** A bot mines by holding its attack button on a block: start, the vanilla progress of its tool tick after tick, stop, a punch each tick. It hits an entity with the attack packet once its attack is charged, uses an item or clicks a block with the use packets, picks its hotbar slot, moves items with inventory clicks, chats and runs commands. Pickups and deaths are the server's own: a bot walks over its drops, and a dead bot asks to respawn at once.
- **The client knows what its packets told it.** The surface of each chunk it holds, the height and the top block of every column, read from the chunk packet and kept up to date by the block updates; the type and position of every entity the server shows it; its inventory, health and food. It never reads the server's world, so it mines only a top block it knows, one layer deep.
- **Bots log in with a kit.** Every bot joins as if it had logged out with an elytra on, diamond tools, a sword, armor in its inventory, food and zombie eggs. It is invulnerable and in survival, except where its scenario says otherwise: `spawner` plays in creative, `dimensions` and `random` have the command rights of a gamemaster, `pvp` is mortal.
- **Profiles are offline-mode.** UUIDs are derived from the bot name. Bots skip authentication and take no player slot.

## Commands
`/fakeplayer` needs permission level 2, so gamemaster. The `scenario` command spawns bots with a specific action, `simulation` is basically an alias of scenarios with just preconfigured parameters.

- `/fakeplayer spawn <count> <spread> <cluster> [scenario] [everyTicks]` spawns `N` players within a bounded radius with the given scenario. The `<cluster>` parameter goes from 0% to 100%, when a player spawns it has a probability of spawning in an existing zone, which creates groups of players. `<everyTicks>`, optional, spreads the spawns out periodically.
- `/fakeplayer scenario set <bot> <scenario>` changes the scenario of one bot.
- `/fakeplayer scenario random <percent> <scenario>` changes the scenario of several bots at random.
- `/fakeplayer scenario list` shows every scenario.
- `/fakeplayer clear` removes every bot.
- `/fakeplayer clear within <radius>` removes every bot around the command within the given radius.
- `/fakeplayer clear <count> <first|last|random>` removes a given number of bots, the first ones, the last ones, or at random.
- `/fakeplayer transport [direct|network]` shows or picks the link of the next bots. Bots use `network` whenever the server listens on a port, `direct` otherwise.
- `/fakeplayer pos` shows the position of every bot.
- `/fakeplayer list` shows how many bots exist.
- `/fakeplayer simulation start <simulation>` starts the simulation.
- `/fakeplayer simulation stop` stops the simulation.
- `/fakeplayer simulation status` shows the state of the simulation.

## Scenarios
| Scenario | Load it produces |
| --- | --- |
| `overstress:idle` | Nothing. The baseline cost of a connected player. |
| `overstress:wander` | Walks a diagonal at 4 blocks/s. |
| `overstress:fly` | Glides with an elytra in a straight line 100 blocks over the surface at 36 blocks/s. |
| `overstress:mine` | Walks at 3 blocks/s and stops to break the top blocks of a 3-wide band ahead of it, each with the best tool of its hotbar, then walks over the drops. Block updates, drops, pickups, lighting. |
| `overstress:fight` | Holds its sword, walks to the nearest mob it sees within 16x8x16 and hits it within 3 blocks each time its attack is charged. |
| `overstress:spawner` | In creative, circles its spawn point and clicks a zombie egg on the ground within reach 10 times a second, while it sees fewer than 300 mobs around it. |
| `overstress:dimensions` | Wanders, and every 15 seconds runs `/execute in <next dimension> run tp` to its spawn point at y 128. |
| `overstress:pvp` | Mortal. Wears its armor through inventory clicks, holds its sword, chases the nearest player or monster it sees and hits it when charged, eats when hungry. When it dies it respawns at once and says gg. |
| `overstress:churn` | Walks back and forth between its spawn point and 450 blocks away at 30 blocks/s. Loads and unloads the same chunks in a loop. |
| `overstress:random` | Runs another scenario for a minute, then draws a new one. |

## Simulations
A simulation is a preconfigured spawn with a duration. Its fields are bots, radius, cluster in %, cluster radius, scenario, duration, spawn interval, and mob spawning. The cluster radius is 32 (touching) or 256 (neighbouring).

| Simulation | Bots | Radius | Cluster | Scenario | Duration | Spread spawn | Mob spawning |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `overstress:smoke` | 8 | 300 | 5% / 32 | idle | 1 min | no | off |
| `overstress:idle` | 50 | 2000 | 5% / 32 | idle | 3 min | no | off |
| `overstress:roam` | 40 | 1500 | 5% / 32 | fly | 3 min | no | off |
| `overstress:border` | 20 | 2100 | 0% / 32 | wander | 3 min | no | off |
| `overstress:mobs` | 20 | 800 | 5% / 32 | spawner | 3 min | no | on |
| `overstress:churn` | 60 | 5000 | 5% / 256 | churn | 10 min | no | off |
| `overstress:ramp` | 300 | 20000 | 0% / 256 | mine | 15 min | 1 bot / 60 ticks | off |
| `overstress:sprawl` | 100 | 50000 | 5% / 256 | fly | 10 min | 1 bot / 60 ticks | off |
| `overstress:flight` | 1 | 0 | 0% / 32 | fly | 5 min | no | off |
| `overstress:worldgen` | 5 | 2000 | 0% / 32 | fly | 3 min | no | off |
| `overstress:spread` | 20 | 3000 | 0% / 32 | idle | 4 min | no | off |
| `overstress:pvp` | 16 | 0 | 100% / 32 | pvp | 3 min | no | on |

## Adding a scenario or a simulation
Scenarios and simulations live in a registry, so you can add your own. Scenarios implement the `BotScenario` class while simulations implement `Simulation`. A scenario steers the bot's client every client tick: it reads what the client knows and drives its movement and its hands, never the server. `standing()` sets the game mode, the invulnerability and the command rights the bot joins with.

```java
Registry.register(BotScenarios.REGISTRY, Identifier.fromNamespaceAndPath("mymod", "afk_farm"), new AfkFarmScenario());
```

```java
Registry.register(Simulations.REGISTRY, Identifier.fromNamespaceAndPath("mymod", "afk_farm"), new DragonFightSimulation());
```
