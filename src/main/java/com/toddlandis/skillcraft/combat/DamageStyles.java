package com.toddlandis.skillcraft.combat;

import com.toddlandis.skillcraft.registry.ModDataMaps;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.ItemStack;

// Works out which damage type a hit is (or null for untyped damage like falls, fire and
// explosions), and looks up weapon styles and monster profiles from the data maps.
public final class DamageStyles {
    private DamageStyles() {}

    // Melee type of an item; unlisted items (and empty hands) hit as crush
    public static DamageStyle weapon(ItemStack stack) {
        WeaponStyle style = stack.isEmpty() ? null : stack.typeHolder().getData(ModDataMaps.WEAPON_STYLES);
        return style != null ? style.style() : DamageStyle.CRUSH;
    }

    public static CombatProfile profile(EntityType<?> type) {
        CombatProfile profile = BuiltInRegistries.ENTITY_TYPE.getResourceKey(type)
                .map(key -> BuiltInRegistries.ENTITY_TYPE.getData(ModDataMaps.COMBAT_PROFILES, key)).orElse(null);
        return profile != null ? profile : CombatProfile.NEUTRAL;
    }

    // How much damage of this type the entity takes (1.0 = normal)
    public static double modifier(Entity target, DamageStyle style) {
        double value = target instanceof StyleVulnerable vulnerable ? vulnerable.damageModifier(style) : profile(target.getType()).modifier(style);
        return Math.clamp(value, 0.25, 2.0);
    }

    // The damage type of this hit, or null if it is untyped
    public static DamageStyle of(DamageSource source) {
        Entity direct = source.getDirectEntity();
        if (direct instanceof SkillArrow arrow) return arrow.weight().style();
        if (direct instanceof ThrownTrident) return DamageStyle.HEAVY;
        if (direct instanceof AbstractArrow) return DamageStyle.MEDIUM;
        if (source.is(DamageTypeTags.IS_PROJECTILE)) return null; // fireballs, wind charges, potions...
        if (source.is(DamageTypes.PLAYER_ATTACK) && direct instanceof Player player) return weapon(player.getMainHandItem());
        if ((source.is(DamageTypes.MOB_ATTACK) || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO)) && direct instanceof LivingEntity mob) {
            return profile(mob.getType()).attackStyle();
        }
        return null;
    }
}
