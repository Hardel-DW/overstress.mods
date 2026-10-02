# Overstress
Overstress est un mod serveur pour NeoForge et Fabric à partir de la 26.1 qui permet de simuler des joueurs en utilisant des scénarios pour effectuer des benchmarks, tester les performances à plusieurs joueurs, ou autre.

Chaque bot est un client sans affichage, fait pour que son coût se rapproche d'un vrai joueur. Il se connecte par le login et la configuration du serveur, le serveur le fait tourner par sa connexion comme tout joueur, et tout ce qu'il fait passe par les paquets d'un client vanilla, que le serveur vérifie.

Certaines choses à noter :
- **Deux liens.** `direct` passe les paquets comme des objets : le serveur ne paie ni l'encodage, ni la compression, ni le socket. `network` se connecte en TCP au port du serveur avec le pipeline vanilla, donc le serveur paie tout. Le bot jette les octets qu'il reçoit, un vrai client les décode sur sa propre machine.
- **Le mouvement est une physique client simple.** Le bot suit la surface des chunks reçus, tombe avec la gravité vanilla, plane en élytre, monte les demi-blocs, saute un bloc entier comme un joueur, et se détourne d'un mur plus haut.
- **Chaque action est un paquet.** Un bot mine en gardant le bouton d'attaque sur un bloc : début, la progression vanilla de son outil tick après tick, fin, un coup de poing par tick. Il frappe une entité par le paquet d'attaque une fois son attaque chargée, utilise un objet ou clique un bloc par les paquets d'utilisation, choisit son slot de hotbar, déplace ses objets par des clics d'inventaire, écrit dans le chat et lance des commandes. Le ramassage et la mort restent au serveur : un bot marche sur ses drops, et un bot mort demande aussitôt à réapparaître.
- **Le client sait ce que ses paquets lui ont dit.** La surface de chaque chunk qu'il tient, la hauteur et le bloc du dessus de chaque colonne, lus dans le paquet de chunk et tenus à jour par les block updates ; le type et la position de chaque entité que le serveur lui montre ; son inventaire, sa vie et sa faim. Il ne lit jamais le monde du serveur, donc il ne mine qu'un bloc du dessus qu'il connaît, sur une couche.
- **Les bots se connectent avec un kit.** Chaque bot arrive comme s'il s'était déconnecté avec une élytre sur lui, des outils en diamant, une épée, une armure dans l'inventaire, de la nourriture et des œufs de zombie. Il est invulnérable et en survie, sauf si son scénario dit autre chose : `spawner` joue en créatif, `dimensions` et `random` ont les droits de commande d'un gamemaster, `pvp` est mortel.
- **Les profils sont en mode offline.** Les UUID dérivent du nom du bot. Les bots ne passent pas par l'authentification et ne prennent aucune place de joueur.

## Commandes
`/fakeplayer` demande le niveau de permission 2, donc gamemaster. La commande `scenario` permet d'invoquer des bots avec une action spécifique, `simulation` est basiquement un alias des scénarios avec juste des paramètres préconfigurés.

- `/fakeplayer spawn <count> <spread> <cluster> [scenario] [everyTicks]` fait apparaître `N` joueurs dans un rayon délimité avec le scénario mentionné. Le paramètre `<cluster>` va de 0% à 100%, quand le joueur apparaît il a une probabilité d'apparaître dans une zone existante, ce qui crée des groupes de joueurs. `<everyTicks>`, optionnel, permet de décaler périodiquement les apparitions.
- `/fakeplayer scenario set <bot> <scenario>` change le scénario d'un bot.
- `/fakeplayer scenario random <percent> <scenario>` change le scénario de plusieurs bots aléatoirement.
- `/fakeplayer scenario list` affiche tous les scénarios.
- `/fakeplayer clear` supprime tous les bots.
- `/fakeplayer clear within <radius>` supprime tous les bots autour de la commande dans le rayon mentionné.
- `/fakeplayer clear <count> <first|last|random>` supprime un certain nombre de bots, les premiers, les derniers, ou aléatoirement.
- `/fakeplayer transport [direct|network]` affiche ou choisit le lien des prochains bots. Les bots prennent `network` dès que le serveur écoute sur un port, `direct` sinon.
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
| `overstress:elytra` | Plane en élytre en ligne droite à une hauteur fixe de Y 350, à 36 blocs/s. |
| `overstress:mine` | Marche à 3 blocs/s et s'arrête pour casser les blocs de sol au-dessus d'une bande de 3 de large devant lui (terre, sable, gravier, pierre et semblables), chacun avec le meilleur outil de sa hotbar, puis marche sur les drops. Block updates, drops, ramassage, lumière. |
| `overstress:fight` | Tient son épée, marche vers le mob le plus proche qu'il voit dans 16x8x16 et le frappe à moins de 3 blocs chaque fois que son attaque est chargée. |
| `overstress:spawner` | En créatif, tourne autour de son point de spawn et clique un œuf de zombie sur le sol à portée 10 fois par seconde, tant qu'il voit moins de 300 mobs autour de lui. |
| `overstress:dimensions` | Se balade, et toutes les 15 secondes lance `/execute in <dimension suivante> run tp` vers son point de spawn à y 128. |
| `overstress:pvp` | Mortel. Met son armure par des clics d'inventaire, tient son épée, poursuit le joueur ou le monstre le plus proche qu'il voit et le frappe une fois chargé, mange quand il a faim. Quand il meurt, il réapparaît aussitôt et dit gg. |
| `overstress:churn` | Fait des allers-retours entre son point de spawn et 450 blocs à 30 blocs/s. Charge et décharge les mêmes chunks en boucle. |
| `overstress:random` | Exécute un autre scénario pendant une minute, puis en tire un nouveau. |

## Simulations
Une simulation est un spawn préconfiguré avec une durée. Ses champs sont bots, rayon, cluster en %, rayon de cluster, scénario, durée, intervalle de spawn, et mob spawning. Le rayon de cluster vaut 32 (touching) ou 256 (neighbouring).

| Simulation | Bots | Rayon | Cluster | Scénario | Durée | Spawn étalé | Mob spawning |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `overstress:smoke` | 8 | 300 | 5% / 32 | idle | 1 min | non | off |
| `overstress:idle` | 50 | 2000 | 5% / 32 | idle | 3 min | non | off |
| `overstress:roam` | 40 | 1500 | 5% / 32 | elytra | 3 min | non | off |
| `overstress:border` | 20 | 2100 | 0% / 32 | wander | 3 min | non | off |
| `overstress:mobs` | 20 | 800 | 5% / 32 | spawner | 3 min | non | on |
| `overstress:churn` | 60 | 5000 | 5% / 256 | churn | 10 min | non | off |
| `overstress:ramp` | 300 | 20000 | 0% / 256 | mine | 15 min | 1 bot / 60 ticks | off |
| `overstress:sprawl` | 100 | 50000 | 5% / 256 | elytra | 10 min | 1 bot / 60 ticks | off |
| `overstress:solo` | 1 | 0 | 0% / 32 | elytra | 5 min | non | off |
| `overstress:worldgen` | 5 | 2000 | 0% / 32 | elytra | 3 min | non | off |
| `overstress:spread` | 20 | 3000 | 0% / 32 | idle | 4 min | non | off |
| `overstress:pvp` | 16 | 0 | 100% / 32 | pvp | 3 min | non | on |

## Ajouter un scénario ou une simulation
Les scénarios et les simulations vivent dans un registre, vous pouvez donc en ajouter. Les scénarios implémentent la classe `BotScenario` tandis que les simulations implémentent `Simulation`. Un scénario dirige le client du bot à chaque tick client : il lit ce que le client sait et pilote son mouvement et ses mains, jamais le serveur. `standing()` fixe le mode de jeu, l'invulnérabilité et les droits de commande avec lesquels le bot se connecte.

```java
Registry.register(BotScenarios.REGISTRY, Identifier.fromNamespaceAndPath("mymod", "afk_farm"), new AfkFarmScenario());
```

```java
Registry.register(Simulations.REGISTRY, Identifier.fromNamespaceAndPath("mymod", "afk_farm"), new DragonFightSimulation());
```
