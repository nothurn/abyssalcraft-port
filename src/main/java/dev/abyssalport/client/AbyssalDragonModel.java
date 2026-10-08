package dev.abyssalport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.abyssalport.content.WastelandContent;
import dev.abyssalport.entity.AbyssalDragonEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.util.Mth;

/** Independently authored articulated block-grid dragon; no upstream model geometry. */
public final class AbyssalDragonModel extends EntityModel<AbyssalDragonEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(WastelandContent.id("abyssal_dragon"), "main");
    private final ModelPart root, head, jaw, leftWing, rightWing, leftTip, rightTip, tail, tailTip;
    public AbyssalDragonModel(ModelPart baked) {
        root = baked.getChild("dragon"); head = root.getChild("neck").getChild("head"); jaw = head.getChild("jaw");
        leftWing = root.getChild("left_wing"); rightWing = root.getChild("right_wing");
        leftTip = leftWing.getChild("tip"); rightTip = rightWing.getChild("tip");
        tail = root.getChild("tail"); tailTip = tail.getChild("tip");
    }
    public static LayerDefinition createLayer() {
        var mesh = new MeshDefinition(); var root = mesh.getRoot().addOrReplaceChild("dragon", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-6, -5, -12, 12, 10, 24), PartPose.offset(0, 10, 0));
        var neck = root.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(80, 0).addBox(-4, -4, -8, 8, 8, 8), PartPose.offset(0, -1, -12));
        var head = neck.addOrReplaceChild("head", CubeListBuilder.create().texOffs(116, 0).addBox(-5, -5, -10, 10, 8, 12)
                .texOffs(180, 0).addBox(-5, -10, -2, 3, 6, 3).addBox(2, -10, -2, 3, 6, 3), PartPose.offset(0, 0, -7));
        head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(0, 40).addBox(-5, 0, -10, 10, 3, 12), PartPose.offset(0, 2, 0));
        var left = root.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(80, 40).addBox(0, -1, -8, 28, 1, 18), PartPose.offset(5, -3, -6));
        left.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(80, 62).addBox(0, -1, -7, 20, 1, 14), PartPose.offset(27, 0, 0));
        var right = root.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(80, 40).mirror().addBox(-28, -1, -8, 28, 1, 18), PartPose.offset(-5, -3, -6));
        right.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(80, 62).mirror().addBox(-20, -1, -7, 20, 1, 14), PartPose.offset(-27, 0, 0));
        var tail = root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 64).addBox(-3, -3, 0, 6, 6, 16), PartPose.offset(0, 0, 11));
        tail.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(0, 90).addBox(-2, -2, 0, 4, 4, 15), PartPose.offset(0, 0, 15));
        for (int side : new int[]{-1, 1}) for (int end : new int[]{-1, 1}) {
            var leg = root.addOrReplaceChild("leg_"+side+"_"+end, CubeListBuilder.create().texOffs(180, 40)
                    .addBox(-2, 0, -2, 4, 8, 4), PartPose.offset(side*4, 3, end*8));
            leg.addOrReplaceChild("claw", CubeListBuilder.create().texOffs(200, 40).addBox(-2.5F, 0, -5, 5, 2, 8), PartPose.offset(0, 7, 0));
        }
        return LayerDefinition.create(mesh, 256, 128);
    }
    @Override public void setupAnim(AbyssalDragonEntity entity, float walk, float amount, float age, float yaw, float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        float flap = Mth.sin(age*.17F), trailing = Mth.sin(age*.17F-.65F);
        leftWing.zRot = .18F+flap*.45F; rightWing.zRot = -leftWing.zRot;
        leftTip.zRot = -.15F+trailing*.3F; rightTip.zRot = -leftTip.zRot;
        tail.yRot = Mth.sin(age*.075F)*.1F; tailTip.yRot = Mth.sin(age*.075F-.7F)*.14F;
        head.yRot = Mth.clamp(yaw, -20, 20)*Mth.DEG_TO_RAD; head.xRot = Mth.clamp(pitch, -12, 12)*Mth.DEG_TO_RAD;
        jaw.xRot = .05F+Math.max(0, flap)*.08F;
        root.xRot = Mth.clamp((float)-entity.getDeltaMovement().y*.2F, -.12F, .12F);
        for (int side : new int[]{-1, 1}) for (int end : new int[]{-1, 1}) root.getChild("leg_"+side+"_"+end).xRot = .3F+flap*.035F;
    }
    @Override public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
        root.render(pose, buffer, light, overlay, color);
    }
}
