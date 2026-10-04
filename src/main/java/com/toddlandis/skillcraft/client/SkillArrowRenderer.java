package com.toddlandis.skillcraft.client;

import com.toddlandis.skillcraft.combat.SkillArrow;

import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.resources.Identifier;

// Light and heavy arrows in flight. They reuse the vanilla arrow texture for now (the inventory
// icons are our own); swap in custom projectile textures here later.
public class SkillArrowRenderer extends ArrowRenderer<SkillArrow, ArrowRenderState> {
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/projectiles/arrow.png");

    public SkillArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected Identifier getTextureLocation(ArrowRenderState state) {
        return TEXTURE;
    }

    @Override
    public ArrowRenderState createRenderState() {
        return new ArrowRenderState();
    }
}
