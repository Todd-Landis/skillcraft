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
| Defence | Combat | Armour | Gates armour; typed armour defends against stab/slash/crush/ranged |
| Constitution | Combat | Health | Max HP scales with level; XP comes from dealing damage |
| Mining | Gathering | Ores | Level-gated ores, ore veins that deplete and respawn, pickaxe tiers, rare gems |
| Woodcutting | Gathering | Trees | Level-gated tree types (oak → willow → maple → yew → magic-style equivalents), stumps that regrow, success chance per swing |
| Fishing | Gathering | Fishing rod | Fishing spots that move, method/bait per fish, level-gated catches |
| Farming | Gathering | Crops | Growth stages, disease and protection, herb/tree patches, compost tiers |
| Smithing | Artisan | Furnace/anvil | Smelt bars → forge items at a custom anvil, quality affected by level |
| Crafting | Artisan | Crafting table | Leather, jewellery, glass; level-gated recipes |
| Firemaking | Artisan | Fire | Log tiers burn for XP; campfires used for cooking and buffs |
| Construction | Artisan | Building | Blueprint-based furniture and utility blocks (bank chest, altars, teleports) built from planks and bars |
| Agility | Support | Parkour | Agility courses with obstacles; unlocks shortcuts and movement perks |
| Sailing | Support | Boats | Upgradeable ships, sea regions and island content |

Planned for later: Magic, Prayer, Cooking, Herblore, Fletching, Thieving, Slayer, Hunter, Runecrafting (renamed equivalents).

## XP curve
OSRS formula. Level 99 = 13,034,431 XP. XP is capped at 200M. A server config multiplier (`xpMultiplier`) scales all gains. Implemented in `skill/XpTable.java`.

## Tiers
Content is grouped into 10-level tiers: **I** = 1, **II** = 10, **III** = 20 … **X** = 90. **Vanilla content tops out around level 50 (tiers I–V).** Tiers VI–X are reserved for custom woods, ores, metals and gear.

| Tier | Level | Woodcutting | Mining | Gear |
|---|---|---|---|---|
| I | 1 | Oak, birch, spruce | Coal, copper | Wood, stone, copper |
| II | 10 | Jungle, acacia, mangrove | Iron, quartz | Iron |
| III | 20 | Cherry, dark oak | Redstone, gold, lapis | Gold |
| IV | 30 | Pale oak | Diamond | Diamond |
| V | 40 | Crimson, warped | Emerald, ancient debris | Netherite (vanilla gear caps ~50) |
| VI–X | 50–90 | *custom* | *custom* | *custom* |

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
- **Getting nodes:** craft them at the **Attunement Altar** from their own resource (e.g. 32 oak logs make an oak node). Rare natural nodes come in Phase 7 world generation.
- Node stats (level, XP, drop, cost, depletion, regrow time) live in `node/NodeType.java`. Block models are generated by `scripts/gen_node_assets.sh`.

## Attunement Altar and minigames
1. Right-click the altar with the resource to load it.
2. When it's full, right-click with an empty hand to start the minigame. Sneak + empty hand takes the materials back out.
3. **Success:** the node pops out and you get 5× that node's XP. **Failure:** 25% of the materials are lost. Leaving after you've started counts as a failure.

Each loaded altar **belongs to the player who loaded it** until it's emptied. Only that player can add materials, withdraw, attune, or break it while it's loaded.

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

Higher tiers make every minigame faster and tighter.

## Smithing & Crafting stations
Modeled on survival games like RuneScape: Dragonwilds, Valheim and Enshrouded. There are **processing stations** (load materials, walk away) and **recipe benches** (pick from a recipe list, queue, the bench works over time). The vanilla crafting table stays for building materials. **Gear** can only be made at the new stations, and the matching vanilla recipes are switched off by `neoforge:never` overrides in `data/minecraft/recipe/`. The mod loads after NeoForge so these overrides beat NeoForge's own recipe copies.

### Smelter (Smithing, processing station)
Ore plus coal or charcoal makes ingots, one at a time. It runs on its own while the chunk is loaded. Coal is used when each ingot finishes. XP builds up and is collected with the ingots. The ore slot only accepts ores you have the level for. Vanilla furnaces can no longer smelt metal ores.

| Ore | Smithing level | Coal | XP | Time |
|---|---|---|---|---|
| Copper | 1 | 1 | 6.25 | 5s |
| Iron | 10 | 2 | 12.5 | 7s |
| Gold | 20 | 3 | 22.5 | 9s |
| Ancient debris → netherite scrap | 40 | 8 | 50 | 15s |

Data: `data/skillcraft/data_maps/item/smelting.json`.

### Forging Anvil (Smithing) and Crafting Bench (Crafting): recipe benches
- The screen has a recipe grid (locked recipes dimmed), a details panel showing level, XP, time and ingredients (have/need), Make ×1 / ×5 / ×10 / All, and a queue with a progress bar and Cancel.
- **Queueing takes the ingredients right away.** The bench then works through up to 8 jobs on its own, putting results in 9 output slots and pausing if they're full.
- **Cancel** refunds exactly what's left of your jobs. Breaking the bench drops the reserved ingredients.
- **XP goes to whoever queued the job.** If they're offline, it's paid the next time they open the bench.
- Effects: anvil clinks and sparks, bench woodworking sounds.
- **Anvil:** within each metal, items unlock in this order: shovel, sword, hoe, axe, pickaxe, boots, helmet, leggings, chestplate. Copper 1–9, iron 10–18, gold 20–28, diamond 30–38. Netherite ingot at 40. Netherite upgrades at 41–49 (diamond item + ingot + template), and they keep enchantments.
- **Crafting Bench:** wooden and stone tools at level 1, so nobody is stuck without a pickaxe. Leather armor 5–11, fishing rod 5, bow 10, shield 15, crossbow 25.
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
- [ ] **Phase 6 – More combat content:** higher-tier weapon/ammo/armour families, special attacks, more mid-tier bosses with niche uniques
- [ ] **Phase 7 – World generation:** new ores, tree types, structures and regions tied to skills; rare natural skilling nodes
- [ ] **Phase 8+:** Fishing, Farming, Firemaking, Construction, Agility, Sailing, more bosses

## IP guidelines
- Mechanics and formulas may be inspired by OSRS.
- Do **not** copy Jagex names (items, NPCs, places, "Rune"-branded terms), textures, sounds or wiki text. Use original names and art.
