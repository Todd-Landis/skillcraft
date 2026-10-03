package com.toddlandis.skillcraft.loot;

import com.toddlandis.skillcraft.SkillCraft;

import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

// Mobs don't drop station gear (their spawn equipment or loot) unless it was made by a player
// and the mob picked it up.
@EventBusSubscriber(modid = SkillCraft.MODID)
public final class MobDropEvents {
    private MobDropEvents() {}

    @SubscribeEvent
    static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Mob)) return;
        event.getDrops().removeIf(drop -> CraftedGear.isUnearned(drop.getItem()));
    }
}
