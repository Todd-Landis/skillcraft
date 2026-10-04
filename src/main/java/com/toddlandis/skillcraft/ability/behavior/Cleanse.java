package com.toddlandis.skillcraft.ability.behavior;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.toddlandis.skillcraft.ability.AbilityBehavior;
import com.toddlandis.skillcraft.ability.AbilityContext;
import com.toddlandis.skillcraft.registry.ModEffects;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

// Unshackle: removes slows, roots and stuns, then grants immunity to them for `immunity` ticks
public record Cleanse(int immunity) implements AbilityBehavior {
    public static final MapCodec<Cleanse> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.intRange(0, 1200).fieldOf("immunity").forGetter(Cleanse::immunity)
    ).apply(i, Cleanse::new));

    @Override
    public MapCodec<Cleanse> codec() {
        return CODEC;
    }

    @Override
    public boolean execute(AbilityContext context) {
        var player = context.player();
        player.removeEffect(ModEffects.ROOTED);
        player.removeEffect(ModEffects.SLOWED);
        player.removeEffect(ModEffects.STUNNED);
        player.removeEffect(MobEffects.SLOWNESS);
        if (immunity > 0) player.addEffect(new MobEffectInstance(ModEffects.UNSHACKLED, immunity, 0, false, true, true));
        context.level().sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1, player.getZ(), 16, 0.4, 0.6, 0.4, 0.05);
        Fx.sound(context.level(), player, SoundEvents.CHAIN_BREAK, 1f, 1f);
        return true;
    }

    @Override
    public List<Component> details() {
        return List.of(Component.translatable("ability.skillcraft.detail.cleanse", String.format("%.1f", immunity / 20.0)));
    }
}
