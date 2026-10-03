package com.toddlandis.skillcraft.boss;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.arena.ArenaInstance;
import com.toddlandis.skillcraft.arena.ArenaManager;
import com.toddlandis.skillcraft.loot.EarnedBy;
import com.toddlandis.skillcraft.registry.ModDataComponents;
import com.toddlandis.skillcraft.registry.ModItems;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

// Slagheart's rewards. Every eligible player (dealt at least 5% of the boss's health) gets their
// own roll of loot_table/gameplay/slagheart_rewards.json. Boss gear in the roll is stamped as
// earned. In an arena the loot waits in the reward chest; elsewhere (e.g. /summon) it drops.
public final class SlagheartRewards {
    public static final ResourceKey<LootTable> TABLE = ResourceKey.create(Registries.LOOT_TABLE,
            Identifier.fromNamespaceAndPath(SkillCraft.MODID, "gameplay/slagheart_rewards"));
    public static final double MIN_CONTRIBUTION = 0.05;

    private SlagheartRewards() {}

    public static void onDefeated(ServerLevel level, SlagheartEntity boss) {
        Map<UUID, List<ItemStack>> loot = new HashMap<>();
        double needed = boss.getMaxHealth() * MIN_CONTRIBUTION;
        for (Map.Entry<UUID, Double> entry : boss.damageByPlayer().entrySet()) {
            if (entry.getValue() < needed) continue;
            if (!(level.getPlayerByUUID(entry.getKey()) instanceof ServerPlayer player)) continue;
            loot.put(player.getUUID(), roll(level, player, boss.position()));
        }

        ArenaInstance instance = boss.instanceId() >= 0 ? ArenaManager.get(level.getServer()).instance(boss.instanceId()) : null;
        if (instance != null) {
            ArenaManager.get(level.getServer()).victory(level, instance, loot);
            return;
        }
        for (List<ItemStack> items : loot.values()) {
            for (ItemStack stack : items) level.addFreshEntity(new ItemEntity(level, boss.getX(), boss.getY() + 1, boss.getZ(), stack));
        }
    }

    // One roll of the reward table for this player
    public static List<ItemStack> roll(ServerLevel level, ServerPlayer player, Vec3 origin) {
        LootTable table = level.getServer().reloadableRegistries().getLootTable(TABLE);
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, origin)
                .withOptionalParameter(LootContextParams.THIS_ENTITY, player)
                .withLuck(player.getLuck())
                .create(LootContextParamSets.CHEST);
        List<ItemStack> items = new ArrayList<>(table.getRandomItems(params));
        for (ItemStack stack : items) stamp(stack, player);
        return items;
    }

    // Boss uniques are marked "Won from Slagheart by <player>"
    public static void stamp(ItemStack stack, ServerPlayer player) {
        if (stack.is(ModItems.EMBERFALL_MAUL.get())) {
            stack.set(ModDataComponents.EARNED_BY.get(), new EarnedBy("entity.skillcraft.slagheart", player.getName().getString()));
        }
    }
}
