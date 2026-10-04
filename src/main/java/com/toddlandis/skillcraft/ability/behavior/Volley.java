package com.toddlandis.skillcraft.ability.behavior;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.toddlandis.skillcraft.ability.AbilityBehavior;
import com.toddlandis.skillcraft.ability.AbilityContext;
import com.toddlandis.skillcraft.ability.AbilityEvents;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

// Volley: a fan of arrows, each a share of a fully drawn shot. Uses up to max_ammo arrows from
// the inventory (the rest of the fan is free and can't be picked up); needs at least one arrow.
public record Volley(int arrows, float spread, float damage, int maxAmmo) implements AbilityBehavior {
    private static final double FULL_SHOT_BASE = 2.0; // a vanilla arrow's base damage
    private static final float FULL_SHOT_SPEED = 3.0f;

    public static final MapCodec<Volley> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.intRange(1, 15).fieldOf("arrows").forGetter(Volley::arrows),
            Codec.floatRange(0, 120).optionalFieldOf("spread", 30f).forGetter(Volley::spread),
            Codec.floatRange(0, 10).fieldOf("damage").forGetter(Volley::damage),
            Codec.intRange(0, 15).optionalFieldOf("max_ammo", 3).forGetter(Volley::maxAmmo)
    ).apply(i, Volley::new));

    @Override
    public MapCodec<Volley> codec() {
        return CODEC;
    }

    @Override
    public boolean execute(AbilityContext context) {
        ServerPlayer player = context.player();
        ItemStack bow = player.getMainHandItem();
        ItemStack ammo = player.getProjectile(bow);
        boolean free = player.isCreative();
        if (ammo.isEmpty() && !free) {
            context.runtime().failKey = "message.skillcraft.ability_no_ammo";
            return false;
        }
        int paid = free ? arrows : Math.min(maxAmmo, ammo.getCount());
        for (int i = 0; i < arrows; i++) {
            float yaw = arrows == 1 ? 0 : -spread / 2 + spread * i / (arrows - 1);
            Arrow arrow = new Arrow(context.level(), player, new ItemStack(Items.ARROW), bow.copy());
            arrow.shootFromRotation(player, player.getXRot(), player.getYRot() + yaw, 0f, FULL_SHOT_SPEED, 1f);
            arrow.setBaseDamage(FULL_SHOT_BASE * damage);
            arrow.setCritArrow(true);
            arrow.pickup = i < paid && !free ? AbstractArrow.Pickup.ALLOWED : AbstractArrow.Pickup.CREATIVE_ONLY;
            AbilityEvents.markAbilityProjectile(arrow);
            context.level().addFreshEntity(arrow);
        }
        if (!free) ammo.shrink(paid);
        player.swing(InteractionHand.MAIN_HAND, true);
        Fx.sound(context.level(), player, SoundEvents.ARROW_SHOOT, 1f, 0.8f);
        return true;
    }

    @Override
    public List<Component> details() {
        return List.of(Component.translatable("ability.skillcraft.detail.volley", arrows, Math.round(damage * 100), maxAmmo));
    }
}
