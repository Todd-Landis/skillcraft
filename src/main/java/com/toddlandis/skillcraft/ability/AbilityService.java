package com.toddlandis.skillcraft.ability;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.toddlandis.skillcraft.Config;
import com.toddlandis.skillcraft.network.AbilityCooldownPayload;
import com.toddlandis.skillcraft.network.AbilityDeniedPayload;
import com.toddlandis.skillcraft.network.AbilityUnlockedPayload;
import com.toddlandis.skillcraft.registry.ModAttachments;
import com.toddlandis.skillcraft.skill.Skill;
import com.toddlandis.skillcraft.skill.SkillManager;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

// The server side of the ability bar. The client only ever asks "use slot N" or "set this bar";
// everything is checked and run here (spec 2, non-negotiables):
//   unlocked (level / Codex) -> weapon class held -> windup not in progress -> cooldown -> global
//   cooldown (unless off-GCD) -> Momentum -> item costs -> the behaviour itself (e.g. a target in reach)
// A check that fails costs nothing. Cooldowns and Momentum are never saved.
public final class AbilityService {
    public enum Denied { UNKNOWN, EMPTY, DEAD, COMING_LATER, LOCKED, CODEX, WEAPON, BUSY, COOLDOWN, GCD, MOMENTUM, ITEMS, NO_TARGET, IN_COMBAT, INVALID }

    private static final double WINDUP_MOVE_TOLERANCE = 0.35;

    private AbilityService() {}

    public static AbilityData data(Player player) {
        return player.getData(ModAttachments.ABILITIES);
    }

    // Writing the attachment back is what syncs it to the owner's client
    public static void save(ServerPlayer player, AbilityData data) {
        player.setData(ModAttachments.ABILITIES, data);
    }

    // --- Unlocking ---

    // Null if unlocked, otherwise why not. Creative players have everything (handy for testing).
    public static Denied lockState(Player player, AbilityDefinition def, Identifier id) {
        if (def.category().comingLater()) return Denied.COMING_LATER;
        if (player.isCreative()) return null;
        AbilityDefinition.Requirement req = def.requires();
        if (SkillManager.level(player, req.skill()) < req.level()) return Denied.LOCKED;
        if (req.codex() && !data(player).hasLearned(id)) return Denied.CODEX;
        return null;
    }

    public static boolean isUnlocked(Player player, Identifier id) {
        AbilityDefinition def = AbilityManager.get(id);
        return def != null && lockState(player, def, id) == null;
    }

    // --- Using ---

    public static Denied useSlot(ServerPlayer player, int slot) {
        Identifier id = data(player).activeSlot(slot);
        if (id == null) return Denied.EMPTY;
        return use(player, id);
    }

    public static Denied use(ServerPlayer player, Identifier id) {
        AbilityDefinition def = AbilityManager.get(id);
        Denied denied = check(player, id, def);
        if (denied != null) {
            deny(player, id, def, denied);
            return denied;
        }
        AbilityRuntime runtime = AbilityRuntime.of(player);
        long now = player.level().getGameTime();
        if (def.windup() > 0 && !runtime.toggles.contains(id)) {
            runtime.windupId = id;
            runtime.windupEnd = now + def.windup();
            runtime.windupPos = player.position();
            com.toddlandis.skillcraft.network.ModNetwork.sendTo(player, new AbilityCooldownPayload(id, 0, 0, def.windup(), false));
            return null;
        }
        denied = execute(player, id, def);
        if (denied != null) deny(player, id, def, denied);
        return denied;
    }

    // Every check except the behaviour's own (which needs to run to know)
    public static Denied check(ServerPlayer player, Identifier id, AbilityDefinition def) {
        if (def == null) return Denied.UNKNOWN;
        if (!player.isAlive() || player.isSpectator()) return Denied.DEAD;
        Denied locked = lockState(player, def, id);
        if (locked != null) return locked;
        if (!def.requires().weaponMatches(player.getMainHandItem(), player.getOffhandItem())) return Denied.WEAPON;
        AbilityRuntime runtime = AbilityRuntime.of(player);
        if (runtime.toggles.contains(id)) return null; // pressing an active toggle turns it off
        long now = player.level().getGameTime();
        if (runtime.windupId != null) return Denied.BUSY;
        if (now < runtime.readyAt(id)) return Denied.COOLDOWN;
        if (!def.offGcd() && now < runtime.gcdUntil) return Denied.GCD;
        if (data(player).momentum() + 1.0E-4f < def.momentumRequired(skillLevel(player, def))) return Denied.MOMENTUM;
        if (!player.isCreative()) {
            for (AbilityDefinition.ItemCost cost : def.itemCosts()) {
                if (countItem(player, cost.item()) < cost.count()) return Denied.ITEMS;
            }
        }
        return null;
    }

    // Runs the ability, then pays for it (only if it actually did something)
    static Denied execute(ServerPlayer player, Identifier id, AbilityDefinition def) {
        AbilityRuntime runtime = AbilityRuntime.of(player);
        AbilityContext context = new AbilityContext(player, player.level(), id, def);
        long now = context.now();

        if (runtime.toggles.contains(id)) {
            stopToggle(player, id, def);
            return null;
        }
        if (!def.behavior().execute(context)) return Denied.NO_TARGET;

        AbilityData data = data(player);
        int level = skillLevel(player, def);
        float momentum = data.momentum();
        momentum = def.abilityClass() == AbilityClass.ULTIMATE ? 0 : momentum - def.momentumCost(level);
        data.setMomentum(momentum + def.momentum().generate());
        save(player, data);
        if (!player.isCreative()) {
            for (AbilityDefinition.ItemCost cost : def.itemCosts()) removeItem(player, cost.item(), cost.count());
        }
        if (def.isToggle()) {
            runtime.toggles.add(id);
        } else {
            runtime.readyAt.put(id, now + def.cooldown());
        }
        if (!def.offGcd()) runtime.gcdUntil = now + Config.GLOBAL_COOLDOWN.get();
        def.sound().ifPresent(sound -> player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 1f, 1f));
        com.toddlandis.skillcraft.network.ModNetwork.sendTo(player, new AbilityCooldownPayload(id, def.isToggle() ? 0 : def.cooldown(),
                def.offGcd() ? 0 : Config.GLOBAL_COOLDOWN.get(), 0, def.isToggle()));
        return null;
    }

    private static void stopToggle(ServerPlayer player, Identifier id, AbilityDefinition def) {
        AbilityRuntime runtime = AbilityRuntime.of(player);
        runtime.toggles.remove(id);
        if (def != null) {
            def.behavior().endToggle(new AbilityContext(player, player.level(), id, def));
            runtime.readyAt.put(id, player.level().getGameTime() + def.cooldown());
        }
        com.toddlandis.skillcraft.network.ModNetwork.sendTo(player, new AbilityCooldownPayload(id, def == null ? 0 : def.cooldown(), 0, 0, false));
    }

    public static int skillLevel(Player player, AbilityDefinition def) {
        return SkillManager.level(player, def.requires().skill());
    }

    // --- Every server tick, per player (also called directly by GameTests for fake players) ---

    public static void tick(ServerPlayer player) {
        AbilityRuntime runtime = AbilityRuntime.of(player);
        long now = player.level().getGameTime();

        // Windup: moving cancels it with nothing spent
        if (runtime.windupId != null) {
            Identifier id = runtime.windupId;
            AbilityDefinition def = AbilityManager.get(id);
            if (def == null || !player.isAlive() || player.position().subtract(runtime.windupPos).horizontalDistance() > WINDUP_MOVE_TOLERANCE) {
                runtime.windupId = null;
                com.toddlandis.skillcraft.network.ModNetwork.sendTo(player, new AbilityCooldownPayload(id, 0, 0, 0, false));
                player.sendOverlayMessage(Component.translatable("message.skillcraft.ability_windup_cancelled", AbilityDefinition.name(id)).withStyle(ChatFormatting.GRAY));
            } else if (now >= runtime.windupEnd) {
                runtime.windupId = null;
                Denied denied = check(player, id, def);
                if (denied == null) denied = execute(player, id, def);
                if (denied != null) {
                    com.toddlandis.skillcraft.network.ModNetwork.sendTo(player, new AbilityCooldownPayload(id, 0, 0, 0, false));
                    deny(player, id, def, denied);
                }
            }
        }

        // Toggles drain Momentum each tick and switch off when it runs out (or the weapon is gone)
        if (!runtime.toggles.isEmpty()) {
            AbilityData data = data(player);
            boolean changed = false;
            for (Iterator<Identifier> it = new HashSet<>(runtime.toggles).iterator(); it.hasNext(); ) {
                Identifier id = it.next();
                AbilityDefinition def = AbilityManager.get(id);
                float drain = def == null ? 0 : def.toggle().map(AbilityDefinition.Toggle::drainPerSecond).orElse(0f) / 20f;
                if (def == null || data.momentum() < drain || !def.requires().weaponMatches(player.getMainHandItem(), player.getOffhandItem())) {
                    stopToggle(player, id, def);
                    continue;
                }
                data.setMomentum(data.momentum() - drain);
                changed |= drain > 0;
                def.behavior().tickToggle(new AbilityContext(player, player.level(), id, def));
            }
            if (changed && now % 5 == 0) save(player, data);
        }

        // Heal over time (Second Wind)
        if (now < runtime.healUntil && now % 20 == 0 && player.isAlive()) player.heal(runtime.healPerSecond);

        // Momentum decays out of combat
        if (now % 20 == 0) {
            AbilityData data = data(player);
            if (data.momentum() > 0 && now >= data.combatUntil() - Config.COMBAT_WINDOW.get() + Config.MOMENTUM_DECAY_DELAY.get()) {
                data.setMomentum(data.momentum() - Config.MOMENTUM_DECAY_PER_SECOND.get().floatValue());
                save(player, data);
            }
        }
    }

    // --- Momentum and combat state ---

    public static void gainMomentum(ServerPlayer player, double amount) {
        if (amount <= 0) return;
        AbilityData data = data(player);
        data.setMomentum((float) (data.momentum() + amount));
        save(player, data);
    }

    public static void markCombat(ServerPlayer player) {
        AbilityData data = data(player);
        long until = player.level().getGameTime() + Config.COMBAT_WINDOW.get();
        if (until > data.combatUntil()) {
            data.setCombatUntil(until);
            save(player, data);
        }
    }

    // --- Loadouts ---

    // Changes one of the 3 bars (and optionally makes it active). Every ability must be unlocked;
    // changing or switching to the active bar is blocked in combat (spec 2.1).
    public static Denied setLoadout(ServerPlayer player, int loadout, Identifier[] slots, String name, boolean activate) {
        if (loadout < 0 || loadout >= AbilityData.LOADOUTS || slots.length > AbilityData.SLOTS) return Denied.INVALID;
        AbilityData data = data(player);
        Identifier[] clean = new Identifier[AbilityData.SLOTS];
        Set<Identifier> seen = new HashSet<>();
        for (int s = 0; s < slots.length; s++) {
            Identifier id = slots[s];
            if (id == null) continue;
            AbilityDefinition def = AbilityManager.get(id);
            if (def == null || lockState(player, def, id) != null) return Denied.LOCKED;
            if (seen.add(id)) clean[s] = id; // the same ability twice: keep the first
        }
        boolean touchesActive = loadout == data.activeLoadout() || activate;
        boolean changesBar = !java.util.Arrays.equals(clean, data.bar(loadout));
        if (touchesActive && (changesBar || loadout != data.activeLoadout()) && data.inCombat(player.level().getGameTime())) {
            deny(player, null, null, Denied.IN_COMBAT);
            return Denied.IN_COMBAT;
        }
        data.setBar(loadout, clean);
        if (name != null) data.setName(loadout, name);
        if (activate) data.setActiveLoadout(loadout);
        save(player, data);
        return null;
    }

    public static Denied selectLoadout(ServerPlayer player, int loadout) {
        AbilityData data = data(player);
        if (loadout < 0 || loadout >= AbilityData.LOADOUTS) return Denied.INVALID;
        return setLoadout(player, loadout, data.bar(loadout), null, true);
    }

    // --- Unlock toasts ---

    // Called from SkillManager on every level-up
    public static void onLevelUp(ServerPlayer player, Skill skill, int oldLevel, int newLevel) {
        for (var entry : AbilityManager.all().entrySet()) {
            AbilityDefinition def = entry.getValue();
            AbilityDefinition.Requirement req = def.requires();
            if (req.skill() != skill || req.level() <= oldLevel || req.level() > newLevel || def.category().comingLater()) continue;
            if (req.codex() && !data(player).hasLearned(entry.getKey())) continue;
            com.toddlandis.skillcraft.network.ModNetwork.sendTo(player, new AbilityUnlockedPayload(entry.getKey()));
        }
    }

    public static void announceUnlock(ServerPlayer player, Identifier id) {
        com.toddlandis.skillcraft.network.ModNetwork.sendTo(player, new AbilityUnlockedPayload(id));
    }

    // --- Feedback ---

    // A short reason on the overlay and a "no" sound, at most every half second per reason
    public static void deny(ServerPlayer player, Identifier id, AbilityDefinition def, Denied reason) {
        AbilityRuntime runtime = AbilityRuntime.of(player);
        String failKey = runtime.failKey;
        runtime.failKey = null;
        long now = player.level().getGameTime();
        String key = reason.name() + id;
        if (key.equals(runtime.lastDenyKey) && now - runtime.lastDenyTick < 10) return;
        runtime.lastDenyKey = key;
        runtime.lastDenyTick = now;
        Component name = id == null ? Component.empty() : AbilityDefinition.name(id);
        Component message = switch (reason) {
            case LOCKED -> Component.translatable("message.skillcraft.ability_locked", name, def.requires().skill().displayName(), def.requires().level());
            case CODEX -> Component.translatable("message.skillcraft.ability_codex", name);
            case WEAPON -> Component.translatable("message.skillcraft.ability_weapon", name, weaponName(def));
            case COOLDOWN -> Component.translatable("message.skillcraft.ability_cooldown", name,
                    String.format("%.1f", Math.max(0, AbilityRuntime.of(player).readyAt(id) - now) / 20.0));
            case MOMENTUM -> Component.translatable("message.skillcraft.ability_momentum", name, Math.round(def.momentumRequired(skillLevel(player, def))));
            case ITEMS -> Component.translatable("message.skillcraft.ability_items", name);
            case NO_TARGET -> Component.translatable(failKey != null ? failKey : "message.skillcraft.ability_no_target", name);
            case IN_COMBAT -> Component.translatable("message.skillcraft.ability_in_combat");
            case COMING_LATER -> Component.translatable("message.skillcraft.ability_coming_later");
            case BUSY, GCD -> null; // too frequent to be worth a message; the HUD shows it
            default -> Component.translatable("message.skillcraft.ability_unknown");
        };
        if (message != null) player.sendOverlayMessage(message.copy().withStyle(ChatFormatting.RED));
        com.toddlandis.skillcraft.network.ModNetwork.sendTo(player, new AbilityDeniedPayload());
    }

    public static Component weaponName(AbilityDefinition def) {
        AbilityDefinition.Requirement req = def.requires();
        return req.offhand().isPresent() ? tagName(req.offhand().get()) : req.weapon().map(AbilityService::tagName).orElse(Component.empty());
    }

    public static Component tagName(TagKey<Item> tag) {
        String path = tag.location().getPath();
        return Component.translatable("weapon_class.skillcraft." + path.substring(path.lastIndexOf('/') + 1));
    }

    // --- Items (for item-cost abilities) ---

    private static int countItem(Player player, Item item) {
        int count = 0;
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }

    private static void removeItem(Player player, Item item, int count) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize() && count > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.is(item)) continue;
            int taken = Math.min(count, stack.getCount());
            stack.shrink(taken);
            count -= taken;
        }
    }
}
