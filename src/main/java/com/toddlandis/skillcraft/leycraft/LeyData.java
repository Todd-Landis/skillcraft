package com.toddlandis.skillcraft.leycraft;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

// The three Leycraft data map values (docs/LEYCRAFT.md), kept together because each is tiny.
public final class LeyData {
    private LeyData() {}

    // data_maps/block/ley_resonance.json: a block near a Ley Altar adds `points` to one element
    // (40 points = 100%, see Config.LEY_POINTS_FOR_FULL)
    public record Resonance(LeyElement element, double points) {
        public static final Codec<Resonance> CODEC = RecordCodecBuilder.create(i -> i.group(
                LeyElement.CODEC.fieldOf("element").forGetter(Resonance::element),
                Codec.doubleRange(0, 1000).fieldOf("points").forGetter(Resonance::points)
        ).apply(i, Resonance::new));
    }

    // data_maps/item/ley_essence.json: an item a Ley Altar accepts, and what one is worth in
    // ley shards (a ley shard is 1, an amethyst shard 0.5). Scales both motes and XP.
    public record Essence(double value) {
        public static final Codec<Essence> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.doubleRange(0, 64).fieldOf("value").forGetter(Essence::value)
        ).apply(i, Essence::new));
    }

    // data_maps/item/ley_weaves.json, keyed by mote: the Leycraft level to weave it, the XP per
    // shard woven, and the base motes per shard (before resonance, weave and level bonuses)
    public record Weave(int level, double xp, double motesPerShard) {
        public static final Codec<Weave> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(1, 99).fieldOf("level").forGetter(Weave::level),
                Codec.doubleRange(0, 10000).fieldOf("xp").forGetter(Weave::xp),
                Codec.doubleRange(0, 64).fieldOf("motes_per_shard").forGetter(Weave::motesPerShard)
        ).apply(i, Weave::new));
    }
}
