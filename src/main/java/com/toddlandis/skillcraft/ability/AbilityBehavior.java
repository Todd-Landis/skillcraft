package com.toddlandis.skillcraft.ability;

import java.util.List;

import com.mojang.serialization.MapCodec;

import net.minecraft.network.chat.Component;

// What an ability actually does. Each behaviour is a small Java class with its numbers read from
// the ability's JSON ("behavior": {"type": "skillcraft:melee_strike", ...}), so most new abilities
// are just a JSON file reusing an existing behaviour. Register new types in AbilityBehaviors.
public interface AbilityBehavior {
    MapCodec<? extends AbilityBehavior> codec();

    // Runs on the server after every check has passed. Return false if nothing happened (e.g. no
    // target in reach): the ability then costs nothing and goes on no cooldown.
    boolean execute(AbilityContext context);

    // Extra tooltip lines for the Ability Book ("Damage: 130% crush", "Radius: 4 blocks")
    default List<Component> details() {
        return List.of();
    }

    // Called every tick while a toggle ability is on (see AbilityDefinition.Toggle)
    default void tickToggle(AbilityContext context) {}

    // Called when a toggle ability turns off
    default void endToggle(AbilityContext context) {}
}
