package com.toddlandis.skillcraft.ability.behavior;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.toddlandis.skillcraft.ability.AbilityBehavior;
import com.toddlandis.skillcraft.ability.AbilityContext;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

// Vault (forward) and Tumble (backward): a real jump with momentum, so normal collision stops it at
// walls and barriers (it can't leave a boss arena). The launch speed is worked out from the
// distance and lift so the landing is about `distance` blocks away on flat ground.
// next_shot: optional bonus for the next projectile (Tumble's +20%).
public record Leap(float distance, boolean backward, float lift, Optional<NextShot.Shot> nextShot) implements AbilityBehavior {
    private static final double GRAVITY = 0.08, AIR_DRAG = 0.91;

    public static final MapCodec<Leap> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.floatRange(0, 12).fieldOf("distance").forGetter(Leap::distance),
            Codec.BOOL.optionalFieldOf("backward", false).forGetter(Leap::backward),
            Codec.floatRange(0.1f, 1.5f).optionalFieldOf("lift", 0.5f).forGetter(Leap::lift),
            NextShot.Shot.CODEC.codec().optionalFieldOf("next_shot").forGetter(Leap::nextShot)
    ).apply(i, Leap::new));

    @Override
    public MapCodec<Leap> codec() {
        return CODEC;
    }

    @Override
    public boolean execute(AbilityContext context) {
        ServerPlayer player = context.player();
        Vec3 dir = player.getLookAngle().multiply(1, 0, 1).normalize();
        if (dir.lengthSqr() < 1.0E-4) return false;
        if (backward) dir = dir.reverse();
        double speed = launchSpeed(distance, lift);
        player.setDeltaMovement(dir.x * speed, lift, dir.z * speed);
        player.hurtMarked = true; // sends the new motion to the client
        player.resetFallDistance();
        context.level().sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.1, player.getZ(), 10, 0.3, 0.05, 0.3, 0.02);
        Fx.sound(context.level(), player, SoundEvents.BREEZE_JUMP, 0.8f, 1.2f);
        nextShot.ifPresent(shot -> NextShot.arm(context, shot));
        return true;
    }

    // Horizontal speed that covers `distance` while airborne (air drag 0.91 per tick, gravity 0.08)
    public static double launchSpeed(double distance, double lift) {
        double airTicks = Math.max(4, 2 * lift / GRAVITY);
        double travelPerSpeed = (1 - Math.pow(AIR_DRAG, airTicks)) / (1 - AIR_DRAG);
        return distance / travelPerSpeed;
    }

    @Override
    public List<Component> details() {
        List<Component> lines = new ArrayList<>();
        lines.add(Fx.blocks(backward ? "leap_back" : "leap", distance));
        nextShot.ifPresent(shot -> lines.addAll(shot.details()));
        return lines;
    }
}
