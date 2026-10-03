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
| Melee | Combat | Swords, axes | Attack styles (accurate/aggressive/defensive), weapon speed tiers, special attacks |
| Ranged | Combat | Bows, crossbows | Ammo tiers, range and draw-speed tradeoffs, positioning |
| Constitution | Combat | Health | Max HP scales with level; XP comes from taking and dealing damage |
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
Content is grouped into 15-level tiers: **I** = 1, **II** = 15, **III** = 30, **IV** = 45, **V** = 60, **VI** = 75, **VII** = 90.

| Tier | Woodcutting | Mining |
|---|---|---|
| I (1) | Oak, birch, spruce | Coal, copper |
| II (15) | Jungle, acacia, mangrove | Iron, quartz |
| III (30) | Cherry, dark oak | Redstone, gold, lapis |
| IV (45) | Pale oak | Diamond |
| V (60) | Crimson, warped | Emerald |
| VI (75) | *custom wood (later)* | Ancient debris |
| VII (90) | *custom wood (later)* | *custom ore (later)* |

## Gathering: world vs. nodes
- **World blocks give no XP.** Breaking logs and ores in the world is purely for materials, and higher-tier blocks still need the level to break.
- **Skilling nodes are how gathering skills are trained.**
  - Place a node and right-click it with an axe or pickaxe. You then gather automatically every 1.5s while you stay within about 4 blocks.
  - Each attempt can fail. Success chance rises with level and tool tier (gold is fastest, then netherite, diamond, iron, stone, wood).
  - A success gives full XP and one item. It also has a chance to **deplete** the node into a stump or empty rock, which **regrows** after a time set by tier.
  - **Scarcity limits (per tier)** keep high-tier resources from becoming infinite. A player can only work one node at a time, so long regrow times mean needing many nodes, and spacing makes those nodes cost land and walking time:

    | Tier | I | II | III | IV | V | VI | VII |
    |---|---|---|---|---|---|---|---|
    | Regrow | 20s | 45s | 2m | 5m | 8m | 12m | 15m |
    | Max same-type nodes | 6 | 4 | 3 | 2 | 1 | 1 | 1 |
    | …within radius | 8 | 12 | 16 | 24 | 32 | 48 | 64 |

    Spacing is checked on placement using Minecraft's point-of-interest index (`registry/ModPoiTypes`, `node/NodePlacement`). Different node types don't count against each other.
  - **Gathering fatigue (per player, per resource type).** Every successful gather from any node adds 1 to that player's meter for that resource. The meter drains continuously. When it's full, the player can't gather that resource until it drains, and a message shows the wait. There's a warning at 75%. The limit is personal, so it doesn't matter whose nodes you use or how dense they are. In multiplayer, a high-level player's dense farm doesn't let lower-level friends out-gather their own limits. Spacing rules are the same for everyone.

    | Tier | I | II | III | IV | V | VI | VII |
    |---|---|---|---|---|---|---|---|
    | Sustained rate at required level (/hr) | 400 | 200 | 100 | 40 | 20 | 8 | 4 |
    | …at level 99 (3×) | 1200 | 600 | 300 | 120 | 60 | 24 | 12 |

    Burst capacity is 15 minutes' worth (minimum 2). The meter is saved on the player (`node/GatheringFatigue`, attachment `gathering_fatigue`). Node tooltips show your personal limit.
  - **Depletion chance scales with level:** `deplete = 1 − (1 − floor) × progress`.
    - `progress` runs from 0 at the node's required level to 1 at level 99.
    - `floor = 25% + 12% × (tier − 1)`: I 25%, II 37%, III 49%, IV 61%, V 73%, VI 85%, VII 97%.
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

Minigames are modular. `client/minigame/Minigame` is the interface, `MinigameKind` names each one, `Minigames.create()` maps kind to implementation, and `NodeType.minigame()` picks one per node. Current minigames:
- **Strike the Grain** (woodcutting): tap when the sweeping marker is in the zone. 5 hits needed, 2 misses allowed.
- **Temper the Vein** (mining): hold to build heat and release in the gold band. Overheating cracks it. 4 hits needed, 2 misses allowed.

Higher tiers make every minigame faster and tighter.

## Level gates (vanilla content)
Tunable without code in `src/main/resources/data/skillcraft/data_maps/`:
- `item/skill_requirements.json`: tools and weapons. Pickaxes need Mining, axes need Woodcutting to chop *and* Melee to fight, swords need Melee, crossbows need Ranged. Tiers: copper 5, iron 10, gold 15, diamond 30, netherite 50.
- `block/skill_resources.json`: the level needed to break each world log and ore.

## Technical architecture
- **Player skill data:** a NeoForge *data attachment* on the player stores XP for each skill (with decimals) and is saved with the player.
- **Sync:** the attachment syncs automatically, and only to its owner. A packet sends each XP gain for the HUD popups.
- **Server-authoritative:** all XP, level checks, gathering rolls and altar results are decided on the server. The client only draws screens and runs minigame input.
- **Data-driven content:** level requirements live in JSON data maps, so content can be tuned without code changes.

## Multiplayer notes / known gaps
- The attunement minigame result is reported by the client. A modified client could claim success. Fix (planned): the server sends a random seed, the client sends back its press timings, and the server replays the minigame to verify them.
- Altars have no owner, so anyone can withdraw materials someone else loaded. Planned: lock withdrawal to the player who loaded it.
- Node gathering has no per-player ownership. On servers you may want claim or protection mods, or a later "owned node" option.

## Roadmap
- [x] **Phase 1 – Foundation:** project setup, skill list, XP curve
- [x] **Phase 2 – Skill framework:** saved XP on each player, sync to client, XP drops and level-up messages, Skills screen (K), `/skill` admin command, level-gated vanilla tools/weapons/ores/logs with XP
- [x] **Phase 3 – Woodcutting & Mining:** skilling nodes (21 types, tiers I–VI), auto-gathering with success rolls, depletion and regrowth, Relocator, Attunement Altar with modular minigames, no XP from world blocks
- [ ] **Phase 3.5 – Multiplayer hardening:** server-verified minigame results, altar ownership
- [ ] **Phase 4 – Smithing & Crafting:** bars, forging, gear recipes with level requirements
- [ ] **Phase 5 – Combat:** Melee, Ranged and Constitution; equipment requirements; attack styles
- [ ] **Phase 6 – First boss:** arena, phases, telegraphed attacks, unique drops
- [ ] **Phase 7 – World generation:** new ores, tree types, structures and regions tied to skills; rare natural skilling nodes
- [ ] **Phase 8+:** Fishing, Farming, Firemaking, Construction, Agility, Sailing, more bosses

## IP guidelines
- Mechanics and formulas may be inspired by OSRS.
- Do **not** copy Jagex names (items, NPCs, places, "Rune"-branded terms), textures, sounds or wiki text. Use original names and art.
