package com.toddlandis.skillcraft.registry;

import com.toddlandis.skillcraft.SkillCraft;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, SkillCraft.MODID);

    // Applied by the Emberfall Maul: the target takes +15% damage from every source (see combat/CombatDamage)
    public static final DeferredHolder<MobEffect, MobEffect> SUNDERED = EFFECTS.register("sundered", () -> new SimpleEffect(MobEffectCategory.HARMFUL, 0xB0471A));

    // MobEffect's constructor is protected
    private static final class SimpleEffect extends MobEffect {
        SimpleEffect(MobEffectCategory category, int color) {
            super(category, color);
        }
    }
}
