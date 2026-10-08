package dev.abyssalport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.abyssalport.block.entity.RitualAltarBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;

/** The central infusion ingredient is synchronized and rendered just like pedestal offerings. */
public final class RitualAltarRenderer implements BlockEntityRenderer<RitualAltarBlockEntity> {
    private final ItemRenderer items;

    public RitualAltarRenderer(BlockEntityRendererProvider.Context context) { items = context.getItemRenderer(); }

    @Override
    public void render(RitualAltarBlockEntity altar, float partialTick, PoseStack pose,
            MultiBufferSource buffers, int light, int overlay) {
        if (altar.getInput().isEmpty()) return;
        pose.pushPose();
        pose.translate(0.5, 1.02, 0.5);
        pose.scale(0.65F, 0.65F, 0.65F);
        items.renderStatic(altar.getInput(), ItemDisplayContext.GROUND, light, overlay, pose,
                buffers, altar.getLevel(), 0);
        pose.popPose();
    }
}
