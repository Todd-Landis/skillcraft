package com.toddlandis.skillcraft.leycraft;

import java.util.Locale;

import com.mojang.serialization.Codec;
import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.combat.DamageStyle;
import com.toddlandis.skillcraft.magic.Motes;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;

// The four elements a Ley Altar can weave (docs/LEYCRAFT.md). Each is one of the magic damage
// types and has a mote. Order matters: on a resonance tie the earlier element wins, so Arcane
// (the "nothing special around it" element) comes first.
public enum LeyElement implements StringRepresentable {
    ARCANE(DamageStyle.ARCANE),
    FIRE(DamageStyle.FIRE),
    FROST(DamageStyle.FROST),
    STORM(DamageStyle.STORM);

    public static final Codec<LeyElement> CODEC = StringRepresentable.fromEnum(LeyElement::values);

    private final DamageStyle style;
    // Biomes that add resonance for this element: data/skillcraft/tags/worldgen/biome/ley_resonance/<element>.json
    private final TagKey<Biome> biomes;

    LeyElement(DamageStyle style) {
        this.style = style;
        this.biomes = TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(SkillCraft.MODID, "ley_resonance/" + getSerializedName()));
    }

    public DamageStyle style() { return style; }
    public TagKey<Biome> biomes() { return biomes; }
    public Item mote() { return Motes.of(style); }

    // Coloured element name, e.g. "Fire" in yellow
    public Component displayName() { return style.displayName(); }

    // What drifts up from an altar of this element
    public ParticleOptions particle() {
        return switch (this) {
            case ARCANE -> ParticleTypes.ENCHANT;
            case FIRE -> ParticleTypes.SMALL_FLAME;
            case FROST -> ParticleTypes.SNOWFLAKE;
            case STORM -> ParticleTypes.ELECTRIC_SPARK;
        };
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
