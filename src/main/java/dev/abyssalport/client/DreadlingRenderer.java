package dev.abyssalport.client;

import dev.abyssalport.content.WastelandContent;
import dev.abyssalport.entity.DreadlingEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class DreadlingRenderer extends MobRenderer<DreadlingEntity, DreadlingModel> {
    public DreadlingRenderer(EntityRendererProvider.Context context) {
        super(context, new DreadlingModel(context.bakeLayer(DreadlingModel.LAYER)), .4F);
    }
    @Override public ResourceLocation getTextureLocation(DreadlingEntity entity) { return WastelandContent.id("textures/entity/dreadling.png"); }
}
