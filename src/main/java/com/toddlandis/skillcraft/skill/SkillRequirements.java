package com.toddlandis.skillcraft.skill;

import java.util.Map;

import com.mojang.serialization.Codec;

// Skill levels needed to use an item, e.g. {"mining": 30} for a diamond pickaxe.
// An axe can need both Woodcutting (to chop) and Melee (to fight); each is checked
// only for the matching action.
public record SkillRequirements(Map<Skill, Integer> levels) {
    public static final Codec<SkillRequirements> CODEC = Codec.unboundedMap(Skill.CODEC, Codec.intRange(1, XpTable.MAX_LEVEL))
            .xmap(SkillRequirements::new, SkillRequirements::levels);

    public int levelFor(Skill skill) {
        return levels.getOrDefault(skill, 1);
    }
}
