# Handoff: Phase 6 — Combat Abilities (ability bar)

You're continuing **SkillCraft**, an OSRS-inspired RPG mod for Minecraft Java Edition (Minecraft 26.2, NeoForge 26.2.0.88, Java 25). The owner, Todd, is new to modding and wants you to work **autonomously and thoroughly**. Make sensible calls and record them in the docs. Ask only if something is truly blocking.

## Start here
1. **Branch:** if PR #1 (Phase 5, combat and Slagheart) is merged, branch from `main`; otherwise branch from `phase-5-boss`. Name the branch `phase-6-abilities`. Open a PR at the end and don't merge it.
2. **Read:**
   - `CLAUDE.md` (conventions, commands, 26.2 API notes)
   - **`docs/ABILITIES_SPEC.md` (your feature spec, the source of truth)**
   - `docs/COMBAT.md` (the damage pipeline, styles, typed armour, Sundered)
   - `docs/BOSS_SLAGHEART.md` (boss windows and the arena model)
   - `docs/DESIGN.md` (tiers: vanilla content caps around level 50)
3. Skim `combat/`, `boss/`, `client/` (especially `BankScreen` for responsive layouts and `XpDropOverlay` for HUD layers) and `registry/`.

## What to build (in this order, committing as you go)
1. **Framework:**
   - the `skillcraft:ability` datapack registry with a dispatch codec for behaviours
   - the player `abilities` attachment (loadouts, learned codices, Momentum)
   - server-side cooldowns and the global cooldown
   - the network payloads, with validation (unlocked, weapon class, cooldown, Momentum, in-combat rules)
   - weapon-class item tags
   - level-up unlock toasts
2. **HUD:** 4 slots right of the hotbar, plus keybinds, cooldown sweeps, the Momentum bar and "can't use" dimming. It must fit every GUI scale.
3. **Ability Book (J):** tabs, cards, drag-to-slot, 3 loadouts, search, the "usable with my weapon" filter, and locked Spells/Prayers tabs. Use the bank screen's responsive panel approach.
4. **Abilities:** at least the **14+** listed in spec §3, including the Weapon Arts **Concussive Blow**, **Vault** and **Forgequake**, plus the new effects (Bleed, Rooted, Dazed and so on).
   - **All damage must go through the existing combat damage pipeline**, so matchups, Sundered and XP stay consistent.
   - **Abilities give no XP of their own.**
5. **Bosses:** the crowd-control rules from spec §2.6 (Daze instead of stun, capped slows, no knockback; never cancel a windup that has already started). Add the **Codex** item and the *Codex of the Forgequake* to Slagheart's loot table, and re-run the loot harness so the existing rate targets still pass.
6. **Docs:** update the spec with every decision you made, `docs/COMBAT.md`, `docs/DESIGN.md` (roadmap Phase 6) and `README.md`, and add lang entries for everything.

## Non-negotiables
- **Server-authoritative:** the client only sends "use slot N" or "set loadout". Movement abilities must raycast-stop at blocks and can't leave a boss arena.
- **Data-driven:** ability numbers live in JSON so `/reload` can tune them.
- **Original names and art only** (no Jagex or other game IP). Generate icons procedurally (see `scripts/textures/`) and keep the generator.
- **Build for later:** don't build Spells or Prayers, but the framework must accept item-cost abilities and toggle-with-drain abilities without a rewrite. Prove it with a unit-level test or a stub behaviour.

## Verification (report real numbers)
- `./gradlew build` is clean, `./gradlew runGameTestServer` passes, and CI stays green.
- **GameTests:**
  - ability validation: locked, wrong weapon, cooldown, Momentum, global cooldown, off-GCD
  - Momentum gain and decay
  - damage through the pipeline, e.g. Concussive Blow on a skeleton shows the crush matchup
  - boss Daze rules
  - a movement ability stopping at a wall
  - a loadout swap blocked in combat
- **DPS harness:** simulate about 60 s against a dummy, auto-attacks only vs. a scripted rotation, and report the ratio. Target **+15–25%** (spec §6).
- **Screenshots:** if you have a display, extend the showcase (`dev/Showcase`, `client/ShowcaseClient`, run with `-Pshowcase=<scene>`) with a scene for the **HUD bar** and the **Ability Book**. Run it, look at the screenshots, and fix layout problems. If you're headless, list exactly what Todd should check.
- **Final report to Todd:** what you built, the decisions you made (including answers to spec §7), the test and harness numbers, and what needs an in-game feel check.
