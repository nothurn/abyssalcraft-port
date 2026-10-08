package dev.abyssalport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.abyssalport.content.WastelandContent;
import dev.abyssalport.entity.DreadSlugEntity;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class DreadSlugRenderer extends EntityRenderer<DreadSlugEntity> {
    public static final ModelLayerLocation LAYER=new ModelLayerLocation(WastelandContent.id("dread_slug"),"main");
    private final ModelPart root;
    public DreadSlugRenderer(EntityRendererProvider.Context context) { super(context); root=context.bakeLayer(LAYER); }
    public static LayerDefinition createLayer() {
        var mesh=new MeshDefinition(); mesh.getRoot().addOrReplaceChild("slug",CubeListBuilder.create().texOffs(0,0).addBox(-2,-1.5F,-3,4,3,6),PartPose.ZERO);
        return LayerDefinition.create(mesh,32,32);
    }
    @Override public ResourceLocation getTextureLocation(DreadSlugEntity entity) { return WastelandContent.id("textures/entity/dread_slug.png"); }
    @Override public void render(DreadSlugEntity entity,float yaw,float partialTick,PoseStack pose,MultiBufferSource buffer,int light) {
        pose.pushPose(); pose.mulPose(Axis.YP.rotationDegrees(180-yaw)); pose.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
        root.render(pose,buffer.getBuffer(RenderType.entityCutout(getTextureLocation(entity))),light,OverlayTexture.NO_OVERLAY); pose.popPose();
        super.render(entity,yaw,partialTick,pose,buffer,light);
    }
}
