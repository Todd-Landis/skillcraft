# SkillCraft

An Old School RuneScape–inspired RPG mod for Minecraft Java Edition (NeoForge). It adds skills you level up, resources gated behind those levels, crafting chains, bosses built around fight mechanics, and horizontal gear progression, while keeping Minecraft's building and exploring at the centre.

> Working title. The mod ID `skillcraft` is set in `gradle.properties` and `SkillCraft.MODID`.

See [docs/DESIGN.md](docs/DESIGN.md) for the design and roadmap.

## Tech
| | |
|---|---|
| Minecraft | 26.2 |
| Loader | NeoForge 26.2.0.88 |
| Java | 25 |
| Build | Gradle + ModDevGradle |

## Getting started
1. Install **JDK 25**, for example [Eclipse Temurin](https://adoptium.net/).
2. Install **IntelliJ IDEA Community Edition**.
3. In IntelliJ, open this folder (`File → Open`) and wait for the Gradle import to finish. The first import downloads Minecraft and takes several minutes.
4. Launch the game with your mod loaded: open the Gradle panel and run `skillcraft → Tasks → mod development → runClient`. After the first run, it is also in the run dropdown at the top of the window.
   You can also run it from a terminal:
   ```
   ./gradlew runClient
   ```
5. In game: press **K** for the Skills screen, **H** for the Bond Codex, **V** to change attack style, **J** for the Ability Book, and **R / Z / B / N** for the four ability slots. Chop down grown trees until one drops an Oak Heart (or take one from the creative tab), craft an Attunement Altar, load the heart, play the minigame, then place the node and right-click it with an axe. With cheats on, `/skill set @s mining 30` helps test level gates.
6. Build a mod jar with `./gradlew build`. The jar is written to `build/libs/`.
7. Run the automated tests with `./gradlew runGameTestServer`. For a full local test plan (screenshot showcase, in-game checklist), see [docs/HANDOFF_LOCAL_TESTING.md](docs/HANDOFF_LOCAL_TESTING.md).

## Project layout
```
src/main/java/com/toddlandis/skillcraft/
  SkillCraft.java          main mod class (runs on client and server)
  SkillCraftClient.java    client-only setup (screens, HUD, key bindings)
  Config.java              server config values
  skill/                   skills, XP curve, player skill data, SkillManager (XP + level-ups)
  gathering/               level gates for breaking world blocks
  node/                    skilling nodes, NodeType stats, auto-gathering sessions, Elemental Hearts (drops from the vanilla world)
  altar/                   Attunement Altar (turns Elemental Hearts into nodes)
  leycraft/                Leycraft: Ley Altar, resonance, weaving motes (docs/LEYCRAFT.md)
  bonding/                 Beastbonding: Bond Codex, summons, Cliffstrider, Cloudray (docs/BEASTBONDING.md)
  magic/                   wands and staves (MagicFocus), motes, bolts (SpellBolt), casting
  prayer/                  Faith, blessings in combat, Votive Shrine, bone offerings
  agility/                 parkour XP, safe falls and sprint perks, recent movement for Sidestep
  ability/                 combat abilities: definitions (JSON), behaviours, the ability bar,
                           Momentum, cooldowns, crowd control, Codex
  combat/                  damage types, matchups, typed armour, combat XP, attack styles,
                           Constitution health, equipment level gates, light/heavy arrows
  arena/                   instanced boss arena dimension: plots, protection, entry/exit
  boss/                    Slagheart boss (attack state machine, projectile, rewards)
  command/                 /skill admin command
  network/                 server -> client packets
  registry/                attachments and data map registrations
  client/                  Skills screen, XP drop HUD, key binding, tooltips
  altar/minigame/          attunement minigame rules (shared with the server for verification)
  smithing/                Smelter (processing station)
  cooking/                 Cooking Range (processing station with burn chance), cooking recipes
  fishing/                 fishing spots (entities on open water), shoals, water rules, auto-fishing, line
  bench/                   Forging Anvil + Crafting Bench (recipe benches, queue)
  loot/                    keeps generated chests and mob drops free of crafted gear
  bank/                    OSRS-style player bank (Bank Chest)
  client/minigame/         minigame drawing
  client/fishing/          fishing spot renderer (ripples and foam)
  client/boss/             Slagheart model, renderer and glow layer
  client/ability/          ability HUD bar, Ability Book screen, ability keys
  gametest/                GameTests (./gradlew runGameTestServer, also run in CI)
  dev/                     dev-only screenshot showcase (-Pshowcase; does nothing otherwise)
src/main/resources/data/skillcraft/data_maps/   level requirements, XP values, combat data, boss and effect tuning (JSON)
src/main/resources/data/skillcraft/skillcraft/ability/   one JSON per combat ability
src/main/resources/data/skillcraft/dimension*/  the boss arena dimension
src/main/resources/assets/skillcraft/neoforge/animations/   boss keyframe animations (JSON)
src/main/resources/assets/skillcraft/
  lang/en_us.json          display names
  textures/                block, item and GUI textures (to come)
src/main/templates/META-INF/neoforge.mods.toml   mod metadata
docs/DESIGN.md             design and roadmap
docs/COMBAT.md             combat system rules and numbers
docs/BOSS_SLAGHEART.md     the first boss
docs/ABILITIES_SPEC.md     combat abilities: spec and as-built decisions
docs/AGILITY_MAGIC_PRAYER.md   Agility, Magic and Prayer: design, numbers and decisions
docs/HANDOFF_LOCAL_TESTING.md   how to build, test and iterate locally
CLAUDE.md                  notes for Claude Code (commands, conventions, 26.2 API notes)
scripts/ci/                client smoke test (CI) and screenshot preview tool
scripts/textures/          procedural texture generators (run with java <file>.java)
```

## Resources
- NeoForge docs: https://docs.neoforged.net/
- NeoForged Discord: https://discord.neoforged.net/
- OSRS Wiki, for mechanics reference only: https://oldschool.runescape.wiki/

## Legal
This is a fan project. It is not affiliated with Jagex or Mojang. Mechanics are inspired by OSRS, but all names, textures and text in this mod are original. The NeoForge template files are MIT-licensed (see `TEMPLATE_LICENSE.txt`).
