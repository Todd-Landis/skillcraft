# Boss: Slagheart, the Forge Warden

The first SkillCraft boss. It's a mid-level fight for **combat levels 25–40** in roughly **iron–diamond gear (tiers III–IV)**. The design goal: you win by reading telegraphs, moving and switching damage types, not by out-gearing it.

## Theme and lore (original)
Long before the villages, a guild of smiths dug the **Sunken Forge** deep underground to work metals hotter than any furnace could hold. To guard it they built a warden from blackened iron plates riveted around a **heart of living slag**. When the guild vanished, the warden kept the fires banked and waited.

Light a **Summoning Brazier** with an **Ember Sigil** and the warden wakes to drive you out. It's a hunched colossus about 3.5 blocks tall:
- a riveted chest with a furnace grille, behind which the slag-heart glows
- two **chest doors** that swing open when it vents
- a huge **anvil-headed hammer fist** on the right arm and **forge tongs** on the left
- a narrow visor slit glowing orange
- twin chimney stacks on its back that smoke as it heats up

## Research: mechanic patterns adapted (no names, art or text copied)
| Pattern seen in mid-level OSRS bosses | How Slagheart uses it |
|---|---|
| Telegraphed AoE with safe spots (giant mole/scurrius-style slams) | **Hammerfall** marks a ring on the floor before it lands |
| Style switching shown by stance (Zulrah / Demonic-gorilla-lite) | **Banked** and **Vented** stances change which damage types work |
| Vulnerability window after a big attack | It **vents** after every rotation, taking ×1.5 damage and stopping attacks |
| Hazard tiles (Sarachnis webs, Obor/Bryophyta ground effects) | **Ember Rain** turns floor cells to magma before they erupt |
| Add waves that change priorities | **Cinder Mites** stoke its fire: it takes half damage while they live |
| HP-threshold phases | 100–65% / 65–30% / 30–0% |
| A learnable rotation | Fixed attack order per phase, adjusted by range |

## Stats (data map `entity_type/boss_tuning.json`, tunable without code)
| Stat | Value |
|---|---|
| Health | 500 (+60% per extra party member, up to 4 players) |
| Armour | 8 (vanilla points) |
| Knockback resistance | 100% |
| Speed | 0.24 (a little slower than a player walking) |
| Size | 2.2 × 3.6 blocks |

Damage below is the raw value at Normal difficulty, before armour. It's scaled the usual vanilla way on Easy and Hard.

## Stances (the core mechanic)
| Stance | Look | Stab | Slash | Crush | Light | Medium | Heavy |
|---|---|---|---|---|---|---|---|
| **Banked** (default) | doors shut, dull red glow | 0.8 | **0.6** | **1.25** | 0.6 | 0.85 | **1.2** |
| **Vented** (window) | doors open, white-hot heart, steam | **1.5** | 1.2 | 1.0 | **1.4** | 1.2 | 1.0 |

Vented also stacks an extra **×1.5** vulnerability on top of the table.

- **Main weapon:** crush (warhammer or mace) or heavy arrows.
- **Burst weapon:** a spear or light arrows for the window.

Swapping is optional but rewarded: horizontal progression in action.

## Phases and attack rotation
Each attack has a **windup** (animation + sound + particles), then a **strike**. The boss turns to face its target at the start of a windup, then **locks its aim**, so stepping away during the windup always works.

### Phase 1: "Banked Fire" (100% → 65%)
Rotation: `Hammerfall → (Tong Sweep if close, else Slag Spit) → Hammerfall → Slag Spit → Vent (3s)`

### Phase 2: "Stoked" (65% → 30%)
- **Transition (2.5s, invulnerable):** a roar animation and a ravager-style roar. A heat pulse knocks back everyone within 6 blocks (4 damage). Then **Cinder Mites** spawn: 2 + 1 per extra player. They're small magma cubes with 8 HP each, weak to slash and resistant to crush.
- **While any mite lives, Slagheart is Stoked:** it takes **50% damage**, flames ring its body, and the mites' death sounds signal progress. The intended read is to kill the adds first, ideally with a sword.
- Rotation: `Hammerfall → Ember Rain → (Sweep/Spit) → Hammerfall → Slag Spit → Vent (2.5s)`

### Phase 3: "White Heat" (30% → 0%)
- **Transition (2.5s, invulnerable):** it leaps to the arena centre, and a second mite wave spawns.
- Windups are **25% faster**, and Hammerfall becomes a **double slam**.
- Rotation: `Forge Quake → Hammerfall ×2 → Ember Rain → Slag Spit → Tong Sweep/Spit → Vent (2s)`

### Soft enrage
- At **5:00** it warns in chat: "Slagheart's plates glow white."
- From **6:00**, every 60 s adds +20% damage and −10% windup time (up to 4 stacks).

So a fight can't be stalled forever, but a normal kill never sees it.

## Attacks
| Attack | Type | Telegraph (animation / sound / particles) | Strike | Counterplay |
|---|---|---|---|---|
| **Hammerfall** | crush | Hammer arm rises overhead over 1.5s. A rising anvil ring sound. A ring of flame particles (radius 3) appears where the target stood when the windup started. | 14 damage + upward knockback + Slowness II 1s within 3 blocks of the ring centre. Dust and lava burst. | Step out of the ring. 1.5s is about 6 blocks of walking. |
| **Tong Sweep** | slash | Tongs pull back across the body over 0.9s. A metallic scrape. Sweep particles across the 150° front arc. | 10 damage + knockback in a 150° cone, radius 5 | Back out past 5 blocks or circle behind it. The arc locks when the windup starts. |
| **Slag Spit** | ranged | The chest heart flares and the doors crack. Bellows/blaze inhale over 1.0s. Smoke from the chimneys. | Three slow, arcing slag globs at the target (one volley per player). 6 damage + 3s fire on hit. Where a glob lands, a 1-cell **slag puddle** (magma) lasts 4s. | Strafe sideways, since the globs are slow and visible. Stand behind a pillar to block line of sight. Don't stand in puddles. |
| **Ember Rain** (P2+) | fire | Both fists raised and slammed (1.25s). Floor cells flip to **magma** as the telegraph: 6 + 2 per player, always including one under each player. | 1.5s after the flip, the cells erupt in fire columns: 8 damage + 4s fire to anyone standing on one. The magma fades 2s later. | Step off glowing cells. |
| **Forge Quake** (P3) | crush | It crouches at the centre (1.0s). Rumbling and blast sounds. | Three shockwave rings expand at 8 blocks/s, 0.75s apart, shown as flame circles at floor level. 8 damage + knockback if you're **on the ground** as a ring passes. | **Jump** each ring. The ring is a thin band, so time the jump to its edge. |
| **Heat Pulse** (phase change) | fire | Roar animation | 4 damage + knockback within 6 blocks | Keep your distance at the 65% and 30% marks |
| **Vent** | — | Doors swing open, steam hisses, the heart turns white-hot | No attack for 3/2.5/2s. Takes ×1.5 damage plus the Vented stance table. | Punish! A spear or light arrows do the most here. |

In melee, Slagheart only uses Hammerfall and Tong Sweep. It never punches without a telegraph.

## Arena: the Sunken Forge (instanced)
- **Dimension:** `skillcraft:arena`. A void flat generator (one air layer, `the_void` biome, no features or structures) with a custom dimension type:
  - fixed time
  - no skylight, no weather, no ceiling
  - beds and respawn anchors don't work
  - nothing can spawn naturally (the void biome has no spawn list, and monster spawn light is 0)
- **Instances:** arena plots sit **1,024 blocks apart** along the X axis at y = 64. A plot is built from code (`arena/ArenaBuilder`) **every time an instance starts**, so whatever the last fight left behind (magma cells, dropped items, a stray mite) is wiped first. Plots are reused once freed.
- **Layout:**
  - a circular floor, radius 15, of polished deepslate and deepslate tiles, with a ring of forge-grate cells and a central anvil dais
  - **4 basalt pillars** at radius 9 for line-of-sight cover against Slag Spit
  - a 6-high blackstone-brick rim wall with shroomlight and lantern lighting
  - a solid **barrier dome** (walls and ceiling) so no one can leave the plot or pearl out
  - the **Exit Portal** at the south edge, where players arrive
- **Can't be damaged or built in** (`arena/ArenaProtection`, the whole `skillcraft:arena` dimension), for everyone except creative-mode operators:
  - block breaking and placing are cancelled
  - interacting with blocks is denied (except the reward chest and the exit portal), as are bucket and flint-and-steel use
  - explosions keep their damage but break no blocks
  - fluid placement is cancelled
  - farmland trampling is cancelled
  - the boss's own floor effects are applied by code, which the protection doesn't block
- **Entry:**
  1. Craft a **Summoning Brazier** (crafting table) and place it anywhere outside the arena.
  2. Forge an **Ember Sigil** at the Forging Anvil (Smithing 20: 3 iron ingots, 1 gold ingot, 6 coal, 4 redstone dust).
  3. Right-click the brazier with the sigil. The sigil is used up, and **you plus up to 3 players standing within 5 blocks of the brazier** are taken into a fresh instance.
  4. Slagheart rises over 3 seconds (an awaken animation). The fight starts after that.
- **Ownership:** each instance belongs to the player who used the sigil. Only its party can be in it. There's no way to walk into someone else's plot (1,024 blocks apart and walled with barriers), and the arena dimension can't be reached any other way (no portals; only ops can `/tp` there).
- **Exit rules:**
  - **Victory:** the boss collapses (death animation), then a **Reward Chest** and the **Exit Portal** glow. Each eligible player right-clicks the chest for their own loot, then right-clicks the portal to leave. After 90s everyone left is sent home automatically, and **unclaimed loot goes into their inventory** (overflow drops at their feet back home).
  - **Death:** you keep your items and **lose the attempt**, as in OSRS safe-death boss rooms. Death is cancelled: you're healed to full, cleared of fire and effects, and sent back to where you entered. Your share of the attempt ends (no loot).
  - **Leaving early** (Exit Portal mid-fight): forfeits the attempt.
  - **Logout or crash mid-fight:** you're taken out of the instance at once. The return point is saved on the player (attachment `arena_return`), so at your next login in the arena dimension you're moved back. If that point is gone, it falls back to your respawn point or world spawn. Nobody can get stuck.
  - When the **last player leaves**, the instance closes: the boss, mites and globs are discarded, and the plot is freed.
- **Server restart:** instances live only in memory and are never saved. After a restart there are no active instances. Any boss or mite entity that loads in the arena dimension with an unknown instance id removes itself on its first tick, and players are moved home when they log in.

## Rewards
Each **eligible** participant gets their **own roll** of the reward table. To be eligible you must have dealt at least **5% of the boss's max health**, which stops people leeching from a party. Loot is rolled from `loot_table/gameplay/slagheart_rewards.json`.

### Unique: the Emberfall Maul (1/120 per eligible player per kill)
A two-handed slag-iron maul with a real identity: **party-support burst crush**.
- **Stats:** 12 attack damage (netherite sword 8, netherite axe 10), 0.7 attacks/s, crush. Requires **Melee 45** (tier V, with tier VI hitting power).
- **Sunder:** a fully charged hit (attack strength ≥ 90%) applies **Sundered** to the target for 6 s. Sundered targets take **+15% damage from every source**, so your whole party benefits.
- **Tradeoffs:**
  - **Weighty:** −10% movement speed while it's in your main hand.
  - **Two-handed:** you can't raise a shield while holding it.
  - Its sustained DPS (8.4 at full charge) is *below* a netherite sword's (12.8), so it isn't strictly better. It's for armoured, crush-weak targets and team fights.
- **Stamp:** it's marked "Won from Slagheart by \<player\>" (component `skillcraft:earned_by`), so the gear rules treat it as earned.
- **Rarity, and why 1/120:** a kill takes about 2.5–3.5 min plus 1–2 min of setup (sigil crafting and walking), so about 10–12 kills an hour. At 1/120 the median player gets it in ~7 h and the average in ~10–12 h. That's a satisfying mid-tier chase that's shorter than OSRS mid-boss uniques (≈1/100–1/400 at faster kills), because this is the mod's *first* boss and gates a Melee 45 weapon, not end-game.

### Resource table (per eligible player per kill)
| Pool | Rolls | Contents |
|---|---|---|
| Fuel | 1 (always) | 6–10 coal |
| Main | 2 | raw iron 4–8 (w18), raw gold 3–5 (w14), raw copper 9–13 (w10), redstone 6–12 (w10), lapis 4–8 (w8), quartz 5–9 (w6), dark oak logs 6–12 (w8), cherry logs 6–12 (w5), pale oak logs 4–8 (w5), amethyst shards 3–6 (w5), diamond 1–2 (w6), emerald 1 (w3), ancient debris 1 (w1) |
| Seeds | 1 at 50% | wheat / beetroot / pumpkin / melon seeds 4–8, torchflower seeds 1–2, pitcher pod 1 |
| Sigil refund | 1 at 1/20 | Ember Sigil (your next attempt is free) |
| Unique | 1 at 1/120 | Emberfall Maul |

There are **no ingots and no vanilla gear**: raw ores only, so Smithing is still needed. The table also goes through the global loot modifier rules.

### Balance against gathering (one "gathering-minute" = what a player at the resource's required level gathers in a minute at the fatigue-limited rate in DESIGN.md)
Measured with the loot-rate GameTest (`gametest/LootRateTests`, 20,000 rolls), which fails if a kill drifts outside 6.5–9 gathering-minutes. See **Verified numbers** below.

> **Decision (rebalance):** the first draft of the main pool (about 1.8× these stack sizes) measured **12.74 gathering-minutes** per kill, about 1.7× the target, and would have made bossing the best way to get every resource on the table. The main-pool stacks were cut to about 0.55× and the weights left alone, so variety per kill is unchanged.

- One kill pays about **7–8 gathering-minutes** (measured: see below) of mixed mid-tier resources. At ~11 kills an hour that's about 1.4× the resource rate of gathering one resource at its required level.
- The sigil (~3 gathering-minutes of ore + coal + smelting) has to come out of that, which brings the net to about **1.0–1.2×**. The kill's combat XP (~2,700) and the unique chance sit on top.
- So bossing is a **good** way to stock up on a *variety* of resources but **not a dominant** way to get any *single* one. For example, diamonds come at ~2 per hour from bossing versus 40 per hour mining at level 30.

### Combat XP
There's no separate kill bonus. Damage XP through the normal system works out to about **2,000 Melee/Ranged/Defence + 667 Constitution per solo kill**. That's roughly one level per kill around level 35, which is comparable to an OSRS mid-boss.

## Expected kill time and death risk (solo, at the target level)
- **Combat 30, diamond warhammer** (9 damage, 1.0/s):
  - × 1.25 crush matchup × 1.07 level bonus ≈ 12 DPS at full uptime.
  - Real uptime of about 50% (dodging windups, waiting out transitions, half damage while mites live) brings that to ~6 DPS.
  - Venting adds ×1.5 for about 15% of the fight.
  - **Kill ≈ 2–2.5 min.**
- **Combat 25, iron sword only** (slash ×0.6): ~2.9 DPS effective, so **≈ 3.5–4 min**. That's still doable, but the stance table tells you to bring a crush weapon.
- **Death risk:**
  - **Iron plate:** Hammerfall hits for ~9–10 after armour, about 3 mistakes from 26 HP.
  - **Padded:** ~8.6 per Hammerfall (crush), but Tong Sweep (slash) hurts more.
  - **Diamond:** ~5.
  - A player who reads the telegraphs takes only Slag Spit chip damage and Ember Rain mistakes. Food matters. We expect 1–2 deaths while learning the fight and very few after.

## Model, animation and effects
- **Model:**
  - `client/boss/SlagheartModel`, a Java `LayerDefinition` with 30+ cubes on a 256×128 texture
  - bones: root → hips → torso → (head, chest doors L/R, heart, chimneys L/R, shoulders → arms → forearms → hammer / tongs) and legs → feet
  - an **emissive layer** (`slagheart_glow.png`) so the heart, visor and grille cracks glow in the dark. Its brightness follows the stance.
- **Animations:**
  - vanilla keyframe `AnimationDefinition`s loaded from JSON through NeoForge's animation loader (`assets/skillcraft/neoforge/animations/entity/slagheart/*.json`)
  - clips: `idle`, `walk`, `awaken`, `hammerfall`, `tong_sweep`, `slag_spit`, `ember_rain`, `forge_quake`, `vent`, `roar` (phase change) and `death`
  - **no extra dependency:** we checked GeckoLib and decided against it. Vanilla keyframes plus NeoForge's loader cover everything, and Todd can edit the JSON.
- **Textures:** `scripts/textures/GenSlagheartTexture.java` generates the base and glow textures procedurally:
  - riveted plates, soot, hot cracks, the grille and the visor
  - it uses the same cube list as the model, so every face is painted with its own bevel
  - re-run it with `java scripts/textures/GenSlagheartTexture.java`, or paint over the PNGs
- **Boss bar:** a vanilla boss event, red with notched 10-segment progress, shown to the instance's players only. The name shows the phase.
- **Sounds:** new events in `sounds.json` (`slagheart.*`: awaken, step, hammer_windup, hammer_slam, sweep, spit, ember, quake, vent, roar, hurt, death). They currently point at vanilla audio (anvil, ravager, blaze, iron golem, basalt) and are ready for custom `.ogg` files.

## How it fits the tiers
- **Entry:** tier II–III materials (iron, gold, redstone), crafted at Smithing 20.
- **The fight:** tuned for tier III–IV gear.
- **Rewards:** a tier III–IV resource mix, plus a tier V unique that's useful up to tier VI.

It teaches the combat triangle (crush for the shell, stab for the heart, slash for the adds) before the mod's custom tiers VI–X arrive.

## Verified numbers
From `./gradlew runGameTestServer` in CI (GitHub Actions, build run on commit `ae58cdb`). All 11 required GameTests pass.

**Loot harness** (`gametest/LootRateTests`, 20,000 rolls, one roll = one eligible player's kill):

| Item | Per kill | Gathering-minutes |
|---|---|---|
| coal | 8.02 | 1.20 |
| raw iron | 2.18 | 0.65 |
| raw copper | 2.23 | 0.33 |
| raw gold | 1.14 | 0.68 |
| redstone | 1.81 | 1.09 |
| lapis | 0.96 | 0.58 |
| quartz | 0.88 | 0.26 |
| dark oak logs | 1.43 | 0.86 |
| cherry logs | 0.92 | 0.55 |
| pale oak logs | 0.61 | 0.91 |
| amethyst shards | 0.44 | – |
| diamond | 0.185 | 0.28 |
| emerald | 0.058 | 0.29 |
| ancient debris | 0.022 | 0.11 |
| seeds (all kinds) | 2.63 | – |
| **Total** | | **7.79** |

- **Emberfall Maul:** 183 in 20,000 = 1/109 (target 1/120; expected 167 ± 13, so within 1.3 sd). Every one was stamped `earned_by`.
- **Ember Sigil refund:** 1,045 in 20,000 = 1/19.1 (target 1/20).
- **Gear rules:** no ingot or other smelted material and no unstamped gear in any of the 20,000 rolls (asserted per stack).
- **Rebalance:** the first table measured 12.74 gathering-minutes. See the decision note under *Balance against gathering*.

**Boss fight** (`boss_phases`, `boss_stances`):
- A 10-damage hit during phase 2 with mites alive, while stoked, landed for 5.0.
- Phase 3 starts at 145 / 500 health (29%) on tick 160 of the scripted fight.
- Stances: banked ×1.25 crush / ×0.6 slash and vented ×1.5 stab, read from `boss_tuning.json`.
- The boss starts in its awaken animation and is invulnerable during it. Phase 2 starts below 65% with a roar and mites, and the boss is stoked while the mites live.

**Arena** (`arena_*`):
- The `skillcraft:arena` dimension type loads.
- Survival players can't break blocks, place blocks or blow anything up there.
- Enter, then exit, returns you to where you started, closes the instance and removes the boss.
- Death in the arena is cancelled: you're healed and sent home, and the instance closes. On logout you're removed from the instance and sent home at your next login.
- A dedicated server started with the mod logs `Arena dimension skillcraft:arena loaded`. The GameTest server only creates vanilla dimensions, so the arena tests use the Nether as a stand-in (`ArenaDimension.useStandInForTests`).

**Combat** (`combat_matchups`, `combat_xp`, `constitution`):
- Data maps resolve:
  - skeletons take ×1.4 from crush and ×0.7 from stab
  - warhammers crush, swords slash, spears stab
  - a full padded set gives +18% crush defence
- A survival player hitting a zombie gets Melee XP that follows the damage dealt, at a 3:1 Melee to Constitution ratio (4 : 1.33 per damage point).
- Baby mobs give no XP. (The spawner and creative rules are code paths that aren't covered by a test.)
- Constitution bonus: +0 HP at level 1, +6 at 35, +20 at 99. Max health is 28 at Constitution 40.
