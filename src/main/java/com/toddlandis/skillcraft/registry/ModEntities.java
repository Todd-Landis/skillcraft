package com.toddlandis.skillcraft.registry;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.boss.SlagGlob;
import com.toddlandis.skillcraft.boss.SlagheartEntity;
import com.toddlandis.skillcraft.combat.SkillArrow;
import com.toddlandis.skillcraft.fishing.FishingLine;
import com.toddlandis.skillcraft.fishing.FishingSpotEntity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid = SkillCraft.MODID)
public class ModEntities {
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(SkillCraft.MODID);

    // The first boss (see docs/BOSS_SLAGHEART.md)
    public static final DeferredHolder<EntityType<?>, EntityType<SlagheartEntity>> SLAGHEART = ENTITIES.registerEntityType("slagheart",
            SlagheartEntity::new, MobCategory.MONSTER, builder -> builder.sized(2.2f, 3.6f).eyeHeight(3.1f).fireImmune().clientTrackingRange(10).noLootTable());

    // Slagheart's Slag Spit projectile
    public static final DeferredHolder<EntityType<?>, EntityType<SlagGlob>> SLAG_GLOB = ENTITIES.registerEntityType("slag_glob",
            SlagGlob::new, MobCategory.MISC, builder -> builder.sized(0.5f, 0.5f).clientTrackingRange(6).updateInterval(5).noLootTable());

    // Light and heavy arrows (vanilla arrows are "medium")
    public static final DeferredHolder<EntityType<?>, EntityType<SkillArrow>> LIGHT_ARROW = ENTITIES.registerEntityType("light_arrow",
            SkillArrow::new, MobCategory.MISC, builder -> builder.sized(0.5f, 0.5f).eyeHeight(0.13f).clientTrackingRange(4).updateInterval(20).noLootTable());
    public static final DeferredHolder<EntityType<?>, EntityType<SkillArrow>> HEAVY_ARROW = ENTITIES.registerEntityType("heavy_arrow",
            SkillArrow::new, MobCategory.MISC, builder -> builder.sized(0.5f, 0.5f).eyeHeight(0.13f).clientTrackingRange(4).updateInterval(20).noLootTable());

    // Fishing spots: churning water on a pond (see fishing/FishingSpotEntity)
    public static final DeferredHolder<EntityType<?>, EntityType<FishingSpotEntity>> FISHING_SPOT = ENTITIES.registerEntityType("fishing_spot",
            FishingSpotEntity::new, MobCategory.MISC, builder -> builder.sized(1.0f, 0.25f).clientTrackingRange(8).updateInterval(5).noLootTable());

    // The bobber shown while fishing at a spot (never saved)
    public static final DeferredHolder<EntityType<?>, EntityType<FishingLine>> FISHING_LINE = ENTITIES.registerEntityType("fishing_line",
            FishingLine::new, MobCategory.MISC, builder -> builder.sized(0.25f, 0.25f).clientTrackingRange(4).updateInterval(2).noSave().noSummon().noLootTable());

    // Magic bolts from wands, staves and spells (docs/AGILITY_MAGIC_PRAYER.md)
    public static final DeferredHolder<EntityType<?>, EntityType<com.toddlandis.skillcraft.magic.SpellBolt>> SPELL_BOLT = ENTITIES.registerEntityType("spell_bolt",
            com.toddlandis.skillcraft.magic.SpellBolt::new, MobCategory.MISC, builder -> builder.sized(0.3f, 0.3f).clientTrackingRange(6).updateInterval(2).noLootTable());

    @SubscribeEvent
    static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(SLAGHEART.get(), SlagheartEntity.createAttributes().build());
    }
}
