package dev.abyssalport.client;

import dev.abyssalport.content.WastelandContent;
import dev.abyssalport.entity.GhoulEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

public final class GhoulRenderer extends HumanoidMobRenderer<GhoulEntity,GhoulModel> {
    public GhoulRenderer(EntityRendererProvider.Context context) {
        super(context,new GhoulModel(context.bakeLayer(GhoulModel.LAYER)),.45F);
        addLayer(new HumanoidArmorLayer<>(this,new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR)),context.getModelManager()));
    }
    @Override public ResourceLocation getTextureLocation(GhoulEntity entity) {
        String skin=switch(entity.kind()) { case NORMAL->"ghoul"; case SHADOW->"shadow_ghoul"; case DREADED->"dreaded_ghoul";
            case DEPTHS->"depths_ghoul"+(entity.getGhoulType()==0?"":"_"+entity.getGhoulType()); };
        return WastelandContent.id("textures/entity/"+skin+".png");
    }
}
