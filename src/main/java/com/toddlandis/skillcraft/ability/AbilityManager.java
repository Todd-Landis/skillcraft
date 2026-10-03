package com.toddlandis.skillcraft.ability;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.network.AbilityDefinitionsPayload;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;

// Loads every ability from data/<namespace>/skillcraft/ability/*.json and sends them to clients.
// (A reload listener rather than a datapack registry, so /reload retunes abilities live:
// NeoForge only reloads datapack registries when a world loads.)
@EventBusSubscriber(modid = SkillCraft.MODID)
public final class AbilityManager extends SimpleJsonResourceReloadListener<AbilityDefinition> {
    public static final ResourceKey<net.minecraft.core.Registry<AbilityDefinition>> KEY =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(SkillCraft.MODID, "ability"));
    private static final AbilityManager INSTANCE = new AbilityManager();

    // Sorted by category, then level, so the Ability Book lists them in a sensible order
    private static volatile Map<Identifier, AbilityDefinition> definitions = Map.of();

    private AbilityManager() {
        super(AbilityDefinition.CODEC, FileToIdConverter.registry(KEY));
    }

    public static AbilityDefinition get(Identifier id) {
        return id == null ? null : definitions.get(id);
    }

    public static Map<Identifier, AbilityDefinition> all() {
        return definitions;
    }

    // For tests: add or replace a definition without a datapack
    public static void putForTests(Identifier id, AbilityDefinition definition) {
        Map<Identifier, AbilityDefinition> copy = new LinkedHashMap<>(definitions);
        copy.put(id, definition);
        definitions = sorted(copy);
    }

    @Override
    protected void apply(Map<Identifier, AbilityDefinition> loaded, ResourceManager manager, ProfilerFiller profiler) {
        definitions = sorted(loaded);
        SkillCraft.LOGGER.info("Loaded {} abilities", definitions.size());
    }

    public static Map<Identifier, AbilityDefinition> sorted(Map<Identifier, AbilityDefinition> map) {
        List<Map.Entry<Identifier, AbilityDefinition>> entries = map.entrySet().stream()
                .sorted(Comparator.<Map.Entry<Identifier, AbilityDefinition>>comparingInt(e -> e.getValue().category().ordinal())
                        .thenComparingInt(e -> e.getValue().requires().level())
                        .thenComparing(e -> e.getKey().toString()))
                .toList();
        Map<Identifier, AbilityDefinition> result = new LinkedHashMap<>();
        for (Map.Entry<Identifier, AbilityDefinition> entry : entries) result.put(entry.getKey(), entry.getValue());
        return java.util.Collections.unmodifiableMap(result);
    }

    @SubscribeEvent
    static void addListener(AddServerReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath(SkillCraft.MODID, "abilities"), INSTANCE);
    }

    // On login and after /reload
    @SubscribeEvent
    static void sync(OnDatapackSyncEvent event) {
        AbilityDefinitionsPayload payload = new AbilityDefinitionsPayload(definitions);
        event.getRelevantPlayers().forEach(player -> com.toddlandis.skillcraft.network.ModNetwork.sendTo(player, payload));
    }
}
