package com.toddlandis.skillcraft.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

// A gatherable block: {"skill": "mining", "level": 15, "xp": 35}
public record SkillResource(Skill skill, int level, double xp) {
    public static final Codec<SkillResource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Skill.CODEC.fieldOf("skill").forGetter(SkillResource::skill),
            Codec.intRange(1, XpTable.MAX_LEVEL).optionalFieldOf("level", 1).forGetter(SkillResource::level),
            Codec.DOUBLE.fieldOf("xp").forGetter(SkillResource::xp)
    ).apply(instance, SkillResource::new));
}
