# Handoff: testing Phase 5 locally (combat + Slagheart)

These notes are for Todd and for a Claude Code session running on Todd's laptop. They cover how to build and run the `phase-5-boss` branch, what has already been checked in CI, what only a person can judge in-game, and how to iterate.

Project conventions, commands and 26.2 API notes are in `CLAUDE.md`. Claude Code loads that file automatically.

## 1. Setup (once)
1. Install **JDK 25** (for example Eclipse Temurin 25) and check it with `java -version`.
2. Get the branch:
   ```
   git fetch origin
   git checkout phase-5-boss
   git pull
   ```
3. Build: `./gradlew build` in Git Bash, or `gradlew.bat build` in PowerShell or cmd.
   - The first run downloads Minecraft and NeoForge (about 1 GB) and takes several minutes.
   - It must end with `BUILD SUCCESSFUL`.
4. Run the GameTests: `./gradlew runGameTestServer`. It must end with `All 11 required tests passed :)`.
   - The `[loot_rates]` lines in its output are the loot harness numbers.
   - `Arena dimension skillcraft:arena is not loaded` is expected here, because the test server only creates vanilla dimensions.

## 2. Automatic screenshot check (about 3 minutes; a local Claude session can run it)
The showcase loads a world, puts the boss in front of the camera next to two zombies:
- one in padded armour holding a diamond warhammer
- one holding the Emberfall Maul

It then plays every boss animation, saves screenshots and quits the game.

**It replaces your hotbar and spawns entities, so only use a throwaway world.**

1. Create the world. Pick one:
   - **In-game:** run `./gradlew runClient` and create a **Superflat** world called `showcase` (cheats on), then quit.
   - **From the command line:** `bash scripts/ci/client_smoke.sh world`. This starts a dedicated server that creates a flat world, and needs `run/eula.txt`, which the script writes. The world is copied to `run/saves/showcase`.
2. Run `./gradlew runClient -Pshowcase -Pworld=showcase`.
3. Check the results:
   - **Screenshots:** about 21 files, `run/screenshots/showcase_*.png`, two per animation plus `idle`, `hud` and `night_vent`.
   - **Log:** it should contain `[showcase] done: N screenshots`.
   - **Problems:** search the log for `ERROR` or `WARN` lines that mention `skillcraft`, `Missing`, `Unable to load model` or `animation`.

The **Client smoke** workflow can run the same steps in CI (manual trigger only; Xvfb and software rendering). Its first run did not finish within 11 minutes and was cancelled, so running it locally is the reliable route for now.

## 3. In-game test plan (Todd)
Use a creative superflat world with cheats on. Commands:

```
/skill set @s melee 45
/skill set @s ranged 30
/skill set @s defence 30
/skill set @s constitution 40
/skill set @s smithing 20
/give @s skillcraft:summoning_brazier
/give @s skillcraft:ember_sigil 4
/give @s skillcraft:diamond_warhammer
/give @s skillcraft:emberfall_maul
/give @s skillcraft:heavy_arrow 32
/give @s skillcraft:light_arrow 32
/give @s minecraft:bow
/give @s skillcraft:padded_chestplate
/gamemode survival
```

**Switch to survival before you fight.** The boss ignores creative and spectator players, and creative players earn no combat XP.

### A. Combat basics (overworld)
- [ ] Hit zombies and skeletons with a sword, a spear and a warhammer.
  - Skeletons should show "Effective" for crush and "Resisted" for stab.
  - XP drops appear for Melee and Constitution.
- [ ] Press **V** to cycle the attack style. The Skills screen footer (**K**) shows it, and Defensive sends XP to Defence.
- [ ] Sneak and right-click a mob with an empty hand to examine its weaknesses.
- [ ] Shoot light and heavy arrows. Light arrows fly flat and fast. Heavy arrows arc, hit harder and knock back more.
- [ ] **Level gates:**
  - Run `/skill set @s melee 1`; a warhammer swing should be blocked with a message.
  - Run `/skill set @s defence 1` and try on padded armour; it should pop back into your inventory.
  - Set your levels back afterwards.
- [ ] Check the tooltips: weapon style, ammo weight, typed defence, the maul and sigil lore, and the earned-by line.
- [ ] **Constitution 40:** you should have 28 max health (14 hearts).

### B. The boss
1. Place the **Summoning Brazier** and right-click it with an **Ember Sigil**. You (and up to 3 friends within 5 blocks) are teleported into the arena.
2. Check the arena:
   - [ ] the floor pattern, pillars, rim wall, lighting and the barrier dome
   - [ ] you can't break, place, or use buckets or flint and steel
3. Check the fight:
   - [ ] **Awaken:** it plays an awaken animation and can't be damaged during it.
   - [ ] **Phase 1:**
     - Hammerfall: a ring telegraph, then a slam; step out of the ring.
     - Tong Sweep: a 150° cone in front; back off or get behind it.
     - Slag Spit: arcing globs that leave puddles.
     - Vent: the chest doors open and the heart glows. It's weak to stab here (×1.5).
   - [ ] **Phase 2** (below 65%): a roar plus a heat-pulse knockback, Cinder Mites, and Ember Rain (3×3 magma tiles that erupt after 1.5 s). It takes half damage while mites live.
   - [ ] **Phase 3** (below 30%): double Hammerfall and Forge Quake rings you jump over.
   - [ ] **Boss bar:** red, notched, and the name shows the phase.
   - [ ] **Enrage after 6 minutes:** a warning, then stacking damage.
   - [ ] **Death:** the death animation, then the Reward Chest appears in the centre.
   - [ ] Right-click the chest for your loot. Ores, logs, gems and seeds only (no ingots); a sigil 1 time in 20; the maul 1 in 120.
   - [ ] Right-click the Exit Portal to go home. After 90 s everyone is sent home automatically.
4. Check the edge cases:
   - [ ] **Dying in the arena:** you're healed and sent home with your items, and the attempt is lost.
   - [ ] **Logging out mid-fight:** when you log back in, you're back where you entered.
   - [ ] **Closing the game mid-fight, then reloading:** you're sent home, and no boss is left over.
   - [ ] **Two players:** both enter together and boss health scales up (+60% per extra player). Only players who dealt at least 5% of its health get loot.
5. Check the maul:
   - [ ] A fully charged hit applies **Sundered** (an effect icon on the target, +15% damage taken).
   - [ ] You move 10% slower while holding it.
   - [ ] You can't raise a shield in the offhand.

### D. Combat abilities (Phase 6)
Details are in `docs/ABILITIES_SPEC.md` §8.

**Screenshots first.** Run `./gradlew runClient -Pshowcase=abilities -Pworld=showcase`. It saves about 11 screenshots: the HUD bar ready and on cooldown, plus the Ability Book at GUI scales 2, 3 and 4.

**Setup:** run `/skill set @s melee 40`, `/skill set @s ranged 40`, `/skill set @s defence 40` and `/skill set @s constitution 40`, then switch to **survival**. Creative unlocks everything, which hides the level gates.

- [ ] **Ability Book (J):**
  - Tabs work, including the locked Spells and Prayers tabs.
  - Locked cards say what unlocks them.
  - Search and the "My weapon" filter work.
  - Drag a card onto a slot, click a slot and then a card, and right-click a slot to clear it.
  - Switch between the 3 bars, rename one, and use "Use this bar".
- [ ] **HUD bar:**
  - It sits right of the hotbar and doesn't overlap the offhand slot, attack indicator or XP drops.
  - Try GUI scales 1–4 and a left-handed main hand.
  - The cooldown sweep, the global cooldown flash and the windup bar show up.
  - Slots dim when you're holding the wrong weapon or lack Momentum.
  - The Momentum bar fills as you fight and drains about 10 s after combat ends.
- [ ] **Keys:** R, Z, B and N fire the 4 slots, and you can rebind them under Controls → "SkillCraft – Abilities".
- [ ] **Feel of each ability:**
  - **Lunge:** dash and hit.
  - **Hamstring:** the target is slowed.
  - **Rending Cut:** Bleed damage numbers appear.
  - **Sweeping Arc:** a short windup, then hits everything in front of you.
  - **Battle Trance:** you hit harder and take more damage.
  - **Quick Draw:** an instant full-power bow shot.
  - **Pinning Shot:** the target is rooted.
  - **Tumble:** you leap backwards.
  - **Volley:** a fan of 5 arrows that uses up to 3 of yours.
  - **Deadeye:** arrows fly straight; heavy arrows pierce.
  - **Brace, Riposte** (time it right before a hit), **Unshackle, Second Wind, Bulwark, Last Stand, Sprint Burst.**
  - **Weapon Arts:**
    - Concussive Blow and Earthbreaker need a warhammer or mace.
    - Vault needs a spear; check it clears Forge Quake rings.
    - Skewer, Cleaving Chop with an axe, and Shield Bash with a shield in the off hand.
    - Piercing Bolt needs a crossbow.
    - **Off-hand arts:**
      - Twinfang: a sword or axe in the off hand. The off-hand arm should swing, and the damage should follow the off-hand blade.
      - Torchbrand: a torch in the off hand sets the target alight.
    - Forgequake: read the Codex (`/give @s skillcraft:codex[skillcraft:codex_ability="skillcraft:forgequake"]`), then hold the Emberfall Maul.
- [ ] **Walls and the arena:** Lunge, Vault and Tumble never pass through walls and can't leave the arena.
- [ ] **In combat:** switching bars is refused with a message.
- [ ] **Unlock toast:** level up into a new ability, e.g. `/skill set @s defence 9`, then earn XP to reach 10. A toast should appear.
- [ ] **Slagheart:**
  - Concussive Blow dazes it and its next attack is delayed, but a slam that's already winding up still happens.
  - Slows are mild and knockback does nothing.
  - Spending Surges and Ultimates during **Vent** feels rewarding.
  - Kills should feel roughly 15–20% faster than without abilities.

### C. Things only a person can judge
These matter most:
- **Slagheart model and animations:** proportions, whether every attack reads clearly, and whether windups are long enough to react to.
- **The glow layer:** whether the heart, visor and cracks look good in the dark.
- **Item icons:** warhammers, arrows, padded armour, sigil and maul.
- **Padded armour** worn on a player.
- **Sounds:** they currently reuse vanilla audio (anvil, ravager, blaze and so on). Check that the volumes are sensible.
- **Ability feel:** windup lengths, cooldowns and Momentum pacing. They're tuned in `data/skillcraft/skillcraft/ability/*.json` (applied with `/reload`) and in the server config.
- **Fight feel:** is it too easy or too hard at combat 25–40 with iron or diamond gear? The numbers to tune are in `data/skillcraft/data_maps/entity_type/boss_tuning.json` (`/reload` applies them).

## 4. Iterating with local Claude
- **Starting:** open Claude Code in the repo folder and point it at this file and `docs/BOSS_SLAGHEART.md`. A prompt that works:
  > Read docs/HANDOFF_LOCAL_TESTING.md and CLAUDE.md. Run the build, the GameTests and the screenshot showcase (section 2), look at every screenshot, and report problems with file and line pointers. Then wait for my in-game notes.
- **Bug reports:** paste the relevant log lines from `run/logs/latest.log`, or the crash report from `run/crash-reports/`, and say what you did and what you expected.
- **Quick tuning loops:**
  - **Boss numbers, loot table, mob weaknesses, level gates:** edit the JSON under `src/main/resources/data/skillcraft/`, then `/reload`.
    - After editing the loot table, re-run `./gradlew runGameTestServer`. The loot test fails if a kill drifts outside 6.5–9 gathering-minutes or the drop rates drift.
  - **Animations:** edit `assets/skillcraft/neoforge/animations/entity/slagheart/*.json`, then press **F3+T** in-game.
    - Use `minecraft:rotation` in degrees and `minecraft:position` in pixels.
    - The bone names are in `client/boss/SlagheartModel`. A wrong bone name logs an error instead of crashing.
  - **Textures:** edit `scripts/textures/GenSlagheartTexture.java` or `GenCombatTextures.java`, run it with `java <file>.java`, then press **F3+T**. You can also paint over the PNGs directly. If you do, stop re-running the generator for that file.
  - **Model shape:** edit `client/boss/SlagheartModel.createBodyLayer()`, which needs a game restart.
    - If you move or resize cubes, update the matching entry in the texture generator's cube table so the UVs still line up.
- **Before pushing:** run `./gradlew build` and `./gradlew runGameTestServer`, and keep CI green. The Build workflow runs on every push.

## 5. Known gaps and ideas
- **Not yet checked in-game on a real GPU:** everything in section 3C.
- **The Ember Sigil recipe** (anvil, Smithing 20) needs the full Smithing chain. Use `/give` for quick tests.
- **Sounds** are placeholders using vanilla audio. Drop `.ogg` files in `assets/skillcraft/sounds/` and point `sounds.json` at them.
- **Arena instances are in memory only:** a crash loses the fight (by design; nobody gets stranded).
- **Ideas for later:**
  - more mid-tier bosses reusing the arena and boss tuning data map, each with a unique that fills a niche (a stab and a ranged unique would round out the triangle)
  - custom audio
  - higher gear tiers (VI–X)
