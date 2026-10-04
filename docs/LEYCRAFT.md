# Leycraft (the mote-making skill)

**Status:** designed and built on `claude/fervent-archimedes-ijjnlf`, on top of Elemental Hearts. The numbers here are the shipped defaults; almost all of them are JSON or server config.

Leycraft is SkillCraft's take on a rune-making skill. You mine **ley shards**, carry them to a **Ley Altar**, and weave them into the **motes** that Magic spends. Two ideas are borrowed as mechanics only:
- From classic MMO rune-making: an essence you mine in bulk, an altar that turns it into spell reagents, and higher levels giving more reagents per essence.
- From *RuneScape: Dragonwilds* (2025): crafting at an altar has a **quick** mode and a slower, **hands-on** mode that pays more per essence.

**What's ours (the novel part): the altar draws on its surroundings.** A Ley Altar has no recipe menu. It weaves whichever element the world around it **resonates** with, and the stronger the resonance, the more motes each shard gives:
- An altar ringed with lava and magma weaves **Ember Motes**.
- One on a glacier weaves **Frost Motes**.
- One on a mountaintop under a thunderstorm, bristling with lightning rods, weaves **Storm Motes**.
- One among bookshelves and amethyst, or with nothing special around it, weaves **Arcane Motes**.

So players build small themed altar sites around the world, or bring the world to the altar (buckets of lava, packed ice, lightning rods). It's the same back-and-forth as Elemental Hearts: the vanilla world feeds the skill.

All names, art and text are original.

## 1. The loop
1. **Mine ley shards** from a **Ley Stone** node (Mining 1). Get the node the usual way: a **Ley Heart**, found by breaking amethyst clusters in geodes, attuned at the Attunement Altar.
2. **Build a Ley Altar** and dress its surroundings for the element you want.
3. **Load it** with shards (or amethyst shards, which are a weaker essence).
4. **Weave:**
   - **Quick weave** (sneak + use with an empty hand): instant, base yield.
   - **Careful weave** (use with an empty hand): a short minigame, *Steady the Current*, for up to **×1.5** motes and **+25% XP**.
5. Spend the motes on Magic bolts and spells (docs/AGILITY_MAGIC_PRAYER.md).

Motes are **no longer made at the Crafting Bench.** That recipe was a stand-in until this skill existed.

## 2. Essence
| Essence | Value | Where from |
|---|---|---|
| **Ley Shard** (new) | 1 | Ley Stone node: 2 per success |
| Amethyst Shard (vanilla) | 0.5 | Geodes and budding amethyst farms |

- The value scales both the motes and the XP: one amethyst shard counts as half a ley shard. That keeps a vanilla-only path open from day one, while making the node worth having.
- Data: `data_maps/item/ley_essence.json`.

**Ley Stone node:** Mining level 1, 10 Mining XP per success, gives 2 Ley Shards, Tier I (20 s regrow, up to 6 within 8 blocks). It uses the mining minigame (*Temper the Vein*) to attune.

**Ley Heart:** 3% per natural amethyst cluster broken, up to +50% at Mining 99 (about one heart per one or two geodes). Clusters a player placed never drop one, the same as every heart. Data: `data_maps/block/elemental_hearts.json`.

## 3. The Ley Altar
- **Recipe** (crafting table): amethyst shard, lapis, amethyst shard / 3 calcite / 3 polished deepslate.
- **Holds** up to 64 essence of one kind.
- **Owned while loaded,** like the Attunement Altar: only the player who loaded it can weave from it or break it. Breaking it returns the essence.
- **Interactions:**
  - **Use with essence:** load it.
  - **Use with an empty hand:** empty altar → shows its resonance. Loaded → careful weave.
  - **Sneak + use with an empty hand:** quick weave.
- **The altar shows its element:** small particles rise from it (enchant glyphs, flames, snowflakes or sparks). It re-reads its surroundings when placed and every time it's used.

## 4. Resonance
The altar scans a **9×9×9 cube** around itself (4 blocks each way) and adds up **resonance points** for each element from `data_maps/block/ley_resonance.json`. **40 points = 100%.**

| Element | Blocks (points each) | Biome (+25%) | Weather and height |
|---|---|---|---|
| **Fire** | lava 8, lava cauldron 6, magma block 4, fire / soul fire 4, campfire / soul campfire 3, netherrack 1 | Nether, hot biomes | |
| **Frost** | blue ice 6, packed ice 4, ice 3, powder snow 3, snow block 2, snow layer 1 | snowy, icy and cold biomes | |
| **Storm** | lightning rod (any copper stage) 8, oxidized copper 3, weathered copper 2 | mountains, windswept, oceans | thunderstorm with open sky +50%, rain with open sky +15%, Y ≥ 120 with open sky +25% |
| **Arcane** | enchanting table 8, budding amethyst 4, amethyst block 3, crying obsidian 3, amethyst cluster 2, bookshelf / chiseled bookshelf 1 | the End, magical biomes | |

- Biome groups are biome tags (`skillcraft:ley_resonance/<element>`), so modpacks can add their own biomes.
- Each element caps at 100%.
- **The altar weaves the strongest element.** Ties go to Arcane, and an altar with no resonance at all weaves Arcane at 0%.
- **Why blocks a player places count:** decorating your altar is the point. Natural surroundings (the Nether, a glacier, a thunderstorm) are the free version; building it yourself costs materials.
- **Level gate:** if the strongest element needs a higher Leycraft level than you have, the altar refuses. It tells you which element and level, so you can change the surroundings.

## 5. Yield and XP
```
motes = essence value × 4 × (1 + resonance) × weave × (1 + 0.5 × level progress)
```
- `resonance`: 0 to 1, from section 4.
- `weave`:
  - Quick: ×1.0.
  - Careful, success: ×1.5 with no misses, then −0.1 per miss (×1.4, ×1.3, ×1.2).
  - Careful, failure: ×0.8. A failed weave still makes motes, just fewer.
- `level progress`: 0 at the mote's level, rising to 1 at Leycraft 99.
- The fractional part of the whole batch is rounded at random, so averages are exact.

**Range per ley shard:** 4 motes (quick, no resonance, at the mote's level) up to **18** (full resonance, perfect careful weave, Leycraft 99).

| Mote | Leycraft level | XP per shard | Magic uses it for |
|---|---|---|---|
| **Arcane Mote** | 1 | 6 | wand bolts, Hearthward, Blink, Arcane Ward, Starfall |
| **Ember Mote** | 5 | 8 | Fireball (Magic 5) |
| **Frost Mote** | 12 | 9.5 | Rime Snare (Magic 12), Frost Nova |
| **Storm Mote** | 20 | 11 | Chain Lightning (Magic 22) |

- XP is per shard of essence value (an amethyst shard gives half), and a successful careful weave gives +25%.
- Data: `data_maps/item/ley_weaves.json` (keyed by mote: level, XP per shard, base motes per shard).
- Global numbers live in the server config: scan radius, points for 100%, biome and weather bonuses, weave multipliers, and the level bonus.

**Examples:**
- **A new player:** an altar in the plains with 64 ley shards and a quick weave at Leycraft 1 gives 256 Arcane Motes and 384 XP.
- **Leycraft 40, a Nether fire altar** (lava pool and magma, so 100% fire) with a perfect careful weave: about 14 Ember Motes per shard, so 64 shards give about 910 motes and 640 XP.

**Pace check:**
- A Ley Stone node gives about 800 shards an hour at Mining 1 (fatigue 400 successes an hour), rising to 2,400 at 99.
- At Leycraft 1, weaving those carefully is about 6,000 Leycraft XP an hour, plus 4,000 Mining XP from the node.
- A wand fight spends about 75 Arcane Motes a minute, so an hour at the node funds hours of wand use. Staves still supply their own element for free.

## 6. Careful weave: *Steady the Current*
A one-button minigame, server-verified like every altar minigame (seeded, inputs replayed on the server, timing checked).
- A **ley current** (a glowing bar) rises while you hold the button and sinks when you let go. It has momentum, so you feather it.
- A **band** drifts up and down the channel.
- Every 1.25 s a **pulse** runs down the channel: if the current is inside the band, that's a hit, otherwise a miss.
- **8 hits** to finish; **3 misses** allowed.
- **Pulses don't count until your first press,** so closing it untouched is free, the same as the other minigames.
- **Harder for higher motes:** the band is narrower and drifts faster (tier = the mote's level tier).

## 7. Finding sources: the Ley Tuner
Hunting for lava, a glacier or a geode shouldn't be a chore, so Leycraft comes with a finder.

- **Ley Tuner** (crafting table): 2 amethyst shards and 3 copper ingots in a fork shape.
- **Use:** it rings and points to the **nearest strong source** of its element:
  - **Fire:** lava, magma, fire, campfires.
  - **Frost:** blue ice, packed ice, ice, powder snow.
  - **Storm:** lightning rods, oxidized copper.
  - **Arcane:** enchanting tables, **budding amethyst** (so it leads to geodes, where Ley Hearts drop), amethyst blocks, crying obsidian.
  - A source is any block in `ley_resonance.json` worth at least 3 points.
- **The answer:** the action bar names the block, the distance, a compass direction and the height ("Fire: Magma Block, 23 blocks north-east (6 below)"). A short trail of the element's particles leads off toward it, and the chime **rises in pitch the closer it is**.
- **Range:** 32 blocks plus half your Leycraft level (81 at 99). It only searches loaded chunks, so it never generates terrain.
- **Fallback:** if no source block is in range, it looks for the nearest **biome** of that element, up to 1,600 blocks away (like `/locate biome`). If you're already standing in one, it says an altar here gets the biome bonus.
- **Sneak + use** retunes it: Arcane → Fire → Frost → Storm. Each tuner remembers its element, and its name shows it ("Ley Tuner (Fire)").
- **Cooldown:** 3 seconds between rings.
- All of this is server config (`leyTuner*`).
- **Why a tool, not an ability:** it's available from Leycraft 1 with no unlock, it works without a free ability slot, and it scales with level through its range.

## 8. Decisions
- **New skill: Leycraft (Artisan),** rather than folding it into Magic or Crafting. Rune-making is its own skill in the games this draws on, and an Artisan skill that feeds a combat skill is the healthy shape.
- **No recipe menu: the surroundings decide the element.** This is the novel part, and it needs no new screen.
- **Amethyst still works, at half value,** so Magic never stalls behind a node a player hasn't found yet.
- **Quick weave isn't punished** (×1.0) and careful weave isn't required. The minigame is a reward for engagement, not a tax.
- **Weaving doesn't use fatigue.** The essence is already limited by gathering fatigue at the node.
- **Bench mote recipes removed.** Keeping them would have made the skill pointless.

## 9. Later
- More motes for tier VI–X spells (e.g. a void or tide element), each with its own resonance blocks.
- Ley rifts generated in the world (rare natural Ley Stone), when world generation arrives.
- A **ley pouch** that carries extra essence, unlocked by level.
- **Jewellery** with stored mote charges, once the equipment-slots work (rings and amulets) lands.
