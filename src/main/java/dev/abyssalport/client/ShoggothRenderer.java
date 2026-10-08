package dev.abyssalport.client;

import dev.abyssalport.entity.ShoggothEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class ShoggothRenderer extends MobRenderer<ShoggothEntity, ShoggothModel> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("abyssalport", "textures/entity/lesser_shoggoth.png");

    public ShoggothRenderer(EntityRendererProvider.Context context) {
        super(context, new ShoggothModel(context.bakeLayer(ShoggothModel.LAYER)), 0.8F);
    }

    @Override
    public ResourceLocation getTextureLocation(ShoggothEntity entity) {
        return entity.habitat()==ShoggothEntity.Habitat.OVERWORLD?TEXTURE:
                ResourceLocation.fromNamespaceAndPath("abyssalport","textures/entity/"+(entity.habitat()==ShoggothEntity.Habitat.ABYSSAL?"abyssal":"dreaded")+"_shoggoth.png");
    }
    @Override protected void scale(ShoggothEntity entity,com.mojang.blaze3d.vertex.PoseStack poses,float partialTick) {
        float size=entity.kind()==ShoggothEntity.Kind.LESSER?.95F:entity.kind()==ShoggothEntity.Kind.NORMAL?1.3F:1.9F;
        poses.scale(size,size,size); shadowRadius=entity.kind().width*.5F;
    }
}
