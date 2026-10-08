package dev.abyssalport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.abyssalport.block.entity.RitualPedestalBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Shows the actual saved offering rather than a misleading fixed model. */
public final class RitualPedestalRenderer implements BlockEntityRenderer<RitualPedestalBlockEntity> {
    private final ItemRenderer itemRenderer;

    public RitualPedestalRenderer(BlockEntityRendererProvider.Context context) {
        itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(RitualPedestalBlockEntity pedestal, float partialTick, PoseStack pose,
            MultiBufferSource buffers, int light, int overlay) {
        ItemStack offering = pedestal.getOffering();
        if (offering.isEmpty()) {
            return;
        }
        pose.pushPose();
        pose.translate(0.5, 1.02, 0.5);
        pose.scale(0.55F, 0.55F, 0.55F);
        itemRenderer.renderStatic(offering, ItemDisplayContext.GROUND, light, overlay, pose,
                buffers, pedestal.getLevel(), 0);
        pose.popPose();
    }
}
