# Overstress
Overstress est un mod serveur pour NeoForge et Fabric à partir de la 26.1 qui permet de simuler des joueurs en utilisant des scénarios pour effectuer des benchmarks, tester les performances à plusieurs joueurs, ou autre.

Chaque bot est un client sans affichage, fait pour que son coût se rapproche d'un vrai joueur. Il se connecte par le login et la configuration du serveur, le serveur le fait tourner par sa connexion comme tout joueur, et tout ce qu'il fait passe par les paquets d'un client vanilla, que le serveur vérifie.

Certaines choses à noter :
- **Deux liens.** `direct` passe les paquets comme des objets : le serveur ne paie ni l'encodage, ni la compression, ni le socket. `network` se connecte en TCP au port du serveur avec le pipeline vanilla, donc le serveur paie tout. Le bot jette les octets qu'il reçoit, un vrai client les décode sur sa propre machine.
- **Le mouvement est une physique client simple.** Le bot suit la surface des chunks reçus, tombe avec la gravité vanilla, plane en élytre, monte un bloc entier comme avec step assist, et creuse ou se détourne d'un mur plus haut.
- **Chaque action est un paquet.** Un bot mine en gardant le bouton d'attaque sur un bloc : début, la progression vanilla de son outil tick après tick, fin, un coup de poing par tick. Il frappe une entité par le paquet d'attaque une fois son attaque chargée, utilise un objet ou clique un bloc par les paquets d'utilisation, choisit son slot de hotbar, déplace ses objets par des clics d'inventaire, écrit dans le chat et lance des commandes. Le ramassage et la mort restent au serveur : un bot marche sur ses drops, et un bot mort demande aussitôt à réapparaître.
- **Le client sait ce que ses paquets lui ont dit.** La surface de chaque chunk qu'il tient : la hauteur de chaque colonne, son bloc du dessus et le bloc juste dessous, lus dans le paquet de chunk et tenus à jour par les block updates ; le type et la position de chaque entité que le serveur lui montre ; son inventaire, sa vie et sa faim. Il ne lit jamais le monde du serveur, donc il ne mine qu'un bloc du dessus qu'il connaît.
- **Les bots se connectent avec un kit.** Chaque bot arrive comme s'il s'était déconnecté avec une élytre sur lui, des outils en diamant, une épée, une armure dans l'inventaire, de la nourriture et des œufs de zombie. Il est invulnérable et en survie, sauf si son scénario dit autre chose : `spawner` joue en créatif, `pvp` est mortel.
- **Les profils sont en mode offline.** Les UUID dérivent du nom du bot. Les bots ne passent pas par l'authentification et ne prennent aucune place de joueur.

## Commandes
`/overstress` demande le niveau de permission 2, donc gamemaster. `/overstress player` gère les bots à la main, `/overstress simulation` lance une expérience préconfigurée et reproductible.

- `/overstress player spawn <count> <spread> [cluster] [scenario] [everyTicks]` fait apparaître `N` joueurs dans un rayon délimité avec le scénario mentionné, ou un scénario tiré au hasard pour chaque bot quand il est omis. Le paramètre `[cluster]` va de 0% à 100%, quand le joueur apparaît il a une probabilité d'apparaître dans une zone existante, ce qui crée des groupes de joueurs. `[everyTicks]` permet de décaler périodiquement les apparitions.
- `/overstress player scenario set <bot> <scenario>` change le scénario d'un bot.
- `/overstress player scenario random <percent> <scenario>` donne le scénario à ce pourcentage des bots, tirés au hasard. Les autres bots passent en `idle`.
- `/overstress player scenario list` affiche les bots, regroupés par scénario.
- `/overstress player clear` supprime tous les bots.
- `/overstress player clear within <radius>` supprime tous les bots autour de la commande dans le rayon mentionné.
- `/overstress player clear <count> [first|last|random]` supprime un certain nombre de bots, les premiers, les derniers, ou aléatoirement. Sans précision, les derniers arrivés.
- `/overstress player transport [direct|network]` affiche ou choisit le lien des prochains bots. Les bots prennent `network` dès que le serveur écoute sur un port, `direct` sinon.
- `/overstress player pos` affiche les positions de tous les bots.
- `/overstress player list` affiche combien de bots existent.
- `/overstress simulation start <simulation>` démarre la simulation.
- `/overstress simulation stop` arrête la simulation.
- `/overstress simulation status` affiche l'état de la simulation.

## Scénarios
| Scénario | Charge produite |
| --- | --- |
| `overstress:idle` | Rien. Le coût de base d'un joueur connecté. |
| `overstress:wander` | Marche en diagonale à 4 blocs/s. |
| `overstress:elytra` | Plane en élytre en ligne droite à une hauteur fixe de Y 350, à 36 blocs/s. |
| `overstress:mine` | Creuse tout droit dans une des quatre directions à 3 blocs/s, avec le meilleur outil de sa hotbar. À la surface il casse le bloc du dessus de la colonne devant lui, une fois par colonne, puis avance. Face à un mur ou au fond d'un trou, il casse le bloc devant lui à hauteur de tête et celui au-dessus, monte d'un bloc, et recommence, un escalier jusqu'à la surface. Il se laisse tomber d'une marche trop haute, marche sur l'eau, ne casse jamais un liquide, et tourne d'un quart de tour quand un bloc ne se casse pas. Le bot ne voit que la surface, donc le serveur, qui voit les blocs, lui dit quel bloc casser et où poser le pied. Le bot casse et marche avec de vrais paquets. Block updates, drops, lumière. |
| `overstress:fight` | Tient son épée, marche vers le mob le plus proche qu'il voit dans 16x8x16 et le frappe à moins de 3 blocs chaque fois que son attaque est chargée. |
| `overstress:spawner` | En créatif, tourne autour de son point de spawn et clique un œuf de zombie sur le sol à portée 10 fois par seconde, tant qu'il voit moins de 300 mobs autour de lui. |
| `overstress:nether` | Le serveur construit un portail du Nether à côté du bot et le place dedans, comme un joueur qui entre et attend. Dix secondes après chaque arrivée, le serveur sort le bot d'un bloc du portail, puis l'y remet une seconde plus tard, et ainsi de suite entre l'overworld et le Nether. Le bot ne marche pas dans le Nether, où il ne connaît aucun sol. |
| `overstress:end` | Le serveur pose un bloc de portail de l'End à côté du bot et le place dedans après dix secondes. Dans l'End, le serveur tue le dragon et, dix secondes après l'arrivée, place le bot dans le portail de sortie. Le bot passe les crédits comme un client et revient au spawn du monde, d'où le serveur le replace dans son portail, et ainsi de suite. |
| `overstress:gateway` | Rejoint l'End comme `overstress:end`, puis saute de passerelle en passerelle. Le serveur tue le dragon et, dix secondes après chaque arrivée, place le bot dans la passerelle chargée la plus proche, sur l'île principale ou à côté de son point d'arrivée sur les îles extérieures. |
| `overstress:pvp` | Mortel. Met son armure par des clics d'inventaire, tient son épée, poursuit le joueur ou le monstre le plus proche qu'il voit et le frappe une fois chargé, mange quand il a faim. Quand il meurt, il réapparaît aussitôt et dit gg. |
| `overstress:churn` | Plane en élytre à Y 350 et 36 blocs/s, 450 blocs dans sa direction, puis 450 blocs en arrière, en boucle. Charge et décharge les mêmes chunks encore et encore. |
| `overstress:random` | Exécute un autre scénario pendant une minute, puis en tire un nouveau. |

## Simulations
Une simulation est une expérience reproductible. Au départ elle retire tous les bots présents, puis place les siens sur un cercle autour de l'origine du monde, avec des graines fixes : deux runs donnent les mêmes positions. À la fin de sa durée elle retire ses bots. Une seule simulation tourne à la fois.

Tant qu'elle tourne, elle gèle le temps, la météo et les random ticks, et règle l'apparition des mobs selon sa colonne Mob spawning. Les gamerules reprennent leur valeur à l'arrêt de la simulation ou du serveur.

Ses champs sont bots, rayon, cluster en %, rayon de cluster, scénario, durée, intervalle de spawn, et mob spawning. Le rayon de cluster vaut 32 (touching) ou 256 (neighbouring).

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
Les scénarios et les simulations vivent dans un registre, vous pouvez donc en ajouter. Un scénario implémente l'interface `BotScenario`, une simulation est un record `Simulation` dont les champs suivent l'ordre donné plus haut, avec les durées en ticks. Un scénario dirige le client du bot à chaque tick client : il lit ce que le client sait et pilote son mouvement et ses mains, jamais le serveur. `standing()` fixe le mode de jeu et l'invulnérabilité avec lesquels le bot se connecte.

```java
Registry.register(BotScenarios.REGISTRY, Identifier.fromNamespaceAndPath("mymod", "afk_farm"), new AfkFarmScenario());
```

```java
Registry.register(Simulations.REGISTRY, Identifier.fromNamespaceAndPath("mymod", "afk_farm"), new Simulation(20, 500, 0, 32, afkFarm, 3 * SharedConstants.TICKS_PER_MINUTE, 0, false));
```
