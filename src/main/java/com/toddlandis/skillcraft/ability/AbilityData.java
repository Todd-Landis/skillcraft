package com.toddlandis.skillcraft.ability;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

// A player's ability setup (attachment "abilities"): learned Codex abilities, 3 saved bars of 4
// slots, which bar is active, and Momentum. Saved and kept through death, except Momentum and the
// in-combat timer, which are synced to the owner but never saved (they reset on logout).
public final class AbilityData {
    public static final int SLOTS = 4;
    public static final int LOADOUTS = 3;
    public static final int NAME_MAX = 16;
    public static final float MAX_MOMENTUM = 100f;

    private static final Codec<List<String>> BAR_CODEC = Codec.STRING.listOf();

    // Saved as {"learned": [...], "loadouts": [["skillcraft:lunge", "", ...], ...], "names": [...], "active": 0}
    public static final MapCodec<AbilityData> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Identifier.CODEC.listOf().optionalFieldOf("learned", List.of()).forGetter(d -> List.copyOf(d.learned)),
            BAR_CODEC.listOf().optionalFieldOf("loadouts", List.of()).forGetter(AbilityData::barsAsStrings),
            Codec.STRING.listOf().optionalFieldOf("names", List.of()).forGetter(d -> List.of(d.names)),
            Codec.INT.optionalFieldOf("active", 0).forGetter(d -> d.active)
    ).apply(i, AbilityData::fromSaved));

    public static final StreamCodec<ByteBuf, AbilityData> STREAM_CODEC = StreamCodec.of(
            (buf, data) -> {
                ByteBufCodecs.VAR_INT.encode(buf, data.learned.size());
                for (Identifier id : data.learned) Identifier.STREAM_CODEC.encode(buf, id);
                for (int l = 0; l < LOADOUTS; l++) {
                    ByteBufCodecs.STRING_UTF8.encode(buf, data.names[l]);
                    for (int s = 0; s < SLOTS; s++) ByteBufCodecs.STRING_UTF8.encode(buf, data.bars[l][s] == null ? "" : data.bars[l][s].toString());
                }
                ByteBufCodecs.VAR_INT.encode(buf, data.active);
                buf.writeFloat(data.momentum);
                buf.writeLong(data.combatUntil);
            },
            buf -> {
                AbilityData data = new AbilityData();
                int learned = ByteBufCodecs.VAR_INT.decode(buf);
                for (int i = 0; i < learned; i++) data.learned.add(Identifier.STREAM_CODEC.decode(buf));
                for (int l = 0; l < LOADOUTS; l++) {
                    data.names[l] = ByteBufCodecs.STRING_UTF8.decode(buf);
                    for (int s = 0; s < SLOTS; s++) data.bars[l][s] = parse(ByteBufCodecs.STRING_UTF8.decode(buf));
                }
                data.active = ByteBufCodecs.VAR_INT.decode(buf);
                data.momentum = buf.readFloat();
                data.combatUntil = buf.readLong();
                return data;
            });

    private final Set<Identifier> learned = new HashSet<>();
    private final Identifier[][] bars = new Identifier[LOADOUTS][SLOTS];
    private final String[] names = {"", "", ""};
    private int active;
    private float momentum;
    private long combatUntil;

    public AbilityData() {}

    private static AbilityData fromSaved(List<Identifier> learned, List<List<String>> bars, List<String> names, int active) {
        AbilityData data = new AbilityData();
        data.learned.addAll(learned);
        for (int l = 0; l < Math.min(LOADOUTS, bars.size()); l++) {
            List<String> bar = bars.get(l);
            for (int s = 0; s < Math.min(SLOTS, bar.size()); s++) data.bars[l][s] = parse(bar.get(s));
        }
        for (int l = 0; l < Math.min(LOADOUTS, names.size()); l++) data.names[l] = clampName(names.get(l));
        data.active = Math.clamp(active, 0, LOADOUTS - 1);
        return data;
    }

    private List<List<String>> barsAsStrings() {
        List<List<String>> result = new ArrayList<>();
        for (Identifier[] bar : bars) {
            List<String> slots = new ArrayList<>();
            for (Identifier id : bar) slots.add(id == null ? "" : id.toString());
            result.add(slots);
        }
        return result;
    }

    private static Identifier parse(String text) {
        return text == null || text.isEmpty() ? null : Identifier.tryParse(text);
    }

    public static String clampName(String name) {
        String trimmed = name == null ? "" : name.strip();
        return trimmed.length() > NAME_MAX ? trimmed.substring(0, NAME_MAX) : trimmed;
    }

    public boolean hasLearned(Identifier id) {
        return learned.contains(id);
    }

    public boolean learn(Identifier id) {
        return learned.add(id);
    }

    public Set<Identifier> learned() {
        return Set.copyOf(learned);
    }

    public int activeLoadout() {
        return active;
    }

    public void setActiveLoadout(int loadout) {
        active = Math.clamp(loadout, 0, LOADOUTS - 1);
    }

    public Identifier slot(int loadout, int slot) {
        return bars[loadout][slot];
    }

    // The ability in a slot of the active bar, or null
    public Identifier activeSlot(int slot) {
        return slot < 0 || slot >= SLOTS ? null : bars[active][slot];
    }

    public Identifier[] bar(int loadout) {
        return bars[loadout].clone();
    }

    public void setBar(int loadout, Identifier[] slots) {
        for (int s = 0; s < SLOTS; s++) bars[loadout][s] = s < slots.length ? slots[s] : null;
    }

    public String name(int loadout) {
        return names[loadout];
    }

    public void setName(int loadout, String name) {
        names[loadout] = clampName(name);
    }

    public float momentum() {
        return momentum;
    }

    public void setMomentum(float value) {
        momentum = Math.clamp(value, 0f, MAX_MOMENTUM);
    }

    public long combatUntil() {
        return combatUntil;
    }

    public void setCombatUntil(long tick) {
        combatUntil = tick;
    }

    public boolean inCombat(long now) {
        return now < combatUntil;
    }
}
