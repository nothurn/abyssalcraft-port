package dev.abyssalport.client;

import dev.abyssalport.menu.EnergyDepositionerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.*;

/** Own GUI pixels; source two-slot layout, no copied original GUI texture. */
@OnlyIn(Dist.CLIENT)
public final class EnergyDepositionerScreen extends AbstractContainerScreen<EnergyDepositionerMenu> {
    public EnergyDepositionerScreen(EnergyDepositionerMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); imageWidth = 176; imageHeight = 166; inventoryLabelY = 72; }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) { super.render(graphics, mouseX, mouseY, partialTick); renderTooltip(graphics, mouseX, mouseY); }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        graphics.fill(x - 2, y - 2, x + 178, y + 168, 0xFF55505A); graphics.fill(x, y, x + 176, y + 166, 0xFF202329);
        for (var slot : menu.slots) {
            graphics.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, 0xFF55505A);
            graphics.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, 0xFF343840);
        }
        graphics.fill(x + 76, y + 40, x + 100, y + 54, 0xFF343840);
        graphics.fill(x + 76, y + 40, x + 76 + Math.clamp(menu.progress(), 0, 200) * 24 / 200, y + 54, 0xFF7AAD85);
        graphics.drawCenteredString(font, ">", x + 88, y + 42, 0xFFE9E0D5);
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, title, 88, 6, 0xFFE9E0D5);
        graphics.drawCenteredString(font, menu.energy() + " / 10000 PE", 88, 20, 0xFF7AAD85);
        graphics.drawString(font, playerInventoryTitle, 8, 72, 0xFFA9A7AC, false);
    }
}
