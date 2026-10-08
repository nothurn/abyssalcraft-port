package dev.abyssalport.client;

import com.mojang.blaze3d.vertex.*;
import dev.abyssalport.content.WastelandContent;
import dev.abyssalport.entity.DreadSpawnEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

/** Own eight-legged silhouette; distance-driven alternating stance and bent swing legs. */
public final class DreadSpawnModel extends EntityModel<DreadSpawnEntity> {
    public static final ModelLayerLocation LAYER=new ModelLayerLocation(WastelandContent.id("dread_spawn"),"main");
    private final ModelPart root,head;
    private final ModelPart[] legs=new ModelPart[8];
    public DreadSpawnModel(ModelPart root) {
        this.root=root; head=root.getChild("head");
        for(int index=0;index<8;index++) legs[index]=root.getChild("leg_"+index);
    }
    public static LayerDefinition createLayer() {
        var mesh=new MeshDefinition(); var root=mesh.getRoot();
        root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0).addBox(-5,-4,-3,10,8,10),PartPose.offset(0,13,0));
        root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,20).addBox(-4,-3,-5,8,6,6),PartPose.offset(0,12,-3));
        for(int index=0;index<8;index++) {
            int side=index<4?-1:1, pair=index%4;
            var leg=root.addOrReplaceChild("leg_"+index,CubeListBuilder.create().texOffs(32,20)
                    .addBox(side<0?-7:0,-1,-1,7,2,2),PartPose.offsetAndRotation(side*4,14,-2+pair*2.5F,0,(pair-1.5F)*side*.3F,side*.45F));
            var shin=leg.addOrReplaceChild("shin",CubeListBuilder.create().texOffs(32,26)
                    .addBox(side<0?-6:0,-1,-1,6,2,2),PartPose.offsetAndRotation(side*7,0,0,0,0,side*.6F));
            shin.addOrReplaceChild("foot",CubeListBuilder.create().texOffs(32,32).addBox(-1,0,-2,2,3,3),PartPose.offset(side*6,0,0));
        }
        return LayerDefinition.create(mesh,64,64);
    }
    @Override public void setupAnim(DreadSpawnEntity entity,float swing,float amount,float age,float yaw,float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        head.yRot=Mth.clamp(yaw,-25,25)*Mth.DEG_TO_RAD; head.xRot=Mth.clamp(pitch,-20,20)*Mth.DEG_TO_RAD;
        float stride=Mth.clamp(amount,0,1),phase=swing*.8F;
        for(int index=0;index<8;index++) {
            int side=index<4?-1:1, pair=index%4;
            float step=phase+(pair%2==0?0:Mth.PI)+(side<0?0:Mth.PI);
            float lift=Math.max(0,Mth.sin(step))*stride;
            legs[index].yRot+=(Mth.cos(step)*.35F*stride)*side;
            legs[index].zRot-=side*lift*.28F;
            legs[index].getChild("shin").zRot+=side*lift*.5F;
            legs[index].getChild("shin").getChild("foot").zRot-=side*lift*.22F;
        }
    }
    @Override public void renderToBuffer(PoseStack pose,VertexConsumer buffer,int light,int overlay,int color) { root.render(pose,buffer,light,overlay,color); }
}
