package dev.abyssalport.client;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.entity.ShadowCreatureEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class ShadowCreatureRenderer extends MobRenderer<ShadowCreatureEntity, ShadowCreatureModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AbyssalPort.MOD_ID, "textures/entity/shadow_creature.png");
    private final ResourceLocation texture;

    public ShadowCreatureRenderer(EntityRendererProvider.Context context) {
        this(context, ShadowCreatureEntity.Kind.CREATURE);
    }
    public ShadowCreatureRenderer(EntityRendererProvider.Context context, ShadowCreatureEntity.Kind kind) {
        super(context, new ShadowCreatureModel(context.bakeLayer(switch(kind) {
            case CREATURE -> ShadowCreatureModel.LAYER; case MONSTER -> ShadowCreatureModel.MONSTER_LAYER;
            case BEAST -> ShadowCreatureModel.BEAST_LAYER; })), kind==ShadowCreatureEntity.Kind.BEAST?.6F:.3F);
        texture=kind==ShadowCreatureEntity.Kind.CREATURE?TEXTURE:ResourceLocation.fromNamespaceAndPath(
            AbyssalPort.MOD_ID,"textures/entity/"+(kind==ShadowCreatureEntity.Kind.BEAST?"shadow_beast":"shadow_monster")+".png");
        float headScale=kind==ShadowCreatureEntity.Kind.CREATURE?.75F:kind==ShadowCreatureEntity.Kind.BEAST?1.25F:1;
        addLayer(new net.minecraft.client.renderer.entity.layers.CustomHeadLayer<>(this,context.getModelSet(),
                headScale,headScale,headScale,context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(ShadowCreatureEntity entity) {
        return texture;
    }
}
