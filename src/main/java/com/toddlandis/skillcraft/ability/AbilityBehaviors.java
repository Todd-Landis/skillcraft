package com.toddlandis.skillcraft.ability;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.ability.behavior.Buff;
import com.toddlandis.skillcraft.ability.behavior.Cleanse;
import com.toddlandis.skillcraft.ability.behavior.Cleave;
import com.toddlandis.skillcraft.ability.behavior.DashStrike;
import com.toddlandis.skillcraft.ability.behavior.GroundSlam;
import com.toddlandis.skillcraft.ability.behavior.HealOverTime;
import com.toddlandis.skillcraft.ability.behavior.Leap;
import com.toddlandis.skillcraft.ability.behavior.LineThrust;
import com.toddlandis.skillcraft.ability.behavior.MeleeStrike;
import com.toddlandis.skillcraft.ability.behavior.NextShot;
import com.toddlandis.skillcraft.ability.behavior.Parry;
import com.toddlandis.skillcraft.ability.behavior.ToggleEffect;
import com.toddlandis.skillcraft.ability.behavior.Volley;

import net.minecraft.resources.Identifier;

// The behaviour types an ability JSON can use, keyed by "type". The JSON format is the dispatch
// codec below: {"type": "skillcraft:ground_slam", "radius": 4, ...}.
public final class AbilityBehaviors {
    private static final BiMap<Identifier, MapCodec<? extends AbilityBehavior>> TYPES = HashBiMap.create();

    static {
        register("melee_strike", MeleeStrike.CODEC);
        register("dash_strike", DashStrike.CODEC);
        register("cleave", Cleave.CODEC);
        register("ground_slam", GroundSlam.CODEC);
        register("line_thrust", LineThrust.CODEC);
        register("leap", Leap.CODEC);
        register("next_shot", NextShot.CODEC);
        register("volley", Volley.CODEC);
        register("buff", Buff.CODEC);
        register("heal_over_time", HealOverTime.CODEC);
        register("parry", Parry.CODEC);
        register("cleanse", Cleanse.CODEC);
        register("toggle_effect", ToggleEffect.CODEC);
    }

    public static final Codec<AbilityBehavior> CODEC = Identifier.CODEC
            .validate(id -> TYPES.containsKey(id) ? DataResult.success(id) : DataResult.error(() -> "Unknown ability behaviour type " + id))
            .dispatch("type", behavior -> TYPES.inverse().get(behavior.codec()), TYPES::get);

    private AbilityBehaviors() {}

    // Other mods (or later SkillCraft phases) can add behaviour types here
    public static void register(String name, MapCodec<? extends AbilityBehavior> codec) {
        TYPES.put(Identifier.fromNamespaceAndPath(SkillCraft.MODID, name), codec);
    }
}
