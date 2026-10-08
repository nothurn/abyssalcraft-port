package dev.abyssalport.client;

import dev.abyssalport.content.*;
import dev.abyssalport.entity.CorruptedAnimalEntity;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;

public final class CorruptedAnimalRenderer extends MobRenderer<CorruptedAnimalEntity,CorruptedAnimalModel>{
    public CorruptedAnimalRenderer(EntityRendererProvider.Context context,CorruptedAnimalEntity.Kind kind){
        super(context,new CorruptedAnimalModel(context.bakeLayer(CorruptedAnimalModel.layer(kind)),kind),kind==CorruptedAnimalEntity.Kind.CHICKEN?.15F:.5F);
        if(kind==CorruptedAnimalEntity.Kind.SHEEP)addLayer(new CorruptedSheepSkinLayer(this,context));
    }
    @Override public ResourceLocation getTextureLocation(CorruptedAnimalEntity entity){return WastelandContent.id("textures/entity/"+MobEcologyContent.name(entity.kind(),entity.demon())+".png");}
}
