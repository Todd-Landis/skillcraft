package com.toddlandis.skillcraft.client.bonding;

import com.toddlandis.skillcraft.SkillCraft;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

// The Cloudray: a great sky-manta. A flat body with a blunt head and two curled head-fins, broad
// wings that beat slowly (harder the faster it flies), and a long whip tail with a small fin.
//
// Texture offsets and box sizes MUST match scripts/textures/GenCloudrayTexture.java, which paints
// each cube face (texture 128 x 64).
public class CloudrayModel extends EntityModel<CloudrayRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(SkillCraft.MODID, "cloudray"), "main");

    private final ModelPart body;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart tail;
    private final ModelPart tailFin;

    public CloudrayModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.leftWing = body.getChild("left_wing");
        this.rightWing = body.getChild("right_wing");
        this.tail = body.getChild("tail");
        this.tailFin = tail.getChild("tail_fin");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-8, -2, -10, 16, 4, 20), PartPose.offset(0, 17, 0));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 24).addBox(-5, -1.5f, -6, 10, 3, 6), PartPose.offset(0, 0, -10));
        head.addOrReplaceChild("left_horn", CubeListBuilder.create()
                .texOffs(32, 24).addBox(-1, -1, -5, 2, 2, 5), PartPose.offsetAndRotation(-4, 0, -5, 0.15f, 0.25f, 0));
        head.addOrReplaceChild("right_horn", CubeListBuilder.create()
                .texOffs(32, 24).mirror().addBox(-1, -1, -5, 2, 2, 5), PartPose.offsetAndRotation(4, 0, -5, 0.15f, -0.25f, 0));
        body.addOrReplaceChild("left_wing", CubeListBuilder.create()
                .texOffs(0, 34).addBox(-22, -0.5f, -8, 22, 1, 16), PartPose.offset(-8, 0, -1));
        body.addOrReplaceChild("right_wing", CubeListBuilder.create()
                .texOffs(0, 34).mirror().addBox(0, -0.5f, -8, 22, 1, 16), PartPose.offset(8, 0, -1));
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(80, 0).addBox(-1, -1, 0, 2, 2, 18), PartPose.offset(0, 0, 10));
        tail.addOrReplaceChild("tail_fin", CubeListBuilder.create()
                .texOffs(80, 22).addBox(-3, -0.5f, 0, 6, 1, 3), PartPose.offset(0, 0, 18));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(CloudrayRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        float effort = Mth.clamp(state.flightSpeed * 3f, 0f, 1f);
        float beat = Mth.sin(t * (0.16f + 0.1f * effort));
        float flap = beat * (0.3f + 0.3f * effort);
        leftWing.zRot = flap;
        rightWing.zRot = -flap;
        // The wing tips trail the stroke a little, like a ray's
        leftWing.xRot = Mth.sin(t * (0.16f + 0.1f * effort) - 0.8f) * 0.08f;
        rightWing.xRot = leftWing.xRot;
        body.y += -beat * 0.6f;
        tail.xRot = Mth.sin(t * 0.12f - 1f) * 0.12f;
        tail.yRot = Mth.sin(t * 0.07f) * 0.15f;
        tailFin.xRot = Mth.sin(t * 0.12f - 2f) * 0.25f;
    }
}
