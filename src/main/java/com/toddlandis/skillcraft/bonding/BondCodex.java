package com.toddlandis.skillcraft.bonding;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;

// A player's Bond Codex (docs/BEASTBONDING.md): every creature they've bonded, and which species
// pages they've filled. It lives on the player (attachment "bond_codex"), not on the book item, so
// creatures can't be duplicated by copying items. Changed only on the server (see BondService); the
// owner's client gets a light copy without the creatures' saved data, for the screen.
public class BondCodex {
    public enum State implements StringRepresentable {
        STORED,     // in the codex, ready to summon
        OUT,        // summoned: alive in the world as `entity`
        FALLEN,     // died; rekindle it at a Rekindling Shrine
        REKINDLING; // coming back at `readyAt` (game time)

        public static final Codec<State> CODEC = StringRepresentable.fromEnum(State::values);
        @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    }

    public static final class Bond {
        public final UUID id;
        public final Identifier species;    // the bondable.json key, e.g. minecraft:spider
        public String name;                 // custom name, or "" for the species name
        public CompoundTag data;            // the creature's saved state (server only)
        public State state;
        public long readyAt;                // REKINDLING: game time it's back
        public UUID entity;                 // OUT: the live entity
        public float health;
        public float maxHealth;

        public Bond(UUID id, Identifier species, String name, CompoundTag data, State state, long readyAt, Optional<UUID> entity, float health, float maxHealth) {
            this.id = id;
            this.species = species;
            this.name = name;
            this.data = data;
            this.state = state;
            this.readyAt = readyAt;
            this.entity = entity.orElse(null);
            this.health = health;
            this.maxHealth = maxHealth;
        }

        static final Codec<Bond> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("id").forGetter(b -> b.id),
                Identifier.CODEC.fieldOf("species").forGetter(b -> b.species),
                Codec.STRING.optionalFieldOf("name", "").forGetter(b -> b.name),
                CompoundTag.CODEC.optionalFieldOf("data", new CompoundTag()).forGetter(b -> b.data),
                State.CODEC.fieldOf("state").forGetter(b -> b.state),
                Codec.LONG.optionalFieldOf("ready_at", 0L).forGetter(b -> b.readyAt),
                UUIDUtil.CODEC.optionalFieldOf("entity").forGetter(b -> Optional.ofNullable(b.entity)),
                Codec.FLOAT.optionalFieldOf("health", 0f).forGetter(b -> b.health),
                Codec.FLOAT.optionalFieldOf("max_health", 0f).forGetter(b -> b.maxHealth)
        ).apply(i, Bond::new));

        // Everything but the saved creature, for the owner's screen
        static final StreamCodec<ByteBuf, Bond> STREAM_CODEC = StreamCodec.of(
                (buf, b) -> {
                    UUIDUtil.STREAM_CODEC.encode(buf, b.id);
                    Identifier.STREAM_CODEC.encode(buf, b.species);
                    ByteBufCodecs.STRING_UTF8.encode(buf, b.name);
                    ByteBufCodecs.VAR_INT.encode(buf, b.state.ordinal());
                    ByteBufCodecs.VAR_LONG.encode(buf, b.readyAt);
                    ByteBufCodecs.FLOAT.encode(buf, b.health);
                    ByteBufCodecs.FLOAT.encode(buf, b.maxHealth);
                },
                buf -> new Bond(UUIDUtil.STREAM_CODEC.decode(buf), Identifier.STREAM_CODEC.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf),
                        new CompoundTag(), State.values()[ByteBufCodecs.VAR_INT.decode(buf)], ByteBufCodecs.VAR_LONG.decode(buf), Optional.empty(),
                        ByteBufCodecs.FLOAT.decode(buf), ByteBufCodecs.FLOAT.decode(buf)));
    }

    private final List<Bond> bonds;
    private final Set<Identifier> pages;

    public BondCodex() {
        this(new ArrayList<>(), new LinkedHashSet<>());
    }

    private BondCodex(List<Bond> bonds, Set<Identifier> pages) {
        this.bonds = new ArrayList<>(bonds);
        this.pages = new LinkedHashSet<>(pages);
    }

    public static final MapCodec<BondCodex> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Bond.CODEC.listOf().optionalFieldOf("bonds", List.of()).forGetter(c -> c.bonds),
            Identifier.CODEC.listOf().optionalFieldOf("pages", List.of()).forGetter(c -> List.copyOf(c.pages))
    ).apply(i, (bonds, pages) -> new BondCodex(bonds, new LinkedHashSet<>(pages))));

    public static final StreamCodec<ByteBuf, BondCodex> STREAM_CODEC = StreamCodec.composite(
            Bond.STREAM_CODEC.apply(ByteBufCodecs.list()), c -> c.bonds,
            Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()), c -> List.copyOf(c.pages),
            (bonds, pages) -> new BondCodex(bonds, new LinkedHashSet<>(pages)));

    public List<Bond> bonds() { return bonds; }
    public Set<Identifier> pages() { return pages; }

    public Bond find(UUID id) {
        for (Bond bond : bonds) if (bond.id.equals(id)) return bond;
        return null;
    }

    public Bond findByEntity(UUID entity) {
        for (Bond bond : bonds) if (bond.state == State.OUT && entity.equals(bond.entity)) return bond;
        return null;
    }

    public void add(Bond bond) { bonds.add(bond); }
    public void remove(Bond bond) { bonds.remove(bond); }
}
