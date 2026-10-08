package dev.abyssalport.client;

import dev.abyssalport.content.WastelandContent;
import dev.abyssalport.entity.AbyssalZombieEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

public final class AbyssalZombieRenderer extends HumanoidMobRenderer<AbyssalZombieEntity, AbyssalZombieModel> {
    public AbyssalZombieRenderer(EntityRendererProvider.Context context) {
        super(context, new AbyssalZombieModel(context.bakeLayer(AbyssalZombieModel.LAYER)), .5F);
        addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR)), context.getModelManager()));
    }
    @Override public ResourceLocation getTextureLocation(AbyssalZombieEntity entity) { return WastelandContent.id("textures/entity/abyssal_zombie.png"); }
}
