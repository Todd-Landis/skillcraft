package com.toddlandis.skillcraft;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.toddlandis.skillcraft.registry.ModAttachments;
import com.toddlandis.skillcraft.skill.Skill;
import com.toddlandis.skillcraft.skill.XpTable;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

// The value here should match the mod_id in gradle.properties
@Mod(SkillCraft.MODID)
public class SkillCraft {
    public static final String MODID = "skillcraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    // The constructor is the first code that runs when the mod is loaded.
    // NeoForge passes in the mod event bus and container automatically.
    public SkillCraft(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("SkillCraft loaded with {} skills (level {} requires {} XP)",
                Skill.values().length, XpTable.MAX_LEVEL, XpTable.xpForLevel(XpTable.MAX_LEVEL));
    }
}
