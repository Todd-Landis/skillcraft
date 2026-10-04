package com.toddlandis.skillcraft.client;

import java.util.ArrayList;
import java.util.List;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.boss.SlagheartAction;
import com.toddlandis.skillcraft.boss.SlagheartEntity;
import com.toddlandis.skillcraft.dev.Showcase;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.entity.animation.json.AnimationLoader;
import net.neoforged.neoforge.client.event.ClientTickEvent;

// Client half of the dev showcase (see dev/Showcase): once in a world, builds the scene on the
// integrated server, plays each boss animation, saves screenshots to run/screenshots/showcase_*.png
// and quits. Logs "[showcase] done" at the end so CI can tell a full run from a crash.
@EventBusSubscriber(modid = SkillCraft.MODID, value = Dist.CLIENT)
public final class ShowcaseClient {
    private record Step(int tick, Runnable action) {}

    private static final List<Step> STEPS = new ArrayList<>();
    private static int ticks = -1;
    private static int next;
    private static SlagheartEntity boss;
    private static int shots;

    private ShowcaseClient() {}

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        if (!Showcase.ENABLED) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        IntegratedServer server = mc.getSingleplayerServer();
        if (mc.player == null && mc.gui.screen() != null) {
            confirmPrompts(mc.gui.screen());
            return;
        }
        if (mc.player == null || mc.level == null || server == null) {
            if (++waiting % 200 == 1) SkillCraft.LOGGER.info("[showcase] waiting: player {}, level {}, server {}, screen none", mc.player != null, mc.level != null, server != null);
            return;
        }
        if (ticks < 0) {
            if (mc.gui.screen() != null) return; // still loading
            plan(mc, server);
        }
        ticks++;
        while (next < STEPS.size() && STEPS.get(next).tick() <= ticks) {
            STEPS.get(next++).action().run();
        }
    }

    private static void plan(Minecraft mc, IntegratedServer server) {
        ticks = 0;
        if ("builds".equals(System.getProperty("skillcraft.showcase.scene"))) {
            planBuilds(mc, server);
            return;
        }
        if ("abilities".equals(System.getProperty("skillcraft.showcase.scene"))) {
            planAbilities(mc, server);
            return;
        }
        int t = 20;
        at(t, () -> server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
            Showcase.command(server, "time set noon");
            Showcase.command(server, "weather clear");
            boss = Showcase.buildScene(player);
        }));
        // Give chunks time to mesh (software rendering in CI is slow)
        t += 200;
        at(t, () -> shot(mc, "idle"));
        for (SlagheartAction action : SlagheartAction.values()) {
            if (action == SlagheartAction.NONE) continue;
            int length = animationTicks(action);
            at(t += 10, () -> server.execute(() -> boss.showcasePose(action)));
            int start = t;
            at(start + Math.max(2, length * 35 / 100), () -> shot(mc, action.animation() + "_a"));
            at(start + Math.max(4, length * 70 / 100), () -> shot(mc, action.animation() + "_b"));
            t = start + length + 5;
        }
        at(t += 10, () -> server.execute(() -> boss.showcasePose(SlagheartAction.NONE)));
        at(t += 20, () -> shot(mc, "hud"));
        // Night: the glow layer should stand out
        at(t += 10, () -> server.execute(() -> {
            Showcase.command(server, "time set midnight");
            boss.showcasePose(SlagheartAction.VENT);
        }));
        at(t += 30, () -> shot(mc, "night_vent"));
        at(t += 40, () -> {
            SkillCraft.LOGGER.info("[showcase] done: {} screenshots", shots);
            mc.stop();
        });
    }

    // -Pshowcase=builds: station blocks up close, then the arena (floor view, dome from below)
    private static void planBuilds(Minecraft mc, IntegratedServer server) {
        net.minecraft.core.BlockPos[] arena = new net.minecraft.core.BlockPos[1];
        int t = 20;
        at(t, () -> server.execute(() -> {
            Showcase.command(server, "time set noon");
            Showcase.command(server, "weather clear");
            Showcase.buildStations(server.getPlayerList().getPlayers().getFirst());
        }));
        at(t += 120, () -> shot(mc, "stations"));
        at(t += 10, () -> server.execute(() -> arena[0] = Showcase.buildArena(server.getPlayerList().getPlayers().getFirst())));
        // {name, dx, dz, yaw, pitch} relative to the arena center; yaw 180 = facing north
        Object[][] views = {
                {"arena_floor", 0, 13, 180f, -14f},     // from the gate: forge, columns, dome
                {"arena_forge", 0, -6, 180f, -8f},      // the Great Forge up close
                {"arena_column", 1, 1, -45f, -35f},     // a grand column
                {"arena_dome", 0, 4, 180f, -70f},       // looking up at the dome
                {"lobby_gate", 0, 34, 180f, 0f},        // lobby, looking toward the corridor
                {"lobby_back", 0, 26, 0f, 5f},          // lobby, looking at the exit portal and bank
        };
        for (Object[] view : views) {
            at(t += 10, () -> server.execute(() -> Showcase.view(server.getPlayerList().getPlayers().getFirst(),
                    net.minecraft.world.phys.Vec3.atBottomCenterOf(arena[0].offset((int) view[1], 0, (int) view[2])), (float) view[3], (float) view[4])));
            at(t += view == views[0] ? 200 : 60, () -> shot(mc, (String) view[0]));
        }
        at(t += 40, () -> {
            SkillCraft.LOGGER.info("[showcase] done: {} screenshots", shots);
            mc.stop();
        });
    }

    // -Pshowcase=abilities: the HUD bar (ready, then with a cooldown sweep) and the Ability Book,
    // at GUI scales 2, 3 and 4 so both the side-by-side and stacked layouts get checked
    private static void planAbilities(Minecraft mc, IntegratedServer server) {
        int t = 20;
        at(t, () -> server.execute(() -> {
            Showcase.command(server, "time set noon");
            Showcase.command(server, "weather clear");
            Showcase.abilityScene(server.getPlayerList().getPlayers().getFirst());
        }));
        at(t += 120, () -> shot(mc, "abilities_hud"));
        at(t += 5, () -> net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new com.toddlandis.skillcraft.network.UseAbilityPayload(3)));
        at(t += 5, () -> net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new com.toddlandis.skillcraft.network.UseAbilityPayload(0)));
        at(t += 10, () -> shot(mc, "abilities_hud_cooldowns"));
        int[] scales = {2, 3, 4};
        for (int scale : scales) {
            at(t += 10, () -> {
                mc.options.guiScale().set(scale);
                mc.resizeGui();
            });
            at(t += 10, () -> mc.gui.setScreen(new com.toddlandis.skillcraft.client.ability.AbilityBookScreen()));
            at(t += 20, () -> shot(mc, "ability_book_scale" + scale));
            at(t += 2, () -> {
                if (mc.gui.screen() instanceof com.toddlandis.skillcraft.client.ability.AbilityBookScreen book) book.showTab(com.toddlandis.skillcraft.ability.AbilityCategory.WEAPON_ART);
            });
            at(t += 10, () -> shot(mc, "ability_book_arts_scale" + scale));
            at(t += 2, () -> mc.gui.setScreen(null));
            at(t += 10, () -> shot(mc, "abilities_hud_scale" + scale));
        }
        // Phase 7: a staff, a spell bar, two blessings on (Faith bar and icons), a bolt in flight, the new tabs
        at(t += 10, () -> {
            mc.options.guiScale().set(2);
            mc.resizeGui();
            server.execute(() -> Showcase.magicScene(server.getPlayerList().getPlayers().getFirst()));
        });
        at(t += 30, () -> shot(mc, "magic_hud"));
        at(t += 2, () -> server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
            com.toddlandis.skillcraft.magic.MagicCasting.castBolt(player, player.getMainHandItem());
        }));
        at(t += 3, () -> shot(mc, "magic_bolt"));
        com.toddlandis.skillcraft.ability.AbilityCategory[] tabs = {com.toddlandis.skillcraft.ability.AbilityCategory.SPELL,
                com.toddlandis.skillcraft.ability.AbilityCategory.PRAYER, com.toddlandis.skillcraft.ability.AbilityCategory.AGILITY};
        at(t += 10, () -> mc.gui.setScreen(new com.toddlandis.skillcraft.client.ability.AbilityBookScreen()));
        for (com.toddlandis.skillcraft.ability.AbilityCategory tab : tabs) {
            at(t += 5, () -> {
                if (mc.gui.screen() instanceof com.toddlandis.skillcraft.client.ability.AbilityBookScreen book) book.showTab(tab);
            });
            at(t += 15, () -> shot(mc, "ability_book_" + tab.getSerializedName()));
        }
        at(t += 2, () -> mc.gui.setScreen(null));
        at(t += 10, () -> {
            mc.options.guiScale().set(0);
            mc.resizeGui();
        });
        at(t += 40, () -> {
            SkillCraft.LOGGER.info("[showcase] done: {} screenshots", shots);
            mc.stop();
        });
    }

    // Opening a world can stop on a "are you sure?" screen (backup or experimental-features prompt)
    // that nobody can click in CI: log what we are waiting on, and press its confirm button
    private static int waiting;

    private static void confirmPrompts(net.minecraft.client.gui.screens.Screen screen) {
        waiting++;
        if (waiting % 100 == 1) SkillCraft.LOGGER.info("[showcase] waiting on screen {} \"{}\"", screen.getClass().getName(), screen.getTitle().getString());
        if (waiting % 100 != 50) return;
        for (var child : screen.children()) {
            if (!(child instanceof net.minecraft.client.gui.components.Button button) || !button.active) continue;
            String label = button.getMessage().getString().toLowerCase(java.util.Locale.ROOT);
            if (label.contains("yes") || label.contains("proceed") || label.contains("continue") || label.contains("know what")) {
                SkillCraft.LOGGER.info("[showcase] pressing \"{}\" on {}", button.getMessage().getString(), screen.getClass().getSimpleName());
                button.onPress(new net.minecraft.client.input.MouseButtonEvent(button.getX() + button.getWidth() / 2., button.getY() + button.getHeight() / 2.,
                        new net.minecraft.client.input.MouseButtonInfo(org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT, 0)));
                return;
            }
        }
    }

    private static void at(int tick, Runnable action) {
        STEPS.add(new Step(tick, action));
    }

    private static int animationTicks(SlagheartAction action) {
        AnimationDefinition definition = AnimationLoader.INSTANCE.getAnimation(Identifier.fromNamespaceAndPath(SkillCraft.MODID, "slagheart/" + action.animation()));
        return definition == null ? 40 : Math.max(10, Math.round(definition.lengthInSeconds() * 20));
    }

    private static void shot(Minecraft mc, String name) {
        shots++;
        Screenshot.grab(mc.gameDirectory, "showcase_" + name + ".png", mc.gameRenderer.mainRenderTarget(), 1,
                message -> SkillCraft.LOGGER.info("[showcase] {}: {}", name, message.getString()));
    }
}
