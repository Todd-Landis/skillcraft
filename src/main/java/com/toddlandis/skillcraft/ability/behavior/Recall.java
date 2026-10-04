package com.toddlandis.skillcraft.ability.behavior;

import java.util.List;

import com.mojang.serialization.MapCodec;
import com.toddlandis.skillcraft.ability.AbilityBehavior;
import com.toddlandis.skillcraft.ability.AbilityContext;
import com.toddlandis.skillcraft.ability.AbilityService;
import com.toddlandis.skillcraft.arena.ArenaDimension;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.portal.TeleportTransition;

// Hearthward: home to your respawn point (bed or anchor, else world spawn). The channel is the
// ability's windup, so moving cancels it. Refused in combat and inside a boss arena.
public record Recall() implements AbilityBehavior {
    public static final MapCodec<Recall> CODEC = MapCodec.unit(new Recall());

    @Override
    public MapCodec<Recall> codec() {
        return CODEC;
    }

    @Override
    public boolean execute(AbilityContext context) {
        ServerPlayer player = context.player();
        if (ArenaDimension.is(player.level())) {
            context.runtime().failKey = "message.skillcraft.ability_not_in_arena";
            return false;
        }
        if (AbilityService.data(player).inCombat(context.now())) {
            context.runtime().failKey = "message.skillcraft.ability_not_in_combat";
            return false;
        }
        context.level().sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1, player.getZ(), 40, 0.4, 0.8, 0.4, 0.3);
        Fx.sound(context.level(), player, SoundEvents.ENDERMAN_TELEPORT, 0.8f, 0.8f);
        TeleportTransition home = player.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.DO_NOTHING);
        player.teleport(home);
        player.resetFallDistance();
        return true;
    }

    @Override
    public List<Component> details() {
        return List.of(Component.translatable("ability.skillcraft.detail.recall"));
    }
}
