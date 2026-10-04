package com.toddlandis.skillcraft.client.fishing;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.fishing.FishingSpotEntity;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

// Draws a fishing spot as foam and ripples lying flat on the water: two translucent rings turning
// in opposite directions and gently pulsing, with the bubbles and splashes coming from the
// entity's own particles. Nothing is drawn while the shoal has swum off.
public class FishingSpotRenderer extends EntityRenderer<FishingSpotEntity, FishingSpotRenderer.State> {
    private static final Identifier RIPPLES = Identifier.fromNamespaceAndPath(SkillCraft.MODID, "textures/entity/fishing_spot/ripples.png");
    private static final Identifier FOAM = Identifier.fromNamespaceAndPath(SkillCraft.MODID, "textures/entity/fishing_spot/foam.png");
    private static final RenderType RIPPLES_TYPE = RenderTypes.entityTranslucent(RIPPLES);
    private static final RenderType FOAM_TYPE = RenderTypes.entityTranslucent(FOAM);
    // Lift above the entity's origin so the rings lie just on top of the water surface
    private static final float LIFT = (float) (FishingSpotEntity.SURFACE_Y - FishingSpotEntity.SURFACE_OFFSET) + 0.015f;

    public static class State extends EntityRenderState {
        boolean depleted;
    }

    public FishingSpotRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(FishingSpotEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.depleted = entity.isDepleted();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.depleted) {
            float t = state.ageInTicks;
            poseStack.pushPose();
            poseStack.translate(0, LIFT, 0);

            // Outer ripples: slow turn, breathing in and out
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(t * 1.5f));
            float outer = 1.45f + 0.12f * Mth.sin(t * 0.12f);
            int outerAlpha = 150 + (int) (40 * Mth.sin(t * 0.12f + 1.0f));
            collector.submitCustomGeometry(poseStack, RIPPLES_TYPE, (pose, buffer) -> flatQuad(pose, buffer, state.lightCoords, outer, outerAlpha));
            poseStack.popPose();

            // Inner foam: turns the other way, a touch higher so the two don't flicker
            poseStack.pushPose();
            poseStack.translate(0, 0.004f, 0);
            poseStack.mulPose(Axis.YP.rotationDegrees(-t * 3.0f));
            float inner = 0.85f + 0.08f * Mth.sin(t * 0.3f);
            collector.submitCustomGeometry(poseStack, FOAM_TYPE, (pose, buffer) -> flatQuad(pose, buffer, state.lightCoords, inner, 210));
            poseStack.popPose();

            poseStack.popPose();
        }
        super.submit(state, poseStack, collector, camera);
    }

    // A square of the given width lying flat, centred on the origin, facing up
    private static void flatQuad(PoseStack.Pose pose, VertexConsumer buffer, int light, float size, int alpha) {
        float h = size / 2;
        int color = (Mth.clamp(alpha, 0, 255) << 24) | 0xFFFFFF;
        vertex(buffer, pose, light, color, -h, -h, 0, 0);
        vertex(buffer, pose, light, color, -h, h, 0, 1);
        vertex(buffer, pose, light, color, h, h, 1, 1);
        vertex(buffer, pose, light, color, h, -h, 1, 0);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, int light, int color, float x, float z, float u, float v) {
        buffer.addVertex(pose, x, 0, z)
                .setColor(color)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0, 1, 0);
    }
}
