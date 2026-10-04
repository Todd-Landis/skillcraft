package com.toddlandis.skillcraft.registry;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.combat.SkillArrow;
import com.toddlandis.skillcraft.combat.SkillArrowItem;
import com.toddlandis.skillcraft.node.NodeType;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SkillCraft.MODID);

    public static final Identifier KNOCKBACK_ID = Identifier.fromNamespaceAndPath(SkillCraft.MODID, "weapon_knockback");
    public static final Identifier WEIGHT_ID = Identifier.fromNamespaceAndPath(SkillCraft.MODID, "weapon_weight");

    // One item per node: a block item for woodcutting/mining nodes, a shoal for fishing spots
    public static final Map<NodeType, DeferredItem<? extends Item>> NODES = new EnumMap<>(NodeType.class);

    static {
        for (NodeType type : NodeType.values()) {
            if (type.isSpot()) {
                NODES.put(type, ITEMS.registerItem(type.blockName(), p -> new com.toddlandis.skillcraft.fishing.ShoalItem(type, p), p -> p.stacksTo(16)));
            } else {
                NODES.put(type, ITEMS.registerSimpleBlockItem(type.blockName(), ModBlocks.NODES.get(type), p -> p.stacksTo(16)));
            }
        }
    }

    public static final DeferredItem<BlockItem> ATTUNEMENT_ALTAR = ITEMS.registerSimpleBlockItem("attunement_altar", ModBlocks.ATTUNEMENT_ALTAR);

    public static final DeferredItem<BlockItem> SMELTER = ITEMS.registerSimpleBlockItem("smelter", ModBlocks.SMELTER);

    // --- Cooking ---
    public static final DeferredItem<BlockItem> COOKING_RANGE = ITEMS.registerSimpleBlockItem("cooking_range", ModBlocks.COOKING_RANGE);
    // What a failed cook leaves behind: not edible
    public static final DeferredItem<Item> BURNT_FOOD = ITEMS.registerSimpleItem("burnt_food");
    // Cooked fish that vanilla can't cook (Fishing tiers III and IV)
    public static final DeferredItem<Item> SEARED_REEF_FISH = ITEMS.registerSimpleItem("seared_reef_fish",
            p -> p.food(new FoodProperties.Builder().nutrition(7).saturationModifier(0.7f).build()));
    public static final DeferredItem<Item> PREPARED_PUFFERFISH = ITEMS.registerSimpleItem("prepared_pufferfish",
            p -> p.food(new FoodProperties.Builder().nutrition(8).saturationModifier(0.9f).build()));

    public static final DeferredItem<BlockItem> FORGING_ANVIL =ITEMS.registerSimpleBlockItem("forging_anvil", ModBlocks.FORGING_ANVIL);
    public static final DeferredItem<BlockItem> CRAFTING_BENCH = ITEMS.registerSimpleBlockItem("crafting_bench", ModBlocks.CRAFTING_BENCH);

    public static final DeferredItem<BlockItem> BANK_CHEST = ITEMS.registerSimpleBlockItem("bank_chest", ModBlocks.BANK_CHEST);

    // Right-click a node with this to pick it up
    public static final DeferredItem<Item> RELOCATOR = ITEMS.registerSimpleItem("relocator", p -> p.stacksTo(1));

    // Found in the vanilla world (trees, ores, fishing); the Attunement Altar turns one into a node.
    // Which node is in its skillcraft:heart_node component (see node/ElementalHeartItem).
    public static final DeferredItem<com.toddlandis.skillcraft.node.ElementalHeartItem> ELEMENTAL_HEART = ITEMS.registerItem("elemental_heart",
            com.toddlandis.skillcraft.node.ElementalHeartItem::new, p -> p.stacksTo(16).rarity(Rarity.UNCOMMON));

    // --- Combat (docs/COMBAT.md) ---

    // Warhammers: the crush weapon family. Slower and heavier than swords, with extra knockback.
    // Damage = 1 (base) + material bonus + 5, speed 1.0/s
    public static final Map<String, DeferredItem<Item>> WARHAMMERS = new LinkedHashMap<>();

    static {
        warhammer("copper", ToolMaterial.COPPER, false);
        warhammer("iron", ToolMaterial.IRON, false);
        warhammer("golden", ToolMaterial.GOLD, false);
        warhammer("diamond", ToolMaterial.DIAMOND, false);
        warhammer("netherite", ToolMaterial.NETHERITE, true);
    }

    private static void warhammer(String metal, ToolMaterial material, boolean fireResistant) {
        WARHAMMERS.put(metal, ITEMS.registerItem(metal + "_warhammer", Item::new, p -> {
            p = p.sword(material, 5.0f, -3.0f).attributes(weaponAttributes(5.0f + material.attackDamageBonus(), -3.0f, 0.4, 0));
            return fireResistant ? p.fireResistant() : p;
        }));
    }

    public static final DeferredItem<SkillArrowItem> LIGHT_ARROW = ITEMS.registerItem("light_arrow", p -> new SkillArrowItem(SkillArrow.Weight.LIGHT, p));
    public static final DeferredItem<SkillArrowItem> HEAVY_ARROW = ITEMS.registerItem("heavy_arrow", p -> new SkillArrowItem(SkillArrow.Weight.HEAVY, p));

    // Padded armour: light quilted armour that soaks up crush and ranged hits
    public static final ResourceKey<EquipmentAsset> PADDED_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(SkillCraft.MODID, "padded"));
    public static final ArmorMaterial PADDED = new ArmorMaterial(12,
            Map.of(ArmorType.BOOTS, 2, ArmorType.LEGGINGS, 4, ArmorType.CHESTPLATE, 5, ArmorType.HELMET, 2, ArmorType.BODY, 5),
            15, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0f, 0.0f, ItemTags.WOOL, PADDED_ASSET);
    public static final DeferredItem<Item> PADDED_HELMET = ITEMS.registerItem("padded_helmet", Item::new, p -> p.humanoidArmor(PADDED, ArmorType.HELMET));
    public static final DeferredItem<Item> PADDED_CHESTPLATE = ITEMS.registerItem("padded_chestplate", Item::new, p -> p.humanoidArmor(PADDED, ArmorType.CHESTPLATE));
    public static final DeferredItem<Item> PADDED_LEGGINGS = ITEMS.registerItem("padded_leggings", Item::new, p -> p.humanoidArmor(PADDED, ArmorType.LEGGINGS));
    public static final DeferredItem<Item> PADDED_BOOTS = ITEMS.registerItem("padded_boots", Item::new, p -> p.humanoidArmor(PADDED, ArmorType.BOOTS));

    // --- Boss (docs/BOSS_SLAGHEART.md) ---

    public static final DeferredItem<BlockItem> SUMMONING_BRAZIER = ITEMS.registerSimpleBlockItem("summoning_brazier", ModBlocks.SUMMONING_BRAZIER);

    // Consumed at a Summoning Brazier to open the way to Slagheart's arena
    public static final DeferredItem<Item> EMBER_SIGIL = ITEMS.registerSimpleItem("ember_sigil", p -> p.stacksTo(16).rarity(Rarity.UNCOMMON));

    // Teaches one ability for good; which one is in its skillcraft:codex_ability component
    public static final DeferredItem<com.toddlandis.skillcraft.ability.CodexItem> CODEX = ITEMS.registerItem("codex", com.toddlandis.skillcraft.ability.CodexItem::new,
            p -> p.stacksTo(1).rarity(Rarity.RARE));

    // What Slag Spit projectiles look like (not obtainable)
    public static final DeferredItem<Item> SLAG_GLOB = ITEMS.registerSimpleItem("slag_glob");

    // Slagheart's unique drop: 12 damage two-handed crush maul, 0.7 attacks/s, -10% move speed while held.
    // Fully charged hits apply Sundered (see combat/MaulEvents).
    public static final DeferredItem<Item> EMBERFALL_MAUL = ITEMS.registerItem("emberfall_maul", Item::new, p -> p
            .sword(ToolMaterial.NETHERITE, 7.0f, -3.3f)
            .attributes(weaponAttributes(7.0f + ToolMaterial.NETHERITE.attackDamageBonus(), -3.3f, 0.6, -0.10))
            .durability(1800)
            .rarity(Rarity.EPIC)
            .fireResistant());

    // --- Magic (docs/AGILITY_MAGIC_PRAYER.md) ---

    // Motes: spell reagents, one per element (woven at a Ley Altar, see docs/LEYCRAFT.md)
    public static final DeferredItem<Item> ARCANE_MOTE = ITEMS.registerSimpleItem("arcane_mote");
    public static final DeferredItem<Item> EMBER_MOTE = ITEMS.registerSimpleItem("ember_mote");
    public static final DeferredItem<Item> FROST_MOTE = ITEMS.registerSimpleItem("frost_mote");
    public static final DeferredItem<Item> STORM_MOTE = ITEMS.registerSimpleItem("storm_mote");

    // Wands and staves; their power, element and cast speed are in data_maps/item/magic_focus.json
    public static final DeferredItem<com.toddlandis.skillcraft.magic.FocusItem> APPRENTICE_WAND = focus("apprentice_wand", Rarity.COMMON);
    public static final DeferredItem<com.toddlandis.skillcraft.magic.FocusItem> EMBER_STAFF = focus("ember_staff", Rarity.COMMON);
    public static final DeferredItem<com.toddlandis.skillcraft.magic.FocusItem> RIME_STAFF = focus("rime_staff", Rarity.COMMON);
    public static final DeferredItem<com.toddlandis.skillcraft.magic.FocusItem> TEMPEST_STAFF = focus("tempest_staff", Rarity.COMMON);
    public static final DeferredItem<com.toddlandis.skillcraft.magic.FocusItem> ARCANIST_WAND = focus("arcanist_wand", Rarity.UNCOMMON);
    public static final DeferredItem<com.toddlandis.skillcraft.magic.FocusItem> STARSEER_STAFF = focus("starseer_staff", Rarity.RARE);

    private static DeferredItem<com.toddlandis.skillcraft.magic.FocusItem> focus(String name, Rarity rarity) {
        return ITEMS.registerItem(name, com.toddlandis.skillcraft.magic.FocusItem::new, p -> p.stacksTo(1).rarity(rarity));
    }

    // What a spell bolt looks like is its element's mote (see magic/SpellBolt)

    // --- Prayer ---

    public static final DeferredItem<BlockItem> VOTIVE_SHRINE = ITEMS.registerSimpleBlockItem("votive_shrine", ModBlocks.VOTIVE_SHRINE);

    // --- Leycraft (docs/LEYCRAFT.md) ---

    // Essence mined from a Ley Stone node; woven into motes at a Ley Altar
    public static final DeferredItem<Item> LEY_SHARD = ITEMS.registerSimpleItem("ley_shard");
    public static final DeferredItem<BlockItem> LEY_ALTAR = ITEMS.registerSimpleBlockItem("ley_altar", ModBlocks.LEY_ALTAR);

    // Main-hand attributes for heavy weapons: damage, speed, extra knockback, and a movement penalty
    private static ItemAttributeModifiers weaponAttributes(float damage, float speed, double knockback, double moveSpeed) {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, damage, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, speed, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        if (knockback != 0) builder.add(Attributes.ATTACK_KNOCKBACK, new AttributeModifier(KNOCKBACK_ID, knockback, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        if (moveSpeed != 0) builder.add(Attributes.MOVEMENT_SPEED, new AttributeModifier(WEIGHT_ID, moveSpeed, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.MAINHAND);
        return builder.build();
    }
}
