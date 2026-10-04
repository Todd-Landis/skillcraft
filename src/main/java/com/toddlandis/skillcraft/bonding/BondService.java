package com.toddlandis.skillcraft.bonding;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.toddlandis.skillcraft.Config;
import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.arena.ArenaDimension;
import com.toddlandis.skillcraft.registry.ModAttachments;
import com.toddlandis.skillcraft.registry.ModBlocks;
import com.toddlandis.skillcraft.registry.ModDataMaps;
import com.toddlandis.skillcraft.skill.Skill;
import com.toddlandis.skillcraft.skill.SkillManager;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

// Everything Beastbonding does to creatures (docs/BEASTBONDING.md), all on the server:
//   bond    - a creature goes into the player's Bond Codex (saved exactly as it is) and leaves the world
//   summon  - it comes back out beside the player, tagged as theirs
//   store   - it goes back into the codex, saved as it is now
//   release - it leaves the codex as an ordinary creature
//   death   - it becomes Fallen; rekindle it at a Rekindling Shrine for its tribute and some time
// The codex copy is the one that counts: a live summon the codex doesn't list as Out is removed.
public final class BondService {
    // Tags in a live summon's NeoForge persistent data
    public static final String OWNER_TAG = "skillcraft_bond_owner";
    public static final String BOND_TAG = "skillcraft_bond_id";
    private static final Identifier BONUS_ID = Identifier.fromNamespaceAndPath(SkillCraft.MODID, "bond_bonus");

    private static final Map<UUID, Long> LAST_SUMMON = new HashMap<>();

    private BondService() {}

    // --- Lookups ---

    public static BondCodex codex(Player player) {
        return player.getData(ModAttachments.BOND_CODEX);
    }

    // Saves the codex back, which also syncs the owner's screen
    public static void save(ServerPlayer player, BondCodex codex) {
        player.setData(ModAttachments.BOND_CODEX, codex);
    }

    public static Bondable bondable(EntityType<?> type) {
        return BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type).getData(ModDataMaps.BONDABLE);
    }

    public static Optional<EntityType<?>> species(Identifier id) {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(id);
    }

    public static Bondable bondable(Identifier species) {
        return species(species).map(BondService::bondable).orElse(null);
    }

    public static int level(Player player) {
        return SkillManager.level(player, Skill.BEASTBONDING);
    }

    public static int slots(int level) {
        return Config.BOND_SLOTS_BASE.get() + level / Config.BOND_SLOTS_EVERY.get();
    }

    public static int companionLimit(int level) {
        return level >= Config.BOND_SECOND_COMPANION_LEVEL.get() ? 2 : 1;
    }

    public static UUID ownerOf(Entity entity) {
        String owner = entity.getPersistentData().getStringOr(OWNER_TAG, "");
        try {
            return owner.isEmpty() ? null : UUID.fromString(owner);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean isBonded(Entity entity) {
        return ownerOf(entity) != null;
    }

    // The species name for messages ("Wolf", "Cliffstrider") or the bond's custom name
    public static Component displayName(BondCodex.Bond bond) {
        if (!bond.name.isEmpty()) return Component.literal(bond.name);
        Bondable data = bondable(bond.species);
        return species(bond.species).map(type -> data != null ? data.summonType(type).getDescription() : type.getDescription())
                .orElse(Component.literal(bond.species.toString()));
    }

    // Lets the next summon happen at once (GameTests summon several times in one tick)
    public static void resetSummonCooldown(Player player) {
        LAST_SUMMON.remove(player.getUUID());
    }

    // --- Bonding ---

    // Use the Bond Codex on a creature. Returns true if it went into the codex.
    public static boolean bond(ServerPlayer player, LivingEntity target) {
        if (isBonded(target)) {
            message(player, Component.translatable(player.getUUID().equals(ownerOf(target)) ? "message.skillcraft.bond_already" : "message.skillcraft.bond_not_yours"), ChatFormatting.RED);
            return false;
        }
        Bondable data = bondable(target.getType());
        if (data == null) {
            message(player, Component.translatable("message.skillcraft.bond_cannot", target.getType().getDescription()), ChatFormatting.RED);
            return false;
        }
        int level = level(player);
        Component speciesName = data.summonType(target.getType()).getDescription();
        if (level < data.level()) {
            message(player, Component.translatable("message.skillcraft.bond_level", data.level(), speciesName), ChatFormatting.RED);
            return false;
        }
        BondCodex codex = codex(player);
        if (codex.bonds().size() >= slots(level)) {
            message(player, Component.translatable("message.skillcraft.bond_full", slots(level)), ChatFormatting.RED);
            return false;
        }
        if (data.bond() == Bondable.Method.TAMED && !tamedBy(target, player)) {
            message(player, Component.translatable("message.skillcraft.bond_tame_first", speciesName), ChatFormatting.RED);
            return false;
        }
        if (data.bond() == Bondable.Method.OFFERING && data.offering().isPresent() && !player.isCreative()) {
            Bondable.Cost offering = data.offering().get();
            if (!take(player, offering, false)) {
                message(player, Component.translatable("message.skillcraft.bond_offering", offering.count(), offering.item().getDefaultInstance().getHoverName(), speciesName), ChatFormatting.RED);
                return false;
            }
            take(player, offering, true);
        }

        // A changed creature (spider -> Cliffstrider) starts fresh as its new kind; others keep everything
        CompoundTag saved = data.summonAs().isPresent() ? new CompoundTag() : snapshot(target);
        Identifier species = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        String name = target.hasCustomName() ? target.getCustomName().getString() : "";
        BondCodex.Bond bond = new BondCodex.Bond(UUID.randomUUID(), species, name, saved, BondCodex.State.STORED, 0, Optional.empty(),
                target.getHealth(), target.getMaxHealth());
        codex.add(bond);
        boolean newPage = codex.pages().add(species);
        save(player, codex);

        ServerLevel level0 = player.level();
        level0.sendParticles(ParticleTypes.HAPPY_VILLAGER, target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(), 20, 0.4, 0.4, 0.4, 0.05);
        level0.playSound(null, target.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 1.2f);
        target.ejectPassengers();
        target.discard();

        double xp = data.bondXp() * (newPage ? Config.BOND_FIRST_PAGE_MULTIPLIER.get() : 1.0);
        SkillManager.awardXp(player, Skill.BEASTBONDING, xp);
        message(player, Component.translatable(newPage ? "message.skillcraft.bond_new_page" : "message.skillcraft.bonded", displayName(bond)), ChatFormatting.GREEN);
        return true;
    }

    // Tamed by this player: a tamed wolf/cat/parrot they own, or a tamed horse-like they own
    static boolean tamedBy(LivingEntity target, Player player) {
        if (target instanceof AbstractHorse horse && !horse.isTamed()) return false;
        // Compare owner ids rather than looking the owner up, which needs them loaded in the level
        return target instanceof OwnableEntity ownable && ownable.getOwnerReference() != null
                && player.getUUID().equals(ownable.getOwnerReference().getUUID());
    }

    // Checks (consume = false) or takes (consume = true) an item cost from the inventory
    static boolean take(Player player, Bondable.Cost cost, boolean consume) {
        int need = cost.count();
        for (int i = 0; i < player.getInventory().getContainerSize() && need > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.is(cost.item())) continue;
            int used = Math.min(need, stack.getCount());
            if (consume) stack.shrink(used);
            need -= used;
        }
        return need <= 0;
    }

    // --- Summoning and storing ---

    // Summons a stored bond beside the player. Returns the creature, or null (with a message why).
    public static Entity summon(ServerPlayer player, UUID bondId) {
        BondCodex codex = codex(player);
        BondCodex.Bond bond = codex.find(bondId);
        if (bond == null || bond.state != BondCodex.State.STORED) return null;
        Bondable data = bondable(bond.species);
        Optional<EntityType<?>> species = species(bond.species);
        if (data == null || species.isEmpty()) return null;
        ServerLevel level = player.level();
        if (ArenaDimension.is(level)) {
            message(player, Component.translatable("message.skillcraft.bond_no_arena"), ChatFormatting.RED);
            return null;
        }
        if (level(player) < data.level()) {
            message(player, Component.translatable("message.skillcraft.bond_level", data.level(), displayName(bond)), ChatFormatting.RED);
            return null;
        }
        long now = level.getGameTime();
        Long last = LAST_SUMMON.get(player.getUUID());
        if (last != null && now - last < Config.BOND_SUMMON_COOLDOWN.get() && now >= last) return null;

        // One mount and one (or two) companions out at once: the oldest of the same kind goes back
        int allowed = data.kind() == Bondable.Kind.MOUNT ? 1 : companionLimit(level(player));
        int out = 0;
        BondCodex.Bond oldest = null;
        for (BondCodex.Bond other : codex.bonds()) {
            Bondable otherData = bondable(other.species);
            if (other.state != BondCodex.State.OUT || otherData == null || otherData.kind() != data.kind()) continue;
            out++;
            if (oldest == null) oldest = other;
        }
        if (out >= allowed && oldest != null) store(player, oldest);

        Entity entity = data.summonType(species.get()).create(level, EntitySpawnReason.MOB_SUMMONED);
        if (entity == null) return null;
        if (!bond.data.isEmpty()) {
            entity.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), bond.data));
        } else if (!bond.name.isEmpty()) {
            entity.setCustomName(Component.literal(bond.name));
        }
        entity.setUUID(UUID.randomUUID());
        Vec3 spot = spawnSpot(player, entity);
        entity.snapTo(spot.x, spot.y, spot.z, player.getYRot(), 0f);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.getPersistentData().putString(OWNER_TAG, player.getUUID().toString());
        entity.getPersistentData().putString(BOND_TAG, bond.id.toString());
        if (entity instanceof Mob mob) mob.setPersistenceRequired();
        if (entity instanceof IronGolem golem) golem.setPlayerCreated(true); // never turns on players
        if (entity instanceof LivingEntity living) applyBonuses(living, data, level(player));
        level.addFreshEntity(entity);

        bond.state = BondCodex.State.OUT;
        bond.entity = entity.getUUID();
        save(player, codex);
        LAST_SUMMON.put(player.getUUID(), now);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, spot.x, spot.y + 0.5, spot.z, 16, 0.4, 0.4, 0.4, 0.05);
        level.playSound(null, BlockPos.containing(spot), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.4f);
        return entity;
    }

    // Two blocks in front of the player if there's room, otherwise where they stand
    private static Vec3 spawnSpot(ServerPlayer player, Entity entity) {
        Vec3 look = Vec3.directionFromRotation(0, player.getYRot());
        Vec3 ahead = player.position().add(look.scale(2));
        entity.setPos(ahead.x, ahead.y, ahead.z);
        if (player.level().noCollision(entity)) return ahead;
        return player.position();
    }

    // Level bonuses: +speed for mounts, +health for everyone (transient, so they're never saved twice)
    static void applyBonuses(LivingEntity entity, Bondable data, int level) {
        AttributeInstance health = entity.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            float before = entity.getHealth() / Math.max(1f, entity.getMaxHealth());
            health.addOrUpdateTransientModifier(new AttributeModifier(BONUS_ID, Config.BOND_HEALTH_PER_LEVEL.get() * level, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            entity.setHealth(before * entity.getMaxHealth());
        }
        if (data.kind() != Bondable.Kind.MOUNT) return;
        double speed = Config.BOND_SPEED_PER_LEVEL.get() * level;
        boost(entity.getAttribute(Attributes.MOVEMENT_SPEED), speed);
        boost(entity.getAttribute(Attributes.FLYING_SPEED), speed);
    }

    private static void boost(AttributeInstance instance, double amount) {
        if (instance != null) instance.addOrUpdateTransientModifier(new AttributeModifier(BONUS_ID, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    // Puts a summoned bond back into the codex, saved as it is now
    public static boolean store(ServerPlayer player, BondCodex.Bond bond) {
        if (bond.state != BondCodex.State.OUT) return false;
        BondCodex codex = codex(player);
        Entity entity = findLive(player.level().getServer(), bond.entity);
        if (entity instanceof LivingEntity living && living.isAlive()) {
            living.ejectPassengers();
            if (living.isPassenger()) living.stopRiding();
            bond.data = snapshot(living);
            bond.health = living.getHealth();
            bond.maxHealth = living.getMaxHealth();
            if (living.level() instanceof ServerLevel where) {
                where.sendParticles(ParticleTypes.POOF, living.getX(), living.getY() + living.getBbHeight() / 2, living.getZ(), 12, 0.3, 0.3, 0.3, 0.02);
            }
            living.discard();
        }
        // If it couldn't be found (an unloaded chunk) it goes back as it was when summoned; the stray
        // copy is removed if it ever loads (see BondEvents.onJoin)
        bond.state = BondCodex.State.STORED;
        bond.entity = null;
        save(player, codex);
        return true;
    }

    public static void storeAll(ServerPlayer player) {
        BondCodex codex = codex(player);
        for (BondCodex.Bond bond : codex.bonds()) {
            if (bond.state == BondCodex.State.OUT) store(player, bond);
        }
    }

    // Lets a stored creature go: it appears beside the player as an ordinary creature
    public static boolean release(ServerPlayer player, UUID bondId) {
        BondCodex codex = codex(player);
        BondCodex.Bond bond = codex.find(bondId);
        if (bond == null || bond.state != BondCodex.State.STORED) return false;
        Bondable data = bondable(bond.species);
        Optional<EntityType<?>> species = species(bond.species);
        if (species.isPresent()) {
            ServerLevel level = player.level();
            Entity entity = species.get().create(level, EntitySpawnReason.MOB_SUMMONED);
            if (entity != null) {
                // A changed creature goes back to being its vanilla self (a Cliffstrider becomes a spider again)
                if (!bond.data.isEmpty() && (data == null || data.summonAs().isEmpty())) {
                    entity.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), bond.data));
                }
                if (!bond.name.isEmpty()) entity.setCustomName(Component.literal(bond.name));
                entity.setUUID(UUID.randomUUID());
                Vec3 spot = spawnSpot(player, entity);
                entity.snapTo(spot.x, spot.y, spot.z, player.getYRot(), 0f);
                entity.getPersistentData().remove(OWNER_TAG);
                entity.getPersistentData().remove(BOND_TAG);
                level.addFreshEntity(entity);
            }
        }
        codex.remove(bond);
        save(player, codex);
        message(player, Component.translatable("message.skillcraft.bond_released", displayName(bond)), ChatFormatting.YELLOW);
        return true;
    }

    // --- Death and rekindling ---

    // A summoned creature died: it becomes Fallen, keeping everything it carried
    public static void fallen(ServerPlayer owner, LivingEntity entity) {
        BondCodex codex = codex(owner);
        BondCodex.Bond bond = codex.findByEntity(entity.getUUID());
        if (bond == null) return;
        bond.data = snapshot(entity);
        bond.health = 0;
        bond.maxHealth = entity.getMaxHealth();
        bond.state = BondCodex.State.FALLEN;
        bond.entity = null;
        save(owner, codex);
        message(owner, Component.translatable("message.skillcraft.bond_fallen", displayName(bond)), ChatFormatting.RED);
    }

    public static boolean nearShrine(ServerPlayer player) {
        int r = Config.BOND_SHRINE_RADIUS.get();
        BlockPos center = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
            if (player.level().getBlockState(pos).is(ModBlocks.REKINDLING_SHRINE.get())) return true;
        }
        return false;
    }

    // At a Rekindling Shrine: pay a Fallen bond's tribute and start bringing it back
    public static boolean rekindle(ServerPlayer player, UUID bondId) {
        BondCodex codex = codex(player);
        BondCodex.Bond bond = codex.find(bondId);
        if (bond == null || bond.state != BondCodex.State.FALLEN) return false;
        Bondable data = bondable(bond.species);
        if (data == null) return false;
        if (!nearShrine(player)) {
            message(player, Component.translatable("message.skillcraft.bond_need_shrine"), ChatFormatting.RED);
            return false;
        }
        Bondable.Cost tribute = data.tribute();
        if (!player.isCreative()) {
            if (!take(player, tribute, false)) {
                message(player, Component.translatable("message.skillcraft.bond_tribute", tribute.count(), tribute.item().getDefaultInstance().getHoverName(), displayName(bond)), ChatFormatting.RED);
                return false;
            }
            take(player, tribute, true);
        }
        bond.state = BondCodex.State.REKINDLING;
        bond.readyAt = gameTime(player.level().getServer()) + data.rekindleSeconds() * 20L;
        save(player, codex);
        player.level().sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 1, player.getZ(), 20, 0.6, 0.4, 0.6, 0.02);
        player.level().playSound(null, player.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 0.8f, 1.2f);
        message(player, Component.translatable("message.skillcraft.bond_rekindling", displayName(bond), data.rekindleSeconds()), ChatFormatting.GOLD);
        return true;
    }

    // Called every second for each player: finishes rekindles whose time has come
    public static void tick(ServerPlayer player) {
        BondCodex codex = codex(player);
        long now = gameTime(player.level().getServer());
        boolean changed = false;
        for (BondCodex.Bond bond : codex.bonds()) {
            if (bond.state != BondCodex.State.REKINDLING || now < bond.readyAt) continue;
            reviveData(bond);
            bond.state = BondCodex.State.STORED;
            changed = true;
            Bondable data = bondable(bond.species);
            if (data != null) SkillManager.awardXp(player, Skill.BEASTBONDING, data.bondXp() * 0.5);
            message(player, Component.translatable("message.skillcraft.bond_rekindled", displayName(bond)), ChatFormatting.GREEN);
        }
        if (changed) save(player, codex);
    }

    // Full health, no fire, no lingering death state
    static void reviveData(BondCodex.Bond bond) {
        float max = bond.maxHealth > 0 ? bond.maxHealth : 20f;
        if (!bond.data.isEmpty()) {
            bond.data.putFloat("Health", max);
            for (String key : new String[]{"DeathTime", "HurtTime", "HurtByTimestamp", "Fire", "active_effects"}) bond.data.remove(key);
        }
        bond.health = max;
    }

    // The overworld's clock, so rekindle timers agree across dimensions
    static long gameTime(MinecraftServer server) {
        return server.overworld().getGameTime();
    }

    // --- Helpers ---

    public static CompoundTag snapshot(Entity entity) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, entity.level().registryAccess());
        entity.saveWithoutId(output);
        CompoundTag tag = output.buildResult();
        tag.remove("UUID");
        return tag;
    }

    public static Entity findLive(MinecraftServer server, UUID uuid) {
        if (uuid == null || server == null) return null;
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(uuid);
            if (entity != null) return entity;
        }
        return null;
    }

    static void message(ServerPlayer player, Component text, ChatFormatting color) {
        player.sendOverlayMessage(text.copy().withStyle(color));
    }
}
