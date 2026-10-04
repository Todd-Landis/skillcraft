package com.toddlandis.skillcraft.prayer;

import com.toddlandis.skillcraft.Config;
import com.toddlandis.skillcraft.registry.ModAttachments;
import com.toddlandis.skillcraft.registry.ModDataMaps;
import com.toddlandis.skillcraft.skill.Skill;
import com.toddlandis.skillcraft.skill.SkillManager;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

// Faith (the blessing resource) and Prayer training. See docs/AGILITY_MAGIC_PRAYER.md section 3.
//   - Maximum Faith = faithBase (config) + Prayer level
//   - Blessings drain it (AbilityService.tick); it comes back slowly while no blessing is on,
//     fully at a Votive Shrine, and on respawn
//   - Bury an offering (sneak + use) for its XP, or offer it at a shrine for more
public final class PrayerService {
    private PrayerService() {}

    public static PlayerFaith faith(Player player) {
        return player.getData(ModAttachments.FAITH);
    }

    public static float maxFaith(Player player) {
        return Config.FAITH_BASE.get() + SkillManager.level(player, Skill.PRAYER);
    }

    // Current Faith, against the up-to-date maximum (server side)
    public static float points(ServerPlayer player) {
        PlayerFaith faith = faith(player);
        faith.setMax(maxFaith(player));
        return faith.points();
    }

    // Changes Faith in place; call save() to sync it (done every few ticks while draining)
    public static void add(ServerPlayer player, float amount) {
        PlayerFaith faith = faith(player);
        faith.setMax(maxFaith(player));
        faith.setPoints(faith.points() + amount);
    }

    public static void set(ServerPlayer player, float value) {
        PlayerFaith faith = faith(player);
        faith.setMax(maxFaith(player));
        faith.setPoints(value);
        save(player, faith);
    }

    public static void save(ServerPlayer player, PlayerFaith faith) {
        faith.setMax(maxFaith(player));
        player.setData(ModAttachments.FAITH, faith);
    }

    public static void restore(ServerPlayer player) {
        set(player, maxFaith(player));
    }

    // --- Offerings ---

    public static PrayerOffering offering(ItemStack stack) {
        return stack.isEmpty() ? null : stack.typeHolder().getData(ModDataMaps.PRAYER_OFFERINGS);
    }

    // Uses up `count` of the held offering for Prayer XP; returns how many were used
    public static int offer(ServerPlayer player, ItemStack stack, int count, boolean atShrine) {
        PrayerOffering offering = offering(stack);
        if (offering == null) return 0;
        int used = player.isCreative() ? count : Math.min(count, stack.getCount());
        if (used <= 0) return 0;
        double xp = offering.xp() * used * (atShrine ? Config.SHRINE_XP_MULTIPLIER.get() : 1.0);
        Component name = stack.getHoverName();
        if (!player.isCreative()) stack.shrink(used);
        SkillManager.awardXp(player, Skill.PRAYER, xp);
        ServerLevel level = player.level();
        level.sendParticles(atShrine ? ParticleTypes.END_ROD : ParticleTypes.ASH, player.getX(), player.getY() + 0.5, player.getZ(), atShrine ? 10 : 6, 0.4, 0.3, 0.4, 0.02);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), atShrine ? SoundEvents.AMETHYST_BLOCK_RESONATE : SoundEvents.ROOTED_DIRT_PLACE,
                SoundSource.PLAYERS, 0.8f, atShrine ? 1.2f : 0.8f);
        player.sendOverlayMessage(Component.translatable(atShrine ? "message.skillcraft.prayer_offered" : "message.skillcraft.prayer_buried",
                used, name).withStyle(ChatFormatting.GOLD));
        return used;
    }
}
