# SkillCraft Combat

Phase 5 adds combat XP, a combat triangle built on damage types, typed armour, attack styles, the Defence skill and Constitution health. It's built to grow: new weapons, ammo, armour and monsters plug in through data maps, not code.

**Goals**
- **Horizontal choices.** Monsters are weak to some damage types and resist others, so carrying a second weapon or a different ammo pays off. Gear can fit a niche without being strictly better.
- **Keep the vanilla feel.** No miss rolls (a whiff in Minecraft feels like lag). Vanilla attack cooldown, damage and armour still apply, and our numbers are small multipliers on top.
- **Readable.** Every weapon, ammo and armour tooltip shows its type and values. Hits say "Effective" or "Resisted". Sneak and right-click a monster with an empty hand to examine it.

## Damage types
| Type | Group | Typical sources |
|---|---|---|
| Stab | Melee | Spears, tridents (melee), pickaxes |
| Slash | Melee | Swords, axes, hoes |
| Crush | Melee | Warhammers, maces, shovels, fists and anything else |
| Light | Ranged | Light arrows: fast, flat, low damage |
| Medium | Ranged | Vanilla arrows (normal, tipped, spectral) |
| Heavy | Ranged | Heavy arrows, thrown tridents: slow, arcing, high damage, extra knockback |

| Fire | Magic | Ember Staff, Fireball |
| Frost | Magic | Rime Staff, Rime Snare, Frost Nova |
| Storm | Magic | Tempest Staff, Chain Lightning |
| Arcane | Magic | wands, Starseer Staff, Starfall |

The four magic types (elements) arrived with the Magic skill. They're covered in [AGILITY_MAGIC_PRAYER.md](AGILITY_MAGIC_PRAYER.md).

Each damage type belongs to a **discipline**: melee (stab, slash, crush), ranged (the ammo weights) or magic (the elements). The discipline decides which skill a hit trains, and which skill's level bonus it gets: Melee, Ranged or **Magic**. Prayer blessings that boost or ward a discipline use it too.

### Where the data lives (`data/skillcraft/data_maps/`)
- `item/weapon_styles.json`: item → `{"style": "crush"}`. Items that aren't listed hit as **crush**.
- `item/armor_defences.json`: armour item → percentage points per group, e.g. `{"stab": 2.1, "slash": 2.8, "crush": -1.4, "ranged": 2.1}`.
- `entity_type/combat_profiles.json`: monster → `{"modifiers": {"crush": 1.4, "stab": 0.7}, "attack": "crush"}`. `modifiers` multiplies the damage it takes from each type (1.0 if missing). `attack` is the type its melee hits deal to players. Projectiles count as **ranged**.
- Ammo type is set by the projectile entity (`combat/SkillArrow`, vanilla arrows = medium).

## Damage formula
**Player hitting a monster** (`combat/CombatDamage`):
```
damage × matchup × (1 + 0.0025 × (skillLevel − 1)) × styleBonus × (Sundered ? 1.15 : 1)
```
- `matchup` is the monster's modifier for the damage type, clamped to 0.25–2.0.
- `skillLevel` is the hit's discipline skill: Melee, Ranged or Magic. It's +0% at level 1, up to +24.5% at 99.
- `styleBonus` is ×1.05 on Aggressive.
- A matchup of ≥1.15 shows **Effective** (enchanted-hit sparks and a chime); ≤0.85 shows **Resisted** (a grey puff and a dull clank). The overlay message shows at most once a second.

**Anything hitting a player:**
```
damage × (1 − typedDefence/100) × (1 − 0.0015 × (Defence − 1)) × (Defensive ? 0.95 : 1)
```
- `typedDefence` is the sum of the worn armour's points for the incoming group (stab, slash, crush or ranged), clamped to −25…+40.
- The Defence level part is up to −14.7% at 99.
- This only applies to damage from an entity (melee, projectiles, boss attacks), not falls, fire, drowning and so on.
- Vanilla armour points and toughness still apply first, as normal.

## Armour: typed defence (full set totals)
Each piece carries part of the set total: helmet 20%, chestplate 35%, leggings 30%, boots 15%.

| Armour | Class | Stab | Slash | Crush | Ranged | Defence req. |
|---|---|---|---|---|---|---|
| Leather | light | −4 | 0 | +8 | +6 | 1 |
| **Padded** (new) | light | +4 | +4 | **+18** | **+12** | 20 |
| Chainmail | medium | −4 | **+16** | −6 | +2 | 10 |
| Copper | heavy | +4 | +6 | −2 | +4 | 5 |
| Iron | heavy | +6 | +8 | −4 | +6 | 10 |
| Gold | heavy | +2 | +4 | −6 | +2 | 20 |
| Diamond | heavy | +8 | +10 | 0 | +8 | 30 |
| Netherite | heavy | +10 | +12 | +4 | +10 | 40 |
| Turtle shell | — | +2 | 0 | 0 | +4 | 15 |

Mail stops cuts but not points or blows. Plate turns points and edges but carries blunt force. Quilted padding soaks up blows and arrows. So against a crushing boss, padded armour (13 armour points) can beat iron plate (15 points). That's the tradeoff we want.

Wearing armour above your Defence level isn't possible. It drops back into your inventory with a message. (`combat/EquipmentGate`)

## New gear (basic tier)
Stab and slash already have full vanilla families (spears, swords and axes, wood through netherite). Crush didn't, so it gets a full family, and ranged gets two new ammo weights.

| Item | Type | Damage / speed | Notes | Made at | Use req. |
|---|---|---|---|---|---|
| Copper / Iron / Golden / Diamond / Netherite **Warhammer** | crush | 7/8/6/9/10 dmg, 1.0 atk/s | +0.4 knockback | Anvil (Smithing 4/13/23/33, netherite upgrade 45) | Melee 5/10/20/30/40 |
| **Light Arrow** | light | base 1.3, ×1.2 speed, less drop | cheap, flat | Crafting Bench (Crafting 1), ×6 | Ranged 1 |
| **Heavy Arrow** | heavy | base 3.0, ×0.8 speed, more drop, +1 knockback | punches through shells and plates | Crafting Bench (Crafting 15), ×4 | Ranged 20 |
| **Padded** cap / gambeson / leggings / boots | light armour | 2/5/4/2 armour | crush and ranged specialist | Crafting Bench (Crafting 20–23) | Defence 20 |

Also:
- Vanilla spears (wood → netherite) were craftable at a normal crafting table, which broke "gear only from players". They now go through the Crafting Bench (wood, stone) and the Forging Anvil (metals, netherite upgrade). They're gated like swords.
- Chainmail can now be forged at the anvil from iron nuggets (Smithing 11–17).
- Arrow ammo with a Ranged requirement is checked when you start drawing the bow.

Textures are generated by `scripts/textures/GenCombatTextures.java` (run with `java scripts/textures/GenCombatTextures.java`). Edit the palettes there or paint over the PNGs.

## Attack styles (key **V** cycles them; shown on the Skills screen)
| Style | XP per damage point | Bonus |
|---|---|---|
| **Aggressive** | 4 Melee (or Ranged for projectiles) | +5% damage dealt |
| **Controlled** | 2 Melee/Ranged + 2 Defence | none |
| **Defensive** | 4 Defence | −5% damage taken |

Every style also gives **1.33 Constitution XP per damage point**.

## Combat XP rules (`combat/CombatXp`)
- XP is paid on the **health actually removed**, capped at what the target had left, so overkill pays nothing.
- XP is paid only for damage to **mobs** (`Mob`). There's none for players, armour stands, **baby** mobs, or animals tamed by anyone.
- Creative and spectator players get no XP.
- **Spawner mobs** (`EntitySpawnReason.SPAWNER`) give 50% XP, so mob-spawner grinders are slower than fighting. Trial spawners give full XP because they run out.
- Projectiles credit their owner.
- Constitution XP comes from **dealing** damage only. Taking damage could be farmed with fall damage, so we skipped it. (This differs from the original design note.)

Per kill, that's roughly a zombie (20 HP) = 80 Melee + 27 Constitution, and Slagheart (500 HP) ≈ 2,000 + 667.

## Constitution: max health
`bonus HP = 2 × floor(level / 10)`, plus 2 more at level 99. It's applied as a permanent attribute modifier (`skillcraft:constitution`) on login, respawn and level-up.

| Level | 1–9 | 10 | 20 | 30 | 40 | 50 | 60 | 70 | 80 | 90 | 99 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| Max HP | 20 | 22 | 24 | 26 | 28 | 30 | 32 | 34 | 36 | 38 | **40** |

Early game feels vanilla. A mid-game player (levels 25–40) has 2–4 extra hearts, and a maxed player has double health.

## Defence (new skill)
It sits in the Combat category. Train it with the Controlled or Defensive styles. It gates armour (table above) and reduces entity damage by up to 14.7%.

## Monster weaknesses (defaults in `combat_profiles.json`)
| Monster | Weak to | Resists | Its melee |
|---|---|---|---|
| Zombie family (husk, drowned, zombie villager) | slash 1.2 | crush 0.9 | crush (drowned: stab) |
| Skeleton family (stray, bogged) | crush 1.4, heavy 1.2 | stab 0.7, light 0.6, slash 0.9 | crush |
| Wither skeleton | crush 1.3, heavy 1.2 | stab 0.75, light 0.6 | slash |
| Spider, cave spider | stab 1.25, light 1.2 | crush 0.85 | stab |
| Slime, magma cube | slash 1.3 | crush 0.6, stab 0.8 | crush |
| Iron golem | crush 1.4 | slash 0.6, stab 0.6, light 0.5 | crush |
| Ravager | stab 1.2 | crush 0.8, light 0.7 | crush |
| Phantom | light 1.3 | heavy 0.7 | stab |
| Shulker | crush 1.3, heavy 1.3 | light 0.5, stab 0.7 | crush |
| Breeze | heavy 1.3, crush 1.2 | light 0.5, medium 0.7 | crush |
| Creaking | slash 1.3 | stab 0.7 | crush |
| Piglin brute | crush 1.2 | slash 0.85 | slash |
| …and more | | | |

Unlisted monsters take ×1.0 from everything and hit as crush.

## Extending
- **New weapon:** register the item, give it a `weapon_styles` entry and a `skill_requirements` entry, and give it a bench recipe in `scripts/gen_bench_recipes.sh`.
- **New armour:** add `armor_defences` and `skill_requirements` (Defence) entries.
- **New ammo:** add a `SkillArrow.Weight`, or any projectile whose damage style resolves through `DamageStyles.of(...)`.
- **New monster:** add a `combat_profiles` entry.

## Combat abilities (Phase 6)
The ability bar adds a few decisions per fight on top of the rules above. The full design is in [ABILITIES_SPEC.md](ABILITIES_SPEC.md); §8 there records what was built.
- **The bar:** 4 slots on the HUD (keys **R, Z, B, N**), 3 saved bars, and the **Ability Book** on **J**.
- **Momentum (0–100):** built by landing hits and taking them.
  - Basic abilities are free, and many build Momentum.
  - Surges need 50 and spend 25.
  - Ultimates need a full bar and spend all of it.
- **Cooldowns:** each ability has its own, plus a 1 s global cooldown. Defensives are off the global cooldown.
- **How abilities use the combat rules:** every ability hit is dealt through `CombatDamage`, so all of the following apply exactly as for a normal swing:
  - type matchups
  - the Melee/Ranged level bonus
  - attack style
  - Sundered
  - typed armour
  - combat XP
- **Damage** is a share of the held weapon's normal hit, so abilities scale with gear and stay horizontal.
- **No extra XP:** abilities give the usual XP for the damage they deal and nothing on top.
- **Weapon Arts:** they only work with their weapon class (item tags in `tags/item/weapon_class/`, or a damage type from `weapon_styles`), so a warhammer fights differently from a spear.
- **Off-hand arts:** some arts need an item in the off hand. Shield Bash needs a shield, Torchbrand a torch, and Twinfang a sword or axe. Twinfang hits with the off-hand blade's own damage and type, and that blade's level gate still applies.
- **Bosses:**
  - stuns become **Daze** (the next attack comes 1 s later, at most once every 20 s, and a started windup is never cancelled)
  - roots and slows become at most −30% for half the time
  - knockback does nothing
  - Bleed and Sundered work normally
- **Tuning without code:**
  - ability numbers: `data/skillcraft/skillcraft/ability/*.json`
  - effect numbers: `data_maps/mob_effect/effect_tuning.json`
  - Momentum gain, global cooldown and combat window: `config/skillcraft-common.toml`
  - The JSON files reload with `/reload`.
- **Balance check (`ability_dps` GameTest, measured):** 60 s on a dummy with a diamond sword at Melee 40.
  - Auto-attacks alone: **14.76 DPS**.
  - Auto-attacks plus a sword rotation (Battle Trance, Sweeping Arc, Rending Cut, Lunge): **17.97 DPS**.
  - That's **×1.217**, inside the +15–25% target.
- **Extending:** a new ability is usually just a JSON file reusing a behaviour (see the list in the spec, §8.2). A new behaviour is a small class registered in `AbilityBehaviors`.
