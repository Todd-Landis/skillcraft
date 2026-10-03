# Handoff: Phase 5 — Combat skills + first boss (instanced arena)

You are continuing development of **SkillCraft**, an Old School RuneScape–inspired RPG mod for Minecraft Java Edition. The owner (Todd) is new to modding. He wants you to work **autonomously and thoroughly**: research, design, build, verify and document, making sensible decisions on your own. Record every judgment call in the design doc instead of stopping to ask. Ask only if something is truly blocking.

## Repo and environment
- GitHub: `Todd-Landis/skillcraft` (private). Default branch `main`. **Work on a new branch `phase-5-boss`**, commit in logical steps, push, and open a PR at the end. Don't merge it.
- Minecraft **26.2**, NeoForge **26.2.0.88**, ModDevGradle, **Java 25**. If Java 25 isn't installed, install a JDK 25 (e.g. Temurin) first.
- Build: `./gradlew build`. The GitHub Actions CI builds on every push and must stay green.
- **Read first:** `docs/DESIGN.md` (vision, tiers, every system and its numbers), `README.md` (layout), then skim `src/main/java/com/toddlandis/skillcraft/`.

## What exists (don't rebuild it)
- 13 skills with the OSRS XP curve. Player XP is a synced data attachment. `SkillManager.awardXp(player, skill, xp)` handles level-ups, XP drops and sync. Skills screen on **K**. `/skill get|set|addxp|reset`.
- **Tiers:** 10-level bands (I = 1 … X = 90). **Vanilla content caps around level 50 (tiers I–V). Tiers VI–X are for custom content.**
- Gathering via skilling nodes, Attunement Altar with server-verified minigames, Smelter, Forging Anvil and Crafting Bench (data-driven bench recipes), OSRS-style bank.
- **Gear only comes from players:** station-made gear is stamped `skillcraft:crafted_by`. Chests, villagers and mobs never supply unstamped gear or ingots (`loot/`, villager trade tag overrides).
- `combat/CombatGateEvents` already gates weapons by Melee/Ranged level. **There is no combat XP yet**, and Constitution does nothing yet.
- Data-driven tuning through data maps in `data/skillcraft/data_maps/`. Everything is server-authoritative and built for multiplayer.

## Your task
### A. Combat foundations (keep it lean; the boss needs it)
- **Combat XP**, OSRS-style: Melee XP from melee damage dealt to living mobs (≈4 XP per damage point), Ranged from projectile damage, and Constitution ≈1.33 XP per damage point from either. Guard against farming (e.g. no XP from damage to players, armor stands or baby mobs; consider spawner-mob limits).
- **Constitution:** max health scales with level via an attribute modifier. Keep vanilla feel at low levels and make it meaningful at high ones. Document the curve.
- Optional: simple OSRS attack styles (accurate / aggressive / defensive choosing which skill gets XP). Only if it stays clean.

### B. The first boss (the main event)
A **mid-level boss** for players around combat level 25–40, using roughly iron–diamond gear (tiers III–IV). **Light mechanics, but not a damage sponge:** the fight should be won by reading telegraphs and moving, not by out-gearing it.

1. **Research.** Look at mid-level OSRS bosses for *mechanic patterns* (e.g. Scurrius, Giant Mole, Sarachnis, Obor, Bryophyta, Deranged Archaeologist, Barrows, Zulrah-lite rotation ideas). Do **not** copy names, lore, art, sounds or wiki text (Jagex IP). Invent an original boss.
2. **Design doc first.** Write `docs/BOSS_<NAME>.md` before coding. Cover theme and lore, stats, 2–3 phases, every attack with its telegraph (sound + particle + animation) and counterplay, adds/hazards, enrage or soft-timer, expected kill time and death risk at the target level, rewards, and how it fits the tiers.
3. **Mechanics ideas** to adapt or improve: telegraphed AoE slams with safe spots; alternating melee/ranged styles signalled by stance (where wrong positioning is punished); hazard tiles; a vulnerability window after a big attack; add waves; HP-threshold phase changes; a simple attack rotation players can learn. Keep it readable in Minecraft's camera and controls.
4. **Instanced arena dimension:**
   - A custom dimension (`data/skillcraft/dimension*/`, void/flat generator, no mob spawning, fixed time and weather).
   - Each fight gets its own **instance** (e.g. arena plots spaced far apart in that dimension), built from a structure template or code, and cleaned up or reset after.
   - **The arena can't be damaged or built in.** Cancel block break/place, explosions, fluids, and so on in that dimension. Prefer real enforcement over adventure mode alone.
   - **Entry:** a craftable or obtainable entry mechanic in the overworld (e.g. an altar plus a summoning item made from earned resources), with solo or small-party support. **Exit:** on victory (reward chest or drop pickup, then return), on death (decide on and document item rules; OSRS-like "you keep your items and lose the attempt" is a reasonable default for this mod), and on logout or crash mid-fight (the player must never get stuck in the dimension).
   - Multiplayer: per-instance ownership, no one wandering into someone else's instance, and nothing leaking when the server restarts.
5. **Model work:**
   - Make the boss look **really cool**: a custom entity model (Java `LayerDefinition`/`EntityModel`, or Blockbench-exported geometry) with a custom texture and keyframe animations for idle, walk, each attack windup and strike, the phase change and death.
   - Check what animation tooling works on 26.2 (vanilla keyframe animation definitions vs. a library like GeckoLib, if it has a 26.2 build). Prefer no extra dependency unless it clearly pays off.
   - Textures: generate them procedurally (scripted pixel art) if you can't author images. Make them good, and keep sources so Todd can replace them.
   - Add a **boss health bar** (vanilla boss event) and distinct sounds (new sound events in `sounds.json`, vanilla audio is fine for now).
6. **Rewards:**
   - **One good unique drop.** An original item with a real mechanical identity, fitting the horizontal-progression pillar: a tradeoff rather than strictly better. It should sit in **tier V–VI** power terms. Give it a level requirement via the existing `skill_requirements` data map, and stamp it as earned so the gear rules allow it. Choose a fair rarity (OSRS mid-boss uniques are roughly 1/100–1/400; justify yours).
   - **A balanced resource drop table** (loot table JSON): raw ores, logs, coal, seeds and gems appropriate to the boss's tier. Balance it against the gathering rates in DESIGN.md (node yields, gathering fatigue) so killing the boss is a good but not dominant way to get resources.
   - Respect the existing rules: **no unstamped vanilla gear and no ingots** from the boss (those come from Smithing).
   - Verify the drop rates with a roll harness (see Verification). Grant combat XP for the kill through the normal system.

### C. Docs and polish
Update `docs/DESIGN.md` (roadmap Phase 5, the combat XP rules, the boss summary and link) and `README.md` (layout). Add lang entries for everything.

## Hard-won 26.2 / NeoForge API notes (save yourself time)
- **Always check APIs against the real sources before using them.** Decompiled Minecraft is at `build/moddev/artifacts/minecraft-patched-26.2.0.88-sources.jar` (after one build). NeoForge sources are in the Gradle cache (`neoforge-26.2.0.88-sources.jar`). Extract them with the JDK `jar xf …` tool; **`unzip` silently extracts nothing from these jars.**
- Renames in 26.x: `ResourceLocation` → `Identifier`; `GuiGraphics` → `GuiGraphicsExtractor`; screen `render` → `extractRenderState`; `renderBg` → `extractBackground`; `renderLabels` → `extractLabels`; `minecraft.setScreen` → `minecraft.gui.setScreen`.
- More renames: `level.random` is protected (use `getRandom()`); `BlockPos.getCenter` → `Vec3.atCenterOf`; `Item.getName()` needs a stack (use `getDefaultInstance().getHoverName()`); `Player.sendOverlayMessage` / `sendSystemMessage`; block break event is `event.level.block.BreakBlockEvent`.
- Player data uses data attachments (`registry/ModAttachments`). Attachments with `.sync(...)` sync automatically on change.
- Data maps (`registry/ModDataMaps`) are synced to clients and support `#tag` keys.
- **NeoForge ships its own copies of ~394 vanilla recipes.** The mod declares `ordering="AFTER"` on NeoForge in `neoforge.mods.toml` so our overrides win. Disable recipes with `{"neoforge:conditions":[{"type":"neoforge:never"}]}`.
- Global loot modifiers load automatically from `data/<ns>/loot_modifiers/*.json`. There is no `global_loot_modifiers.json` list in this version.
- Villager trades are data-driven (`villager_trade` + tag pools); see `scripts/gen_villager_trade_overrides.sh`.
- `ContainerData` syncs values as 16-bit shorts.
- Menu slot positions are final, so responsive screens move `leftPos`/`topPos` per panel (see `client/BankScreen`).
- The dev machine is Windows with Git Bash and has **no Python**. Write files with your file tools rather than shell heredocs that contain apostrophes.

## Verification (do all of it; don't claim untested things work)
1. `./gradlew build` is clean.
2. Run `./gradlew runGameTestServer` (headless server, no EULA needed). Then grep the output for `WARN`/`ERROR` lines that mention `skillcraft`, data parsing, dimensions or loot. This catches bad JSON, registry problems and client-only code on the server.
3. **Write real GameTests** where practical: the arena can't be broken, a player can enter and exit an instance, the boss spawns and phase transitions trigger, and loot rolls.
4. **Loot balance harness:** a temporary `ServerStartedEvent` debug class (delete it afterwards) that rolls the boss loot table thousands of times and logs averages and the unique rate. Report the numbers in the boss doc. A pure-logic harness can use the mod classpath from a temporary Gradle init script that prints `sourceSets.main.runtimeClasspath`.
5. If you have a display, `./gradlew runClient -Pworld="New World"` loads straight into a world. Check the logs for missing model or texture warnings. If you're headless, say what still needs a visual check by Todd.
6. Keep CI green, and push regularly.

## Ground rules
- Original names and art only, with no Jagex IP. Mechanics may be inspired by OSRS.
- Server-authoritative and multiplayer-safe. Data-driven where it's reasonable (boss stats and loot in JSON or data maps).
- Match the existing code style: short top-of-file comments explaining *why*, the registry classes in `registry/`, and lang keys for every string.
- Commit messages describe the change, ending with `Co-Authored-By` as configured.
- **At the end, report to Todd:** what was built, the design decisions you made and why, what you verified and how (with numbers), what still needs an in-game visual check, and suggested next steps.
