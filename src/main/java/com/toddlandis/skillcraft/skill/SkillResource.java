package com.toddlandis.skillcraft.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

// A level-gated block in the world: {"skill": "mining", "level": 15}.
// Breaking world blocks gives no XP; skilling nodes are how gathering skills are trained.
public record SkillResource(Skill skill, int level) {
    public static final Codec<SkillResource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Skill.CODEC.fieldOf("skill").forGetter(SkillResource::skill),
            Codec.intRange(1, XpTable.MAX_LEVEL).optionalFieldOf("level", 1).forGetter(SkillResource::level)
    ).apply(instance, SkillResource::new));
}
