package com.toddlandis.skillcraft.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

// How good an axe or pickaxe is at gathering from skilling nodes:
//   tier  - 0 (wood) and up. Each tier pushes the node depletion curve along as if you were
//           more levels ahead, so better tools get more items per node.
//   power - multiplier on the success chance per attempt.
// Data-driven: data/skillcraft/data_maps/item/gathering_tools.json
public record GatheringTool(int tier, double power) {
    public static final GatheringTool NONE = new GatheringTool(0, 1.0);

    public static final Codec<GatheringTool> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(0, 10).fieldOf("tier").forGetter(GatheringTool::tier),
            Codec.doubleRange(0.1, 5.0).fieldOf("power").forGetter(GatheringTool::power)
    ).apply(instance, GatheringTool::new));
}
