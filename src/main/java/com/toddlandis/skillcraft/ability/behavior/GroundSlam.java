package com.toddlandis.skillcraft.ability.behavior;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.toddlandis.skillcraft.ability.AbilityBehavior;
import com.toddlandis.skillcraft.ability.AbilityContext;
import com.toddlandis.skillcraft.ability.AbilityDamage;
import com.toddlandis.skillcraft.ability.AbilityTargeting;
import com.toddlandis.skillcraft.combat.DamageStyle;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;

// Earthbreaker, Forgequake: a slam that hits every enemy within `radius` of the caster and
// applies `effects` to each (e.g. Sundered). Needs at least one enemy in range.
public record GroundSlam(float radius, float damage, Optional<DamageStyle> style, List<EffectSpec> effects) implements AbilityBehavior {
    public static final MapCodec<GroundSlam> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.floatRange(0, 12).fieldOf("radius").forGetter(GroundSlam::radius),
            Codec.floatRange(0, 10).fieldOf("damage").forGetter(GroundSlam::damage),
            DamageStyle.CODEC.optionalFieldOf("style").forGetter(GroundSlam::style),
            EffectSpec.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(GroundSlam::effects)
    ).apply(i, GroundSlam::new));

    @Override
    public MapCodec<GroundSlam> codec() {
        return CODEC;
    }

    @Override
    public boolean execute(AbilityContext context) {
        ServerPlayer player = context.player();
        List<LivingEntity> targets = AbilityTargeting.around(player, player.position(), radius, 3);
        if (targets.isEmpty()) return false;
        player.swing(InteractionHand.MAIN_HAND, true);
        for (LivingEntity target : targets) {
            AbilityDamage.strike(player, target, damage, style.orElse(null));
            for (EffectSpec effect : effects) effect.apply(target, player);
        }
        net.minecraft.core.particles.ParticleOptions magic = Fx.element(style.orElse(null));
        if (magic != null) {
            // A spell (Frost Nova): rings of the element's particles and a glassy burst
            for (float r = 1; r <= radius; r += 0.5f) Fx.ring(context.level(), player.position().add(0, 0.4, 0), r, magic);
            Fx.sound(context.level(), player, SoundEvents.GLASS_BREAK, 1f, 0.7f);
            Fx.sound(context.level(), player, SoundEvents.EVOKER_CAST_SPELL, 0.8f, 1.4f);
            return true;
        }
        for (float r = 1; r <= radius; r += 1) Fx.ring(context.level(), player.position(), r, ParticleTypes.LAVA);
        Fx.ring(context.level(), player.position(), radius, ParticleTypes.POOF);
        Fx.sound(context.level(), player, SoundEvents.MACE_SMASH_GROUND_HEAVY, 1f, 0.8f);
        return true;
    }

    @Override
    public List<Component> details() {
        List<Component> lines = new ArrayList<>();
        lines.add(Fx.damage(damage, style));
        lines.add(Fx.blocks("radius", radius));
        for (EffectSpec effect : effects) lines.add(effect.describe());
        return lines;
    }
}
