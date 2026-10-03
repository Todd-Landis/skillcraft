package com.toddlandis.skillcraft.client.boss;

import java.util.EnumMap;
import java.util.Map;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.boss.SlagheartAction;

import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.entity.animation.json.AnimationHolder;
import net.neoforged.neoforge.client.entity.animation.json.AnimationLoader;

// Slagheart's model: a hunched iron colossus about 3.5 blocks tall. A riveted chest with two
// doors over a molten heart, a visored head between chimney stacks, a hammer-fist on the left
// arm and forge tongs on the right.
//
// Texture offsets and box sizes MUST match scripts/textures/GenSlagheartTexture.java, which paints
// each cube face. Animations are JSON keyframes in assets/skillcraft/neoforge/animations/entity/
// slagheart/<name>.json (loaded by NeoForge's animation loader; edit them without recompiling).
public class SlagheartModel extends EntityModel<SlagheartRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(SkillCraft.MODID, "slagheart"), "main");

    private final ModelPart head;
    private final Map<SlagheartAction, Clip> clips = new EnumMap<>(SlagheartAction.class);
    private final Clip walk;

    // A JSON animation, re-baked if a resource reload replaces it
    private static final class Clip {
        final AnimationHolder holder;
        final ModelPart root;
        AnimationDefinition baked;
        KeyframeAnimation animation;

        Clip(ModelPart root, String name) {
            this.root = root;
            this.holder = AnimationLoader.INSTANCE.getAnimationHolder(Identifier.fromNamespaceAndPath(SkillCraft.MODID, "slagheart/" + name));
        }

        KeyframeAnimation get() {
            AnimationDefinition definition = holder.get();
            if (definition != baked) {
                baked = definition;
                animation = definition.bake(root);
            }
            return animation;
        }
    }

    public SlagheartModel(ModelPart root) {
        super(root);
        this.head = root.getChild("hips").getChild("torso").getChild("head");
        for (SlagheartAction action : SlagheartAction.values()) clips.put(action, new Clip(root, action.animation()));
        this.walk = new Clip(root, "walk");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition hips = root.addOrReplaceChild("hips", CubeListBuilder.create()
                .texOffs(140, 0).addBox(-8, -3, -5, 16, 6, 10)
                .texOffs(44, 62).addBox(-9, -2, -6, 18, 4, 2), PartPose.offset(0, 5, 0));

        for (int side : new int[]{-1, 1}) {
            String name = side < 0 ? "left" : "right";
            CubeListBuilder legCubes = CubeListBuilder.create().texOffs(0, 40);
            if (side > 0) legCubes.mirror();
            legCubes.addBox(-3.5f, 0, -4, 7, 14, 8).texOffs(0, 62).addBox(-4.5f, 4, -5, 9, 5, 3);
            PartDefinition leg = hips.addOrReplaceChild(name + "_leg", legCubes, PartPose.offset(5 * side, 1, 0));
            leg.addOrReplaceChild(name + "_foot", CubeListBuilder.create().texOffs(30, 40).addBox(-4.5f, 0, -7, 9, 4, 11), PartPose.offset(0, 14, 0));
        }

        PartDefinition torso = hips.addOrReplaceChild("torso", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-12, -24, -8, 24, 24, 16)
                .texOffs(192, 0).addBox(-8, -21, 8, 16, 14, 4), PartPose.offsetAndRotation(0, -2, 2, 0.2f, 0, 0));
        torso.addOrReplaceChild("heart", CubeListBuilder.create().texOffs(218, 26).addBox(-5, -5, -0.5f, 10, 10, 1), PartPose.offset(0, -12, -8));
        torso.addOrReplaceChild("door_left", CubeListBuilder.create().texOffs(180, 18).addBox(0, 0, -2, 7, 13, 2), PartPose.offset(-7, -19, -8));
        torso.addOrReplaceChild("door_right", CubeListBuilder.create().texOffs(180, 18).mirror().addBox(-7, 0, -2, 7, 13, 2), PartPose.offset(7, -19, -8));
        torso.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(140, 16).addBox(-5, -7, -7, 10, 9, 10)
                .texOffs(198, 18).addBox(-6, -8, -8, 12, 3, 4)
                .texOffs(24, 62).addBox(-1, -10, -6, 2, 4, 8), PartPose.offsetAndRotation(0, -23, -6, -0.2f, 0, 0));
        for (int side : new int[]{-1, 1}) {
            torso.addOrReplaceChild(side < 0 ? "chimney_left" : "chimney_right", CubeListBuilder.create()
                    .texOffs(222, 40).addBox(-2, -12, -2, 4, 12, 4)
                    .texOffs(198, 26).addBox(-2.5f, -14, -2.5f, 5, 2, 5), PartPose.offsetAndRotation(6 * side, -24, 5, -0.15f, 0, 0.12f * side));
        }

        // Left arm: hammer fist
        PartDefinition leftArm = torso.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(122, 40).addBox(-4, -2, -4, 8, 14, 8)
                .texOffs(80, 26).addBox(-5.5f, -6, -6, 9, 9, 12), PartPose.offsetAndRotation(-16, -18, 0, -0.2f, 0, 0.08f));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(154, 40).addBox(-4.5f, 0, -4.5f, 9, 12, 9), PartPose.offset(0, 12, 0));
        leftForearm.addOrReplaceChild("hammer", CubeListBuilder.create()
                .texOffs(190, 40).addBox(-4, -2, -4, 8, 4, 8)
                .texOffs(80, 0).addBox(-7, 2, -8, 14, 10, 16), PartPose.offset(0, 12, 0));

        // Right arm: forge tongs
        PartDefinition rightArm = torso.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(122, 40).mirror().addBox(-4, -2, -4, 8, 14, 8)
                .texOffs(80, 26).mirror().addBox(-3.5f, -6, -6, 9, 9, 12), PartPose.offsetAndRotation(16, -18, 0, -0.2f, 0, -0.08f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(154, 40).mirror().addBox(-4.5f, 0, -4.5f, 9, 12, 9)
                .texOffs(190, 40).mirror().addBox(-4, 12, -4, 8, 4, 8), PartPose.offset(0, 12, 0));
        rightForearm.addOrReplaceChild("tong_front", CubeListBuilder.create().texOffs(238, 40).addBox(-2, 0, -3, 4, 14, 3), PartPose.offsetAndRotation(0, 15, -1, -0.12f, 0, 0));
        rightForearm.addOrReplaceChild("tong_back", CubeListBuilder.create().texOffs(238, 40).mirror().addBox(-2, 0, 0, 4, 14, 3), PartPose.offsetAndRotation(0, 15, 1, 0.12f, 0, 0));

        return LayerDefinition.create(mesh, 256, 128);
    }

    @Override
    public void setupAnim(SlagheartRenderState state) {
        super.setupAnim(state);
        if (state.action == SlagheartAction.NONE) {
            head.yRot += Math.clamp(state.yRot, -40f, 40f) * ((float) Math.PI / 180f);
            walk.get().applyWalk(state.walkAnimationPos, state.walkAnimationSpeed, 1.6f, 2.0f);
            clips.get(SlagheartAction.NONE).get().apply(state.idle, state.ageInTicks);
        }
        for (Map.Entry<SlagheartAction, Clip> entry : clips.entrySet()) {
            if (entry.getKey() == SlagheartAction.NONE) continue;
            entry.getValue().get().apply(state.actions.get(entry.getKey()), state.ageInTicks, state.animationSpeed);
        }
    }
}
