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
5. Build a mod jar with `./gradlew build`. The jar is written to `build/libs/`.

## Project layout
```
src/main/java/com/toddlandis/skillcraft/
  SkillCraft.java          main mod class (runs on client and server)
  SkillCraftClient.java    client-only setup (screens, HUD, key bindings)
  Config.java              server config values
  skill/                   skill list and XP curve
src/main/resources/assets/skillcraft/
  lang/en_us.json          display names
  textures/                block, item and GUI textures (to come)
src/main/templates/META-INF/neoforge.mods.toml   mod metadata
docs/DESIGN.md             design and roadmap
```

## Resources
- NeoForge docs: https://docs.neoforged.net/
- NeoForged Discord: https://discord.neoforged.net/
- OSRS Wiki, for mechanics reference only: https://oldschool.runescape.wiki/

## Legal
This is a fan project. It is not affiliated with Jagex or Mojang. Mechanics are inspired by OSRS, but all names, textures and text in this mod are original. The NeoForge template files are MIT-licensed (see `TEMPLATE_LICENSE.txt`).
