package com.toddlandis.skillcraft.ability.behavior;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.toddlandis.skillcraft.ability.AbilityBehavior;
import com.toddlandis.skillcraft.ability.AbilityContext;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

// The shape future Prayers will take: an effect that stays on while the ability is toggled on
// (the definition's "toggle" drains a resource every tick). No shipped ability uses it yet; the
// abilities_future GameTest proves the framework handles it.
public record ToggleEffect(Holder<MobEffect> effect, int amplifier) implements AbilityBehavior {
    public static final MapCodec<ToggleEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BuiltInRegistries.MOB_EFFECT.holderByNameCodec().fieldOf("effect").forGetter(ToggleEffect::effect),
            Codec.intRange(0, 9).optionalFieldOf("amplifier", 0).forGetter(ToggleEffect::amplifier)
    ).apply(i, ToggleEffect::new));

    @Override
    public MapCodec<ToggleEffect> codec() {
        return CODEC;
    }

    @Override
    public boolean execute(AbilityContext context) {
        refresh(context);
        return true;
    }

    @Override
    public void tickToggle(AbilityContext context) {
        if (context.now() % 20 == 0) refresh(context);
    }

    @Override
    public void endToggle(AbilityContext context) {
        context.player().removeEffect(effect);
    }

    private void refresh(AbilityContext context) {
        context.player().addEffect(new MobEffectInstance(effect, 40, amplifier, false, false, true));
    }

    @Override
    public List<Component> details() {
        return List.of(Component.translatable("ability.skillcraft.detail.toggle", effect.value().getDisplayName()));
    }
}
