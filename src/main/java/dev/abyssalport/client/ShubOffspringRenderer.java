package dev.abyssalport.client;

import dev.abyssalport.content.WastelandContent;
import dev.abyssalport.entity.ShubOffspringEntity;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;

public final class ShubOffspringRenderer extends MobRenderer<ShubOffspringEntity,ShubOffspringModel>{
    public ShubOffspringRenderer(EntityRendererProvider.Context context){super(context,new ShubOffspringModel(context.bakeLayer(ShubOffspringModel.LAYER)),.5F);}
    @Override public ResourceLocation getTextureLocation(ShubOffspringEntity entity){return WastelandContent.id("textures/entity/shub_offspring.png");}
}
