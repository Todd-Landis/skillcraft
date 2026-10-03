package com.toddlandis.skillcraft.ability.behavior;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.toddlandis.skillcraft.ability.AbilityBehavior;
import com.toddlandis.skillcraft.ability.AbilityContext;
import com.toddlandis.skillcraft.ability.AbilityRuntime;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

// Quick Draw, Pinning Shot, Piercing Bolt: the next projectile fired within `window` ticks gets a
// bonus (see AbilityEvents, which applies it when the arrow spawns and when it hits)
public record NextShot(Shot shot) implements AbilityBehavior {
    public static final MapCodec<NextShot> CODEC = Shot.CODEC.xmap(NextShot::new, NextShot::shot);

    // window: ticks to fire the shot in; damage: multiplier; instant_charge: a bow shot counts as
    // fully drawn; root: ticks the target is rooted; pierce: extra targets it passes through
    public record Shot(int window, float damage, boolean instantCharge, int root, int pierce) {
        public static final MapCodec<Shot> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.intRange(1, 1200).fieldOf("window").forGetter(Shot::window),
                Codec.floatRange(0, 10).optionalFieldOf("damage", 1f).forGetter(Shot::damage),
                Codec.BOOL.optionalFieldOf("instant_charge", false).forGetter(Shot::instantCharge),
                Codec.intRange(0, 400).optionalFieldOf("root", 0).forGetter(Shot::root),
                Codec.intRange(0, 10).optionalFieldOf("pierce", 0).forGetter(Shot::pierce)
        ).apply(i, Shot::new));

        public List<Component> details() {
            List<Component> lines = new ArrayList<>();
            if (instantCharge) lines.add(Component.translatable("ability.skillcraft.detail.instant_charge"));
            if (damage != 1f) lines.add(Component.translatable("ability.skillcraft.detail.next_shot_damage", Math.round((damage - 1) * 100)));
            if (root > 0) lines.add(Component.translatable("ability.skillcraft.detail.root", String.format("%.1f", root / 20.0)));
            if (pierce > 0) lines.add(Component.translatable("ability.skillcraft.detail.pierce", pierce));
            lines.add(Component.translatable("ability.skillcraft.detail.window", String.format("%.1f", window / 20.0)));
            return lines;
        }
    }

    @Override
    public MapCodec<NextShot> codec() {
        return CODEC;
    }

    @Override
    public boolean execute(AbilityContext context) {
        arm(context, shot);
        context.level().sendParticles(ParticleTypes.ENCHANTED_HIT, context.player().getX(), context.player().getY() + 1.2, context.player().getZ(), 8, 0.3, 0.3, 0.3, 0.05);
        Fx.sound(context.level(), context.player(), SoundEvents.CROSSBOW_LOADING_END.value(), 0.8f, 1.4f);
        return true;
    }

    static void arm(AbilityContext context, Shot shot) {
        AbilityRuntime runtime = context.runtime();
        runtime.nextShot = shot;
        runtime.nextShotUntil = context.now() + shot.window();
    }

    @Override
    public List<Component> details() {
        return shot.details();
    }
}
