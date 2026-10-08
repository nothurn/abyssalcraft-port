package dev.abyssalport.client;

import dev.abyssalport.content.WastelandContent;
import dev.abyssalport.entity.DreadguardEntity;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;

public final class DreadguardRenderer extends MobRenderer<DreadguardEntity,DreadguardModel> {
    public DreadguardRenderer(EntityRendererProvider.Context context) { super(context,new DreadguardModel(context.bakeLayer(DreadguardModel.LAYER)),.6F); }
    @Override public ResourceLocation getTextureLocation(DreadguardEntity entity) { return WastelandContent.id("textures/entity/dreadguard.png"); }
}
