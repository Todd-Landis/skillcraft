package com.toddlandis.skillcraft.ability.behavior;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import com.toddlandis.skillcraft.combat.DamageStyle;

// Particle, sound and tooltip helpers shared by the behaviours
final class Fx {
    private Fx() {}

    static void at(ServerLevel level, Entity entity, ParticleOptions particle, int count, double spread) {
        level.sendParticles(particle, entity.getX(), entity.getY() + entity.getBbHeight() * 0.6, entity.getZ(), count, spread, spread * 0.6, spread, 0.05);
    }

    static void ring(ServerLevel level, Vec3 center, double radius, ParticleOptions particle) {
        int points = (int) Math.max(12, radius * 10);
        for (int i = 0; i < points; i++) {
            double a = Math.PI * 2 * i / points;
            level.sendParticles(particle, center.x + Math.cos(a) * radius, center.y + 0.1, center.z + Math.sin(a) * radius, 1, 0, 0.05, 0, 0.01);
        }
    }

    static void sound(ServerLevel level, Entity at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.getX(), at.getY(), at.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    static Component damage(float share, java.util.Optional<DamageStyle> style) {
        return style.map(s -> Component.translatable("ability.skillcraft.detail.damage_typed", Math.round(share * 100), s.displayName()))
                .orElse(Component.translatable("ability.skillcraft.detail.damage", Math.round(share * 100)));
    }

    static Component blocks(String key, float value) {
        return Component.translatable("ability.skillcraft.detail." + key, String.format("%.1f", value));
    }
}
