# Overstress
Overstress is a server mod for NeoForge and Fabric from 26.1 onwards that simulates players using scenarios, to run benchmarks, test performance with many players, or anything else.

It spawns players without a client, and they are built so that their cost is close to a real player. To do that, the bots are real instances of the `ServerPlayer` class, placed through the real login path.

A few things to note:
- **Network cost is zero.**
- **Movement is teleportation.**
- **There is no client.**
- **Profiles are offline-mode.** UUIDs are derived from the bot name, so an online-mode server or an auth mod may refuse them.

## Commands
`/fakeplayer` needs permission level 2, so gamemaster. The `scenario` command spawns bots with a specific action, `simulation` is basically an alias of scenarios with just preconfigured parameters.

- `/fakeplayer spawn <count> <spread> <cluster> [scenario] [everyTicks]` spawns `N` players within a bounded radius with the given scenario. The `<cluster>` parameter goes from 0% to 100%, when a player spawns it has a probability of spawning in an existing zone, which creates groups of players. `<everyTicks>`, optional, spreads the spawns out periodically.
- `/fakeplayer scenario set <bot> <scenario>` changes the scenario of one bot.
- `/fakeplayer scenario random <percent> <scenario>` changes the scenario of several bots at random.
- `/fakeplayer scenario list` shows every scenario.
- `/fakeplayer clear` removes every bot.
- `/fakeplayer clear within <radius>` removes every bot around the command within the given radius.
- `/fakeplayer clear <count> <first|last|random>` removes a given number of bots, the first ones, the last ones, or at random.
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
| `overstress:fly` | Flies a straight line 100 blocks over the surface at 36 blocks/s. |
| `overstress:mine` | Walks at 3 blocks/s and breaks a block every 8 ticks. Block updates, drops, lighting. |
| `overstress:fight` | Every 10 ticks, looks for a mob within 16x8x16 around itself, walks to it, then hits it under 3 blocks. |
| `overstress:spawner` | Spawns zombies around itself, up to 300 nearby. |
| `overstress:dimensions` | Wanders, then crosses to the next dimension every 15 seconds. |
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

## Adding a scenario or a simulation
Scenarios and simulations live in a registry, so you can add your own. Scenarios implement the `BotScenario` class while simulations implement `Simulation`.

```java
Registry.register(BotScenarios.REGISTRY, Identifier.fromNamespaceAndPath("mymod", "afk_farm"), new AfkFarmScenario());
```

```java
Registry.register(Simulations.REGISTRY, Identifier.fromNamespaceAndPath("mymod", "afk_farm"), new DragonFightSimulation());
```
