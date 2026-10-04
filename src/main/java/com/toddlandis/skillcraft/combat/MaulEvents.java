package com.toddlandis.skillcraft.combat;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.registry.ModEffects;
import com.toddlandis.skillcraft.registry.ModItems;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

// Emberfall Maul "Sunder": a fully charged hit (attack strength >= 90%) leaves the target
// Sundered for 6 seconds, so it takes +15% damage from everyone (see CombatDamage).
@EventBusSubscriber(modid = SkillCraft.MODID)
public final class MaulEvents {
    public static final float CHARGE_NEEDED = 0.9f;
    public static final int SUNDER_TICKS = 120;

    private MaulEvents() {}

    // Runs after the level gate (lowest priority), before the attack resets the charge meter
    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (event.isCanceled() || !(player.level() instanceof ServerLevel level)) return;
        if (!player.getMainHandItem().is(ModItems.EMBERFALL_MAUL.get()) || !(event.getTarget() instanceof LivingEntity target)) return;
        if (player.getAttackStrengthScale(0.5f) < CHARGE_NEEDED) return;
        target.addEffect(new MobEffectInstance(ModEffects.SUNDERED, SUNDER_TICKS, 0), player);
        level.sendParticles(ParticleTypes.LAVA, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 6, 0.3, 0.3, 0.3, 0);
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.5f, 0.7f);
    }
}
