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
import com.toddlandis.skillcraft.combat.DamageStyle;
import com.toddlandis.skillcraft.magic.MagicCasting;
import com.toddlandis.skillcraft.magic.SpellBolt;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

// Fireball, Rime Snare, Chain Lightning: fire a spell bolt from the main-hand focus.
//   damage: share of the focus's power; style: element (default: the focus's)
//   speed: blocks per tick; radius: burst on impact (0 = only what it hits)
//   ignite: ticks of fire; effects: debuffs on everything hit
//   chain: {count, range, falloff} jumps to nearby enemies after the first hit
//   knockback: shove (not bosses)
// Always fires (a miss still costs the spell), like loosing an arrow.
public record CastBolt(float damage, Optional<DamageStyle> style, float speed, float radius, int ignite, List<EffectSpec> effects,
                       Optional<Chain> chain, float knockback) implements AbilityBehavior {
    public record Chain(int count, float range, float falloff) {
        public static final Codec<Chain> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(1, 10).fieldOf("count").forGetter(Chain::count),
                Codec.floatRange(1, 16).optionalFieldOf("range", 5f).forGetter(Chain::range),
                Codec.floatRange(0, 0.9f).optionalFieldOf("falloff", 0.25f).forGetter(Chain::falloff)
        ).apply(i, Chain::new));
    }

    public static final MapCodec<CastBolt> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.floatRange(0, 10).fieldOf("damage").forGetter(CastBolt::damage),
            DamageStyle.CODEC.optionalFieldOf("style").forGetter(CastBolt::style),
            Codec.floatRange(0.5f, 4f).optionalFieldOf("speed", MagicCasting.BOLT_SPEED).forGetter(CastBolt::speed),
            Codec.floatRange(0, 8).optionalFieldOf("radius", 0f).forGetter(CastBolt::radius),
            Codec.intRange(0, 400).optionalFieldOf("ignite", 0).forGetter(CastBolt::ignite),
            EffectSpec.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(CastBolt::effects),
            Chain.CODEC.optionalFieldOf("chain").forGetter(CastBolt::chain),
            Codec.floatRange(0, 5).optionalFieldOf("knockback", 0f).forGetter(CastBolt::knockback)
    ).apply(i, CastBolt::new));

    @Override
    public MapCodec<CastBolt> codec() {
        return CODEC;
    }

    @Override
    public boolean execute(AbilityContext context) {
        ServerPlayer player = context.player();
        DamageStyle element = style.orElse(AbilityDamage.defaultStyle(player));
        float amount = (float) (AbilityDamage.basePower(player, element) * damage);
        SpellBolt.Payload payload = new SpellBolt.Payload(element, amount, true, radius, ignite, effects,
                chain.map(Chain::count).orElse(0), chain.map(Chain::range).orElse(0f), chain.map(Chain::falloff).orElse(0f), knockback);
        player.swing(InteractionHand.MAIN_HAND, true);
        MagicCasting.fire(player, payload, speed, true);
        return true;
    }

    @Override
    public List<Component> details() {
        List<Component> lines = new ArrayList<>();
        lines.add(Fx.damage(damage, style));
        if (radius > 0) lines.add(Fx.blocks("burst", radius));
        chain.ifPresent(c -> lines.add(Component.translatable("ability.skillcraft.detail.chain", c.count(), String.format("%.0f", c.range()),
                Math.round(c.falloff() * 100))));
        if (ignite > 0) lines.add(Component.translatable("ability.skillcraft.detail.ignite", String.format("%.1f", ignite / 20.0)));
        for (EffectSpec effect : effects) lines.add(effect.describe());
        if (knockback > 0) lines.add(Component.translatable("ability.skillcraft.detail.knockback"));
        return lines;
    }
}
