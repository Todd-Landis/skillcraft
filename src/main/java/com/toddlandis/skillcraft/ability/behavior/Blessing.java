package com.toddlandis.skillcraft.ability.behavior;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.toddlandis.skillcraft.ability.AbilityBehavior;
import com.toddlandis.skillcraft.ability.AbilityContext;
import com.toddlandis.skillcraft.combat.Discipline;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;

// A Prayer blessing: stays on while its toggle drains Faith (docs/AGILITY_MAGIC_PRAYER.md section 3).
// Its numbers are read by PrayerEvents while it's on:
//   damage_dealt: {"melee": 0.10} extra damage for hits of that discipline
//   damage_taken: -0.05 changes all damage you take from creatures (negative = less)
//   protect: {"ranged": 0.40} less damage from hits of that discipline (a ward)
//   heal: {"amount": 1, "interval": 100} health every interval ticks
//   saving_grace: 0.25 a hit that would kill you leaves you at this share of max health instead,
//                 and spends all your Faith
public record Blessing(Map<Discipline, Float> damageDealt, float damageTaken, Map<Discipline, Float> protect, Optional<Heal> heal,
                       float savingGrace) implements AbilityBehavior {
    public record Heal(float amount, int interval) {
        public static final Codec<Heal> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.floatRange(0.1f, 20).fieldOf("amount").forGetter(Heal::amount),
                Codec.intRange(1, 1200).fieldOf("interval").forGetter(Heal::interval)
        ).apply(i, Heal::new));
    }

    private static final Codec<Map<Discipline, Float>> PER_DISCIPLINE = Codec.unboundedMap(Discipline.CODEC, Codec.floatRange(-1, 1));

    public static final MapCodec<Blessing> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            PER_DISCIPLINE.optionalFieldOf("damage_dealt", Map.of()).forGetter(Blessing::damageDealt),
            Codec.floatRange(-0.9f, 1).optionalFieldOf("damage_taken", 0f).forGetter(Blessing::damageTaken),
            PER_DISCIPLINE.optionalFieldOf("protect", Map.of()).forGetter(Blessing::protect),
            Heal.CODEC.optionalFieldOf("heal").forGetter(Blessing::heal),
            Codec.floatRange(0, 1).optionalFieldOf("saving_grace", 0f).forGetter(Blessing::savingGrace)
    ).apply(i, Blessing::new));

    @Override
    public MapCodec<Blessing> codec() {
        return CODEC;
    }

    @Override
    public boolean execute(AbilityContext context) {
        ServerPlayer player = context.player();
        context.level().sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.2, player.getZ(), 10, 0.4, 0.5, 0.4, 0.02);
        Fx.sound(context.level(), player, SoundEvents.AMETHYST_BLOCK_CHIME, 0.9f, 1.4f);
        return true;
    }

    @Override
    public void tickToggle(AbilityContext context) {
        heal.ifPresent(h -> {
            if (context.now() % h.interval() == 0 && context.player().getHealth() < context.player().getMaxHealth()) context.player().heal(h.amount());
        });
    }

    @Override
    public void endToggle(AbilityContext context) {
        Fx.sound(context.level(), context.player(), SoundEvents.AMETHYST_BLOCK_STEP, 0.6f, 0.8f);
    }

    public float dealtBonus(Discipline discipline) {
        return damageDealt.getOrDefault(discipline, 0f);
    }

    public float protection(Discipline discipline) {
        return protect.getOrDefault(discipline, 0f);
    }

    @Override
    public List<Component> details() {
        List<Component> lines = new ArrayList<>();
        damageDealt.forEach((d, v) -> lines.add(Component.translatable("ability.skillcraft.detail.blessing_dealt", Math.round(v * 100), d.displayName())));
        if (damageTaken != 0) lines.add(Component.translatable(damageTaken < 0 ? "ability.skillcraft.detail.blessing_taken_less" : "ability.skillcraft.detail.blessing_taken_more",
                Math.round(Math.abs(damageTaken) * 100)));
        protect.forEach((d, v) -> lines.add(Component.translatable("ability.skillcraft.detail.blessing_protect", Math.round(v * 100), d.displayName())));
        heal.ifPresent(h -> lines.add(Component.translatable("ability.skillcraft.detail.blessing_heal", String.format("%.1f", h.amount()), String.format("%.1f", h.interval() / 20.0))));
        if (savingGrace > 0) lines.add(Component.translatable("ability.skillcraft.detail.saving_grace", Math.round(savingGrace * 100)));
        return lines;
    }
}
