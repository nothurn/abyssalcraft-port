package dev.abyssalport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.abyssalport.AbyssalPort;
import dev.abyssalport.entity.ShoggothEntity;
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

/** Original layered silhouette with exposed locomotor tendrils and independent feelers. */
public final class ShoggothModel extends EntityModel<ShoggothEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID, "lesser_shoggoth"), "main");
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart[] tendrils;
    private final ModelPart[] feelers;

    public ShoggothModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.tendrils = new ModelPart[] {
                root.getChild("front_left"), root.getChild("front_right"),
                root.getChild("back_left"), root.getChild("back_right")
        };
        this.feelers = new ModelPart[] {body.getChild("left_feeler"), body.getChild("right_feeler")};
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-9, -9, -8, 18, 9, 16)
                .texOffs(0, 31).addBox(-11, -11, -9, 22, 5, 18)
                .texOffs(76, 0).addBox(-6, -14, -7, 12, 6, 11)
                .texOffs(80, 24).addBox(-4, -7, 7, 8, 5, 8)
                .texOffs(80, 56).addBox(7, -8, -5, 7, 6, 10), PartPose.offset(0, 18, 0));
        body.addOrReplaceChild("left_feeler", CubeListBuilder.create().texOffs(80, 80)
                .addBox(-1.5F, -7, -1.5F, 3, 7, 3), PartPose.offset(-6, -9, -7));
        body.addOrReplaceChild("right_feeler", CubeListBuilder.create().texOffs(96, 80)
                .addBox(-1.5F, -7, -1.5F, 3, 7, 3), PartPose.offset(6, -9, -7));
        root.addOrReplaceChild("front_left", CubeListBuilder.create().texOffs(0, 80)
                .addBox(-2, 0, -2, 4, 9, 4), PartPose.offset(-11, 15, -7));
        root.addOrReplaceChild("front_right", CubeListBuilder.create().texOffs(20, 80)
                .addBox(-2, 0, -2, 4, 9, 4), PartPose.offset(11, 15, -7));
        root.addOrReplaceChild("back_left", CubeListBuilder.create().texOffs(40, 80)
                .addBox(-2, 0, -2, 4, 9, 4), PartPose.offset(-10, 15, 7));
        root.addOrReplaceChild("back_right", CubeListBuilder.create().texOffs(60, 80)
                .addBox(-2, 0, -2, 4, 9, 4), PartPose.offset(10, 15, 7));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(ShoggothEntity entity, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch) {
        body.y = 18 + Mth.sin(ageInTicks * 0.11F) * 0.24F;
        body.yRot = Mth.sin(ageInTicks * 0.035F) * 0.035F;
        for (int i = 0; i < tendrils.length; i++) {
            float phase = (i == 0 || i == 3) ? 0 : Mth.PI;
            tendrils[i].xRot = Mth.cos(limbSwing * 0.75F + phase) * 0.48F * limbSwingAmount;
            float spread = (i == 0 || i == 2) ? 0.18F : -0.18F;
            tendrils[i].zRot = spread + Mth.sin(ageInTicks * 0.09F + i * 1.6F) * 0.08F;
        }
        for (int i = 0; i < feelers.length; i++) {
            feelers[i].xRot = Mth.sin(ageInTicks * 0.075F + i * 1.7F) * 0.12F;
            feelers[i].zRot = (i == 0 ? -0.13F : 0.13F)
                    + Mth.sin(ageInTicks * 0.065F + i * 2.4F) * 0.09F;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
            int packedOverlay, int color) {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
