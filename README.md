# Overstress
Overstress is a server mod for NeoForge and Fabric from 26.1 onwards that simulates players using scenarios, to run benchmarks, test performance with many players, or anything else.

Each bot is a headless client, built so that its cost is close to a real player. It logs in through the server's own login and configuration, the server ticks it through its connection like any player, and everything it does goes through the packets a vanilla client sends, which the server checks.

A few things to note:
- **Two links.** `direct` hands the packets over as objects: the server pays no encoding, no compression and no socket. `network` connects over TCP to the server's own port with the vanilla pipeline, so the server pays all of it. The bot drops the bytes it receives, a real client decodes them on its own machine.
- **Movement is a simple client physics.** The bot follows the surface of the chunks it received, falls with the vanilla gravity, glides with an elytra, steps up a full block like with step assist, and digs through or turns away from a higher wall.
- **Every action is a packet.** A bot mines by holding its attack button on a block: start, the vanilla progress of its tool tick after tick, stop, a punch each tick. It hits an entity with the attack packet once its attack is charged, uses an item or clicks a block with the use packets, picks its hotbar slot, moves items with inventory clicks, chats and runs commands. Pickups and deaths are the server's own: a bot walks over its drops, and a dead bot asks to respawn at once.
- **The client knows what its packets told it.** The surface of each chunk it holds: the height of every column, its top block and the block under it, read from the chunk packet and kept up to date by the block updates; the type and position of every entity the server shows it; its inventory, health and food. It never reads the server's world, so it only mines a top block it knows.
- **Bots log in with a kit.** Every bot joins as if it had logged out with an elytra on, diamond tools, a sword, armor in its inventory, food and zombie eggs. It is invulnerable and in survival, except where its scenario says otherwise: `spawner` plays in creative, `pvp` is mortal.
- **Profiles are offline-mode.** UUIDs are derived from the bot name. Bots skip authentication and take no player slot.

## Commands
`/overstress` needs permission level 2, so gamemaster. `/overstress player` manages the bots by hand, `/overstress simulation` runs a preconfigured, reproducible experiment.

- `/overstress player spawn <count> <spread> [cluster] [scenario] [everyTicks]` spawns `N` players within a bounded radius with the given scenario, or a scenario drawn at random for each bot when it is left out. The `[cluster]` parameter goes from 0% to 100%, when a player spawns it has a probability of spawning in an existing zone, which creates groups of players. `[everyTicks]` spreads the spawns out periodically.
- `/overstress player scenario set <bot> <scenario>` changes the scenario of one bot.
- `/overstress player scenario random <percent> <scenario>` gives the scenario to this percentage of the bots, drawn at random. The other bots go `idle`.
- `/overstress player scenario list` shows the bots, grouped by scenario.
- `/overstress player clear` removes every bot.
- `/overstress player clear within <radius>` removes every bot around the command within the given radius.
- `/overstress player clear <count> [first|last|random]` removes a given number of bots, the first ones, the last ones, or at random. The last ones by default.
- `/overstress player transport [direct|network]` shows or picks the link of the next bots. Bots use `network` whenever the server listens on a port, `direct` otherwise.
- `/overstress player pos` shows the position of every bot.
- `/overstress player list` shows how many bots exist.
- `/overstress simulation start <simulation>` starts the simulation.
- `/overstress simulation stop` stops the simulation.
- `/overstress simulation status` shows the state of the simulation.

## Scenarios
| Scenario | Load it produces |
| --- | --- |
| `overstress:idle` | Nothing. The baseline cost of a connected player. |
| `overstress:wander` | Walks a diagonal at 4 blocks/s. |
| `overstress:elytra` | Glides with an elytra in a straight line at a fixed height of Y 350, at 36 blocks/s. |
| `overstress:mine` | Digs straight along one of the four directions at 3 blocks/s, with the best tool of its hotbar. On the surface it breaks the top block of the column in front of it, once per column, then walks on. Facing a wall or at the bottom of a hole, it breaks the block ahead at head height and the one above, steps up one block, and so on, a staircase back to the surface. It lets itself fall off a drop, walks on water, never breaks a liquid, and turns a quarter when a block cannot be broken. The bot only sees the surface, so the server, which sees the blocks, tells it which block to break and where to step; the bot breaks and walks with real packets. Block updates, drops, lighting. |
| `overstress:fight` | Holds its sword, walks to the nearest mob it sees within 16x8x16 and hits it within 3 blocks each time its attack is charged. |
| `overstress:spawner` | In creative, circles its spawn point and clicks a zombie egg on the ground within reach 10 times a second, while it sees fewer than 300 mobs around it. |
| `overstress:nether` | The server builds a nether portal next to the bot and places it inside, like a player who walks in and waits. Ten seconds after each arrival, the server moves the bot one block out of the portal, then back in a second later, and so on between the overworld and the nether. The bot does not walk in the nether, where it knows no floor. |
| `overstress:end` | The server sets an end portal block next to the bot and places it inside after ten seconds. In the End, the server kills the dragon and, ten seconds after the arrival, places the bot in the exit portal. The bot skips the credits like a client and lands back at the world spawn, from where the server places it in its portal again, and so on. |
| `overstress:gateway` | Reaches the End like `overstress:end`, then hops through the end gateways. The server kills the dragon and, ten seconds after each arrival, places the bot in the nearest loaded gateway, on the main island or next to where it arrived on the outer islands. |
| `overstress:pvp` | Mortal. Wears its armor through inventory clicks, holds its sword, chases the nearest player or monster it sees and hits it when charged, eats when hungry. When it dies it respawns at once and says gg. |
| `overstress:churn` | Glides with an elytra at Y 350 and 36 blocks/s, 450 blocks along its heading, then 450 blocks back, in a loop. Loads and unloads the same chunks over and over. |
| `overstress:random` | Runs another scenario for a minute, then draws a new one. |

## Simulations
A simulation is a reproducible experiment. When it starts it removes every bot, then places its own on a ring around the world origin, with fixed seeds: two runs give the same positions. At the end of its duration it removes its bots. Only one simulation runs at a time.

While it runs, it freezes the time, the weather and the random ticks, and sets mob spawning from its Mob spawning column. The gamerules get their value back when the simulation or the server stops.

Its fields are bots, radius, cluster in %, cluster radius, scenario, duration, spawn interval, and mob spawning. The cluster radius is 32 (touching) or 256 (neighbouring).

| Simulation | Bots | Radius | Cluster | Scenario | Duration | Spread spawn | Mob spawning |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `overstress:smoke` | 8 | 300 | 5% / 32 | idle | 1 min | no | off |
| `overstress:idle` | 50 | 2000 | 5% / 32 | idle | 3 min | no | off |
| `overstress:roam` | 40 | 1500 | 5% / 32 | elytra | 3 min | no | off |
| `overstress:border` | 20 | 2100 | 0% / 32 | wander | 3 min | no | off |
| `overstress:mobs` | 20 | 800 | 5% / 32 | spawner | 3 min | no | on |
| `overstress:churn` | 60 | 5000 | 5% / 256 | churn | 10 min | no | off |
| `overstress:ramp` | 300 | 20000 | 0% / 256 | mine | 15 min | 1 bot / 60 ticks | off |
| `overstress:sprawl` | 100 | 50000 | 5% / 256 | elytra | 10 min | 1 bot / 60 ticks | off |
| `overstress:solo` | 1 | 0 | 0% / 32 | elytra | 5 min | no | off |
| `overstress:worldgen` | 5 | 2000 | 0% / 32 | elytra | 3 min | no | off |
| `overstress:spread` | 20 | 3000 | 0% / 32 | idle | 4 min | no | off |
| `overstress:pvp` | 16 | 0 | 100% / 32 | pvp | 3 min | no | on |

## Adding a scenario or a simulation
Scenarios and simulations live in a registry, so you can add your own. A scenario implements the `BotScenario` interface, a simulation is a `Simulation` record whose fields follow the order given above, with durations in ticks. A scenario steers the bot's client every client tick: it reads what the client knows and drives its movement and its hands, never the server. `standing()` sets the game mode and the invulnerability the bot joins with.

```java
Registry.register(BotScenarios.REGISTRY, Identifier.fromNamespaceAndPath("mymod", "afk_farm"), new AfkFarmScenario());
```

```java
Registry.register(Simulations.REGISTRY, Identifier.fromNamespaceAndPath("mymod", "afk_farm"), new Simulation(20, 500, 0, 32, afkFarm, 3 * SharedConstants.TICKS_PER_MINUTE, 0, false));
```
