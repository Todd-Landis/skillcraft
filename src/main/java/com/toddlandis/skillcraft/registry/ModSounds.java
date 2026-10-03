package com.toddlandis.skillcraft.registry;

import java.util.function.Supplier;

import com.toddlandis.skillcraft.SkillCraft;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

// Sounds for skilling nodes, so they sound different from breaking normal blocks.
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

    private static Supplier<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(SkillCraft.MODID, name)));
    }
}
