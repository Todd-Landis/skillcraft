package com.toddlandis.skillcraft.client;

import net.neoforged.neoforge.common.ModConfigSpec;

// Per-player display settings, saved to config/skillcraft-client.toml and editable in-game from
// Mods > SkillCraft > Config.
public final class ClientConfig {
    public enum AbilityBarPosition { RIGHT_EDGE, HOTBAR }

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.EnumValue<AbilityBarPosition> ABILITY_BAR_POSITION = BUILDER
            .comment("Where the ability bar sits: RIGHT_EDGE (a vertical column on the right side of the screen) or HOTBAR (a row beside the hotbar).")
            .defineEnum("abilityBarPosition", AbilityBarPosition.RIGHT_EDGE);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ClientConfig() {}

    public static boolean barOnRightEdge() {
        try {
            return ABILITY_BAR_POSITION.get() == AbilityBarPosition.RIGHT_EDGE;
        } catch (IllegalStateException notLoadedYet) {
            return true;
        }
    }
}
