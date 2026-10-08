package dev.abyssalport.client;

import dev.abyssalport.content.WastelandContent;
import dev.abyssalport.entity.GhoulEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

/** Own jointed silhouette and UVs, using native humanoid attack/equipment poses. */
public final class GhoulModel extends HumanoidModel<GhoulEntity> {
    public static final ModelLayerLocation LAYER=new ModelLayerLocation(WastelandContent.id("ghoul"),"main");
    private final ModelPart leftElbow,rightElbow,leftKnee,rightKnee;
    public GhoulModel(ModelPart root) {
        super(root); leftElbow=leftArm.getChild("elbow"); rightElbow=rightArm.getChild("elbow");
        leftKnee=leftLeg.getChild("knee"); rightKnee=rightLeg.getChild("knee");
    }
    public static LayerDefinition createLayer() {
        var mesh=new MeshDefinition(); var root=mesh.getRoot();
        root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,0).addBox(-4,-8,-4.5F,8,8,8),PartPose.offset(0,0,-1));
        root.addOrReplaceChild("hat",CubeListBuilder.create(),PartPose.ZERO);
        root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,16).addBox(-4,0,-2.5F,8,12,5),PartPose.ZERO);
        for(int side:new int[]{-1,1}) {
            boolean left=side==1; String prefix=left?"left":"right";
            var arm=root.addOrReplaceChild(prefix+"_arm",CubeListBuilder.create().texOffs(32,16).mirror(left)
                    .addBox(-1.5F,-2,-1.5F,3,7,3),PartPose.offset(side*5.5F,2,0));
            arm.addOrReplaceChild("elbow",CubeListBuilder.create().texOffs(32,28).mirror(left)
                    .addBox(-1.5F,0,-1.5F,3,7,3),PartPose.offset(0,5,0));
            var leg=root.addOrReplaceChild(prefix+"_leg",CubeListBuilder.create().texOffs(0,40).mirror(left)
                    .addBox(-1.5F,0,-1.5F,3,6,3),PartPose.offset(side*2,12,0));
            leg.addOrReplaceChild("knee",CubeListBuilder.create().texOffs(16,40).mirror(left)
                    .addBox(-1.5F,0,-1.5F,3,6,3),PartPose.offset(0,6,0));
        }
        return LayerDefinition.create(mesh,64,64);
    }
    @Override public void setupAnim(GhoulEntity entity,float phase,float amount,float age,float yaw,float pitch) {
        super.setupAnim(entity,phase,amount,age,Mth.clamp(yaw,-55,55),Mth.clamp(pitch,-30,30));
        AnimationUtils.animateZombieArms(leftArm,rightArm,entity.isAggressive(),attackTime,age);
        float stride=Mth.clamp(amount,0,1),cycle=phase*.6662F;
        // Positive knee angle folds a swing leg back, rather than rigidly seesawing
        // the whole leg. Only the upper thigh carries the native stance motion.
        leftKnee.xRot=Math.max(0,Mth.sin(cycle))*stride*.7F;
        rightKnee.xRot=Math.max(0,-Mth.sin(cycle))*stride*.7F;
        leftElbow.xRot=-.18F-Mth.sin(age*.06F)*.025F;
        rightElbow.xRot=-.18F+Mth.sin(age*.06F)*.025F;
    }
}
