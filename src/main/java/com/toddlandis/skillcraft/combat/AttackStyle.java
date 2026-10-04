package com.toddlandis.skillcraft.combat;

import java.util.Locale;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

// OSRS-style attack styles: they choose where combat XP goes and give a small bonus.
// One setting covers melee and ranged. Cycled with the attack style key (default V).
public enum AttackStyle implements StringRepresentable {
    AGGRESSIVE(1.0, 0.0, 1.05, 1.0),  // all XP to Melee/Ranged, +5% damage dealt
    CONTROLLED(0.5, 0.5, 1.0, 1.0),   // XP split with Defence
    DEFENSIVE(0.0, 1.0, 1.0, 0.95);   // all XP to Defence, -5% damage taken

    public static final Codec<AttackStyle> CODEC = StringRepresentable.fromEnum(AttackStyle::values);
    public static final StreamCodec<ByteBuf, AttackStyle> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(i -> values()[i], AttackStyle::ordinal);

    private final double offenceShare;
    private final double defenceShare;
    private final double damageDealt;
    private final double damageTaken;

    AttackStyle(double offenceShare, double defenceShare, double damageDealt, double damageTaken) {
        this.offenceShare = offenceShare;
        this.defenceShare = defenceShare;
        this.damageDealt = damageDealt;
        this.damageTaken = damageTaken;
    }

    // Share of the 4 XP per damage point that goes to Melee/Ranged
    public double offenceShare() {
        return offenceShare;
    }

    // Share of the 4 XP per damage point that goes to Defence
    public double defenceShare() {
        return defenceShare;
    }

    public double damageDealtMultiplier() {
        return damageDealt;
    }

    public double damageTakenMultiplier() {
        return damageTaken;
    }

    public AttackStyle next() {
        return values()[(ordinal() + 1) % values().length];
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Component displayName() {
        return Component.translatable("combat.skillcraft.attack_style." + getSerializedName());
    }

    public Component description() {
        return Component.translatable("combat.skillcraft.attack_style." + getSerializedName() + ".desc");
    }
}
