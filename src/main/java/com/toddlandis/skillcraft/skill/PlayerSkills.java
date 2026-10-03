package com.toddlandis.skillcraft.skill;

import java.util.EnumMap;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

// A player's XP in every skill. Stored on the player as a data attachment
// (see SkillAttachments), saved with the world and synced to that player's client.
public class PlayerSkills {
    // Saved as {"xp": {"mining": 1234, ...}} so adding new skills later never breaks old saves
    public static final MapCodec<PlayerSkills> CODEC = Codec.unboundedMap(Skill.CODEC, Codec.DOUBLE)
            .xmap(PlayerSkills::new, PlayerSkills::asMap)
            .fieldOf("xp");

    public static final StreamCodec<ByteBuf, PlayerSkills> STREAM_CODEC = StreamCodec.of(
            (buf, skills) -> {
                for (Skill skill : Skill.values()) buf.writeDouble(skills.xp[skill.ordinal()]);
            },
            buf -> {
                PlayerSkills skills = new PlayerSkills();
                for (Skill skill : Skill.values()) skills.xp[skill.ordinal()] = buf.readDouble();
                return skills;
            });

    private final double[] xp = new double[Skill.values().length];

    public PlayerSkills() {}

    private PlayerSkills(Map<Skill, Double> saved) {
        saved.forEach((skill, value) -> xp[skill.ordinal()] = value);
    }

    private Map<Skill, Double> asMap() {
        Map<Skill, Double> map = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values()) {
            if (xp[skill.ordinal()] > 0) map.put(skill, xp[skill.ordinal()]);
        }
        return map;
    }

    public double getXp(Skill skill) {
        return xp[skill.ordinal()];
    }

    public void setXp(Skill skill, double value) {
        xp[skill.ordinal()] = Math.clamp(value, 0, XpTable.MAX_XP);
    }

    public int getLevel(Skill skill) {
        return XpTable.levelForXp(getXp(skill));
    }

    public int totalLevel() {
        int total = 0;
        for (Skill skill : Skill.values()) total += getLevel(skill);
        return total;
    }
}
