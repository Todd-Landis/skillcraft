package com.toddlandis.skillcraft.client.bonding;

import com.mojang.blaze3d.vertex.PoseStack;
import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.bonding.Cloudray;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

// Renders the Cloudray: its body texture, plus storm-blue markings that glow in the dark and
// pulse slowly (an emissive layer, like Slagheart's heart).
public class CloudrayRenderer extends MobRenderer<Cloudray, CloudrayRenderState, CloudrayModel> {
    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(SkillCraft.MODID, "textures/entity/cloudray/cloudray.png");
    public static final Identifier GLOW = Identifier.fromNamespaceAndPath(SkillCraft.MODID, "textures/entity/cloudray/cloudray_glow.png");

    public CloudrayRenderer(EntityRendererProvider.Context context) {
        super(context, new CloudrayModel(context.bakeLayer(CloudrayModel.LAYER)), 1.3f);
        addLayer(new GlowLayer(this));
    }

    @Override
    public Identifier getTextureLocation(CloudrayRenderState state) {
        return TEXTURE;
    }

    @Override
    public CloudrayRenderState createRenderState() {
        return new CloudrayRenderState();
    }

    @Override
    public void extractRenderState(Cloudray entity, CloudrayRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.flightSpeed = (float) entity.getDeltaMovement().length();
    }

    private static final class GlowLayer extends RenderLayer<CloudrayRenderState, CloudrayModel> {
        GlowLayer(RenderLayerParent<CloudrayRenderState, CloudrayModel> parent) {
            super(parent);
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, CloudrayRenderState state, float yRot, float xRot) {
            if (state.isInvisible) return;
            int alpha = (int) (Mth.clamp(0.7f + 0.3f * Mth.sin(state.ageInTicks * 0.08f), 0f, 1f) * 255);
            collector.order(1).submitModel(getParentModel(), state, poseStack, RenderTypes.entityTranslucentEmissive(GLOW), light,
                    OverlayTexture.NO_OVERLAY, ARGB.color(alpha, 255, 255, 255), null, state.outlineColor, null);
        }
    }
}
