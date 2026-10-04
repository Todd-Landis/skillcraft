package com.toddlandis.skillcraft.registry;

import java.util.function.Supplier;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.ability.AbilityData;
import com.toddlandis.skillcraft.ability.AbilityRuntime;
import com.toddlandis.skillcraft.arena.ArenaReturn;
import com.toddlandis.skillcraft.bank.PlayerBank;
import com.toddlandis.skillcraft.combat.AttackStyle;
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

    // Each player's bank (see bank/PlayerBank). Kept through death; synced manually while open.
    public static final Supplier<AttachmentType<PlayerBank>> BANK = ATTACHMENT_TYPES.register("bank",
            () -> AttachmentType.builder(PlayerBank::new)
                    .serialize(PlayerBank.CODEC)
                    .copyOnDeath()
                    .build());

    // Chosen attack style (where combat XP goes). Kept through death; synced to its owner for the UI.
    public static final Supplier<AttachmentType<AttackStyle>> ATTACK_STYLE = ATTACHMENT_TYPES.register("attack_style",
            () -> AttachmentType.builder(() -> AttackStyle.AGGRESSIVE)
                    .serialize(AttackStyle.CODEC.fieldOf("style"))
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to, AttackStyle.STREAM_CODEC)
                    .build());

    // Where to send a player back to when they leave a boss arena (see arena/ArenaReturn).
    // Saved with the player so a logout or crash mid-fight never strands anyone.
    public static final Supplier<AttachmentType<ArenaReturn>> ARENA_RETURN = ATTACHMENT_TYPES.register("arena_return",
            () -> AttachmentType.builder(() -> ArenaReturn.NONE)
                    .serialize(ArenaReturn.CODEC)
                    .copyOnDeath()
                    .build());

    // Ability bars, learned Codex abilities and Momentum (see ability/AbilityData). Kept through death;
    // synced to its owner for the HUD and the Ability Book.
    public static final Supplier<AttachmentType<AbilityData>> ABILITIES = ATTACHMENT_TYPES.register("abilities",
            () -> AttachmentType.builder(AbilityData::new)
                    .serialize(AbilityData.CODEC)
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to, AbilityData.STREAM_CODEC)
                    .build());

    // Faith, drained by Prayer blessings (see prayer/PlayerFaith). Not copied on death, so a
    // respawned player starts full; synced to its owner for the HUD.
    public static final Supplier<AttachmentType<com.toddlandis.skillcraft.prayer.PlayerFaith>> FAITH = ATTACHMENT_TYPES.register("faith",
            () -> AttachmentType.builder(com.toddlandis.skillcraft.prayer.PlayerFaith::new)
                    .serialize(com.toddlandis.skillcraft.prayer.PlayerFaith.CODEC)
                    .sync((holder, to) -> holder == to, com.toddlandis.skillcraft.prayer.PlayerFaith.STREAM_CODEC)
                    .build());

    // Positions of heart-dropping blocks (logs, ores) a player placed in this chunk, so re-placing
    // and re-breaking them never drops Elemental Hearts (see node/HeartEvents)
    public static final Supplier<AttachmentType<com.toddlandis.skillcraft.node.PlacedResources>> PLACED_RESOURCES = ATTACHMENT_TYPES.register("placed_resources",
            () -> AttachmentType.builder(com.toddlandis.skillcraft.node.PlacedResources::new)
                    .serialize(com.toddlandis.skillcraft.node.PlacedResources.CODEC)
                    .build());

    // Cooldowns and short-lived ability states (server-side only, never saved)
    public static final Supplier<AttachmentType<AbilityRuntime>> ABILITY_RUNTIME = ATTACHMENT_TYPES.register("ability_runtime",
            () -> AttachmentType.builder(AbilityRuntime::new).build());
}
