package dev.abyssalport.client;

import dev.abyssalport.menu.EnergyContainerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** Original-free compact reservoir screen; the server owns slots and PE. */
@OnlyIn(Dist.CLIENT)
public final class EnergyContainerScreen extends AbstractContainerScreen<EnergyContainerMenu> {
    private static final int PANEL = 0xFF202329;
    private static final int BORDER = 0xFF55505A;
    private static final int SLOT = 0xFF343840;
    private static final int TEXT = 0xFFE9E0D5;
    private static final int MUTED = 0xFFA9A7AC;
    private static final int ENERGY = 0xFF7AAD85;

    public EnergyContainerScreen(EnergyContainerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 182;
        titleLabelX = 8;
        titleLabelY = 6;
        inventoryLabelX = 8;
        inventoryLabelY = 88;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.fill(x - 2, y - 2, x + imageWidth + 2, y + imageHeight + 2, BORDER);
        graphics.fill(x, y, x + imageWidth, y + imageHeight, PANEL);
        graphics.fill(x + 6, y + 19, x + imageWidth - 6, y + 20, BORDER);
        for (int slot = 0; slot < menu.slots.size(); slot++) {
            int sx = x + menu.slots.get(slot).x;
            int sy = y + menu.slots.get(slot).y;
            graphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, BORDER);
            graphics.fill(sx, sy, sx + 16, sy + 16, SLOT);
        }
        graphics.fill(x + 66, y + 38, x + 110, y + 54, SLOT);
        graphics.drawCenteredString(font, ">", x + 88, y + 42, ENERGY);
        graphics.fill(x + 8, y + 59, x + 168, y + 65, SLOT);
        int capacity = Math.max(1, menu.capacity());
        int filled = (int) ((long) Math.clamp(menu.energy(), 0, capacity) * 160 / capacity);
        graphics.fill(x + 8, y + 59, x + 8 + filled, y + 65, ENERGY);
        graphics.fill(x + 6, y + 82, x + imageWidth - 6, y + 83, BORDER);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        float titleScale = Math.min(1, (imageWidth - 16F) / Math.max(1, font.width(title)));
        graphics.pose().pushPose();
        graphics.pose().translate(titleLabelX, titleLabelY, 0);
        graphics.pose().scale(titleScale, titleScale, 1);
        graphics.drawString(font, title, 0, 0, TEXT, false);
        graphics.pose().popPose();
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, MUTED, false);
        graphics.drawCenteredString(font, Component.translatable("screen.abyssalport.container.input"),
                52, 26, MUTED);
        graphics.drawCenteredString(font, Component.translatable("screen.abyssalport.container.output"),
                124, 26, MUTED);
        graphics.drawCenteredString(font, menu.energy() + " / " + menu.capacity() + " PE",
                imageWidth / 2, 67, TEXT);
    }
}
