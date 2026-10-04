package com.toddlandis.skillcraft.registry;

import java.util.function.Supplier;

import com.toddlandis.skillcraft.SkillCraft;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

// Sounds for skilling nodes (so they sound different from breaking normal blocks) and the
// Slagheart boss fight.
// Which audio files play is set in assets/skillcraft/sounds.json. They currently reuse vanilla
// audio; drop your own .ogg files into assets/skillcraft/sounds/ and point sounds.json at them.
public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, SkillCraft.MODID);

    public static final Supplier<SoundEvent> NODE_CHOP = register("node.chop");
    public static final Supplier<SoundEvent> NODE_PICK = register("node.pick");
    public static final Supplier<SoundEvent> NODE_SUCCESS_WOOD = register("node.success_wood");
    public static final Supplier<SoundEvent> NODE_SUCCESS_ORE = register("node.success_ore");
    public static final Supplier<SoundEvent> NODE_DEPLETE_WOOD = register("node.deplete_wood");
    public static final Supplier<SoundEvent> NODE_DEPLETE_ORE = register("node.deplete_ore");
    public static final Supplier<SoundEvent> NODE_REGROW = register("node.regrow");
    public static final Supplier<SoundEvent> SHOAL_DEPLETE = register("shoal.deplete");
    public static final Supplier<SoundEvent> SHOAL_RESURFACE = register("shoal.resurface");
    public static final Supplier<SoundEvent> RANGE_SIZZLE = register("range.sizzle");
    public static final Supplier<SoundEvent> RANGE_BURN = register("range.burn");

    public static final Supplier<SoundEvent> SLAGHEART_AWAKEN = register("slagheart.awaken");
    public static final Supplier<SoundEvent> SLAGHEART_STEP = register("slagheart.step");
    public static final Supplier<SoundEvent> SLAGHEART_HAMMER_WINDUP = register("slagheart.hammer_windup");
    public static final Supplier<SoundEvent> SLAGHEART_HAMMER_SLAM = register("slagheart.hammer_slam");
    public static final Supplier<SoundEvent> SLAGHEART_SWEEP_WINDUP = register("slagheart.sweep_windup");
    public static final Supplier<SoundEvent> SLAGHEART_SWEEP = register("slagheart.sweep");
    public static final Supplier<SoundEvent> SLAGHEART_INHALE = register("slagheart.inhale");
    public static final Supplier<SoundEvent> SLAGHEART_SPIT = register("slagheart.spit");
    public static final Supplier<SoundEvent> SLAGHEART_EMBER = register("slagheart.ember");
    public static final Supplier<SoundEvent> SLAGHEART_ERUPT = register("slagheart.erupt");
    public static final Supplier<SoundEvent> SLAGHEART_QUAKE = register("slagheart.quake");
    public static final Supplier<SoundEvent> SLAGHEART_VENT = register("slagheart.vent");
    public static final Supplier<SoundEvent> SLAGHEART_ROAR = register("slagheart.roar");
    public static final Supplier<SoundEvent> SLAGHEART_HURT = register("slagheart.hurt");
    public static final Supplier<SoundEvent> SLAGHEART_DEATH = register("slagheart.death");
    public static final Supplier<SoundEvent> ARENA_ENTER = register("arena.enter");

    private static Supplier<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(SkillCraft.MODID, name)));
    }
}
