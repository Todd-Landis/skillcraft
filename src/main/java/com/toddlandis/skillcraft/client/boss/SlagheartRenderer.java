package com.toddlandis.skillcraft.client.boss;

import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.boss.SlagheartAction;
import com.toddlandis.skillcraft.boss.SlagheartEntity;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayerParent;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.AnimationState;

// Renders Slagheart: the base texture plus a glowing layer (heart, visor, grille, cracks) whose
// brightness shows its state: dull while banked, white-hot while vented, flickering while stoked.
public class SlagheartRenderer extends MobRenderer<SlagheartEntity, SlagheartRenderState, SlagheartModel> {
    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(SkillCraft.MODID, "textures/entity/slagheart/slagheart.png");
    public static final Identifier GLOW = Identifier.fromNamespaceAndPath(SkillCraft.MODID, "textures/entity/slagheart/slagheart_glow.png");

    public SlagheartRenderer(EntityRendererProvider.Context context) {
        super(context, new SlagheartModel(context.bakeLayer(SlagheartModel.LAYER)), 1.6f);
        addLayer(new GlowLayer(this));
    }

    @Override
    public Identifier getTextureLocation(SlagheartRenderState state) {
        return TEXTURE;
    }

    @Override
    public SlagheartRenderState createRenderState() {
        return new SlagheartRenderState();
    }

    @Override
    public void extractRenderState(SlagheartEntity entity, SlagheartRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        for (Map.Entry<SlagheartAction, AnimationState> entry : entity.animations.entrySet()) {
            state.actions.get(entry.getKey()).copyFrom(entry.getValue());
        }
        state.idle.copyFrom(entity.idleAnimation);
        state.action = entity.action();
        state.animationSpeed = entity.animationSpeed();
        state.vented = entity.isVented();
        state.stoked = entity.isStoked();
        state.phase = entity.phase();
    }

    // The death animation plays instead of the vanilla tip-over
    @Override
    protected float getFlipDegrees() {
        return 0f;
    }

    private static final class GlowLayer extends RenderLayer<SlagheartRenderState, SlagheartModel> {
        GlowLayer(RenderLayerParent<SlagheartRenderState, SlagheartModel> parent) {
            super(parent);
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, SlagheartRenderState state, float yRot, float xRot) {
            if (state.isInvisible) return;
            float pulse = 0.5f + 0.5f * Mth.sin(state.ageInTicks * 0.15f);
            float brightness;
            if (state.action == SlagheartAction.DEATH) brightness = Math.max(0f, 1f - state.deathTime / 60f);
            else if (state.vented) brightness = 1f;
            else if (state.stoked) brightness = 0.7f + 0.3f * Mth.sin(state.ageInTicks * 0.9f);
            else brightness = 0.55f + 0.15f * pulse + 0.1f * (state.phase - 1);
            int alpha = (int) (Mth.clamp(brightness, 0f, 1f) * 255);
            collector.order(1).submitModel(getParentModel(), state, poseStack, RenderTypes.entityTranslucentEmissive(GLOW), light,
                    OverlayTexture.NO_OVERLAY, ARGB.color(alpha, 255, 255, 255), null, state.outlineColor, null);
        }
    }
}
