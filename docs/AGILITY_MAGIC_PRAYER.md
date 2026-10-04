# Agility, Magic and Prayer (Phase 7)

**Status:** built on `claude/fervent-archimedes-ijjnlf`, on top of Phase 6 (combat abilities). The numbers here are the shipped defaults. Almost all of them are JSON or config, so they can be tuned without code.

Three skills plug into the Phase 6 ability bar:

- **Agility** unlocks movement abilities and passive perks. You train it by moving well: climbing, gap jumps and high dives.
- **Magic** adds a third combat style next to melee and ranged.
  - **Wands** and **staves** fire bolts and cast **spells**: elemental damage, area attacks, crowd control, and utility like Blink, a ward and a home teleport.
  - Spells cost **motes**, crafted consumables. An elemental staff supplies its own element for free.
- **Prayer** adds **blessings**: toggles that drain a new resource, **Faith**, while they are on. You train it by burying bones, or by offering them at a **Votive Shrine** for more XP. The shrine also refills Faith.

All names, art and text are original. OSRS was a reference for mechanics only: elemental staves supplying their element, prayer points draining while prayers are on, and bones at an altar.

## 1. Shared framework changes

### 1.1 New skills
- `MAGIC` and `PRAYER` join the combat skills (Melee, Ranged, Magic, Defence, Prayer, Constitution).
- Skills are saved by name, so existing saves keep their XP.
- `AGILITY` already existed (Support category) and now gets its mechanics.

### 1.2 Disciplines and elements
- Every damage type now belongs to a **discipline**: melee (stab, slash, crush), ranged (light, medium, heavy) or **magic**.
  - The discipline decides which skill gets the combat XP and the level damage bonus: Melee, Ranged or **Magic**.
- **Four magic damage types (elements)** join the combat triangle:

| Element | Colour | Theme | Notes |
|---|---|---|---|
| **Fire** | yellow | burning | Fire spells also set targets alight. Nether mobs resist it. |
| **Frost** | blue | cold | Frost spells slow. Fire creatures are weak to it. |
| **Storm** | white | lightning | Chains between targets. Water creatures are weak to it. |
| **Arcane** | light purple | raw magic | Few resist it. Witches, evokers and the warden do. |

- **Armour:** armour has a new defence group, `magic`, in `armor_defences.json`. No armour has magic defence yet, because no vanilla mob deals typed magic damage. It's ready for magic-using bosses.
- **Mob weaknesses** to elements are in `combat_profiles.json`. Highlights:
  - blazes and magma cubes: fire ×0.25, frost ×1.5
  - strays: frost ×0.5, fire ×1.3
  - drowned and guardians: storm ×1.4
  - spiders: fire ×1.3
  - undead (zombies, skeletons): fire ×1.2
  - witches: arcane ×0.5
- **Slagheart:** its stances now include the elements.
  - Banked: fire ×0.25, frost ×1.3.
  - Vented: fire ×0.5, frost ×1.5.

### 1.3 The Ability Book and categories
- **Tabs:** Melee, Ranged, Defence, Weapon Arts, **Agility** (new), **Spells**, **Prayers**, Utility. Spells and Prayers are no longer locked.
- A tab with nothing in it is hidden. Utility is empty now that Sprint Burst has moved to Agility.
- **Toggles name the resource they drain** (`"resource": "momentum"` or `"faith"`). They can also share an **exclusive group**: turning on a blessing turns off any other blessing in its group.

### 1.4 How spell damage scales
- Melee abilities deal a share of your weapon's hit; spells deal a share of your **focus's power**.
  - `AbilityDamage` picks the right base from the damage type: a magic element uses the power of the wand or staff in your main hand.
  - So the existing area behaviours (`ground_slam`, `cleave`, `line_thrust`, `melee_strike`) also work as spells. Frost Nova is a `ground_slam` with `"style": "frost"`.

## 2. Magic

### 2.1 Foci: wands and staves
A focus is any item listed in `data_maps/item/magic_focus.json`:
- `kind`: `wand` or `staff`
- `power`: the hit of one bolt
- `element`: optional
- `cast_cooldown`: ticks between bolts

| Focus | Kind | Element | Power | Bolt every | Magic req. | Made at (Crafting) | Materials |
|---|---|---|---|---|---|---|---|
| **Apprentice Wand** | wand | arcane | 4 | 0.8 s | 1 | Crafting Bench 5 | stick, 2 amethyst shards |
| **Ember Staff** | staff | fire | 8 | 1.2 s | 10 | Crafting Bench 12 | 2 sticks, 2 amethyst, 6 coal |
| **Rime Staff** | staff | frost | 8 | 1.2 s | 15 | Crafting Bench 16 | 2 sticks, 2 amethyst, 4 snow blocks |
| **Tempest Staff** | staff | storm | 8.5 | 1.2 s | 20 | Crafting Bench 22 | 2 sticks, 3 amethyst, a lightning rod |
| **Arcanist's Wand** | wand | arcane | 6.5 | 0.7 s | 30 | Crafting Bench 30 | blaze rod, 4 amethyst, 6 lapis, 2 gold ingots |
| **Starseer Staff** | staff | arcane | 11 | 1.0 s | 40 | Crafting Bench 40 | 2 blaze rods, 6 amethyst, 2 diamonds, an eye of ender |

- **Wands** are quick and one-handed, so the off hand stays free for a shield or a torch.
- **Staves** hit harder per cast and **supply their element**: an Ember Staff makes fire spells and fire bolts cost no Ember Motes.
- **Gear rules:** foci are stamped `crafted_by` like all station gear, and the Magic level gate sits in `skill_requirements.json`.
- **Speed check:** bolt damage per second at the focus's own level, on the Aggressive style, against a neutral target:

  | Focus | Level | DPS |
  |---|---|---|
  | Apprentice Wand | Magic 1 | 5.3 |
  | Ember Staff | Magic 10 | 7.2 |
  | Rime Staff | Magic 15 | 7.2 |
  | Tempest Staff | Magic 20 | 7.8 |
  | Arcanist's Wand | Magic 30 | 10.5 (it costs a mote per bolt) |
  | Starseer Staff | Magic 40 | 12.7 |

  - For comparison, an iron sword is about 9.6 DPS and a bow about 6–9. A diamond sword at Melee 40 measured 14.8 in the ability_dps harness.
  - Magic trades some damage for range, elements and area spells; elemental matchups can swing it a lot.

### 2.2 Motes (the spell reagents)
Motes are crafted at the Crafting Bench, 16 per craft. They're in the `skillcraft:consumables` tag, so they're never stamped as gear.

| Mote | Materials | Crafting level |
|---|---|---|
| **Arcane Mote** | amethyst shard + lapis lazuli | 1 |
| **Ember Mote** | amethyst shard + 2 coal | 5 |
| **Frost Mote** | amethyst shard + 4 snowballs | 5 |
| **Storm Mote** | amethyst shard + 2 redstone | 8 |

### 2.3 Bolts (the basic attack)
- **Right-click** with a focus to fire a bolt of its element. It costs 1 mote of that element, which is free on a matching staff.
  - A bolt flies straight and fast (no gravity, about 30 blocks) and deals the focus's power through the normal combat pipeline: element matchup, Magic level bonus (+0.25% per level), attack style and Sundered.
  - It pays **4 Magic XP per damage** (split with Defence by attack style, as for melee and ranged) plus Constitution XP.
  - It builds Momentum like an arrow hit.
- **Cooldown:** the item's own cooldown (`cast_cooldown`), shown on the hotbar like an ender pearl's.
- **Level gate:** a focus above your Magic level won't cast, with a message saying why.

### 2.4 Spells (ability-bar abilities, Spells tab)
Spells follow the Phase 6 rules: unlock level, global cooldown, Momentum class, cooldown, windup. They also need a focus in the main hand (Hearthward excepted) and pay their mote cost.

| Spell | Magic | Class | Cost | Effect | CD |
|---|---|---|---|---|---|
| **Fireball** | 5 | Basic | 2 Ember | A fire bolt that bursts on impact: 120% fire to everything within 2.5 blocks, sets them alight for 3 s. | 8 s |
| **Hearthward** | 8 | Basic | 2 Arcane | A 5 s channel (moving cancels it), then home to your spawn point. Not in combat, not in an arena. No focus needed. | 5 min |
| **Rime Snare** | 12 | Basic | 2 Frost | A frost bolt: 80% frost and roots for 2 s (bosses: Slowed). | 9 s |
| **Blink** | 18 | Basic, off-GCD | 1 Arcane | Teleport up to 8 blocks where you look; stops at walls. | 10 s |
| **Chain Lightning** | 22 | Surge | 3 Storm + 1 Arcane | 150% storm to the target, then jumps to 3 more enemies within 5 blocks, losing 25% per jump. | 12 s |
| **Arcane Ward** | 25 | Basic, off-GCD | 2 Arcane | Absorption II (4 extra hearts) for 10 s. | 25 s |
| **Frost Nova** | 28 | Surge | 3 Frost + 1 Arcane | 0.3 s windup, then 120% frost to everything within 4 blocks, Slowed III for 3 s. | 14 s |
| **Starfall** | 40 | Ultimate, staff only | 4 Arcane | 0.5 s windup. 1 s later a star falls on the spot you aimed at (up to 24 blocks): 300% arcane within 4 blocks, with knockback (not bosses). | 45 s |

Spells generate Momentum like melee Basics (+8 for most Basics).

## 3. Prayer

### 3.1 Faith
- **Maximum Faith** = 9 + Prayer level: 10 at level 1, 108 at 99.
- **Drain:** blessings drain Faith every tick while they're on. When Faith runs out, they all switch off.
- **Natural regeneration:** +1 every 30 s while no blessing is on. This is gentler than OSRS (no regeneration there), so a player without a shrine nearby isn't stuck.
- **Full restore:** right-click a **Votive Shrine** with an empty hand, or respawn.
- **Saving and death:** Faith is saved and synced to the owner. Blessings end on death and on logout.

### 3.2 Training: bones
- **Bury:** sneak + right-click while holding an offering (anywhere) to bury one for its base XP.
- **Offer:** right-click a Votive Shrine while holding offerings to offer the **whole stack** for **×3** the XP.
  - Why the whole stack: sneaking skips a block's right-click in vanilla, so "sneak to offer all" can't work. Sneak + use buries one instead.
- **Offerings** (`data_maps/item/prayer_offerings.json`): bone 5 XP, skeleton skull 30, wither skeleton skull 100.
  - Bone meal and bone blocks aren't offerings, so converting between them gains nothing.
- **Votive Shrine:** crafted on a crafting table from chiseled stone bricks, candles and gold.

### 3.3 Blessings (Prayers tab)
- **Toggling:** right-click a blessing in the Ability Book to turn it on or off. Blessings can also go on the ability bar like any ability.
- **Rules:** they're off-GCD and need no Momentum. Turning one off starts a 1 s cooldown.

| Blessing | Prayer | Effect | Drain (Faith/s) | Group |
|---|---|---|---|---|
| **Ironhide** | 1 | −5% damage taken | 0.05 | guard |
| **Keen Edge** | 4 | +5% melee damage | 0.05 | melee |
| **Steady Aim** | 7 | +5% ranged damage | 0.05 | ranged |
| **Inner Flame** | 9 | +5% magic damage | 0.05 | magic |
| **Mending** | 12 | heal 1 HP every 5 s | 0.08 | – |
| **Stoneskin** | 16 | −10% damage taken | 0.10 | guard |
| **Battle Fervor** | 20 | +10% melee damage | 0.10 | melee |
| **Hawkeye** | 23 | +10% ranged damage | 0.10 | ranged |
| **Sage's Focus** | 26 | +10% magic damage | 0.10 | magic |
| **Ward of Steel** | 30 | −40% melee damage taken | 0.25 | ward |
| **Ward of Arrows** | 33 | −40% ranged damage taken | 0.25 | ward |
| **Ward of Sorcery** | 36 | −40% magic damage taken | 0.25 | ward |
| **Saving Grace** | 45 | A hit that would kill you leaves you at 25% health instead; it spends all your Faith | 0.15 | – |

- **Exclusive groups:** a blessing turns off any other blessing in its group, so you can't stack Ironhide with Stoneskin, or two wards.
- **Damage blessings** apply after the combat triangle: a matchup, then +10%.
- **Wards and bosses:** wards use the damage type the hit carries. Slagheart's slams are crush (melee), its slag globs are ranged.
- **Example budget:** at Prayer 30 (39 Faith), Ward of Steel plus Battle Fervor lasts about 1 min 50 s.

## 4. Agility

### 4.1 Training: moving well
Agility XP comes from parkour feats. A **novelty rule** stops farming: each feat pays once per spot (a 4×4×4 block cell) per minute.

| Feat | XP | What counts |
|---|---|---|
| **Climb** | 1 per block | Height gained on ladders, vines, scaffolding and other climbable blocks, counted when you step off. Up to 30 per climb. |
| **Gap jump** | 6, +2 per block past 3 (max 20) | A sprint jump that lands at least 3 blocks (horizontally) from where it took off, over a drop: no ground within 3 blocks below the takeoff height at the midpoint. |
| **High dive** | 10 | Landing in water after falling at least 10 blocks. |

Agility abilities give no XP, so spamming them isn't training. Agility courses are planned for later (§6).

### 4.2 Passive perks
| Perk | Scaling | At 50 | At 99 |
|---|---|---|---|
| **Sure footing** | fall damage starts 0.1 blocks later per level | falls up to 8 blocks are safe (vanilla: 3) | 12.9 blocks |
| **Fleet feet** | +0.05% sprint speed per level | +2.5% | +4.9% |

Both are server config values.

### 4.3 Movement abilities (Agility tab)
All are off-GCD and need no weapon.

| Ability | Agility | Class | Effect | CD |
|---|---|---|---|---|
| **Sprint Burst** | 1 | Basic | 4 s of +30% speed (moved from Constitution 10). | 18 s |
| **Sidestep** | 5 | Basic | Dash 3.5 blocks the way you're moving (backwards if standing still). Stops at walls. | 5 s |
| **Featherfall** | 10 | Basic | Slow Falling for 6 s. | 20 s |
| **Updraft** | 18 | Basic | Jump about 3 blocks straight up, even in mid-air. | 10 s |
| **Grapple Line** | 28 | Basic | Pull yourself to the block you're looking at, up to 18 blocks away. You land just in front of it. | 12 s |
| **Shadowstep** | 38 | Surge | Step behind the enemy you're looking at (within 10 blocks), facing it. Your next melee hit within 3 s deals +25%. | 15 s |

**Arena safety:** every dash and teleport is ray-traced against blocks, so nothing passes through walls or leaves an arena. Shadowstep also checks there's room to stand behind the target.

## 5. Decisions (recorded instead of asking)
1. **Where blessings live:** they're toggled from the Prayers tab (right-click) and can also go on the bar. A separate prayer panel would mean a second screen to learn. Thirteen toggles would also crowd a 4-slot bar, but the bar stays an option for a favourite.
2. **Faith regenerates slowly out of use.** OSRS has no regeneration, but Minecraft worlds can be far from a shrine.
3. **Bolts cost motes, and staves supply their element**, as in OSRS. That gives elemental staves a reason to exist, and keeps the cheap Apprentice Wand from being free unlimited ranged damage.
4. **Spells scale with the focus's power,** not the Magic level alone. Magic gear then progresses like weapons, and the Magic level adds the usual small bonus (+0.25% per level).
5. **No magic defence on armour yet,** because vanilla mobs don't deal typed magic damage. The `magic` group is ready.
6. **Agility XP from parkour, with a novelty rule.** It's the most Minecraft-native way to train movement. Courses with markers are a later phase.
7. **Sprint Burst moved to Agility 1,** as the Phase 6 spec planned.
8. **Hearthward needs no focus,** so it's always an escape option, but it's refused in combat and in arenas.
9. **Wards reduce by 40%, not 100%,** so bosses stay dangerous. This is the number the Phase 6 spec proposed.

## 5.0 Screenshots
These come from the CI showcase (`-Pshowcase=abilities`, software rendering, shown at half size).

| HUD: staff, spell bar, Faith bar, two blessings on | Spells tab |
|---|---|
| ![HUD](images/phase7/magic_hud.jpg) | ![Spells](images/phase7/book_spells.jpg) |

| Prayers tab (On badges, Faith, tooltip) | Agility tab |
|---|---|
| ![Prayers](images/phase7/book_prayers.jpg) | ![Agility](images/phase7/book_agility.jpg) |

## 5.1 Verified numbers
These come from `./gradlew runGameTestServer` in CI. **All 24 GameTests pass**: 20 from earlier phases and 4 new ones. The earlier numbers are unchanged: the melee DPS ratio is still ×1.389, and the loot rates and gathering value are the same.

- **`magic_bolt`:**
  - **Apprentice Wand** at Magic 1 dealt **4.20** = power 4 × Aggressive 1.05. It used 1 Arcane Mote and refused to fire with none left. It paid **16.8 Magic XP** (4 per damage) and no Ranged XP.
  - **Ember Staff** at Magic 10 dealt **8.59** = 8 × level 1.0225 × 1.05, with no Ember Motes in the inventory (the staff supplies them).
  - A Magic 1 player can't cast with the Magic 40 Starseer Staff.
- **`magic_spells`:**
  - **Frost Nova** (Rime Staff, Magic 28) hit two iron golems for **8.61** each = 8 × 1.2 × level 1.0675 × 1.05 × frost 0.8. It slowed both and spent its Arcane Mote; the staff covered the Frost Motes.
  - **Chain Lightning** (Tempest Staff, Magic 22) hit three golems for **18.32 → 13.74 → 10.30** = 8.5 × 1.5 × level 1.0525 × 1.05 × storm 1.3, then −25% per jump.
- **`prayer`:**
  - Maximum Faith at Prayer 45 is 54.
  - Turning on Ward of Steel turns off Ward of Arrows.
  - Ward of Steel is **×0.60** against crush and doesn't touch ranged hits.
  - Battle Fervor is **×1.10** on melee and doesn't touch spells.
  - Two blessings drained **0.70 Faith in 2 s**.
  - 10 bones offered at a shrine paid **150 XP**; 1 buried paid **5 XP**.
  - **Saving Grace** turned a 30-damage hit at 4 health into 0 damage and left 5 health (25% of 20). All Faith was spent, every blessing switched off the next tick, and a new blessing was then refused for lack of Faith.
  - The book refuses to toggle non-blessings.
- **`agility`:**
  - A 4-block sprint jump over a gap paid **8 XP**. The same jump again paid nothing.
  - Climbing a 5-block ladder paid **5 XP**.
  - A 7-block fall at Agility 40 counted as **3 blocks**.
  - **Grapple Line** at a wall stopped with your edge **0.1 blocks** short of the wall face.
  - **Shadowstep** moved you from in front of a zombie to behind it, 5 blocks along, and armed a +25% next melee hit.

## 6. Later
- **Agility courses:** start, checkpoint and finish markers, with XP for completed laps.
- **Runecrafting-style mote altars** (motes are crafted at the bench for now).
- **More:** robes with magic defence, magic-using bosses, prayer potions, and spells at levels 51–99 with custom tiers.
