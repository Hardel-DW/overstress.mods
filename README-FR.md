# Overstress
Overstress est un mod serveur pour NeoForge et Fabric à partir de la 26.1 qui permet de simuler des joueurs en utilisant des scénarios pour effectuer des benchmarks, tester les performances à plusieurs joueurs, ou autre.

Il fait apparaître des joueurs sans client, et ils sont faits de sorte que leur coût se rapproche d'un vrai joueur. Pour cela, les bots sont de vraies instances de la classe `ServerPlayer`, placées par le vrai chemin de login.

Certaines choses à noter :
- **Le coût réseau est nul.**
- **Le mouvement est une téléportation.**
- **Il n'y a pas de client.**
- **Les profils sont en mode offline.** Les UUID dérivent du nom du bot, donc un serveur en mode online ou un mod d'authentification peut les refuser.

## Commandes
`/fakeplayer` demande le niveau de permission 2, donc gamemaster. La commande `scenario` permet d'invoquer des bots avec une action spécifique, `simulation` est basiquement un alias des scénarios avec juste des paramètres préconfigurés.

- `/fakeplayer spawn <count> <spread> <cluster> [scenario] [everyTicks]` fait apparaître `N` joueurs dans un rayon délimité avec le scénario mentionné. Le paramètre `<cluster>` va de 0% à 100%, quand le joueur apparaît il a une probabilité d'apparaître dans une zone existante, ce qui crée des groupes de joueurs. `<everyTicks>`, optionnel, permet de décaler périodiquement les apparitions.
- `/fakeplayer scenario set <bot> <scenario>` change le scénario d'un bot.
- `/fakeplayer scenario random <percent> <scenario>` change le scénario de plusieurs bots aléatoirement.
- `/fakeplayer scenario list` affiche tous les scénarios.
- `/fakeplayer clear` supprime tous les bots.
- `/fakeplayer clear within <radius>` supprime tous les bots autour de la commande dans le rayon mentionné.
- `/fakeplayer clear <count> <first|last|random>` supprime un certain nombre de bots, les premiers, les derniers, ou aléatoirement.
- `/fakeplayer pos` affiche les positions de tous les bots.
- `/fakeplayer list` affiche combien de bots existent.
- `/fakeplayer simulation start <simulation>` démarre la simulation.
- `/fakeplayer simulation stop` arrête la simulation.
- `/fakeplayer simulation status` affiche l'état de la simulation.

## Scénarios
| Scénario | Charge produite |
| --- | --- |
| `overstress:idle` | Rien. Le coût de base d'un joueur connecté. |
| `overstress:wander` | Marche en diagonale à 4 blocs/s. |
| `overstress:fly` | Vole en ligne droite 100 blocs au-dessus de la surface à 36 blocs/s. |
| `overstress:mine` | Marche à 3 blocs/s et casse un bloc tous les 8 ticks. Block updates, drops, lumière. |
| `overstress:fight` | Toutes les 10 ticks, cherche un mob dans 16x8x16 autour de lui, s'en approche puis le frappe à moins de 3 blocs. |
| `overstress:spawner` | Fait apparaître des zombies autour de lui, jusqu'à 300 à proximité. |
| `overstress:dimensions` | Se balade, puis passe à la dimension suivante toutes les 15 secondes. |
| `overstress:churn` | Fait des allers-retours entre son point de spawn et 450 blocs à 30 blocs/s. Charge et décharge les mêmes chunks en boucle. |
| `overstress:random` | Exécute un autre scénario pendant une minute, puis en tire un nouveau. |

## Simulations
Une simulation est un spawn préconfiguré avec une durée. Ses champs sont bots, rayon, cluster en %, rayon de cluster, scénario, durée, intervalle de spawn, et mob spawning. Le rayon de cluster vaut 32 (touching) ou 256 (neighbouring).

| Simulation | Bots | Rayon | Cluster | Scénario | Durée | Spawn étalé | Mob spawning |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `overstress:smoke` | 8 | 300 | 5% / 32 | idle | 1 min | non | off |
| `overstress:idle` | 50 | 2000 | 5% / 32 | idle | 3 min | non | off |
| `overstress:roam` | 40 | 1500 | 5% / 32 | fly | 3 min | non | off |
| `overstress:border` | 20 | 2100 | 0% / 32 | wander | 3 min | non | off |
| `overstress:mobs` | 20 | 800 | 5% / 32 | spawner | 3 min | non | on |
| `overstress:churn` | 60 | 5000 | 5% / 256 | churn | 10 min | non | off |
| `overstress:ramp` | 300 | 20000 | 0% / 256 | mine | 15 min | 1 bot / 60 ticks | off |
| `overstress:sprawl` | 100 | 50000 | 5% / 256 | fly | 10 min | 1 bot / 60 ticks | off |
| `overstress:flight` | 1 | 0 | 0% / 32 | fly | 5 min | non | off |
| `overstress:worldgen` | 5 | 2000 | 0% / 32 | fly | 3 min | non | off |
| `overstress:spread` | 20 | 3000 | 0% / 32 | idle | 4 min | non | off |

## Ajouter un scénario ou une simulation
Les scénarios et les simulations vivent dans un registre, vous pouvez donc en ajouter. Les scénarios implémentent la classe `BotScenario` tandis que les simulations implémentent `Simulation`.

```java
Registry.register(BotScenarios.REGISTRY, Identifier.fromNamespaceAndPath("mymod", "afk_farm"), new AfkFarmScenario());
```

```java
Registry.register(Simulations.REGISTRY, Identifier.fromNamespaceAndPath("mymod", "afk_farm"), new DragonFightSimulation());
```
