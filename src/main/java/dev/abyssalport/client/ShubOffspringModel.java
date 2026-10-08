package dev.abyssalport.client;

import com.mojang.blaze3d.vertex.*;
import dev.abyssalport.content.WastelandContent;
import dev.abyssalport.entity.ShubOffspringEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

/** Owned tall root-creature: four articulated weight-bearing legs and a crown of independent tendrils. */
public final class ShubOffspringModel extends EntityModel<ShubOffspringEntity>{
    public static final ModelLayerLocation LAYER=new ModelLayerLocation(WastelandContent.id("shub_offspring"),"main");
    private final ModelPart root,head;private final ModelPart[] legs=new ModelPart[4],tendrils=new ModelPart[6];
    public ShubOffspringModel(ModelPart root){this.root=root;head=root.getChild("head");for(int i=0;i<4;i++)legs[i]=root.getChild("leg_"+i);for(int i=0;i<6;i++)tendrils[i]=head.getChild("tendril_"+i);}
    public static LayerDefinition createLayer(){var mesh=new MeshDefinition();var root=mesh.getRoot();
        root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,24).addBox(-5,-16,-4,10,16,8),PartPose.offset(0,12,0));
        var head=root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,0).addBox(-4,-8,-4,8,8,8),PartPose.offset(0,-4,0));
        for(int i=0;i<4;i++){int side=i%2==0?-1:1;var leg=root.addOrReplaceChild("leg_"+i,CubeListBuilder.create().texOffs(i*24,56).addBox(-1.5F,0,-1.5F,3,7,3),PartPose.offset(side*4,12,i<2?-3:3));
            var lower=leg.addOrReplaceChild("shin",CubeListBuilder.create().texOffs(i*24,80).addBox(-1,0,-1,2,5,2),PartPose.offset(0,7,0));
            lower.addOrReplaceChild("foot",CubeListBuilder.create().texOffs(104,56).addBox(-1.5F,-1,-3,3,1,4),PartPose.offset(0,5,0));}
        for(int i=0;i<6;i++){int side=i%2==0?-1:1;var tendril=head.addOrReplaceChild("tendril_"+i,CubeListBuilder.create().texOffs(48+i*12,0).addBox(-.5F,-8,-.5F,1,8,1),PartPose.offsetAndRotation(side*(2+i/2),-6,i/2*3-3,0,0,side*.35F));
            tendril.addOrReplaceChild("tip",CubeListBuilder.create().texOffs(104,24).addBox(-.5F,-5,-.5F,1,5,1),PartPose.offset(0,-8,0));}
        return LayerDefinition.create(mesh,128,128);
    }
    @Override public void setupAnim(ShubOffspringEntity entity,float swing,float amount,float age,float yaw,float pitch){root.getAllParts().forEach(ModelPart::resetPose);
        head.yRot=Mth.clamp(yaw,-40,40)*Mth.DEG_TO_RAD;head.xRot=Mth.clamp(pitch,-20,20)*Mth.DEG_TO_RAD;
        for(int i=0;i<4;i++){float cycle=Mth.sin(swing*.65F+((i==1||i==2)?Mth.PI:0))*Mth.clamp(amount,0,1);legs[i].xRot=cycle*.65F;legs[i].getChild("shin").xRot=Math.max(0,-cycle)*.65F;}
        for(int i=0;i<6;i++){tendrils[i].zRot+=Mth.sin(age*.08F+i)*.12F;tendrils[i].getChild("tip").xRot=Mth.sin(age*.1F+i)*.25F;}
    }
    @Override public void renderToBuffer(PoseStack pose,VertexConsumer buffer,int light,int overlay,int color){root.render(pose,buffer,light,overlay,color);}
}
