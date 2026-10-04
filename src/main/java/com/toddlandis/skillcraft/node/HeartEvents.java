package com.toddlandis.skillcraft.node;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.registry.ModAttachments;
import com.toddlandis.skillcraft.registry.ModDataMaps;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

// Elemental Hearts come from playing ordinary Minecraft (docs/DESIGN.md "Elemental Hearts"):
//   - felling a grown tree: a log with natural (not player-placed) leaves near the top of its trunk
//   - mining an ore that drops loot (so with the right pickaxe)
//   - reeling in a fish with a vanilla rod (fishing spots don't count; they're the RPG side)
// Chances are per block or catch, in data_maps/block|item/elemental_hearts.json. Blocks a player
// placed never drop hearts (PlacedResources), so silk-touching and re-placing an ore earns nothing.
@EventBusSubscriber(modid = SkillCraft.MODID)
public final class HeartEvents {
    private static final int TRUNK_LIMIT = 32, CANOPY_RADIUS = 3;

    private HeartEvents() {}

    // Remember player-placed logs and ores
    @SubscribeEvent
    static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof ServerPlayer)) return;
        if (event.getPlacedBlock().typeHolder().getData(ModDataMaps.BLOCK_HEARTS) == null) return;
        markPlaced(level, event.getPos());
    }

    // Creative breaks drop nothing (no BlockDropsEvent), so forget the mark here
    @SubscribeEvent
    static void onBreak(BreakBlockEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getPlayer().isCreative() && !event.isCanceled()) unmark(level, event.getPos());
    }

    @SubscribeEvent
    static void onDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player) || player.isCreative() || event.getDrops().isEmpty()) return;
        ServerLevel level = event.getLevel();
        BlockPos pos = event.getPos();
        boolean placed = unmark(level, pos);
        HeartDrop drop = placed ? null : eligible(level, pos, event.getState());
        if (drop == null || level.getRandom().nextDouble() >= drop.chanceFor(player)) return;
        ItemEntity heart = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, ElementalHeartItem.of(drop.node()));
        heart.setDefaultPickUpDelay();
        event.getDrops().add(heart);
        found(player, level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop.node());
    }

    @SubscribeEvent
    static void onFished(ItemFishedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isCreative()) return;
        for (ItemStack catchStack : event.getDrops()) {
            HeartDrop drop = catchStack.typeHolder().getData(ModDataMaps.FISHED_HEARTS);
            if (drop == null || player.level().getRandom().nextDouble() >= drop.chanceFor(player)) continue;
            ItemStack heart = ElementalHeartItem.of(drop.node());
            if (!player.getInventory().add(heart)) player.drop(heart, false);
            found(player, player.level(), player.getX(), player.getY() + 1, player.getZ(), drop.node());
            return; // at most one heart per catch
        }
    }

    // The heart this block can drop, or null: it must be listed, and a log must be part of a grown tree
    public static HeartDrop eligible(ServerLevel level, BlockPos pos, BlockState state) {
        HeartDrop drop = state.typeHolder().getData(ModDataMaps.BLOCK_HEARTS);
        if (drop == null) return null;
        if (state.is(BlockTags.LOGS) && !partOfTree(level, pos)) return null;
        return drop;
    }

    // Natural leaves near the top of the trunk this log belongs to (or around the log itself)
    public static boolean partOfTree(Level level, BlockPos pos) {
        BlockPos top = pos;
        for (int i = 0; i < TRUNK_LIMIT && level.getBlockState(top.above()).is(BlockTags.LOGS); i++) top = top.above();
        return naturalLeavesNear(level, top) || (top != pos && naturalLeavesNear(level, pos));
    }

    private static boolean naturalLeavesNear(Level level, BlockPos center) {
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-CANOPY_RADIUS, -1, -CANOPY_RADIUS), center.offset(CANOPY_RADIUS, CANOPY_RADIUS, CANOPY_RADIUS))) {
            BlockState state = level.getBlockState(p);
            if (state.is(BlockTags.LEAVES) && state.hasProperty(LeavesBlock.PERSISTENT) && !state.getValue(LeavesBlock.PERSISTENT)) return true;
        }
        return false;
    }

    public static void markPlaced(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        PlacedResources placed = chunk.getData(ModAttachments.PLACED_RESOURCES);
        placed.add(pos);
        chunk.setData(ModAttachments.PLACED_RESOURCES, placed);
        chunk.markUnsaved();
    }

    // Forgets a placed mark; true if there was one
    public static boolean unmark(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        if (!chunk.hasData(ModAttachments.PLACED_RESOURCES)) return false;
        PlacedResources placed = chunk.getData(ModAttachments.PLACED_RESOURCES);
        if (!placed.remove(pos)) return false;
        chunk.markUnsaved();
        return true;
    }

    private static void found(ServerPlayer player, ServerLevel level, double x, double y, double z, NodeType type) {
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 12, 0.3, 0.3, 0.3, 0.05);
        level.playSound(null, x, y, z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 0.9f, 1.3f);
        player.sendOverlayMessage(Component.translatable("message.skillcraft.heart_found", ElementalHeartItem.of(type).getHoverName())
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
