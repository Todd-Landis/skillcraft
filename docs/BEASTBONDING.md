# Beastbonding (replaces Sailing)

**Status:** designed and built on `claude/fervent-archimedes-ijjnlf`. The numbers here are the shipped defaults; per-creature numbers are JSON and global numbers are server config.

Beastbonding is SkillCraft's creature skill. It is **not** a combat-familiar skill like the summoning skills in other MMOs. It's about **mounts and helpers**: you bond creatures into a **Bond Codex**, then call them out or put them away whenever it's convenient.

- **Low levels:** the vanilla pets and mounts you already know (wolves, cats, horses, donkeys, llamas, camels).
- **Middle levels:** mobs that are never pets in vanilla, changed into helpers. A spider becomes a wall-climbing mount; a fox fetches items; an iron golem becomes a bodyguard.
- **Level 62:** a brand-new flying mount, the **Cloudray**.

All names, art and text are original.

## 1. The loop
1. **Bond** a creature: use the **Bond Codex** on it.
   - Pets and horses you've **tamed yourself** bond for free.
   - Other creatures take an **offering**, such as sweet berries for a fox or string for a spider.
   - The creature disappears into your codex, keeping its name, health, saddle, armour and chest.
2. **Summon or store it** from the codex screen (key **H**, or use the codex in the air).
   - Summoning puts it beside you. Storing saves it exactly as it is.
   - You can have **one mount and one companion** out at once, or two companions from Beastbonding 50.
3. **Train** by bonding new creatures (the first of each kind fills a codex page and pays ×5), riding your mounts, and letting your companions work: fights, fetching, carrying.
4. **If a summon dies**, it becomes **Fallen**. You can bring it back at a **Rekindling Shrine** (section 5); it's never lost for good.

## 2. The Bond Codex
- **Item:** the Bond Codex (crafting table: book, lead and gold ingot, shapeless). You need it in hand to bond a creature.
- **Your codex lives on you, not on the book.** Losing the book loses nothing; craft another. That also makes it safe in multiplayer: a book can't be stolen or duplicated to copy creatures.
- **Screen (H):** your bonds, each with its status: **Stored**, **Out**, **Fallen** or **Rekindling** (with time left).
  - Buttons: **Summon**, **Store**, **Release**, and **Rekindle** (only near a Rekindling Shrine).
  - **Release** lets a stored creature go: it appears beside you as an ordinary, unbonded creature.
  - **Codex pages:** every species in the game is listed. Ones you've bonded show their name; the rest show "???" with the level they need. That's the "fill out the codex" goal: "Pages: 7 / 17".
- **Bond slots:** 4, plus 1 per 5 Beastbonding levels (23 at 99).

## 3. The kindred
Data: `data_maps/entity_type/bondable.json`. For each creature:
- level, and whether it's a mount or a companion;
- how it bonds (tamed by you, or an offering);
- the tribute and time to rekindle it;
- XP for bonding it and XP for riding it (per 100 blocks).

| Creature | Level | Kind | How it bonds | What it's good for |
|---|---|---|---|---|
| Wolf | 1 | companion | tamed by you | fights beside you |
| Cat | 1 | companion | tamed by you | creepers and phantoms keep away |
| Parrot | 5 | companion | tamed by you | rides your shoulder, warns of mobs |
| Donkey | 5 | mount | tamed by you | carries a chest |
| Horse | 10 | mount | tamed by you | fast overland travel |
| Snow Golem | 10 | companion | 8 snowballs | pelts monsters, leaves a snow trail |
| Mule | 12 | mount | tamed by you | carries a chest |
| Llama | 15 | mount | tamed by you | carries a chest, spits at enemies |
| **Fox** *(changed)* | 15 | companion | 3 sweet berries | **fetches** dropped items near you into your inventory |
| Allay | 18 | companion | 1 amethyst shard | collects items like the one it holds |
| Camel | 20 | mount | 4 cactus | two riders, dashes |
| Skeleton Horse | 25 | mount | 6 bones | rides through water |
| Zombie Horse | 25 | mount | 6 rotten flesh | sturdy undead mount |
| Strider | 30 | mount | 2 warped fungus | walks on lava (steer with a warped fungus on a stick) |
| **Cliffstrider** *(changed spider)* | 35 | mount | 8 string, on a wild spider | **climbs walls** while ridden |
| **Ironward** *(changed iron golem)* | 45 | companion | 1 iron block, on an iron golem | bodyguard: follows you and fights for you |
| Happy Ghast | 45 | mount | 4 snowballs | slow flying mount that carries four |
| **Cloudray** *(new)* | 62 | mount | 8 Storm Motes, on a lured Cloudray | **fast flying mount** |

- **"Changed" creatures** are summoned as SkillCraft versions of the vanilla mob. A bonded spider comes back as a tame, rideable Cliffstrider, never a hostile spider. A bonded fox or iron golem follows you around and gains its helper behaviour.
- **Mount bonuses:** a summoned mount gets **+0.25% speed and +0.5% health per Beastbonding level** (up to +25% and +50% at 99).

## 4. The Cloudray (level 62)
A new creature with its own model: a great sky-manta. It has broad, slow-beating wings, a whip tail and a pale cloud-coloured belly with glowing storm-blue markings.

- **Finding one:** craft a **Cloudray Lure** (phantom membrane ×2, feather ×4, Storm Mote ×4), then use it **at Y 140 or higher under open sky**.
  - A wild Cloudray glides in and circles nearby for 3 minutes.
  - Bond it with the codex and an offering of 8 Storm Motes.
  - This ties Beastbonding to Leycraft (Storm Motes) and to mountain or tower building.
- **Flying:** it flies where you look.
  - Forward speeds up, and looking up or down climbs or dives.
  - With no input it hovers in place.
  - It's quick: about 14 blocks a second at level 62, faster with level.
  - Getting off in mid-air gives you Slow Falling, so a dismount over a canyon isn't a death sentence.
- It's a mount slot creature. Rekindling it takes 10 minutes and a tribute of 4 Storm Motes and 2 phantom membranes.

## 5. Death and the Rekindling Shrine
- **When a summoned creature dies,** it drops nothing: its saddle, armour and chest stay with its codex entry. The entry becomes **Fallen** and you get a message.
- **The Rekindling Shrine** (crafting table: gold ingot, lantern, gold ingot / flower, bone block, flower / 3 mossy cobblestone) is a small mossy altar with a lantern-lit bowl.
  - Stand within 4 blocks of one, open the codex, and press **Rekindle** on a Fallen bond.
  - It takes the creature's **tribute** from your inventory: golden carrots for horses, bones for wolves, and so on.
  - The bond becomes **Rekindling**, shown with a timer. After the creature's rekindle time (1 minute for a wolf up to 10 for a Cloudray) it's back in your codex at full health.
  - You don't have to wait at the shrine.
- Rekindling pays half the creature's bond XP.
- **Why a shrine plus a tribute:** death costs time and something you gathered, but never the creature itself, which you may have spent hours bonding.

## 6. XP
- **Bonding:** the creature's bond XP, ×5 the first time you bond that species (a new codex page).
- **Riding a bonded mount:** its ride XP per 100 blocks travelled. Horse 25, camel 30, Cliffstrider 40, Happy Ghast 35, Cloudray 60.
  - That's roughly 9,000 XP an hour on a horse at a gallop, more on higher mounts.
- **Companions at work:**
  - 0.5 XP per point of damage a companion deals to a monster.
  - 2 XP per stack a fox fetches.
- **Rekindling:** half the bond XP.

## 7. Rules that keep it safe
- **Server-authoritative:** bonding, summoning, storing and rekindling all happen on the server. The client screen only sends button presses.
- **No duplicates:**
  - Summons are stored automatically when you log out or change dimension.
  - A summon found in the world that your codex doesn't list as Out (left over after a crash) is removed when it loads.
  - The codex copy is the one that counts.
- **Not in the boss arena.** Summoning is refused there.
- **Bonded creatures don't despawn,** don't drop loot or XP when they die, and changed creatures are never hostile to players.

## 8. Decisions
- **Sailing is replaced.** Sailing never had mechanics, so no saves lose XP. Skill loading now also ignores skill names it doesn't know, so removing a skill can never break a save.
- **The codex lives on the player, not on the book,** for safety (no duplicating creatures by copying items) and convenience (key H anywhere).
- **Changed mobs are SkillCraft entities that reuse the vanilla look.** The Cliffstrider uses the spider's model and texture. That gives new behaviour without new art, and saves the modelling budget for the Cloudray.
- **Flying is steer-by-looking,** not jump-to-climb, so it feels like an elytra and needs no extra keys.
- **Not in v1** (roadmap): the nautilus (its taming rules are new in 26.2 and need checking in-game), a Ravager war-mount, more new creatures for levels 70–99, codex-page lore text, and naming bonds from the screen.
