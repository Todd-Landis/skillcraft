package com.toddlandis.skillcraft.leycraft;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.toddlandis.skillcraft.Config;
import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.altar.minigame.MinigameKind;
import com.toddlandis.skillcraft.altar.minigame.MinigameLogic;
import com.toddlandis.skillcraft.altar.minigame.Minigames;
import com.toddlandis.skillcraft.network.ModNetwork;
import com.toddlandis.skillcraft.network.OpenWeavePayload;
import com.toddlandis.skillcraft.network.WeaveResultPayload;
import com.toddlandis.skillcraft.registry.ModDataMaps;
import com.toddlandis.skillcraft.skill.Skill;
import com.toddlandis.skillcraft.skill.SkillManager;
import com.toddlandis.skillcraft.skill.XpTable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// Weaving essence into motes at a Ley Altar (docs/LEYCRAFT.md section 5):
//   motes = essence value x motes_per_shard x (1 + resonance) x weave x (1 + level bonus x progress)
// A quick weave is instant (weave x1.0). A careful weave plays Steady the Current, which is
// server-verified exactly like the Attunement Altar's minigames: the server picks the seed and
// replays the client's inputs, and the number of misses sets the multiplier.
@EventBusSubscriber(modid = SkillCraft.MODID)
public final class LeyWeaving {
    private static final double MAX_DISTANCE_SQ = 8 * 8;
    private static final int TIMING_SLACK_TICKS = 40;

    private record Session(BlockPos pos, LeyElement element, int tier, long seed, long startMillis) {}

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private LeyWeaving() {}

    public static LeyData.Essence essence(ItemStack stack) {
        return stack.isEmpty() ? null : stack.typeHolder().getData(ModDataMaps.LEY_ESSENCE);
    }

    public static LeyData.Weave weave(LeyElement element) {
        return element.mote().getDefaultInstance().typeHolder().getData(ModDataMaps.LEY_WEAVES);
    }

    // --- The maths (pure, so GameTests can check it) ---

    public static double weaveMultiplier(boolean careful, boolean won, int misses) {
        if (!careful) return 1.0;
        if (!won) return Config.LEY_CAREFUL_FAILED.get();
        return Math.max(1.0, Config.LEY_CAREFUL_BEST.get() - Config.LEY_CAREFUL_PER_MISS.get() * misses);
    }

    public static double motesPerShard(LeyData.Weave weave, double resonance, double weaveMultiplier, int playerLevel) {
        double progress = Math.clamp((playerLevel - weave.level()) / (double) Math.max(1, XpTable.MAX_LEVEL - weave.level()), 0, 1);
        return weave.motesPerShard() * (1 + resonance) * weaveMultiplier * (1 + Config.LEY_LEVEL_BONUS.get() * progress);
    }

    // --- Starting a weave ---

    // Everything a weave needs, or null after telling the player why it can't happen
    private record Ready(Resonance.Reading reading, LeyData.Weave weave) {}

    private static Ready check(ServerPlayer player, Level level, BlockPos pos, LeyAltarBlockEntity altar) {
        Resonance.Reading reading = refresh(level, pos);
        if (!altar.isOwner(player)) {
            status(player, Component.translatable("message.skillcraft.altar_owned", altar.ownerName()), ChatFormatting.RED);
            return null;
        }
        if (altar.isEmpty()) {
            status(player, describe(reading), ChatFormatting.LIGHT_PURPLE);
            return null;
        }
        LeyElement element = reading.dominant();
        LeyData.Weave weave = weave(element);
        if (weave == null) return null;
        if (SkillManager.level(player, Skill.LEYCRAFT) < weave.level()) {
            status(player, Component.translatable("message.skillcraft.ley_level", weave.level(), moteName(element)), ChatFormatting.RED);
            return null;
        }
        return new Ready(reading, weave);
    }

    // Instant weave at x1.0; returns how many motes were made (0 if it couldn't weave)
    public static int quickWeave(ServerPlayer player, BlockPos pos) {
        Level level = player.level();
        if (!(level.getBlockEntity(pos) instanceof LeyAltarBlockEntity altar)) return 0;
        Ready ready = check(player, level, pos, altar);
        if (ready == null) return 0;
        return finish(player, pos, altar, ready, weaveMultiplier(false, false, 0), 1.0, "message.skillcraft.ley_woven");
    }

    public static void openCareful(ServerPlayer player, BlockPos pos) {
        Level level = player.level();
        if (!(level.getBlockEntity(pos) instanceof LeyAltarBlockEntity altar)) return;
        Ready ready = check(player, level, pos, altar);
        if (ready == null) return;
        LeyElement element = ready.reading().dominant();
        int tier = XpTable.tierIndex(ready.weave().level());
        long seed = level.getRandom().nextLong();
        SESSIONS.put(player.getUUID(), new Session(pos.immutable(), element, tier, seed, System.currentTimeMillis()));
        ModNetwork.sendTo(player, new OpenWeavePayload(pos, element, tier, seed));
    }

    public static void handleResult(WeaveResultPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        Session session = SESSIONS.remove(player.getUUID());
        if (session == null || !session.pos().equals(payload.pos())) return;
        BlockPos pos = session.pos();
        if (player.distanceToSqr(Vec3.atCenterOf(pos)) > MAX_DISTANCE_SQ) return;
        if (!(player.level().getBlockEntity(pos) instanceof LeyAltarBlockEntity altar) || altar.isEmpty()) return;

        Minigames.ReplayResult result = Minigames.replay(MinigameKind.STEADY_THE_CURRENT, session.tier(), session.seed(), payload.inputs());
        if (result.state() == MinigameLogic.State.RUNNING && !result.started()) return; // closed without playing: nothing happens
        long elapsed = (System.currentTimeMillis() - session.startMillis()) / 50;
        boolean timingOk = result.ticks() <= elapsed + TIMING_SLACK_TICKS;
        if (!timingOk) SkillCraft.LOGGER.warn("Rejected weave from {}: claimed {} ticks but only {} passed", player.getName().getString(), result.ticks(), elapsed);
        boolean won = timingOk && result.state() == MinigameLogic.State.SUCCESS;
        completeCareful(player, pos, session.element(), won, result.misses());
    }

    // The end of a careful weave (also called by GameTests): re-checks the altar, then weaves
    public static int completeCareful(ServerPlayer player, BlockPos pos, LeyElement element, boolean won, int misses) {
        if (!(player.level().getBlockEntity(pos) instanceof LeyAltarBlockEntity altar)) return 0;
        Ready ready = check(player, player.level(), pos, altar);
        if (ready == null) return 0;
        if (ready.reading().dominant() != element) {
            status(player, Component.translatable("message.skillcraft.ley_shifted"), ChatFormatting.YELLOW);
            return 0;
        }
        double xpMultiplier = won ? 1 + Config.LEY_CAREFUL_XP_BONUS.get() : 1.0;
        return finish(player, pos, altar, ready, weaveMultiplier(true, won, misses), xpMultiplier,
                won ? "message.skillcraft.ley_woven_careful" : "message.skillcraft.ley_woven_slipped");
    }

    private static int finish(ServerPlayer player, BlockPos pos, LeyAltarBlockEntity altar, Ready ready, double weaveMultiplier, double xpMultiplier, String messageKey) {
        ServerLevel level = player.level();
        LeyElement element = ready.reading().dominant();
        double shards = altar.shardValue();
        double perShard = motesPerShard(ready.weave(), ready.reading().strength(), weaveMultiplier, SkillManager.level(player, Skill.LEYCRAFT));
        double exact = Math.round(shards * perShard * 1e6) / 1e6; // so 31.9999999 counts as 32
        int motes = (int) Math.floor(exact);
        if (level.getRandom().nextDouble() < exact - motes) motes++; // round the batch fairly
        altar.clear();

        Item mote = element.mote();
        int left = motes;
        while (left > 0) {
            ItemStack stack = new ItemStack(mote, Math.min(left, mote.getDefaultMaxStackSize()));
            left -= stack.getCount();
            if (!player.getInventory().add(stack)) player.drop(stack, false);
        }
        SkillManager.awardXp(player, Skill.LEYCRAFT, shards * ready.weave().xp() * xpMultiplier);

        level.sendParticles(element.particle(), pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 30, 0.35, 0.3, 0.35, 0.05);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 0.8f);
        level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.4f, 1.6f);
        status(player, Component.translatable(messageKey, motes, moteName(element)), ChatFormatting.LIGHT_PURPLE);
        return motes;
    }

    // --- Resonance display ---

    // Re-reads the altar's surroundings and shows the strongest element on the block (for particles)
    public static Resonance.Reading refresh(Level level, BlockPos pos) {
        Resonance.Reading reading = Resonance.read(level, pos);
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(LeyAltarBlock.ELEMENT) && state.getValue(LeyAltarBlock.ELEMENT) != reading.dominant()) {
            level.setBlock(pos, state.setValue(LeyAltarBlock.ELEMENT, reading.dominant()), 3);
        }
        return reading;
    }

    // "Ley Altar resonance: Fire 62% (Frost 10%, Storm 0%, Arcane 0%)"
    public static Component describe(Resonance.Reading reading) {
        MutableComponent others = Component.empty();
        boolean first = true;
        for (LeyElement element : LeyElement.values()) {
            if (element == reading.dominant()) continue;
            if (!first) others.append(", ");
            others.append(element.displayName()).append(" " + percent(reading.of(element)));
            first = false;
        }
        Component main = element(reading.dominant(), reading.strength());
        return Component.translatable("message.skillcraft.ley_status", main, others, moteName(reading.dominant()));
    }

    public static Component element(LeyElement element, double resonance) {
        return element.displayName().copy().append(" " + percent(resonance));
    }

    public static Component moteName(LeyElement element) {
        return element.mote().getDefaultInstance().getHoverName();
    }

    public static String percent(double value) {
        return Math.round(value * 100) + "%";
    }

    static void status(ServerPlayer player, Component text, ChatFormatting color) {
        player.sendOverlayMessage(text.copy().withStyle(color));
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SESSIONS.remove(event.getEntity().getUUID());
    }
}
