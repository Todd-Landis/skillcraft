package com.toddlandis.skillcraft.registry;

import com.toddlandis.skillcraft.SkillCraft;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// Status effects. Attribute-based ones scale with the amplifier the ability JSON gives (e.g.
// Slowed is -10% speed per level), so their strength is data-driven. The rest are markers whose
// numbers live in data_maps/mob_effect/effect_tuning.json and are applied in ability/AbilityEvents.
public class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, SkillCraft.MODID);

    // Applied by the Emberfall Maul: the target takes +15% damage from every source (see combat/CombatDamage)
    public static final DeferredHolder<MobEffect, MobEffect> SUNDERED = EFFECTS.register("sundered", () -> new SimpleEffect(MobEffectCategory.HARMFUL, 0xB0471A));

    // --- Crowd control (bosses get softened versions, see ability/CrowdControl) ---
    // Takes damage every second (the amount is set by the ability that applied it)
    public static final DeferredHolder<MobEffect, MobEffect> BLEEDING = EFFECTS.register("bleeding", () -> new SimpleEffect(MobEffectCategory.HARMFUL, 0x9E1B1B));
    // Can't move or jump
    public static final DeferredHolder<MobEffect, MobEffect> ROOTED = EFFECTS.register("rooted", () -> new SimpleEffect(MobEffectCategory.HARMFUL, 0x5C7A2E)
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, id("rooted"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            .addAttributeModifier(Attributes.JUMP_STRENGTH, id("rooted_jump"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    // -10% movement speed per level
    public static final DeferredHolder<MobEffect, MobEffect> SLOWED = EFFECTS.register("slowed", () -> new SimpleEffect(MobEffectCategory.HARMFUL, 0x6F7C8A)
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, id("slowed"), -0.10, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    // Can't move, jump or attack (normal mobs only; bosses are Dazed instead)
    public static final DeferredHolder<MobEffect, MobEffect> STUNNED = EFFECTS.register("stunned", () -> new SimpleEffect(MobEffectCategory.HARMFUL, 0xE8D44D)
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, id("stunned"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            .addAttributeModifier(Attributes.JUMP_STRENGTH, id("stunned_jump"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    // Boss version of a stun: its next attack comes a moment later (shown on the boss)
    public static final DeferredHolder<MobEffect, MobEffect> DAZED = EFFECTS.register("dazed", () -> new SimpleEffect(MobEffectCategory.HARMFUL, 0xC9B8F0));

    // --- Buffs ---
    // More melee damage dealt, a little more damage taken (effect_tuning: damage_dealt, damage_taken)
    public static final DeferredHolder<MobEffect, MobEffect> BATTLE_TRANCE = EFFECTS.register("battle_trance", () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0xD9472B));
    // Less damage taken, no sprinting (effect_tuning: damage_taken)
    public static final DeferredHolder<MobEffect, MobEffect> BRACED = EFFECTS.register("braced", () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0x8C8C9C));
    // +1 armour per level, and reflects part of melee damage taken (effect_tuning: reflect)
    public static final DeferredHolder<MobEffect, MobEffect> BULWARK = EFFECTS.register("bulwark", () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0x4F6FA8)
            .addAttributeModifier(Attributes.ARMOR, id("bulwark"), 1.0, AttributeModifier.Operation.ADD_VALUE));
    // No single hit takes more than a share of max health; can't be knocked back (effect_tuning: max_hit)
    public static final DeferredHolder<MobEffect, MobEffect> LAST_STAND = EFFECTS.register("last_stand", () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0xB8862E)
            .addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, id("last_stand"), 1.0, AttributeModifier.Operation.ADD_VALUE));
    // Arrows fly straight and hit harder; heavy ammo pierces (effect_tuning: damage, pierce)
    public static final DeferredHolder<MobEffect, MobEffect> DEADEYE = EFFECTS.register("deadeye", () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0x3FA34D));
    // +10% movement speed per level
    public static final DeferredHolder<MobEffect, MobEffect> FLEET = EFFECTS.register("fleet", () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0x7FD6E8)
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, id("fleet"), 0.10, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    // Riposte's parry window, then the counter-attack being ready
    public static final DeferredHolder<MobEffect, MobEffect> PARRYING = EFFECTS.register("parrying", () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0xE0E6F0));
    public static final DeferredHolder<MobEffect, MobEffect> RIPOSTE = EFFECTS.register("riposte", () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0xF2C14E));
    // Immune to slows, roots and stuns
    public static final DeferredHolder<MobEffect, MobEffect> UNSHACKLED = EFFECTS.register("unshackled", () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0xF0F0F0));
    // Healing over time (the amount is set by the ability)
    public static final DeferredHolder<MobEffect, MobEffect> SECOND_WIND = EFFECTS.register("second_wind", () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0xE85D75));

    private static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(SkillCraft.MODID, "effect." + name);
    }

    // MobEffect's constructor is protected
    private static final class SimpleEffect extends MobEffect {
        SimpleEffect(MobEffectCategory category, int color) {
            super(category, color);
        }
    }
}
