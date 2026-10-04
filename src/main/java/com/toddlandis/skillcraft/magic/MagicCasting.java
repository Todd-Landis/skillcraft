package com.toddlandis.skillcraft.magic;

import com.toddlandis.skillcraft.ability.AbilityService;
import com.toddlandis.skillcraft.ability.AbilityEvents;
import com.toddlandis.skillcraft.combat.DamageStyle;
import com.toddlandis.skillcraft.skill.Skill;
import com.toddlandis.skillcraft.skill.SkillManager;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

// Casting from a wand or staff (docs/AGILITY_MAGIC_PRAYER.md section 2.3). The server checks the
// Magic level gate and the mote cost, then fires a bolt; the focus's cooldown shows on the hotbar.
public final class MagicCasting {
    public static final float BOLT_SPEED = 2.2f;

    private MagicCasting() {}

    // Right-click with a focus in the main hand. Returns true if a bolt was fired.
    public static boolean castBolt(ServerPlayer player, ItemStack stack) {
        MagicFocus focus = MagicFocus.of(stack);
        if (focus == null) return false;
        if (!canWield(player, stack)) return false;
        DamageStyle element = focus.elementOrArcane();
        Item mote = Motes.of(element);
        boolean free = player.isCreative() || MagicFocus.supplies(player, mote);
        if (!free && AbilityService.countItem(player, mote) < 1) {
            player.sendOverlayMessage(Component.translatable("message.skillcraft.no_motes", new ItemStack(mote).getHoverName()).withStyle(ChatFormatting.RED));
            return false;
        }
        if (!free) AbilityService.removeItem(player, mote, 1);
        fire(player, SpellBolt.Payload.plain(element, focus.power()), BOLT_SPEED, false);
        player.getCooldowns().addCooldown(stack, focus.castCooldown());
        return true;
    }

    // A focus above your Magic level won't cast (the same gate as wielding a weapon above your Melee level)
    public static boolean canWield(ServerPlayer player, ItemStack stack) {
        int needed = SkillManager.missingLevel(player, stack, Skill.MAGIC);
        if (needed <= 0) return true;
        player.sendOverlayMessage(Component.translatable("message.skillcraft.focus_level", needed).withStyle(ChatFormatting.RED));
        return false;
    }

    // Launches a bolt from the eyes along the look direction
    public static SpellBolt fire(ServerPlayer player, SpellBolt.Payload payload, float speed, boolean ability) {
        SpellBolt bolt = new SpellBolt(player.level(), player, payload);
        bolt.setPos(player.getX(), player.getEyeY() - 0.15, player.getZ());
        bolt.shootFromRotation(player, player.getXRot(), player.getYRot(), 0f, speed, 0.25f);
        if (ability) AbilityEvents.markAbilityProjectile(bolt);
        player.level().addFreshEntity(bolt);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound(payload.element()), SoundSource.PLAYERS, 0.6f, 1.3f);
        return bolt;
    }

    private static net.minecraft.sounds.SoundEvent sound(DamageStyle element) {
        return switch (element) {
            case FIRE -> SoundEvents.BLAZE_SHOOT;
            case FROST -> SoundEvents.POWDER_SNOW_STEP;
            case STORM -> SoundEvents.BREEZE_SHOOT;
            default -> SoundEvents.EVOKER_CAST_SPELL;
        };
    }
}
