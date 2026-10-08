package dev.abyssalport.client;

import com.mojang.blaze3d.vertex.*;
import dev.abyssalport.content.WastelandContent;
import dev.abyssalport.entity.CorruptedAnimalEntity;
import dev.abyssalport.entity.CorruptedAnimalEntity.Kind;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

/** Four owned native rigs: species-sized bodies, alternating diagonal feet and independent head/flaps. */
public final class CorruptedAnimalModel extends EntityModel<CorruptedAnimalEntity> {
    private final ModelPart root,head;private final ModelPart[] legs=new ModelPart[4];private final Kind kind;
    public static ModelLayerLocation layer(Kind kind){return new ModelLayerLocation(WastelandContent.id("corrupted_"+kind.name().toLowerCase(java.util.Locale.ROOT)),"main");}
    public CorruptedAnimalModel(ModelPart root,Kind kind){this.root=root;this.kind=kind;head=root.getChild("head");
        for(int i=0;i<(kind==Kind.CHICKEN?2:4);i++)legs[i]=root.getChild("leg_"+i);}
    public static LayerDefinition createLayer(Kind kind){var mesh=new MeshDefinition();var root=mesh.getRoot();
        if(kind==Kind.CHICKEN){
            root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,32).addBox(-3,-6,-3,6,6,6),PartPose.offset(0,21,0));
            var head=root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,0).addBox(-2,-4,-2,4,5,4),PartPose.offset(0,15,-3));
            head.addOrReplaceChild("beak",CubeListBuilder.create().texOffs(40,0).addBox(-2,0,-2,4,2,2),PartPose.offset(0,-1,-2));
            head.addOrReplaceChild("wattle",CubeListBuilder.create().texOffs(56,0).addBox(-1,0,-1,2,2,2),PartPose.offset(0,1,-2));
            for(int side:new int[]{-1,1})root.addOrReplaceChild(side<0?"right_wing":"left_wing",CubeListBuilder.create().texOffs(side<0?64:80,32)
                .addBox(-.5F,0,-3,1,5,6),PartPose.offset(side*3.5F,15,0));
            for(int i=0;i<2;i++){var leg=root.addOrReplaceChild("leg_"+i,CubeListBuilder.create().texOffs(i*24,64).addBox(-.5F,0,-.5F,1,3,1),PartPose.offset(i==0?-1.5F:1.5F,21,1));
                leg.addOrReplaceChild("foot",CubeListBuilder.create().texOffs(48,64).addBox(-1.5F,-1,-3,3,1,4),PartPose.offset(0,3,0));}
        }else{
            int bodyW=kind==Kind.COW?10:kind==Kind.PIG?10:10,bodyH=kind==Kind.COW?10:kind==Kind.PIG?7:10;
            int bodyD=kind==Kind.PIG?14:16,legH=kind==Kind.PIG?6:10,headW=kind==Kind.COW?8:6,headH=kind==Kind.COW?8:6,headD=kind==Kind.PIG?7:6;
            float hip=24-legH;
            root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,32).addBox(-bodyW/2F,-bodyH,-bodyD/2F,bodyW,bodyH,bodyD),PartPose.offset(0,hip,1));
            var head=root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,0).addBox(-headW/2F,-headH/2F,-headD,headW,headH,headD),PartPose.offset(0,hip-bodyH+headH/2F,-bodyD/2F+1));
            head.addOrReplaceChild("muzzle",CubeListBuilder.create().texOffs(40,0).addBox(-2,-1,-2,4,3,2),PartPose.offset(0,headH/2F-2,-headD));
            for(int side:new int[]{-1,1})head.addOrReplaceChild("ear_"+side,CubeListBuilder.create().texOffs(56,0).addBox(-1,-1,-1,3,2,2),PartPose.offset(side*(headW/2F+1),-headH/2F+1,-2));
            if(kind==Kind.COW)for(int side:new int[]{-1,1})head.addOrReplaceChild("horn_"+side,CubeListBuilder.create().texOffs(72,0).addBox(-.5F,-3,-.5F,1,3,1),PartPose.offset(side*3,-4,-2));
            for(int i=0;i<4;i++){int front=i/2;float x=(i%2==0?-1:1)*(bodyW/2F-2),z=front==0?-bodyD/2F+3:bodyD/2F-2;
                root.addOrReplaceChild("leg_"+i,CubeListBuilder.create().texOffs(i*24,64).addBox(-1.5F,0,-1.5F,3,legH,3),PartPose.offset(x,hip,z));}
            if(kind==Kind.PIG)root.addOrReplaceChild("tail",CubeListBuilder.create().texOffs(88,0).addBox(-1,-1,0,2,2,3),PartPose.offset(0,hip-4,bodyD/2F+1));
        }return LayerDefinition.create(mesh,128,128);
    }
    @Override public void setupAnim(CorruptedAnimalEntity entity,float swing,float amount,float age,float yaw,float pitch){root.getAllParts().forEach(ModelPart::resetPose);
        head.yRot=Mth.clamp(yaw,-55,55)*Mth.DEG_TO_RAD;head.xRot=Mth.clamp(pitch,-35,35)*Mth.DEG_TO_RAD;
        for(int i=0;i<(kind==Kind.CHICKEN?2:4);i++)legs[i].xRot=Mth.cos(swing*.6662F+((i==1||i==2)?Mth.PI:0))*1.15F*Mth.clamp(amount,0,1);
        if(kind==Kind.CHICKEN){float partial=Mth.clamp(age-entity.tickCount,0,1),flap=Mth.lerp(partial,entity.flapOld,entity.flap),amp=Mth.lerp(partial,entity.flapAmountOld,entity.flapAmount);
            root.getChild("right_wing").zRot=-(Mth.sin(flap)+1)*amp;root.getChild("left_wing").zRot=(Mth.sin(flap)+1)*amp;
        }else if(kind==Kind.SHEEP&&entity.eatTicks()>0){int timer=entity.eatTicks();float bend=timer>=4&&timer<=36?1:timer<4?timer/4F:(40-timer)/4F;
            head.y+=bend*5;head.xRot=.6F+Mth.sin(timer*.45F)*.12F;}
    }
    @Override public void renderToBuffer(PoseStack pose,VertexConsumer buffer,int light,int overlay,int color){root.render(pose,buffer,light,overlay,color);}
}
