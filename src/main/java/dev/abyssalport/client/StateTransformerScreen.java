package dev.abyssalport.client;

import dev.abyssalport.block.entity.StateTransformerBlockEntity;
import dev.abyssalport.menu.StateTransformerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.*;

@OnlyIn(Dist.CLIENT)
public final class StateTransformerScreen extends AbstractContainerScreen<StateTransformerMenu> {
    private Button modeButton;
    public StateTransformerScreen(StateTransformerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth = 176; imageHeight = 238; inventoryLabelY = 144;
    }
    private Component modeLabel() { return Component.translatable("screen.abyssalport.state_transformer." + (menu.mode() == 0 ? "pack" : "unpack")); }
    @Override protected void init() {
        super.init(); modeButton = addRenderableWidget(Button.builder(modeLabel(), button -> {
            if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1 - menu.mode());
        }).bounds(leftPos + 2, topPos + 96, 40, 20).build());
    }
    @Override protected void containerTick() {
        super.containerTick(); modeButton.active = menu.progress() == 0; modeButton.setMessage(modeLabel());
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick); renderTooltip(graphics, mouseX, mouseY);
    }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        graphics.fill(x - 2, y - 2, x + 178, y + 240, 0xFF55505A); graphics.fill(x, y, x + 176, y + 238, 0xFF202329);
        for (var slot : menu.slots) {
            graphics.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, 0xFF55505A);
            graphics.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, 0xFF343840);
        }
        graphics.fill(x + 8, y + 43, x + 37, y + 57, 0xFF343840);
        int width = Math.clamp(menu.progress(), 0, StateTransformerBlockEntity.PROCESS_TICKS) * 29 / StateTransformerBlockEntity.PROCESS_TICKS;
        graphics.fill(x + 8, y + 43, x + 8 + width, y + 57, 0xFF7AAD85);
        graphics.drawCenteredString(font, menu.mode() == 0 ? ">" : "<", x + 22, y + 45, 0xFFE9E0D5);
        graphics.fill(x + 6, y + 140, x + 170, y + 141, 0xFF55505A);
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, title, 88, 6, 0xFFE9E0D5);
        graphics.drawString(font, playerInventoryTitle, 8, 144, 0xFFA9A7AC, false);
    }
}
