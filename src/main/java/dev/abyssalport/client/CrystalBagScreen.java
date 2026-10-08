package dev.abyssalport.client;

import dev.abyssalport.menu.CrystalBagMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.*;

@OnlyIn(Dist.CLIENT)
public final class CrystalBagScreen extends AbstractContainerScreen<CrystalBagMenu> {
    public CrystalBagScreen(CrystalBagMenu menu,Inventory inventory,Component title) {
        super(menu,inventory,title); imageWidth=176; imageHeight=menu.rows()*18+131; inventoryLabelY=menu.rows()*18+37;
    }
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {
        super.render(graphics,mouseX,mouseY,partialTick); renderTooltip(graphics,mouseX,mouseY);
    }
    @Override protected void renderBg(GuiGraphics graphics,float partialTick,int mouseX,int mouseY) {
        int x=leftPos,y=topPos;
        graphics.fill(x-2,y-2,x+178,y+imageHeight+2,0xFF55505A); graphics.fill(x,y,x+176,y+imageHeight,0xFF202329);
        for(var slot:menu.slots) {
            graphics.fill(x+slot.x-1,y+slot.y-1,x+slot.x+17,y+slot.y+17,0xFF55505A);
            graphics.fill(x+slot.x,y+slot.y,x+slot.x+16,y+slot.y+16,0xFF343840);
        }
        graphics.fill(x+6,y+menu.rows()*18+33,x+170,y+menu.rows()*18+34,0xFF55505A);
    }
    @Override protected void renderLabels(GuiGraphics graphics,int mouseX,int mouseY) {
        graphics.drawString(font,title,8,6,0xFFE9E0D5,false);
        graphics.drawString(font,playerInventoryTitle,8,inventoryLabelY,0xFFA9A7AC,false);
        graphics.drawString(font,Component.translatable("screen.abyssalport.crystal_bag.capacity",menu.capacity()),8,menu.rows()*18+20,0xFF8AB4C4,false);
    }
}
