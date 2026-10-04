package com.toddlandis.skillcraft.client.ability;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.toddlandis.skillcraft.ability.AbilityClass;
import com.toddlandis.skillcraft.ability.AbilityData;
import com.toddlandis.skillcraft.ability.AbilityDefinition;
import com.toddlandis.skillcraft.ability.AbilityService;
import com.toddlandis.skillcraft.network.AbilityCooldownPayload;
import com.toddlandis.skillcraft.network.AbilityDefinitionsPayload;
import com.toddlandis.skillcraft.network.AbilityUnlockedPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;

// What the client knows about abilities: every definition (sent by the server), and cooldown /
// global cooldown / windup timers for the HUD (in client game ticks). Purely for drawing; the
// server decides everything.
public final class ClientAbilities {
    public static Map<Identifier, AbilityDefinition> definitions = Map.of();

    private record Timer(long start, long end) {}

    private static final Map<Identifier, Timer> COOLDOWNS = new HashMap<>();
    private static final Set<Identifier> TOGGLED = new HashSet<>();
    private static final Map<Identifier, Long> READY_PINGS = new HashMap<>();
    private static Timer gcd = new Timer(0, 0);
    private static Identifier windupId;
    private static Timer windup = new Timer(0, 0);

    private ClientAbilities() {}

    public static long now() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? 0 : mc.level.getGameTime();
    }

    public static AbilityDefinition get(Identifier id) {
        return id == null ? null : definitions.get(id);
    }

    public static AbilityData data(Player player) {
        return AbilityService.data(player);
    }

    // --- Packets ---

    public static void apply(AbilityDefinitionsPayload payload) {
        definitions = com.toddlandis.skillcraft.ability.AbilityManager.sorted(payload.definitions());
    }

    public static void apply(AbilityCooldownPayload payload) {
        long now = now();
        Identifier id = payload.ability();
        if (payload.windup() > 0) {
            windupId = id;
            windup = new Timer(now, now + payload.windup());
            return;
        }
        if (id.equals(windupId)) windupId = null;
        if (payload.cooldown() > 0) COOLDOWNS.put(id, new Timer(now, now + payload.cooldown()));
        if (payload.globalCooldown() > 0) gcd = new Timer(now, now + payload.globalCooldown());
        if (payload.toggledOn()) TOGGLED.add(id);
        else TOGGLED.remove(id);
    }

    public static void unlocked(AbilityUnlockedPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        AbilityDefinition def = get(payload.ability());
        Component name = AbilityDefinition.name(payload.ability());
        Component detail = def == null ? name : Component.translatable("toast.skillcraft.ability_unlocked.detail", name,
                def.requires().skill().displayName(), def.requires().level(), AbilityKeys.BOOK.getTranslatedKeyMessage());
        SystemToast.add(mc.gui.toastManager(), new SystemToast.SystemToastId(6000L), Component.translatable("toast.skillcraft.ability_unlocked"), detail);
        if (mc.player != null) mc.player.playSound(SoundEvents.PLAYER_LEVELUP, 0.5f, 1.4f);
    }

    public static void denied() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.playSound(SoundEvents.VILLAGER_NO, 0.4f, 1.2f);
    }

    // --- Timers ---

    // 0 = ready, 1 = just used
    public static float cooldownFraction(Identifier id) {
        Timer timer = COOLDOWNS.get(id);
        return timer == null ? 0 : fraction(timer);
    }

    public static int cooldownTicksLeft(Identifier id) {
        Timer timer = COOLDOWNS.get(id);
        return timer == null ? 0 : (int) Math.max(0, timer.end() - now());
    }

    public static float gcdFraction() {
        return fraction(gcd);
    }

    public static Identifier windupId() {
        return windupId != null && now() <= windup.end() + 5 ? windupId : null;
    }

    // 0 at the start of the windup, 1 when it fires
    public static float windupProgress() {
        long span = Math.max(1, windup.end() - windup.start());
        return Math.clamp((now() - windup.start()) / (float) span, 0f, 1f);
    }

    public static java.util.Set<Identifier> toggled() {
        return java.util.Collections.unmodifiableSet(TOGGLED);
    }

    public static boolean isToggled(Identifier id) {
        return TOGGLED.contains(id);
    }

    private static float fraction(Timer timer) {
        long now = now();
        if (now >= timer.end()) return 0;
        return (timer.end() - now) / (float) Math.max(1, timer.end() - timer.start());
    }

    // Ultimates flash briefly when they come off cooldown (spec 5: "pings" only for Ultimates)
    public static void tick() {
        long now = now();
        COOLDOWNS.entrySet().removeIf(entry -> {
            if (now < entry.getValue().end()) return false;
            AbilityDefinition def = get(entry.getKey());
            if (def != null && def.abilityClass() == AbilityClass.ULTIMATE) {
                READY_PINGS.put(entry.getKey(), now + 20);
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) mc.player.playSound(SoundEvents.NOTE_BLOCK_CHIME.value(), 0.6f, 1.6f);
            }
            return true;
        });
        READY_PINGS.values().removeIf(until -> now > until);
    }

    public static boolean pinging(Identifier id) {
        Long until = READY_PINGS.get(id);
        return until != null && now() <= until;
    }

    // --- Mirrors of the server checks, for dimming and the book ---

    public static boolean unlocked(Player player, Identifier id, AbilityDefinition def) {
        return def != null && AbilityService.lockState(player, def, id) == null;
    }

    // Unlocked, right weapon, enough Momentum: what the HUD shows as usable (cooldowns aside)
    public static boolean usable(Player player, Identifier id, AbilityDefinition def) {
        if (!unlocked(player, id, def)) return false;
        if (!def.requires().weaponMatches(player.getMainHandItem(), player.getOffhandItem())) return false;
        if (isToggled(id)) return true;
        if (def.drainsFaith() && com.toddlandis.skillcraft.prayer.PrayerService.faith(player).points() <= 0) return false;
        if (!player.isCreative()) {
            for (AbilityDefinition.ItemCost cost : def.itemCosts()) {
                if (!com.toddlandis.skillcraft.magic.MagicFocus.supplies(player, cost.item()) && AbilityService.countItem(player, cost.item()) < cost.count()) return false;
            }
        }
        return data(player).momentum() + 1.0E-4f >= def.momentumRequired(AbilityService.skillLevel(player, def));
    }

    public static void clear() {
        COOLDOWNS.clear();
        TOGGLED.clear();
        READY_PINGS.clear();
        windupId = null;
        gcd = new Timer(0, 0);
    }
}
