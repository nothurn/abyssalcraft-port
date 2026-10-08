package dev.abyssalport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.abyssalport.content.WastelandContent;
import dev.abyssalport.entity.DreadlingEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

/** Own short, uneven-limbed silhouette with articulated swing-phase knees. */
public final class DreadlingModel extends EntityModel<DreadlingEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(WastelandContent.id("dreadling"), "main");
    private final ModelPart root, head, rightArm, leftArm, rightLeg, leftLeg;
    public DreadlingModel(ModelPart root) {
        this.root = root; head = root.getChild("head"); rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm"); rightLeg = root.getChild("right_leg"); leftLeg = root.getChild("left_leg");
    }
    public static LayerDefinition createLayer() {
        var mesh = new MeshDefinition(); var root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8), PartPose.offset(0, 8, 0));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 20).addBox(-4, 0, -2, 8, 9, 4), PartPose.offset(0, 8, 0));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(32, 20).addBox(-3, -1, -2, 3, 7, 4), PartPose.offset(-4, 9, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(48, 20).addBox(0, -1, -2, 3, 11, 4), PartPose.offset(4, 9, 0));
        for (String side : new String[]{"right", "left"}) {
            var thigh = root.addOrReplaceChild(side+"_leg", CubeListBuilder.create().texOffs(0, 40)
                    .addBox(-2, 0, -2, 4, 4, 4), PartPose.offset(side.equals("right") ? -2 : 2, 17, 0));
            thigh.addOrReplaceChild("shin", CubeListBuilder.create().texOffs(20, 40).addBox(-2, 0, -2, 4, 3, 4), PartPose.offset(0, 4, 0));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }
    @Override public void setupAnim(DreadlingEntity entity, float swing, float amount, float age, float yaw, float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        head.yRot = Mth.clamp(yaw, -60, 60) * Mth.DEG_TO_RAD;
        head.xRot = Mth.clamp(pitch, -30, 30) * Mth.DEG_TO_RAD;
        float stride = Mth.clamp(amount, 0, 1), phase = swing * .6662F;
        leftLeg.xRot = Mth.cos(phase) * .65F * stride;
        rightLeg.xRot = -leftLeg.xRot;
        leftLeg.getChild("shin").xRot = Math.max(0, Mth.sin(phase)) * .55F * stride;
        rightLeg.getChild("shin").xRot = Math.max(0, -Mth.sin(phase)) * .55F * stride;
        leftArm.xRot = -leftLeg.xRot * .7F - .12F;
        rightArm.xRot = -rightLeg.xRot * .7F;
        float strike = Mth.sin(Mth.sqrt(attackTime) * Mth.PI) * 1.4F;
        rightArm.xRot -= strike;
    }
    @Override public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
        root.render(pose, buffer, light, overlay, color);
    }
}
