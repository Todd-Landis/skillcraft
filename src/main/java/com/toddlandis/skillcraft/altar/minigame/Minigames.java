package com.toddlandis.skillcraft.altar.minigame;

import java.util.List;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

// Creates minigames and replays them for server-side verification.
public final class Minigames {
    // Hard limits so a client can't make the server simulate forever
    public static final int MAX_TICKS = 20 * 180;
    public static final int MAX_INPUTS = 600;

    private Minigames() {}

    // tier is 0 (Tier I) to 6 (Tier VII); higher is harder. Same seed = same game.
    public static MinigameLogic create(MinigameKind kind, int tier, long seed) {
        return switch (kind) {
            case STRIKE_THE_GRAIN -> new StrikeTheGrainLogic(tier, seed);
            case TEMPER_THE_VEIN -> new TemperTheVeinLogic(tier, seed);
        };
    }

    // One button press or release, at the number of ticks the minigame had run when it happened
    public record Input(int tick, boolean press) {
        public static final StreamCodec<ByteBuf, Input> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Input::tick,
                ByteBufCodecs.BOOL, Input::press,
                Input::new);
        public static final StreamCodec<ByteBuf, List<Input>> LIST_STREAM_CODEC = STREAM_CODEC.apply(ByteBufCodecs.list(MAX_INPUTS));
    }

    public record ReplayResult(MinigameLogic.State state, int ticks, boolean started) {}

    // Re-runs a minigame from its seed with the player's recorded inputs. Inputs are applied
    // before the tick with the same number, exactly as the client did.
    public static ReplayResult replay(MinigameKind kind, int tier, long seed, List<Input> inputs) {
        MinigameLogic logic = create(kind, tier, seed);
        int next = 0;
        for (int tick = 0; tick <= MAX_TICKS; tick++) {
            while (next < inputs.size() && inputs.get(next).tick() == tick) {
                Input input = inputs.get(next++);
                if (input.press()) logic.press(); else logic.release();
                if (logic.state() != MinigameLogic.State.RUNNING) return new ReplayResult(logic.state(), tick, true);
            }
            if (next < inputs.size() && inputs.get(next).tick() < tick) {
                return new ReplayResult(MinigameLogic.State.FAILURE, tick, true); // out of order: tampered
            }
            logic.tick();
            if (logic.state() != MinigameLogic.State.RUNNING) return new ReplayResult(logic.state(), tick + 1, logic.started());
            if (next >= inputs.size() && !logic.started()) break; // nothing more will happen
        }
        return new ReplayResult(logic.state(), MAX_TICKS, logic.started());
    }
}
