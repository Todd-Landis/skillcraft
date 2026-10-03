package com.toddlandis.skillcraft.ability;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

// Everything a behaviour needs while it runs
public record AbilityContext(ServerPlayer player, ServerLevel level, Identifier id, AbilityDefinition definition) {
    public AbilityRuntime runtime() {
        return AbilityRuntime.of(player);
    }

    public long now() {
        return level.getGameTime();
    }
}
