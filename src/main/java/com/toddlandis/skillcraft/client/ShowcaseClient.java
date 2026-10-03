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
        if (mc.player == null || mc.level == null || server == null) return;
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
