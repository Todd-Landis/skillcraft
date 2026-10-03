package com.toddlandis.skillcraft.ability;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.combat.DamageStyle;
import com.toddlandis.skillcraft.combat.DamageStyles;
import com.toddlandis.skillcraft.skill.Skill;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

// One ability, loaded from data/<namespace>/skillcraft/ability/<name>.json (see AbilityManager and
// docs/ABILITIES_SPEC.md). Everything numeric lives here or in the behaviour, so /reload retunes it.
//   cooldown and windup are in ticks (20 = 1 s)
//   item_costs: items consumed on use (for future Spells: runes)
//   toggle: a sustained ability that drains Momentum per second while on (for future Prayers)
public record AbilityDefinition(
        AbilityCategory category,
        AbilityClass abilityClass,
        Requirement requires,
        int cooldown,
        Momentum momentum,
        boolean offGcd,
        int windup,
        List<ItemCost> itemCosts,
        Optional<Toggle> toggle,
        AbilityBehavior behavior,
        Identifier icon,
        Optional<SoundEvent> sound) {

    public static final Codec<AbilityDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            AbilityCategory.CODEC.fieldOf("category").forGetter(AbilityDefinition::category),
            AbilityClass.CODEC.optionalFieldOf("class", AbilityClass.BASIC).forGetter(AbilityDefinition::abilityClass),
            Requirement.CODEC.fieldOf("requires").forGetter(AbilityDefinition::requires),
            Codec.intRange(0, 72000).fieldOf("cooldown").forGetter(AbilityDefinition::cooldown),
            Momentum.CODEC.optionalFieldOf("momentum", Momentum.DEFAULT).forGetter(AbilityDefinition::momentum),
            Codec.BOOL.optionalFieldOf("off_gcd", false).forGetter(AbilityDefinition::offGcd),
            Codec.intRange(0, 200).optionalFieldOf("windup", 0).forGetter(AbilityDefinition::windup),
            ItemCost.CODEC.listOf().optionalFieldOf("item_costs", List.of()).forGetter(AbilityDefinition::itemCosts),
            Toggle.CODEC.optionalFieldOf("toggle").forGetter(AbilityDefinition::toggle),
            AbilityBehaviors.CODEC.fieldOf("behavior").forGetter(AbilityDefinition::behavior),
            Identifier.CODEC.fieldOf("icon").forGetter(AbilityDefinition::icon),
            BuiltInRegistries.SOUND_EVENT.byNameCodec().optionalFieldOf("sound").forGetter(AbilityDefinition::sound)
    ).apply(i, AbilityDefinition::new));

    // Momentum needed before use, after the combat-level discount (see Momentum)
    public float momentumRequired(int skillLevel) {
        return momentum.requires().orElse(abilityClass.defaultRequires()) * Momentum.discount(skillLevel);
    }

    // Momentum spent on use. Ultimates spend the whole bar.
    public float momentumCost(int skillLevel) {
        return momentum.cost().orElse(abilityClass.defaultCost()) * Momentum.discount(skillLevel);
    }

    public boolean isToggle() {
        return toggle.isPresent();
    }

    public static Component name(Identifier id) {
        return Component.translatable(nameKey(id));
    }

    public static String nameKey(Identifier id) {
        return "ability." + id.getNamespace() + "." + id.getPath();
    }

    public static Component description(Identifier id) {
        return Component.translatable(nameKey(id) + ".desc");
    }

    // What unlocks it: a level in a skill, optionally a weapon class (item tag) and/or damage type held,
    // an offhand item (e.g. a shield), and optionally a Codex read
    public record Requirement(Skill skill, int level, Optional<TagKey<Item>> weapon, Optional<DamageStyle> weaponStyle, Optional<TagKey<Item>> offhand, boolean codex) {
        public static final Codec<Requirement> CODEC = RecordCodecBuilder.create(i -> i.group(
                Skill.CODEC.fieldOf("skill").forGetter(Requirement::skill),
                Codec.intRange(1, 99).fieldOf("level").forGetter(Requirement::level),
                TagKey.codec(Registries.ITEM).optionalFieldOf("weapon").forGetter(Requirement::weapon),
                DamageStyle.CODEC.optionalFieldOf("weapon_style").forGetter(Requirement::weaponStyle),
                TagKey.codec(Registries.ITEM).optionalFieldOf("offhand").forGetter(Requirement::offhand),
                Codec.BOOL.optionalFieldOf("codex", false).forGetter(Requirement::codex)
        ).apply(i, Requirement::new));

        public boolean weaponMatches(ItemStack mainHand, ItemStack offHand) {
            return weapon.map(mainHand::is).orElse(true) && weaponStyle.map(style -> DamageStyles.weapon(mainHand) == style).orElse(true)
                    && offhand.map(offHand::is).orElse(true);
        }
    }

    // "generate" is Momentum gained on use; "requires"/"cost" override the class defaults
    public record Momentum(int generate, Optional<Integer> requires, Optional<Integer> cost) {
        public static final Momentum DEFAULT = new Momentum(0, Optional.empty(), Optional.empty());
        public static final Codec<Momentum> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(0, 100).optionalFieldOf("generate", 0).forGetter(Momentum::generate),
                Codec.intRange(0, 100).optionalFieldOf("requires").forGetter(Momentum::requires),
                Codec.intRange(0, 100).optionalFieldOf("cost").forGetter(Momentum::cost)
        ).apply(i, Momentum::new));

        // -1% Momentum cost per 10 levels in the ability's skill (spec 2.2)
        public static float discount(int skillLevel) {
            return 1f - 0.01f * (skillLevel / 10);
        }
    }

    // An item consumed on each use, e.g. runes for a future spell
    public record ItemCost(Item item, int count) {
        public static final Codec<ItemCost> CODEC = RecordCodecBuilder.create(i -> i.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(ItemCost::item),
                Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(ItemCost::count)
        ).apply(i, ItemCost::new));
    }

    // A sustained ability: pressing it turns it on, pressing again (or running dry) turns it off.
    // drain_per_second comes out of Momentum today; Prayer will bring its own resource.
    public record Toggle(float drainPerSecond) {
        public static final Codec<Toggle> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.floatRange(0, 100).fieldOf("drain_per_second").forGetter(Toggle::drainPerSecond)
        ).apply(i, Toggle::new));
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(SkillCraft.MODID, path);
    }
}
