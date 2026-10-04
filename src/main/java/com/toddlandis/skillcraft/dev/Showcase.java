package com.toddlandis.skillcraft.dev;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.boss.SlagheartEntity;
import com.toddlandis.skillcraft.registry.ModBlocks;
import com.toddlandis.skillcraft.registry.ModEntities;
import com.toddlandis.skillcraft.registry.ModItems;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

// Dev-only screenshot showcase, used by the "Client smoke" CI job and handy locally:
//   ./gradlew runServer -Pshowcase    creates the world in run/world, then stops cleanly
//   ./gradlew runClient -Pshowcase -Pworld=<save>   poses the boss, takes screenshots, quits
// Nothing here runs unless the skillcraft.showcase system property is set (build.gradle sets it
// for -Pshowcase). The client half is client/ShowcaseClient.
@EventBusSubscriber(modid = SkillCraft.MODID)
public final class Showcase {
    public static final boolean ENABLED = Boolean.getBoolean("skillcraft.showcase");

    private Showcase() {}

    @SubscribeEvent
    static void onServerStarted(ServerStartedEvent event) {
        if (!ENABLED || !event.getServer().isDedicatedServer()) return;
        SkillCraft.LOGGER.info("[showcase] world created, stopping the server");
        event.getServer().halt(false);
    }

    public static void command(MinecraftServer server, String command) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
    }

    // The player looks north at the boss (AI off, so it only plays the poses it's given), with a
    // zombie in padded armour holding a warhammer on the left and one holding the maul on the right
    public static SlagheartEntity buildScene(ServerPlayer player) {
        ServerLevel level = player.level();
        Vec3 eye = Vec3.atBottomCenterOf(player.blockPosition());
        player.teleport(new TeleportTransition(level, eye, Vec3.ZERO, 180f, 0f, TeleportTransition.DO_NOTHING));

        SlagheartEntity boss = ModEntities.SLAGHEART.get().create(level, EntitySpawnReason.COMMAND);
        boss.snapTo(eye.x, eye.y, eye.z - 8, 0f, 0f);
        boss.setYBodyRot(0f);
        boss.setYHeadRot(0f);
        boss.setNoAi(true);
        boss.setPersistenceRequired();
        level.addFreshEntity(boss);

        Mob left = dummy(level, eye.add(-3.5, 0, -4), ModItems.WARHAMMERS.get("diamond").get());
        left.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.PADDED_HELMET.get()));
        left.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.PADDED_CHESTPLATE.get()));
        left.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.PADDED_LEGGINGS.get()));
        left.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.PADDED_BOOTS.get()));
        Mob right = dummy(level, eye.add(3.5, 0, -4), ModItems.EMBERFALL_MAUL.get());
        right.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.PADDED_HELMET.get()));

        // Hotbar for the HUD screenshot: one of each new item
        ItemLike[] hotbar = {ModItems.WARHAMMERS.get("iron").get(), ModItems.WARHAMMERS.get("netherite").get(), ModItems.EMBERFALL_MAUL.get(),
                ModItems.LIGHT_ARROW.get(), ModItems.HEAVY_ARROW.get(), ModItems.EMBER_SIGIL.get(), ModItems.SUMMONING_BRAZIER.get(),
                ModItems.PADDED_CHESTPLATE.get(), ModItems.SLAG_GLOB.get()};
        for (int i = 0; i < hotbar.length; i++) player.getInventory().setItem(i, new ItemStack(hotbar[i]));
        return boss;
    }

    // "builds" scene, part 1: every station block in a row, 6 blocks north of the player
    public static void buildStations(ServerPlayer player) {
        ServerLevel level = player.level();
        net.minecraft.core.BlockPos base = player.blockPosition();
        var blocks = java.util.List.of(ModBlocks.FORGING_ANVIL.get(), ModBlocks.CRAFTING_BENCH.get(), ModBlocks.SMELTER.get(),
                ModBlocks.BANK_CHEST.get(), ModBlocks.ATTUNEMENT_ALTAR.get(), ModBlocks.SUMMONING_BRAZIER.get());
        for (int i = 0; i < blocks.size(); i++) {
            net.minecraft.core.BlockPos pos = base.offset(-5 + i * 2, 0, -6);
            level.setBlock(pos.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
            level.setBlock(pos, blocks.get(i).defaultBlockState(), 3);
        }
        Vec3 eye = Vec3.atBottomCenterOf(base);
        player.teleport(new TeleportTransition(level, eye, Vec3.ZERO, 180f, 25f, TeleportTransition.DO_NOTHING));
    }

    // "builds" scene, part 2: an arena built high above the player; returns its center
    public static net.minecraft.core.BlockPos buildArena(ServerPlayer player) {
        net.minecraft.core.BlockPos center = player.blockPosition().offset(300, 0, 0).atY(170);
        com.toddlandis.skillcraft.arena.ArenaBuilder.build(player.level(), center);
        return center;
    }

    public static void view(ServerPlayer player, Vec3 at, float yaw, float pitch) {
        player.teleport(new TeleportTransition(player.level(), at, Vec3.ZERO, yaw, pitch, TeleportTransition.DO_NOTHING));
    }

    // -Pshowcase=abilities: a combat-ready player (Melee/Ranged/Defence 50, a full bar, 70 Momentum,
    // diamond sword) facing a training dummy, for HUD and Ability Book screenshots
    public static void abilityScene(ServerPlayer player) {
        ServerLevel level = player.level();
        for (com.toddlandis.skillcraft.skill.Skill skill : new com.toddlandis.skillcraft.skill.Skill[]{com.toddlandis.skillcraft.skill.Skill.MELEE,
                com.toddlandis.skillcraft.skill.Skill.RANGED, com.toddlandis.skillcraft.skill.Skill.DEFENCE, com.toddlandis.skillcraft.skill.Skill.CONSTITUTION}) {
            com.toddlandis.skillcraft.skill.SkillManager.setXp(player, skill, com.toddlandis.skillcraft.skill.XpTable.xpForLevel(50));
        }
        player.getInventory().setItem(0, new ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD));
        player.getInventory().setItem(1, new ItemStack(ModItems.WARHAMMERS.get("diamond").get()));
        player.getInventory().setItem(2, new ItemStack(net.minecraft.world.item.Items.BOW));
        player.getInventory().setItem(3, com.toddlandis.skillcraft.ability.CodexItem.of(net.minecraft.resources.Identifier.fromNamespaceAndPath(SkillCraft.MODID, "forgequake")));
        player.getInventory().setSelectedSlot(0);
        var data = com.toddlandis.skillcraft.ability.AbilityService.data(player);
        data.learn(net.minecraft.resources.Identifier.fromNamespaceAndPath(SkillCraft.MODID, "forgequake"));
        data.setMomentum(70);
        com.toddlandis.skillcraft.ability.AbilityService.save(player, data);
        String[][] bars = {{"lunge", "sweeping_arc", "battle_trance", "brace"}, {"concussive_blow", "earthbreaker", "riposte", "second_wind"}, {"quick_draw", "volley", "tumble", "deadeye"}};
        String[] names = {"Sword", "Hammer", "Bow"};
        for (int l = 0; l < bars.length; l++) {
            net.minecraft.resources.Identifier[] ids = new net.minecraft.resources.Identifier[bars[l].length];
            for (int i = 0; i < ids.length; i++) ids[i] = net.minecraft.resources.Identifier.fromNamespaceAndPath(SkillCraft.MODID, bars[l][i]);
            com.toddlandis.skillcraft.ability.AbilityService.setLoadout(player, l, ids, names[l], l == 0);
        }
        Vec3 at = Vec3.atBottomCenterOf(player.blockPosition());
        player.teleport(new TeleportTransition(level, at, Vec3.ZERO, 180f, 10f, TeleportTransition.DO_NOTHING));
        Mob target = dummy(level, at.add(0, 0, -3), net.minecraft.world.item.Items.IRON_SWORD);
        target.setItemSlot(EquipmentSlot.HEAD, new ItemStack(net.minecraft.world.item.Items.IRON_HELMET));
    }

    // -Pshowcase=fishing: a 9x9 pond north of the player with cod, salmon and reef fish spots, and a
    // lit Cooking Range on the bank. Returns the cod spot (the one the player fishes).
    public static com.toddlandis.skillcraft.fishing.FishingSpotEntity fishingScene(ServerPlayer player) {
        ServerLevel level = player.level();
        net.minecraft.core.BlockPos base = player.blockPosition();
        int surface = base.getY() - 1;
        var stone = net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
        var water = net.minecraft.world.level.block.Blocks.WATER.defaultBlockState();
        var air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -16; dz <= -3; dz++) {
                boolean pond = Math.abs(dx) <= 4 && dz <= -6 && dz >= -14;
                for (int y = surface - 3; y <= surface + 3; y++) {
                    net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(base.getX() + dx, y, base.getZ() + dz);
                    var state = y > surface ? air : pond && y >= surface - 1 ? water : y == surface ? net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState() : stone;
                    level.setBlock(pos, state, 2);
                }
            }
        }
        com.toddlandis.skillcraft.fishing.FishingSpotEntity cod = null;
        Object[][] spots = {{com.toddlandis.skillcraft.node.NodeType.COD, 0, -8}, {com.toddlandis.skillcraft.node.NodeType.SALMON, -2, -11},
                {com.toddlandis.skillcraft.node.NodeType.TROPICAL_FISH, 2, -12}};
        for (Object[] spot : spots) {
            var entity = com.toddlandis.skillcraft.fishing.FishingSpotEntity.create(level, (com.toddlandis.skillcraft.node.NodeType) spot[0],
                    new net.minecraft.core.BlockPos(base.getX() + (int) spot[1], surface, base.getZ() + (int) spot[2]));
            level.addFreshEntity(entity);
            if (cod == null) cod = entity;
        }

        net.minecraft.core.BlockPos rangePos = base.offset(3, 0, -4);
        level.setBlock(rangePos, ModBlocks.COOKING_RANGE.get().defaultBlockState()
                .setValue(com.toddlandis.skillcraft.cooking.CookingRangeBlock.FACING, net.minecraft.core.Direction.WEST), 3);
        if (level.getBlockEntity(rangePos) instanceof com.toddlandis.skillcraft.cooking.CookingRangeBlockEntity range) {
            range.setItem(com.toddlandis.skillcraft.cooking.CookingRangeBlockEntity.SLOT_INPUT, new ItemStack(net.minecraft.world.item.Items.COD, 32));
            range.setItem(com.toddlandis.skillcraft.cooking.CookingRangeBlockEntity.SLOT_FUEL, new ItemStack(net.minecraft.world.item.Items.COAL, 8));
            range.setCook(player);
        }

        com.toddlandis.skillcraft.skill.SkillManager.setXp(player, com.toddlandis.skillcraft.skill.Skill.FISHING, com.toddlandis.skillcraft.skill.XpTable.xpForLevel(30));
        player.getInventory().setItem(0, new ItemStack(net.minecraft.world.item.Items.FISHING_ROD));
        player.getInventory().setItem(1, new ItemStack(ModItems.NODES.get(com.toddlandis.skillcraft.node.NodeType.COD).get()));
        player.getInventory().setItem(2, new ItemStack(ModItems.NODES.get(com.toddlandis.skillcraft.node.NodeType.PUFFERFISH).get()));
        player.getInventory().setItem(3, new ItemStack(ModItems.COOKING_RANGE.get()));
        player.getInventory().setItem(4, new ItemStack(ModItems.SEARED_REEF_FISH.get()));
        player.getInventory().setItem(5, new ItemStack(ModItems.PREPARED_PUFFERFISH.get()));
        player.getInventory().setItem(6, new ItemStack(ModItems.BURNT_FOOD.get()));
        player.getInventory().setSelectedSlot(0);
        return cod;
    }

    private static Mob dummy(ServerLevel level, Vec3 pos, ItemLike held) {
        Mob mob = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
        mob.snapTo(pos.x, pos.y, pos.z, 0f, 0f);
        mob.setYBodyRot(0f);
        mob.setYHeadRot(0f);
        mob.setNoAi(true);
        mob.setInvulnerable(true);
        mob.setPersistenceRequired();
        mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(held));
        level.addFreshEntity(mob);
        return mob;
    }
}
