package dev.abyssalport.client;

import com.mojang.blaze3d.vertex.*;
import dev.abyssalport.content.WastelandContent;
import dev.abyssalport.entity.DreadguardEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

/** Independently authored three-block elite, with articulated knees/elbows and a spray jaw. */
public final class DreadguardModel extends EntityModel<DreadguardEntity> {
    public static final ModelLayerLocation LAYER=new ModelLayerLocation(WastelandContent.id("dreadguard"),"main");
    private final ModelPart root,head,rightArm,leftArm,rightLeg,leftLeg;
    public DreadguardModel(ModelPart root) {
        this.root=root; head=root.getChild("head"); rightArm=root.getChild("right_arm"); leftArm=root.getChild("left_arm");
        rightLeg=root.getChild("right_leg"); leftLeg=root.getChild("left_leg");
    }
    public static LayerDefinition createLayer() {
        var mesh=new MeshDefinition(); var root=mesh.getRoot();
        var head=root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,0).addBox(-6,-10,-5,12,10,10),PartPose.offset(0,-14,0));
        head.addOrReplaceChild("jaw",CubeListBuilder.create().texOffs(80,20).addBox(-3,0,-4,6,2,7),PartPose.offset(0,-2,-2));
        root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,26).addBox(-7,0,-4,14,18,8),PartPose.offset(0,-14,0));
        root.addOrReplaceChild("hip",CubeListBuilder.create().texOffs(48,26).addBox(-6,0,-4,12,4,8),PartPose.offset(0,4,0));
        for(String side:new String[]{"right","left"}) {
            float sign=side.equals("right")?-1:1;
            var arm=root.addOrReplaceChild(side+"_arm",CubeListBuilder.create().texOffs(0,54).addBox(-2,-1,-2.5F,4,10,5),PartPose.offset(sign*9,-12,0));
            arm.addOrReplaceChild("plate",CubeListBuilder.create().texOffs(70,40).addBox(-3,-2,-3.5F,6,4,7),PartPose.ZERO);
            arm.addOrReplaceChild("elbow",CubeListBuilder.create().texOffs(24,72).addBox(-2.5F,0,-2.5F,5,14,5),PartPose.offset(0,9,0));
            var leg=root.addOrReplaceChild(side+"_leg",CubeListBuilder.create().texOffs(0,96).addBox(-3,0,-3,6,8,6),PartPose.offset(sign*3,8,0));
            var shin=leg.addOrReplaceChild("shin",CubeListBuilder.create().texOffs(28,112).addBox(-2.5F,0,-2.5F,5,8,5),PartPose.offset(0,8,0));
            shin.addOrReplaceChild("foot",CubeListBuilder.create().texOffs(80,98).addBox(-3,-3,-6,6,3,9),PartPose.offset(0,8,0));
        }
        return LayerDefinition.create(mesh,128,128);
    }
    @Override public void setupAnim(DreadguardEntity entity,float swing,float amount,float age,float yaw,float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        head.yRot=Mth.clamp(yaw,-55,55)*Mth.DEG_TO_RAD; head.xRot=Mth.clamp(pitch,-25,35)*Mth.DEG_TO_RAD;
        float stride=entity.sprayTimer()>0?0:Mth.clamp(amount,0,1),phase=swing*.55F;
        leftLeg.xRot=Mth.cos(phase)*.48F*stride; rightLeg.xRot=-leftLeg.xRot;
        leftLeg.getChild("shin").xRot=Math.max(0,Mth.sin(phase))*.7F*stride;
        rightLeg.getChild("shin").xRot=Math.max(0,-Mth.sin(phase))*.7F*stride;
        rightArm.xRot=-rightLeg.xRot*.6F; leftArm.xRot=-leftLeg.xRot*.6F;
        rightArm.getChild("elbow").xRot=-.12F; leftArm.getChild("elbow").xRot=-.12F;
        rightArm.xRot-=Mth.sin(Mth.sqrt(attackTime)*Mth.PI)*1.2F;
        if(entity.sprayTimer()>0) { head.getChild("jaw").xRot=.45F; rightArm.xRot=-.2F; leftArm.xRot=-.2F; }
    }
    @Override public void renderToBuffer(PoseStack pose,VertexConsumer buffer,int light,int overlay,int color) { root.render(pose,buffer,light,overlay,color); }
}
