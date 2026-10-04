package com.toddlandis.skillcraft.bonding;

import java.util.Locale;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

// One creature you can bond into the Bond Codex (data_maps/entity_type/bondable.json, docs/BEASTBONDING.md):
//   level            - Beastbonding level to bond and summon it
//   kind             - mount or companion (you can have one mount and one or two companions out)
//   bond             - "tamed" (you must have tamed it yourself) or "offering" (pay `offering`)
//   tribute          - what rekindling it at a Rekindling Shrine costs, and how long it takes
//   bond_xp, ride_xp - Beastbonding XP for bonding it, and per 100 blocks ridden (mounts)
//   summon_as        - a SkillCraft version to summon instead (a bonded spider comes back as a Cliffstrider)
public record Bondable(int level, Kind kind, Method bond, Optional<Cost> offering, Cost tribute, int rekindleSeconds,
                       double bondXp, double rideXp, Optional<Identifier> summonAs) {

    public enum Kind implements StringRepresentable {
        MOUNT, COMPANION;
        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);
        @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    }

    public enum Method implements StringRepresentable {
        TAMED, OFFERING;
        public static final Codec<Method> CODEC = StringRepresentable.fromEnum(Method::values);
        @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    }

    // An item and a count, e.g. 3 sweet berries
    public record Cost(Item item, int count) {
        public static final Codec<Cost> CODEC = RecordCodecBuilder.create(i -> i.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(Cost::item),
                Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(Cost::count)
        ).apply(i, Cost::new));
    }

    public static final Codec<Bondable> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(1, 99).fieldOf("level").forGetter(Bondable::level),
            Kind.CODEC.fieldOf("kind").forGetter(Bondable::kind),
            Method.CODEC.fieldOf("bond").forGetter(Bondable::bond),
            Cost.CODEC.optionalFieldOf("offering").forGetter(Bondable::offering),
            Cost.CODEC.fieldOf("tribute").forGetter(Bondable::tribute),
            Codec.intRange(0, 36000).optionalFieldOf("rekindle_seconds", 60).forGetter(Bondable::rekindleSeconds),
            Codec.doubleRange(0, 100000).fieldOf("bond_xp").forGetter(Bondable::bondXp),
            Codec.doubleRange(0, 100000).optionalFieldOf("ride_xp", 0.0).forGetter(Bondable::rideXp),
            Identifier.CODEC.optionalFieldOf("summon_as").forGetter(Bondable::summonAs)
    ).apply(i, Bondable::new));

    // The entity type a bond of `species` is summoned as
    public EntityType<?> summonType(EntityType<?> species) {
        return summonAs.flatMap(BuiltInRegistries.ENTITY_TYPE::getOptional).orElse(species);
    }
}
