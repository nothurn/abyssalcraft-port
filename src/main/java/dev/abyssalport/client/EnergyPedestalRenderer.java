package dev.abyssalport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.abyssalport.block.EnergyPedestalBlock;
import dev.abyssalport.block.entity.EnergyPedestalBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;

/** Render the actual synchronized singleton rather than a fixed decorative item. */
public final class EnergyPedestalRenderer implements BlockEntityRenderer<EnergyPedestalBlockEntity> {
    private final ItemRenderer items;
    public EnergyPedestalRenderer(BlockEntityRendererProvider.Context context) { items = context.getItemRenderer(); }
    @Override public void render(EnergyPedestalBlockEntity pedestal, float partialTick, PoseStack pose,
            MultiBufferSource buffers, int light, int overlay) {
        if (pedestal.item().isEmpty()) return;
        pose.pushPose(); pose.translate(.5, pedestal.displayItemHeight(), .5);
        if (pedestal.getBlockState().hasProperty(EnergyPedestalBlock.TILTED) && pedestal.getBlockState().getValue(EnergyPedestalBlock.TILTED)) pose.mulPose(Axis.YP.rotationDegrees(45));
        pose.scale(.55F, .55F, .55F);
        items.renderStatic(pedestal.item(), ItemDisplayContext.GROUND, light, overlay, pose, buffers, pedestal.getLevel(), 0);
        pose.popPose();
    }
}
