package com.toddlandis.skillcraft.node;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.toddlandis.skillcraft.skill.SkillManager;
import com.toddlandis.skillcraft.skill.XpTable;

import net.minecraft.world.entity.player.Player;

// Data map value (data_maps/block/elemental_hearts.json and item/elemental_hearts.json): breaking
// this vanilla block, or reeling in this fish with a vanilla rod, has `chance` to give the Elemental
// Heart that attunes into `node`. Higher-tier hearts are rarer; a higher skill level finds them a
// little more often (up to +50% at 99). See docs/DESIGN.md "Elemental Hearts".
public record HeartDrop(NodeType node, double chance) {
    public static final double LEVEL_BONUS_AT_99 = 0.5;

    public static final Codec<HeartDrop> CODEC = RecordCodecBuilder.create(i -> i.group(
            NodeType.CODEC.fieldOf("node").forGetter(HeartDrop::node),
            Codec.doubleRange(0, 1).fieldOf("chance").forGetter(HeartDrop::chance)
    ).apply(i, HeartDrop::new));

    // The chance for this player: the base chance, +0% at the node's level up to +50% at 99
    public double chanceFor(Player player) {
        int level = SkillManager.level(player, node.skill());
        double progress = Math.clamp((level - node.level()) / (double) Math.max(1, XpTable.MAX_LEVEL - node.level()), 0, 1);
        return Math.min(1.0, chance * (1 + LEVEL_BONUS_AT_99 * progress));
    }
}
