package com.toddlandis.skillcraft.bonding;

import com.toddlandis.skillcraft.Config;
import com.toddlandis.skillcraft.registry.ModEntities;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// The Cloudray Lure (docs/BEASTBONDING.md section 4): used high up under open sky, it calls a wild
// Cloudray, which glides in nearby. Bond it with the Bond Codex before it leaves.
public class CloudrayLureItem extends Item {
    public CloudrayLureItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        BlockPos pos = player.blockPosition();
        if (pos.getY() < Config.CLOUDRAY_LURE_Y.get() || !server.canSeeSky(pos.above())) {
            player.sendOverlayMessage(Component.translatable("message.skillcraft.lure_too_low", Config.CLOUDRAY_LURE_Y.get()).withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        Cloudray ray = ModEntities.CLOUDRAY.get().create(server, EntitySpawnReason.MOB_SUMMONED);
        if (ray == null) return InteractionResult.FAIL;
        double angle = server.getRandom().nextDouble() * Math.PI * 2;
        ray.snapTo(player.getX() + Math.cos(angle) * 14, player.getY() + 6, player.getZ() + Math.sin(angle) * 14, (float) Math.toDegrees(angle), 0f);
        ray.getPersistentData().putLong(Cloudray.LURED_TAG, server.getGameTime());
        server.addFreshEntity(ray);
        server.playSound(null, pos, SoundEvents.PHANTOM_FLAP, SoundSource.NEUTRAL, 1.2f, 0.6f);
        player.sendOverlayMessage(Component.translatable("message.skillcraft.lure_called").withStyle(ChatFormatting.AQUA));
        ItemStack stack = player.getItemInHand(hand);
        player.getCooldowns().addCooldown(stack, 100);
        if (!player.isCreative()) stack.shrink(1);
        return InteractionResult.SUCCESS;
    }
}
