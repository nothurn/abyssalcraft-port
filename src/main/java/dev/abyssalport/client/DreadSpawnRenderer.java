package dev.abyssalport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.abyssalport.content.WastelandContent;
import dev.abyssalport.entity.DreadSpawnEntity;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;

public final class DreadSpawnRenderer extends MobRenderer<DreadSpawnEntity,DreadSpawnModel> {
    public DreadSpawnRenderer(EntityRendererProvider.Context context) { super(context,new DreadSpawnModel(context.bakeLayer(DreadSpawnModel.LAYER)),.35F); }
    @Override protected void scale(DreadSpawnEntity entity,PoseStack pose,float partialTick) {
        float scale=switch(entity.kind()) { case SMALL->.533333F; case GREATER->1.066667F; case BEAST->1.6F; };
        pose.scale(scale,scale,scale);
    }
    @Override public ResourceLocation getTextureLocation(DreadSpawnEntity entity) { return WastelandContent.id("textures/entity/"+switch(entity.kind()) {
        case SMALL->"dread_spawn"; case GREATER->"greater_dread_spawn"; case BEAST->"lesser_dreadbeast"; }+".png"); }
}
