package com.toddlandis.skillcraft.registry;

import java.util.function.Supplier;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.node.GatheringFatigue;
import com.toddlandis.skillcraft.skill.PlayerSkills;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

// Data attachments: extra data NeoForge saves alongside players, chunks, etc.
public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, SkillCraft.MODID);

    // Each player's skill XP. Kept through death, and synced only to the player it belongs to.
    public static final Supplier<AttachmentType<PlayerSkills>> SKILLS = ATTACHMENT_TYPES.register("skills",
            () -> AttachmentType.builder(PlayerSkills::new)
                    .serialize(PlayerSkills.CODEC)
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to, PlayerSkills.STREAM_CODEC)
                    .build());

    // Recently gathered resources per node type (server-side only), see GatheringFatigue
    public static final Supplier<AttachmentType<GatheringFatigue>> FATIGUE = ATTACHMENT_TYPES.register("gathering_fatigue",
            () -> AttachmentType.builder(GatheringFatigue::new)
                    .serialize(GatheringFatigue.CODEC)
                    .copyOnDeath()
                    .build());
}
