package dev.abyssalport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.abyssalport.content.WastelandContent;
import dev.abyssalport.entity.CorruptedAnimalEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.Mth;

/** Source player-skin wool behaviour, using an owned skin-UV rig and native asynchronous texture cache. */
public final class CorruptedSheepSkinLayer extends RenderLayer<CorruptedAnimalEntity,CorruptedAnimalModel>{
    public static final ModelLayerLocation LAYER=new ModelLayerLocation(WastelandContent.id("evil_sheep_skin"),"main");
    private final ModelPart root,head,body;private final ModelPart[] legs=new ModelPart[4];
    public CorruptedSheepSkinLayer(RenderLayerParent<CorruptedAnimalEntity,CorruptedAnimalModel> parent,EntityRendererProvider.Context context){super(parent);root=context.bakeLayer(LAYER);head=root.getChild("head");body=root.getChild("body");for(int i=0;i<4;i++)legs[i]=root.getChild("leg_"+i);}
    public static LayerDefinition createLayer(){var mesh=new MeshDefinition();var root=mesh.getRoot();
        root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,0).addBox(-4,-4,-8,8,8,8),PartPose.offset(0,7,-7));
        root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(16,16).addBox(-4,-8,-4,8,12,4,new CubeDeformation(.9F)),PartPose.offsetAndRotation(0,7,0,Mth.HALF_PI,0,0));
        for(int i=0;i<4;i++)root.addOrReplaceChild("leg_"+i,CubeListBuilder.create().texOffs(i%2==0?0:40,16).addBox(-2,0,-2,4,10,4),PartPose.offset(i%2==0?-3:3,14,i<2?-5:6));
        return LayerDefinition.create(mesh,64,64);
    }
    @Override public void render(PoseStack pose,MultiBufferSource buffer,int light,CorruptedAnimalEntity sheep,float swing,float amount,float partial,float age,float yaw,float pitch){
        var profile=sheep.killedProfile();if(sheep.demon()||sheep.isInvisible()||profile==null)return;
        root.getAllParts().forEach(ModelPart::resetPose);head.yRot=Mth.clamp(yaw,-55,55)*Mth.DEG_TO_RAD;head.xRot=Mth.clamp(pitch,-35,35)*Mth.DEG_TO_RAD;
        for(int i=0;i<4;i++)legs[i].xRot=Mth.cos(swing*.6662F+((i==1||i==2)?Mth.PI:0))*1.15F*Mth.clamp(amount,0,1);
        if(sheep.eatTicks()>0){int timer=sheep.eatTicks();head.y+=(timer>=4&&timer<=36?1:timer<4?timer/4F:(40-timer)/4F)*5;head.xRot=.6F+Mth.sin(timer*.45F)*.12F;}
        var skin=Minecraft.getInstance().getSkinManager().getInsecureSkin(profile);
        root.render(pose,buffer.getBuffer(RenderType.entityCutoutNoCull(skin.texture())),light,LivingEntityRenderer.getOverlayCoords(sheep,0));
    }
}
