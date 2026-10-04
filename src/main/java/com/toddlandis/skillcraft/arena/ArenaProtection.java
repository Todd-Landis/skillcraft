package com.toddlandis.skillcraft.arena;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.registry.ModBlocks;

import net.minecraft.util.TriState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

// The arena dimension can't be damaged or built in, by anyone except creative-mode players:
// no breaking, placing, bucket use, fluid spread, trampling or explosion block damage.
// Arena changes made by code (building plots, boss floor effects) aren't affected, because
// they don't go through these events.
@EventBusSubscriber(modid = SkillCraft.MODID)
public final class ArenaProtection {
    private ArenaProtection() {}

    // Creative-mode players (in practice, operators) can still edit the arena for building or debugging
    public static boolean canBypass(Entity entity) {
        return entity instanceof Player player && player.isCreative();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    static void onBreak(BreakBlockEvent event) {
        if (ArenaDimension.is(event.getLevel()) && !canBypass(event.getPlayer())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (ArenaDimension.is(event.getLevel()) && !canBypass(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    static void onFluid(BlockEvent.FluidPlaceBlockEvent event) {
        if (ArenaDimension.is(event.getLevel())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    static void onTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (ArenaDimension.is(event.getLevel())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    static void onExplosion(ExplosionEvent.Detonate event) {
        if (ArenaDimension.is(event.getLevel())) event.getAffectedBlocks().clear();
    }

    // Right-clicking blocks: only the arena's own blocks (reward chest, exit portal) can be used,
    // and no item may be used on any block (buckets, flint and steel, bone meal, spawn eggs...).
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!ArenaDimension.is(event.getLevel()) || canBypass(event.getEntity())) return;
        BlockState state = event.getLevel().getBlockState(event.getPos());
        boolean arenaBlock = state.is(ModBlocks.REWARD_CHEST.get()) || state.is(ModBlocks.EXIT_PORTAL.get());
        event.setUseItem(TriState.FALSE);
        if (!arenaBlock) event.setUseBlock(TriState.FALSE);
    }

    // Buckets are used through the air, not on a block
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (ArenaDimension.is(event.getLevel()) && !canBypass(event.getEntity()) && event.getItemStack().getItem() instanceof BucketItem) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (ArenaDimension.is(event.getLevel()) && !canBypass(event.getEntity())) event.setCanceled(true);
    }
}
