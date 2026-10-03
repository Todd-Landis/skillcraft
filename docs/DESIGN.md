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

## Level gates (vanilla content)
Tunable without code in `src/main/resources/data/skillcraft/data_maps/`:
- `item/skill_requirements.json`: tools and weapons. Pickaxes need Mining, axes need Woodcutting to chop *and* Melee to fight, swords need Melee, crossbows need Ranged. Tiers: copper 5, iron 10, gold 15, diamond 30, netherite 50.
- `block/skill_resources.json`: ores and logs. Each entry has a skill, a level and XP. Common overworld logs are level 1, so every spawn biome can get wood; rarer woods and deeper ores are gated.
- Blocks placed by players give no XP when broken again, which stops place-and-break farming.

## Technical architecture
- **Player skill data:** a NeoForge *data attachment* on the player stores XP for each skill and is saved with the player.
- **Sync:** a custom network packet sends XP changes to the client for the HUD and Skills screen.
- **Data-driven content:** level requirements, XP rewards and drop tables live in JSON data files, so content can be tuned without code changes.
- **One package per skill:** `skill/mining`, `skill/woodcutting`, and so on, each registering its own blocks, items and event handlers.

## Roadmap
- [x] **Phase 1 – Foundation:** project setup, skill list, XP curve
- [x] **Phase 2 – Skill framework:** saved XP on each player, sync to client, XP drops and level-up messages, Skills screen (K), `/skill` admin command, level-gated vanilla tools/weapons/ores/logs with XP
- [ ] **Phase 3 – Woodcutting & Mining:** level-gated trees and ores, depletion and respawn, tool tiers
- [ ] **Phase 4 – Smithing & Crafting:** bars, forging, gear recipes with level requirements
- [ ] **Phase 5 – Combat:** Melee, Ranged and Constitution; equipment requirements; attack styles
- [ ] **Phase 6 – First boss:** arena, phases, telegraphed attacks, unique drops
- [ ] **Phase 7 – World generation:** new ores, tree types, structures and regions tied to skills
- [ ] **Phase 8+:** Fishing, Farming, Firemaking, Construction, Agility, Sailing, more bosses

## IP guidelines
- Mechanics and formulas may be inspired by OSRS.
- Do **not** copy Jagex names (items, NPCs, places, "Rune"-branded terms), textures, sounds or wiki text. Use original names and art.
