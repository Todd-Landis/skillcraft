package com.toddlandis.skillcraft.client.boss;

import java.util.EnumMap;
import java.util.Map;

import com.toddlandis.skillcraft.boss.SlagheartAction;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.AnimationState;

// Everything the Slagheart model needs for one frame, copied from the entity each frame.
public class SlagheartRenderState extends LivingEntityRenderState {
    public final Map<SlagheartAction, AnimationState> actions = new EnumMap<>(SlagheartAction.class);
    public final AnimationState idle = new AnimationState();
    public SlagheartAction action = SlagheartAction.NONE;
    public float animationSpeed = 1f;
    public boolean vented;
    public boolean stoked;
    public int phase = 1;

    public SlagheartRenderState() {
        for (SlagheartAction action : SlagheartAction.values()) actions.put(action, new AnimationState());
    }
}
