package com.toddlandis.skillcraft.fishing;

import com.toddlandis.skillcraft.registry.ModEntities;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// The bobber and line shown while fishing at a spot. It reuses the vanilla fishing hook so the
// player's rod shows as cast and the line is drawn to their hand, but none of the vanilla
// fishing logic runs: it just floats on the spot, bobs, and dips when FishingSessions lands a
// catch. Reeling in (using the rod) ends the fishing session. Never saved with the world.
public class FishingLine extends FishingHook {
    private static final double BOB_HEIGHT = 0.025;
    private static final double DIP_DEPTH = 0.18;

    private Vec3 anchor = Vec3.ZERO;
    private int dipTicks;

    public FishingLine(EntityType<? extends FishingLine> type, Level level) {
        super(type, level);
    }

    public static FishingLine cast(ServerPlayer player, FishingSpotEntity spot) {
        ServerLevel level = player.level();
        FishingLine line = new FishingLine(ModEntities.FISHING_LINE.get(), level);
        // Land the bobber a little way into the spot from the player's side
        Vec3 toPlayer = new Vec3(player.getX() - spot.getX(), 0, player.getZ() - spot.getZ());
        Vec3 offset = toPlayer.lengthSqr() < 1.0E-4 ? Vec3.ZERO : toPlayer.normalize().scale(0.3);
        line.anchor = new Vec3(spot.getX() + offset.x, spot.waterPos().getY() + FishingSpotEntity.SURFACE_Y - 0.05, spot.getZ() + offset.z);
        line.setPos(line.anchor);
        line.setOwner(player);
        level.addFreshEntity(line);
        return line;
    }

    // A fish takes the bait: the bobber is pulled under for a moment
    public void dip() {
        dipTicks = 6;
    }

    @Override
    public void tick() {
        // None of the vanilla hook's physics or bite logic
        baseTick();
        if (level().isClientSide()) {
            getInterpolation().interpolate();
            return;
        }
        Player owner = getPlayerOwner();
        if (!(owner instanceof ServerPlayer player) || !FishingSessions.isLine(player, this)) {
            discard();
            return;
        }
        double bob = Math.sin(level().getGameTime() * 0.25) * BOB_HEIGHT;
        if (dipTicks > 0) {
            dipTicks--;
            bob -= DIP_DEPTH;
        }
        setPos(anchor.x, anchor.y + bob, anchor.z);
    }

    // Using the rod while the line is out reels it in, which ends the session
    @Override
    public int retrieve(ItemStack rod) {
        if (!level().isClientSide() && getPlayerOwner() instanceof ServerPlayer player) {
            FishingSessions.stop(player, "message.skillcraft.fishing_stop");
        }
        discard();
        return 0;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
