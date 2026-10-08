package dev.abyssalport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.abyssalport.AbyssalPort;
import dev.abyssalport.entity.ShadowCreatureEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Small blocky silhouette with independently moving limbs; does not use upstream model assets. */
public final class ShadowCreatureModel extends EntityModel<ShadowCreatureEntity> implements net.minecraft.client.model.HeadedModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID, "shadow_creature"), "main");
    public static final ModelLayerLocation MONSTER_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID, "shadow_monster"), "main");
    public static final ModelLayerLocation BEAST_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID, "shadow_beast"), "main");
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public ShadowCreatureModel(ModelPart root) {
        this.root = root;
        head = root.getChild("head");
        body = root.getChild("body");
        leftArm = root.getChild("left_arm");
        rightArm = root.getChild("right_arm");
        leftLeg = root.getChild("left_leg");
        rightLeg = root.getChild("right_leg");
    }
    @Override public ModelPart getHead() { return head; }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-3, -7, -3, 6, 7, 6), PartPose.offset(0, 14, 0));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 20)
                .addBox(-3, 0, -2, 6, 7, 4), PartPose.offset(0, 14, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(22, 20)
                .addBox(-1, 0, -1.5F, 3, 9, 3), PartPose.offset(4, 15, 0));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(36, 20)
                .addBox(-2, 0, -1.5F, 3, 9, 3), PartPose.offset(-4, 15, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 40)
                .addBox(-1.5F, 0, -1.5F, 3, 4, 3), PartPose.offset(1.5F, 20, 0));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(14, 40)
                .addBox(-1.5F, 0, -1.5F, 3, 4, 3), PartPose.offset(-1.5F, 20, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** Independently drawn native rigs, not rescaled copies of the original reserved models. */
    public static LayerDefinition createLargeLayer(boolean beast) {
        MeshDefinition mesh = new MeshDefinition(); PartDefinition root = mesh.getRoot();
        int headSize = beast ? 10 : 8, torsoWidth = beast ? 12 : 8, torsoHeight = beast ? 18 : 10;
        int legHeight = beast ? 14 : 10, armWidth = beast ? 4 : 3, armHeight = beast ? 18 : 12;
        float hipY = 24 - legHeight, shoulderY = hipY - torsoHeight;
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0,0)
                .addBox(-headSize/2F,-headSize,-headSize/2F,headSize,headSize,headSize), PartPose.offset(0,shoulderY,0));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0,24)
                .addBox(-torsoWidth/2F,0,-3,torsoWidth,torsoHeight,6), PartPose.offset(0,shoulderY,0));
        for (int side : new int[]{-1,1}) {
            String label = side < 0 ? "right" : "left";
            var arm = root.addOrReplaceChild(label+"_arm", CubeListBuilder.create().texOffs(side<0?48:72,24)
                    .addBox(-armWidth/2F,0,-2,armWidth,armHeight,4), PartPose.offset(side*(torsoWidth+armWidth)/2F,shoulderY+1,0));
            if (beast) for (int finger=0;finger<3;finger++) arm.addOrReplaceChild("claw_"+finger,
                    CubeListBuilder.create().texOffs(104,24).addBox(-.5F,0,-.5F,1,4,1),
                    PartPose.offset(-1+finger,armHeight, -1));
            int upper = beast ? 8 : 6, lower = legHeight-upper;
            var leg = root.addOrReplaceChild(label+"_leg", CubeListBuilder.create().texOffs(side<0?0:24,56)
                    .addBox(-2,0,-2,4,upper,4), PartPose.offset(side*(beast?3F:2F),hipY,0));
            leg.addOrReplaceChild("shin",CubeListBuilder.create().texOffs(side<0?48:72,56)
                    .addBox(-2,0,-2,4,lower,4),PartPose.offset(0,upper,0));
        }
        return LayerDefinition.create(mesh,128,128);
    }

    @Override
    public void setupAnim(ShadowCreatureEntity entity, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        float stride = Mth.clamp(limbSwingAmount, 0, 1);
        float cycle = Mth.cos(limbSwing * 0.6662F) * stride;
        leftLeg.xRot = cycle * 0.8F;
        rightLeg.xRot = -cycle * 0.8F;
        leftArm.xRot = -cycle * 0.55F + Mth.sin(ageInTicks * 0.08F) * 0.035F;
        rightArm.xRot = cycle * 0.55F - Mth.sin(ageInTicks * 0.08F) * 0.035F;
        head.yRot = Mth.clamp(netHeadYaw, -45, 45) * Mth.DEG_TO_RAD;
        head.xRot = Mth.clamp(headPitch, -20, 20) * Mth.DEG_TO_RAD;
        body.zRot = Mth.sin(ageInTicks * 0.04F) * 0.025F;
        if (leftLeg.hasChild("shin")) {
            leftLeg.getChild("shin").xRot = Math.max(0,-cycle)*.65F;
            rightLeg.getChild("shin").xRot = Math.max(0,cycle)*.65F;
        }
        float strike = Mth.sin(Mth.sqrt(attackTime)*Mth.PI)*.9F;
        rightArm.xRot -= strike;
        if (entity.kind()==ShadowCreatureEntity.Kind.BEAST) leftArm.xRot -= strike;
        if (entity.isBreathing()) {
            leftArm.xRot = -.2F; rightArm.xRot = -.2F;
            head.xRot = Mth.clamp(head.xRot,-.35F,.35F);
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
            int packedOverlay, int color) {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
