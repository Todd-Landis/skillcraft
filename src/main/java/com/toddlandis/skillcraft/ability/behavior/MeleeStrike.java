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
import com.toddlandis.skillcraft.ability.AbilityEvents;
import com.toddlandis.skillcraft.ability.AbilityTargeting;
import com.toddlandis.skillcraft.ability.CrowdControl;
import com.toddlandis.skillcraft.combat.DamageStyle;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

// One hit on the enemy under the crosshair (within melee reach), with optional extras:
//   damage: share of the weapon's normal hit; style: force a damage type (default: the weapon's)
//   effects: debuffs; stun: ticks (bosses are Dazed instead); knockback: strength
//   disable_shield: ticks a blocking player's shield goes on cooldown
//   bleed: extra damage over bleed_duration ticks, as a share of the weapon's hit
// Used by Hamstring, Rending Cut, Concussive Blow, Cleaving Chop and Shield Bash.
public record MeleeStrike(float damage, Optional<DamageStyle> style, List<EffectSpec> effects, int stun, float knockback,
                          int disableShield, float bleed, int bleedDuration) implements AbilityBehavior {
    public static final MapCodec<MeleeStrike> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.floatRange(0, 10).fieldOf("damage").forGetter(MeleeStrike::damage),
            DamageStyle.CODEC.optionalFieldOf("style").forGetter(MeleeStrike::style),
            EffectSpec.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(MeleeStrike::effects),
            Codec.intRange(0, 200).optionalFieldOf("stun", 0).forGetter(MeleeStrike::stun),
            Codec.floatRange(0, 5).optionalFieldOf("knockback", 0f).forGetter(MeleeStrike::knockback),
            Codec.intRange(0, 400).optionalFieldOf("disable_shield", 0).forGetter(MeleeStrike::disableShield),
            Codec.floatRange(0, 10).optionalFieldOf("bleed", 0f).forGetter(MeleeStrike::bleed),
            Codec.intRange(20, 400).optionalFieldOf("bleed_duration", 80).forGetter(MeleeStrike::bleedDuration)
    ).apply(i, MeleeStrike::new));

    @Override
    public MapCodec<MeleeStrike> codec() {
        return CODEC;
    }

    @Override
    public boolean execute(AbilityContext context) {
        ServerPlayer player = context.player();
        LivingEntity target = AbilityTargeting.meleeTarget(player, 0.5);
        if (target == null) return false;
        player.swing(InteractionHand.MAIN_HAND, true);
        hit(context, target);
        return true;
    }

    // The hit and its extras (shared with the other melee behaviours)
    void hit(AbilityContext context, LivingEntity target) {
        ServerPlayer player = context.player();
        AbilityDamage.strike(player, target, damage, style.orElse(null));
        for (EffectSpec effect : effects) effect.apply(target, player);
        if (stun > 0) CrowdControl.stun(target, stun, player);
        if (knockback > 0) CrowdControl.knockback(target, player, knockback);
        if (disableShield > 0 && target instanceof Player blocker && blocker.isBlocking()) {
            blocker.getCooldowns().addCooldown(blocker.getUseItem(), disableShield);
            blocker.stopUsingItem();
        }
        if (bleed > 0) AbilityEvents.bleed(target, player, (float) (AbilityDamage.weaponHit(player) * bleed), bleedDuration);
        Fx.at(context.level(), target, stun > 0 ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.CRIT, stun > 0 ? 12 : 10, 0.4);
        Fx.sound(context.level(), target, stun > 0 ? SoundEvents.ANVIL_LAND : SoundEvents.PLAYER_ATTACK_STRONG, stun > 0 ? 0.5f : 1f, stun > 0 ? 1.6f : 1f);
    }

    @Override
    public List<Component> details() {
        List<Component> lines = new ArrayList<>();
        lines.add(Fx.damage(damage, style));
        if (bleed > 0) lines.add(Component.translatable("ability.skillcraft.detail.bleed", Math.round(bleed * 100), String.format("%.1f", bleedDuration / 20.0)));
        if (stun > 0) lines.add(Component.translatable("ability.skillcraft.detail.stun", String.format("%.1f", stun / 20.0)));
        for (EffectSpec effect : effects) lines.add(effect.describe());
        if (disableShield > 0) lines.add(Component.translatable("ability.skillcraft.detail.disable_shield", String.format("%.1f", disableShield / 20.0)));
        if (knockback > 0) lines.add(Component.translatable("ability.skillcraft.detail.knockback"));
        return lines;
    }
}
