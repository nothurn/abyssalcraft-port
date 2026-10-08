package dev.abyssalport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.abyssalport.content.WastelandContent;
import dev.abyssalport.entity.AbyssalDragonEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class AbyssalDragonRenderer extends MobRenderer<AbyssalDragonEntity, AbyssalDragonModel> {
    public AbyssalDragonRenderer(EntityRendererProvider.Context context) { super(context, new AbyssalDragonModel(context.bakeLayer(AbyssalDragonModel.LAYER)), 1); }
    @Override protected void scale(AbyssalDragonEntity entity, PoseStack pose, float partialTick) {
        float scale = entity.isBoss() ? 3 : 1; pose.scale(scale, scale, scale);
    }
    @Override public ResourceLocation getTextureLocation(AbyssalDragonEntity entity) {
        return WastelandContent.id("textures/entity/"+(entity.isBoss() ? "asorah" : "spectral_dragon")+".png");
    }
    @Override protected float getFlipDegrees(AbyssalDragonEntity entity) { return 0; }
}
