# SkillCraft Design

## Vision
Bring the long-term progression of Old School RuneScape into Minecraft without losing what makes Minecraft Minecraft. Skills should involve **interesting decisions, not just "break block, get XP"**. Progression should be **horizontal**: new options, regions and tradeoffs instead of only bigger numbers.

## Core pillars
1. **Skills with depth.** Each skill has its own mechanics, tools and failure states, plus a reason to come back to it.
2. **Level-gated content.** Woods, ores, fish, crops, recipes and areas unlock with levels.
3. **Renewable resources.** Skilling nodes run out and regenerate, so players move around and find good spots, and nobody strip-mines one tree forever.
4. **Building stays first-class.** Construction and Crafting add to Minecraft's building instead of replacing it.
5. **Mechanical bosses.** Fights are won by learning patterns and phases and by preparing, not by out-gearing the boss.
6. **Horizontal gear.** Equipment sets have tradeoffs (speed vs. damage, specialised effects, synergy with particular skills).

## Starter skills
| Skill | Category | Minecraft hook | Depth ideas |
|---|---|---|---|
| Melee | Combat | Swords, spears, axes, warhammers | Stab/slash/crush damage types vs. monster weaknesses, attack styles, weapon speed tiers, special attacks |
| Ranged | Combat | Bows, crossbows | Light/medium/heavy ammo, range and draw-speed tradeoffs, positioning |
| Magic | Combat | Wands and staves | Elemental bolts (fire, frost, storm, arcane) vs. monster weaknesses; spells on the ability bar; motes as reagents, staves supply their element ([AGILITY_MAGIC_PRAYER.md](AGILITY_MAGIC_PRAYER.md)) |
| Defence | Combat | Armour | Gates armour; typed armour defends against stab/slash/crush/ranged |
| Prayer | Combat | Bones and shrines | Blessings that drain Faith while on (boosts, wards, Saving Grace); bury or offer bones at a Votive Shrine |
| Constitution | Combat | Health | Max HP scales with level; XP comes from dealing damage |
| Mining | Gathering | Ores | Level-gated ores, ore veins that deplete and respawn, pickaxe tiers, rare gems |
| Woodcutting | Gathering | Trees | Level-gated tree types (oak → willow → maple → yew → magic-style equivalents), stumps that regrow, success chance per swing |
| Fishing | Gathering | Fishing rod | Fishing spots (churning water on real ponds) that swim off and resurface elsewhere; level-gated catches; bait and methods later |
| Farming | Gathering | Crops | Growth stages, disease and protection, herb/tree patches, compost tiers |
| Smithing | Artisan | Furnace/anvil | Smelt bars → forge items at a custom anvil, quality affected by level |
| Crafting | Artisan | Crafting table | Leather, jewellery, glass; level-gated recipes |
| Cooking | Artisan | Cooking Range | Raw food cooks over time; burn chance falls with level until a stop-burn level |
| Firemaking | Artisan | Fire | Log tiers burn for XP; campfires used for cooking and buffs |
| Construction | Artisan | Building | Blueprint-based furniture and utility blocks (bank chest, altars, teleports) built from planks and bars |
| Leycraft | Artisan | Amethyst and the world around you | Mine ley shards, weave them into Magic's motes at a Ley Altar whose element comes from its surroundings (lava, ice, lightning rods, amethyst); quick or careful (minigame) weaves ([LEYCRAFT.md](LEYCRAFT.md)) |
| Agility | Support | Parkour | XP for climbing, gap jumps and high dives (once per spot); movement abilities (Sidestep, Grapple Line, Shadowstep...) and perks (safe falls, faster sprint); courses with obstacles later |
| Beastbonding | Support | Pets, mounts and tamed mobs | Bond creatures into a Bond Codex, then summon or store them at will: vanilla pets and mounts early, changed mobs (wall-climbing Cliffstrider, fetching fox, Ironward golem) mid-game, the new flying Cloudray at 62. Fallen bonds are rekindled at a shrine ([BEASTBONDING.md](BEASTBONDING.md)). Replaced Sailing |

Planned for later: Herblore, Fletching, Thieving, Slayer, Hunter (renamed equivalents). The rune-making skill arrived as Leycraft.

## XP curve
The OSRS formula scaled by **0.75** (`XpTable.CURVE_SCALE`): every level needs 25% less XP, so maxing a skill takes about 25% less time.
- **Level 99 = 9,775,823 XP** (OSRS: 13,034,431).
- The shape is unchanged: total XP still **about doubles every 7 levels**, and level 92 is still the halfway point to 99.
- Some milestones: level 10 = 865, 30 = 10,022, 50 = 75,999, 70 = 553,220, 92 = 4,887,939 (each 75% of the OSRS value).
- XP is capped at 200M. A server config multiplier (`xpMultiplier`) scales all gains on top of this. Implemented in `skill/XpTable.java`.
- **Existing saves:** players keep their XP, so their levels go up when they load in (e.g. 50,000 XP was level 42 and is now 45).
- Tested by the `xp_curve` GameTest.

## Tiers
Content is grouped into 10-level tiers: **I** = 1, **II** = 10, **III** = 20 … **X** = 90. **Vanilla content tops out around level 50 (tiers I–V).** Tiers VI–X are reserved for custom woods, ores, metals and gear.

| Tier | Level | Woodcutting | Mining | Fishing | Gear |
|---|---|---|---|---|---|
| I | 1 | Oak, birch, spruce | Coal, copper | Cod | Wood, stone, copper |
| II | 10 | Jungle, acacia, mangrove | Iron, quartz | Salmon | Iron |
| III | 20 | Cherry, dark oak | Redstone, gold, lapis | Reef (tropical) fish | Gold |
| IV | 30 | Pale oak | Diamond | Pufferfish | Diamond |
| V | 40 | Crimson, warped | Emerald, ancient debris | — | Netherite (vanilla gear caps ~50) |
| VI–X | 50–90 | *custom* | *custom* | *custom* | *custom* |

## Gathering: world vs. nodes
- **World blocks give no XP.** Breaking logs and ores in the world is purely for materials, and higher-tier blocks still need the level to break.
- **Skilling nodes are how gathering skills are trained.**
  - Place a node and right-click it with an axe or pickaxe. You then gather automatically every 1.5s while you stay within about 4 blocks.
  - Each attempt can fail. Success chance rises with level and tool tier (gold is fastest, then netherite, diamond, iron, stone, wood).
  - A success gives full XP and one item. It also has a chance to **deplete** the node into a stump or empty rock, which **regrows** after a time set by tier.
  - **Scarcity limits (per tier)** keep high-tier resources from becoming infinite. A player can only work one node at a time, so long regrow times mean needing many nodes, and spacing makes those nodes cost land and walking time:

    | Tier | I | II | III | IV | V | VI | VII | VIII | IX | X |
    |---|---|---|---|---|---|---|---|---|---|---|
    | Regrow | 20s | 45s | 2m | 5m | 10m | 12m | 14m | 16m | 18m | 20m |
    | Max same-type nodes | 6 | 4 | 3 | 2 | 1 | 1 | 1 | 1 | 1 | 1 |
    | …within radius | 8 | 12 | 16 | 24 | 40 | 48 | 56 | 64 | 72 | 80 |

    Spacing is checked on placement using Minecraft's point-of-interest index (`registry/ModPoiTypes`, `node/NodePlacement`). Different node types don't count against each other.
  - **Gathering fatigue (per player, per resource type).** Every successful gather from any node adds 1 to that player's meter for that resource. The meter drains continuously. When it's full, the player can't gather that resource until it drains, and a message shows the wait. There's a warning at 75%. The limit is personal, so it doesn't matter whose nodes you use or how dense they are. In multiplayer, a high-level player's dense farm doesn't let lower-level friends out-gather their own limits. Spacing rules are the same for everyone.

    | Tier | I | II | III | IV | V | VI | VII | VIII | IX | X |
    |---|---|---|---|---|---|---|---|---|---|---|
    | Sustained rate at required level (/hr) | 400 | 200 | 100 | 40 | 12 | 10 | 8 | 6 | 5 | 4 |
    | …at level 99 (3×) | 1200 | 600 | 300 | 120 | 36 | 30 | 24 | 18 | 15 | 12 |

    Burst capacity is 15 minutes' worth (minimum 2). The meter is saved on the player (`node/GatheringFatigue`, attachment `gathering_fatigue`). Node tooltips show your personal limit.
  - **Depletion chance scales with level:** `deplete = 1 − (1 − floor) × progress`.
    - `progress` runs from 0 at the node's required level to 1 at level 99.
    - `floor = 25% + 8% × (tier − 1)`: I 25%, II 33%, III 41%, IV 49%, V 57% … X 97%.
    - At the required level a node always depletes after one success. Higher-tier nodes stay short-lived even at 99.
    - The node tooltip shows your current depletion chance.
  - **Level controls how much a node gives** (depletion). **Tools control how fast** you get it:
    - **Tool tier → swing speed:** one swing every 0.75s with wood, 0.05s faster per tier, down to 0.55s with netherite. Every second swing is the real attempt.
    - **Tool power → success chance:** wood ×0.85, stone ×1.0, copper ×1.05, iron ×1.15, diamond ×1.3, netherite ×1.4, and **gold ×1.5**. Gold is low tier but high power: it swings slowly but hits often.
    - Values live in `data/skillcraft/data_maps/item/gathering_tools.json`, and tool tooltips show them.
  - Nodes use their own sound events (`node.chop`, `node.pick`, `node.success_*`, `node.deplete_*`, `node.regrow`) defined in `assets/skillcraft/sounds.json`. They currently point at vanilla audio and can be swapped for custom `.ogg` files.
  - Nodes are unbreakable. The **Relocator** picks them up so you can move them.
  - Several players can share a node. Each rolls independently, and depletion ends everyone's session.
- **Getting nodes:** attune an **Elemental Heart** at the **Attunement Altar** (an Oak Heart makes an oak node). Hearts are found by chopping, mining and fishing in the normal Minecraft world (see **Elemental Hearts** below), so the vanilla world and the skilling nodes feed each other. Rare natural nodes come later with world generation.
- Node stats (level, XP, drop, cost, depletion, regrow time) live in `node/NodeType.java`. Block models are generated by `scripts/gen_node_assets.sh`.

## Elemental Hearts
Nodes aren't made from the resources they give. Instead, the normal Minecraft world sometimes yields an **Elemental Heart**, which attunes into that node. This sends players back and forth: playing vanilla Minecraft finds hearts, and hearts become nodes that train the skill.

- **One item, many hearts.** `skillcraft:elemental_heart` carries the node type in the `skillcraft:heart_node` component. It's tinted with the node's colour (`NodeType.color`, item tint `skillcraft:heart`) and named after it ("Diamond Heart"). Its tooltip says what it attunes into and the level needed.
- **Where they drop** (`node/HeartEvents`):
  - **Logs** from a grown tree. The log must be part of a trunk with natural (non-persistent) leaves within 3 blocks of its top, so log cabins and placed logs don't count.
  - **Ores**, mined by a survival player with a tool that actually drops something (so no hearts from a wrong-tier pickaxe).
  - **Fish** reeled in with a vanilla fishing rod (at most one heart per catch). SkillCraft fishing spots don't drop hearts; they give XP instead.
- **Rates** are data-driven (`HeartDrop`):
  - Blocks: `data_maps/block/elemental_hearts.json`. Fish: `data_maps/item/elemental_hearts.json`.
  - Your level in the heart's skill adds up to **+50%** (0% at the node's level, rising to +50% at 99).
  - Higher tiers are rarer per hour of play. Rare ores have a higher chance per block, because the blocks themselves are rare.

  | Source | Chance | About one heart per… |
  |---|---|---|
  | Oak / birch logs | 0.8% | 125 logs (~25 trees) |
  | Spruce logs | 0.7% | 145 logs |
  | Acacia, mangrove, jungle logs | 0.55% / 0.5% / 0.45% | 180–220 logs (jungle trees are big) |
  | Cherry, dark oak, pale oak logs | 0.4% / 0.35% / 0.3% | 250–330 logs |
  | Crimson, warped stems | 0.2% | 500 stems |
  | Coal, copper, iron ore | 1% / 0.8% / 0.7% | 100–145 ores |
  | Nether quartz, redstone ore | 0.6% / 0.8% | 125–165 ores |
  | Gold, lapis ore | 1.2% | 85 ores |
  | Diamond ore | 4% | 25 ores |
  | Emerald ore | 6% | 17 ores |
  | Ancient debris | 8% | 12–13 blocks |
  | Amethyst cluster (Ley Heart, for Leycraft) | 3% | 33 clusters (one or two geodes) |
  | Cod / salmon / pufferfish / tropical fish | 2.5% / 5% / 5% / 40% | ~67 / 80 / 125 / 154 fish |

  Fish chances are balanced against how often each fish is caught, since vanilla fishing gives cod far more often than tropical fish. All of these are rough first numbers to tune by feel.
- **No exploits:**
  - Every block in the heart data map that a player places is remembered per chunk (attachment `placed_resources`, `node/PlacedResources`). Breaking it gives no heart and forgets it. That stops silk-touching an ore and placing it again, or re-placing logs.
  - Creative players never find hearts.
- **Feedback:** a found heart drops with the block's items, with a sparkle, an amethyst chime and a "You found a Diamond Heart!" message.
- **Tested** by the `elemental_hearts` GameTest: a tree log is eligible but a lone log isn't, 2,000 diamond-ore breaks land near the expected rate, placed ores never drop one, and the altar takes a heart but refuses diamonds.

## Attunement Altar and minigames
1. Right-click the altar with an **Elemental Heart** to load it. One heart fills it; you need that skill's level for the node. Plain resources are refused.
2. Right-click with an empty hand to start the minigame. Sneak + empty hand takes the heart back out.
3. **Success:** the node pops out and you get 5× that node's XP. **Failure:** the heart has a **25% chance to crack** (it's lost); otherwise it stays in the altar for another try. Leaving after you've started counts as a failure. On average that's the same loss as the old rule (25% of the materials).

Each loaded altar **belongs to the player who loaded it** until it's emptied. Only that player can add a heart, withdraw, attune, or break it while it's loaded.

Minigames are modular and **server-verified**:
- **Rules** (`altar/minigame/`, shared by client and server): `MinigameLogic` is the interface, `MinigameKind` names each one, `Minigames.create()` maps kind to implementation, and `NodeType.minigame()` picks one per node. All randomness comes from a seed, and time only moves through `tick()`, so the game is deterministic.
- **Drawing** (`client/minigame/MinigameRenderer`) is client-only.
- **Verification:**
  1. The server picks a random seed and sends it.
  2. The client plays and sends back every press and release with its tick number.
  3. The server replays the game with the same seed and inputs.
  4. A win only counts if the replay wins *and* the game couldn't have taken longer than the real time that passed. That rejects instantly computed "perfect" inputs.
- To add a minigame: write a `MinigameLogic`, add a `MinigameKind`, map it in `Minigames.create()`, and add a draw method to `MinigameRenderer`.

Current minigames:
- **Strike the Grain** (woodcutting): tap when the sweeping marker is in the zone. 5 hits needed, 2 misses allowed.
- **Temper the Vein** (mining): hold to build heat and release in the gold band. Overheating cracks it. 4 hits needed, 2 misses allowed.

- **Set the Hook** (fishing): watch the float and strike when a fish pulls it under. Striking at a nibble (a small twitch) or at still water is a miss, and so is letting a bite get away. Bites that pass before your first strike don't count, so closing the game without playing costs nothing. 5 hits needed, 2 misses allowed.

Higher tiers make every minigame faster and tighter (for Set the Hook: shorter bites and more nibbles).

## Fishing (spots on open water)
Fishing works like the other gathering skills, but a fishing spot is **not a block**. It's a patch of churning water (foam, ripples, bubbles and the odd fish breaking the surface) floating on a real pond, like the fishing spots in classic MMOs. You don't see a block outline when you look at it, and it can't be broken.

- **Getting spots:** attune a fish heart at the Attunement Altar (a Cod Heart → a **Cod Shoal**). Fish hearts come from vanilla rod catches (see **Elemental Hearts**). Use the shoal on open water to release it as a spot. The Relocator picks a spot back up as a shoal.
- **Water rules** (`fishing/FishingWater`). A bucket of water in a hole is not a fishing spot:
  - The tile must be a still water source with open air above it.
  - The water must be at least **2 blocks deep** under the spot.
  - At least **5 of the 8 tiles around it** must be open water, so spots can't sit in a 1-wide channel.
  - The pond must have enough connected open-water tiles (counted up to 8 blocks out): **12 + 6 per tier**, so cod needs 12, salmon 18, reef fish 24 and pufferfish 30.
  - Every 2 seconds the spot checks its water. If the pond is drained or built over, the shoal moves to another valid tile of the same pond, or pops back out as an item.
- **Fishing:** right-click the spot with a fishing rod. The player casts a line (the real bobber and line, landing on the spot) and keeps fishing automatically while they stay within about 6 blocks and hold the rod. Clicking the spot again, or using the rod (reeling in), stops.
  - An attempt every **1.5s**, 0.1s faster per rod tier, never faster than 0.8s. Each attempt rolls the same success curve as nodes. On a bite the bobber dips, the fish goes to your inventory, and you get full XP.
  - **Rods:** the vanilla fishing rod is tier 0, power ×1.0 (`gathering_tools.json`). **Lure** adds one tier per level (faster attempts) and **Luck of the Sea** adds +0.05 power per level (more bites). The rod is a Crafting Bench recipe at Crafting **1**, so anyone can start fishing.
  - Depletion and fatigue follow the node rules (`NodeType`). Depleting a spot doesn't leave a stump: **the shoal swims off** (it disappears) and **resurfaces on another tile of the same pond** that can be reached from the bank, after the tier's regrow time.
  - Spacing works like nodes (e.g. at most 6 cod spots within 8 blocks), counted with an entity search instead of the POI index.

| Spot | Level | XP | Attuned from | Smallest pond | Resurfaces after |
|---|---|---|---|---|---|
| Cod | 1 | 20 | Cod Heart | 12 tiles | 20s |
| Salmon | 10 | 45 | Salmon Heart | 18 tiles | 45s |
| Reef fish (tropical) | 20 | 80 | Tropical Heart | 24 tiles | 2m |
| Pufferfish | 30 | 120 | Puffer Heart | 30 tiles | 5m |

- **Vanilla fishing** (casting anywhere) still gives materials but **no Fishing XP**, the same as breaking world blocks. Fish above your Fishing level slip off the hook, and its treasure no longer includes gear such as bows and rods (`loot/StripCraftedGearModifier`). Tested by rolling the vanilla fishing table 400 times at levels 1 and 99: no salmon, reef fish or pufferfish at level 1, all of them at 99.
- **Code:** `fishing/` holds `FishingSpotEntity` (the spot), `ShoalItem` (placing it), `FishingWater` (water rules), `FishingSessions` (auto-fishing) and `FishingLine` (the bobber, a vanilla hook with the vanilla bite logic turned off). The drawing is in `client/fishing/FishingSpotRenderer`, and the ripple and foam textures come from `scripts/textures/GenFishingCookingTextures.java`.
- **Later:** bait and methods (nets, harpoons), spots that drift on their own every few minutes, sea and river spots generated with the world, custom fish for tiers V–X.

## Smithing, Cooking & Crafting stations
Modeled on survival games like RuneScape: Dragonwilds, Valheim and Enshrouded. There are **processing stations** (load materials, walk away) and **recipe benches** (pick from a recipe list, queue, the bench works over time). The vanilla crafting table stays for building materials. **Gear** can only be made at the new stations, and the matching vanilla recipes are switched off by `neoforge:never` overrides in `data/minecraft/recipe/`. The mod loads after NeoForge so these overrides beat NeoForge's own recipe copies.

### Smelter (Smithing, processing station)
Ore plus coal or charcoal makes ingots, one at a time. It runs on its own while the chunk is loaded. Coal is used when each ingot finishes. XP builds up and is collected with the ingots. The ore slot only accepts ores you have the level for. Vanilla furnaces can no longer smelt metal ores.

| Ore | Smithing level | Coal | XP | Time |
|---|---|---|---|---|---|
| Copper | 1 | 1 | 6.25 | 5s |
| Iron | 10 | 2 | 12.5 | 7s |
| Gold | 20 | 3 | 22.5 | 9s |
| Ancient debris → netherite scrap | 40 | 8 | 50 | 15s |

Data: `data/skillcraft/data_maps/item/smelting.json`.

### Cooking Range (Cooking, processing station)
Raw food plus any furnace fuel makes cooked food, one at a time, while the chunk is loaded. It works like the Smelter, with one difference: **food can burn**.
- **Burn chance** falls in a straight line from **50%** at the food's required level to **0%** at its **stop-burn level**. It's rolled when each item finishes, using the Cooking level of whoever loaded the food. Burnt food goes to its own slot, gives no XP and can't be eaten.
- Cooking XP builds up and is collected with the cooked food. The raw-food slot only accepts food you have the level for. Raw food tooltips show your current burn chance.
- **Vanilla furnaces, smokers and campfires no longer cook food.** Their food recipes are switched off with `neoforge:never` overrides, the same as metal ores. Villagers still sell some cooked food; cheap food isn't what Cooking is for, so those trades stay.
- Crafted from 3 bricks, a furnace and 5 cobblestone (`data/skillcraft/recipe/cooking_range.json`).

| Raw food | Cooks into | Cooking level | XP | Never burns at | Time |
|---|---|---|---|---|---|
| Kelp | Dried kelp | 1 | 5 | 10 | 2s |
| Potato | Baked potato | 1 | 10 | 20 | 3s |
| Cod | Cooked cod | 1 | 30 | 30 | 4s |
| Chicken | Cooked chicken | 5 | 35 | 35 | 4s |
| Rabbit | Cooked rabbit | 8 | 40 | 38 | 4s |
| Salmon | Cooked salmon | 10 | 50 | 40 | 4s |
| Mutton | Cooked mutton | 12 | 55 | 42 | 4s |
| Porkchop | Cooked porkchop | 15 | 60 | 45 | 4.5s |
| Beef | Steak | 20 | 70 | 50 | 4.5s |
| Tropical fish | **Seared Reef Fish** (new, 7 hunger) | 25 | 90 | 58 | 5s |
| Pufferfish | **Prepared Pufferfish** (new, 8 hunger, no poison) | 35 | 120 | 68 | 5.5s |

Data: `data/skillcraft/data_maps/item/cooking.json`. Code: `cooking/`. Tested in `gametest/CookingTests`: with 4 cod each, a level 1 cook burns some and a level 99 cook burns none, XP is paid only for unburnt food, and vanilla cooking recipes are gone.

**Later:** multi-ingredient dishes (stews, pies) at the range, cooking on Firemaking fires, cooking gauntlets, food that heals Constitution HP directly.

### Forging Anvil (Smithing) and Crafting Bench (Crafting): recipe benches
- The screen has a recipe grid (locked recipes dimmed), a details panel showing level, XP, time and ingredients (have/need), Make ×1 / ×5 / ×10 / All, and a queue with a progress bar and Cancel.
- **Queueing takes the ingredients right away.** The bench then works through up to 8 jobs on its own, putting results in 9 output slots and pausing if they're full.
- **Cancel** refunds exactly what's left of your jobs. Breaking the bench drops the reserved ingredients.
- **XP goes to whoever queued the job.** If they're offline, it's paid the next time they open the bench.
- Effects: anvil clinks and sparks, bench woodworking sounds.
- **Anvil:** within each metal, items unlock in this order: shovel, sword, hoe, axe, pickaxe, boots, helmet, leggings, chestplate. Copper 1–9, iron 10–18, gold 20–28, diamond 30–38. Netherite ingot at 40. Netherite upgrades at 41–49 (diamond item + ingot + template), and they keep enchantments.
- **Crafting Bench:** wooden and stone tools at level 1, so nobody is stuck without a pickaxe. Leather armor 5–11, fishing rod 1, bow 10, shield 15, crossbow 25.
- Data: `data/skillcraft/data_maps/item/bench_recipes.json`, generated by `scripts/gen_bench_recipes.sh`. That script also writes the vanilla recipe overrides.

### Gear only comes from players
- Bench-made gear is stamped **"Made by <player>"** (data component `skillcraft:crafted_by`).
- **World-generated chests** (`chests/...` loot tables) never contain station gear or smelted ingots/netherite scrap (global loot modifier `skillcraft:strip_crafted_gear`). Tested by rolling village weaponsmith and toolsmith chests 400 times: 477 pieces of gear normally, 0 with the modifier.
- **Mobs** never drop station gear unless it carries a player's stamp, i.e. the mob picked up player-made gear.
- **Villagers and wandering traders** never sell station gear or ingots. `scripts/gen_villager_trade_overrides.sh` rewrites the vanilla `villager_trade` tag pools without those trades (36 removed), so villagers still roll full trade lists from what's left. Trades that *buy* ingots stay.

## Combat (Phase 5)
Full rules and numbers: **[docs/COMBAT.md](COMBAT.md)**. In short:
- **Damage types:** stab, slash, crush (melee) and light, medium, heavy ammo (ranged).
  - Monsters take more or less damage from each (`combat_profiles` data map). Hits show "Effective" or "Resisted".
  - Sneak and right-click a monster with an empty hand to examine its weaknesses.
- **Typed armour:** each armour piece defends differently against stab, slash, crush and ranged (`armor_defences`).
  - Mail is good against slashes, plate against points and edges, and padded armour against blows and arrows.
  - Armour is gated by the new **Defence** skill.
- **Combat XP:** 4 XP per point of health actually removed from a mob, plus 1.33 Constitution XP.
  - **Attack styles** (key **V**) split the 4 XP: Aggressive = all Melee/Ranged (+5% damage), Controlled = half Melee/Ranged and half Defence, Defensive = all Defence (−5% damage taken).
  - There's no XP for damaging players, armour stands, babies or tamed animals, or for creative players. Spawner mobs give half.
- **Constitution:** +1 heart per 10 levels, and one more at 99 (20 → 40 HP).
- **Level bonuses:** Melee/Ranged add +0.25% damage per level, and Defence −0.15% damage taken per level.
- **New gear:**
  - warhammers (crush; copper → netherite)
  - light and heavy arrows
  - padded armour
  - spears and chainmail are now made at the stations too
- **Bosses:** [docs/BOSS_SLAGHEART.md](BOSS_SLAGHEART.md). The first boss, **Slagheart, the Forge Warden**, is fought in an instanced, indestructible arena dimension. See the doc for stances, phases, rewards and balance.

## Bank (OSRS-style)
- **One bank per player**, stored on the player (attachment `bank`). Every **Bank Chest** opens your own bank, and other players see theirs.
- **Each stackable item type uses one slot**, holding up to **2,147,483,647**. Unstackable items take a slot each. Items with different enchantments or damage are kept separate. **800 slots** by default (`bankSlots` in the config).
- **Interface** (familiar to OSRS players):
  - Brown and gold styling and an 8-column grid. Counts show top-left in yellow, then white "K" from 100K, then green "M" from 10M. Hover shows the exact amount.
  - Tabs: **All** plus up to 9. Drag an item onto a tab to file it, or onto **+** to make a new tab.
  - **Swap / Insert** rearrange modes. Drag items to reorder.
  - **Quantity 1 / 5 / 10 / X / All** sets what a left-click withdraws or deposits. X accepts "10k", "1.5m", "2b" and is remembered.
  - Right-click for **Withdraw-1 / 5 / 10 / X / All / All-but-1**, or **Deposit-1 / 5 / 10 / X / All** on inventory items.
  - Clicking an inventory item deposits it, and deposits gather that item from your whole inventory. **Inv** deposits your whole inventory, and **Worn** deposits armor and offhand.
  - **Find** filters by item name.
- The server validates every action (`bank/BankActions`). The client only gets a synced copy while the bank is open.

## Level gates (vanilla content)
Tunable without code in `src/main/resources/data/skillcraft/data_maps/`:
- `item/skill_requirements.json`: tools, weapons, ammo and armour.
  - Pickaxes need Mining. Axes need Woodcutting to chop *and* Melee to fight.
  - Swords, spears and warhammers need Melee. Crossbows and heavy arrows need Ranged. Armour needs Defence.
  - Tiers: copper 5, iron 10 (chainmail 10), gold 20, diamond 30, netherite 40 (vanilla gear caps around 50).
- `block/skill_resources.json`: the level needed to break each world log and ore.

## Technical architecture
- **Player skill data:** a NeoForge *data attachment* on the player stores XP for each skill (with decimals) and is saved with the player.
- **Sync:** the attachment syncs automatically, and only to its owner. A packet sends each XP gain for the HUD popups.
- **Server-authoritative:** all XP, level checks, gathering rolls and altar results are decided on the server. The client only draws screens and runs minigame input.
- **Data-driven content:** level requirements live in JSON data maps, so content can be tuned without code changes.

## Multiplayer notes / known gaps
- Done (3.5): attunement results are verified on the server by replaying the minigame from a seed, and altars are owned while loaded.
- Placed nodes have no owner: anyone can gather from them or pick them up with a Relocator. On servers, use a claim or protection mod, or add an "owned node" option later. Gathering fatigue already limits how much any one player can take.
- A bot that plays the minigame in real time can't be told apart from a skilled human, as in any game. Verification stops faked results, not automation.
- Not yet tested on a real dedicated server with two clients. The headless test server confirms the mod loads without client-only code.

## Roadmap
- [x] **Phase 1 – Foundation:** project setup, skill list, XP curve
- [x] **Phase 2 – Skill framework:** saved XP on each player, sync to client, XP drops and level-up messages, Skills screen (K), `/skill` admin command, level-gated vanilla tools/weapons/ores/logs with XP
- [x] **Phase 3 – Woodcutting & Mining:** skilling nodes (21 types, tiers I–VI), auto-gathering with success rolls, depletion and regrowth, Relocator, Attunement Altar with modular minigames, no XP from world blocks
- [x] **Phase 3.5 – Multiplayer hardening:** server-verified minigame results (seeded replay + timing check), altar ownership, per-player gathering fatigue
- [x] **Phase 4 – Smithing & Crafting:** Smelter, Forging Anvil, Crafting Bench, queued processing, vanilla gear recipes disabled, gear only from players (no chest/mob gear)
- [x] **Phase 4.5 – Bank:** OSRS-style per-player bank (Bank Chest), 2.1B stacks, tabs, search, quantities, rearranging
- [x] **Phase 5 – Combat + first boss:**
  - Melee, Ranged, Defence and Constitution
  - combat XP and attack styles
  - stab/slash/crush and ammo weights with monster weaknesses
  - typed armour, warhammers, light/heavy arrows, padded armour
  - Slagheart in an instanced arena dimension with phases, telegraphs, a unique drop and a resource table
- [x] **Phase 6 – Combat abilities:** (see [ABILITIES_SPEC.md](ABILITIES_SPEC.md))
  - a 4-slot ability bar with Momentum, cooldowns and a global cooldown
  - 27 abilities (Melee, Ranged, Defence, Weapon Arts including off-hand arts, Utility), unlocked by level or Codex
  - the Ability Book (J) with 3 saved bars
  - boss crowd-control rules (Daze, capped slows, no knockback)
  - a framework ready for Spells (item costs) and Prayers (toggles with drain)
- [x] **Phase 6.6 – Fishing & Cooking:** fishing spots on open water (cod, salmon, reef fish, pufferfish) with the Set the Hook minigame, shoals that swim off and resurface instead of stumps, vanilla fishing gated by level; the Cooking skill and the Cooking Range with burn chance; vanilla food cooking switched off
- [x] **Phase 6.7 – Elemental Hearts:** nodes and shoals are attuned from hearts found by felling grown trees, mining ores and rod fishing in the vanilla world (placed blocks never drop one); one heart per attunement, cracking 25% of the time on a failure; XP curve ×0.75 (99 = 9,775,823)
- [x] **Phase 6.8 – Leycraft:** a rune-making skill. Ley shards from a Ley Stone node are woven into motes at a Ley Altar; the altar weaves whichever element its surroundings resonate with (blocks, biome, weather, height). Quick weaves, or careful weaves with the Steady the Current minigame for up to ×1.5. Bench mote recipes removed ([LEYCRAFT.md](LEYCRAFT.md))
- [ ] **Phase 6.5 – More combat content:** higher-tier weapon/ammo/armour families, the remaining abilities (Undertow), more mid-tier bosses with niche uniques and Codices
- [x] **Phase 7 – Agility, Magic and Prayer:** (see [AGILITY_MAGIC_PRAYER.md](AGILITY_MAGIC_PRAYER.md))
  - Magic: wands and staves, elemental damage types, motes, bolts and 8 spells
  - Prayer: Faith, 13 blessings, the Votive Shrine and bone offerings
  - Agility: parkour XP, passive perks and 6 movement abilities
  - the Ability Book's Agility, Spells and Prayers tabs
- [ ] **Phase 7.5 – World generation** (was Phase 7; Agility, Magic and Prayer went first): new ores, tree types, structures and regions tied to skills; rare natural skilling nodes
- [x] **Phase 6.9 – Beastbonding** (replaced Sailing): the Bond Codex (bond, summon, store, release), 18 creatures from wolves to the new flying Cloudray, changed mobs (Cliffstrider, fetching fox, Ironward), level bonuses for mounts, death and rekindling at a Rekindling Shrine ([BEASTBONDING.md](BEASTBONDING.md))
- [ ] **Phase 8+:** Farming, Firemaking, Construction, more bosses; equipment slots (rings, amulets) with crafting phase two

## IP guidelines
- Mechanics and formulas may be inspired by OSRS.
- Do **not** copy Jagex names (items, NPCs, places, "Rune"-branded terms), textures, sounds or wiki text. Use original names and art.
