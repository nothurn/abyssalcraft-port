package dev.abyssalport.client;

import dev.abyssalport.block.entity.CrystallizerBlockEntity;
import dev.abyssalport.menu.CrystallizerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.*;

@OnlyIn(Dist.CLIENT)
public final class CrystallizerScreen extends AbstractContainerScreen<CrystallizerMenu> {
    public CrystallizerScreen(CrystallizerMenu menu,Inventory inventory,Component title) {
        super(menu,inventory,title); imageWidth=176; imageHeight=192; inventoryLabelY=98;
    }
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {
        super.render(graphics,mouseX,mouseY,partialTick); renderTooltip(graphics,mouseX,mouseY);
    }
    @Override protected void renderBg(GuiGraphics graphics,float partialTick,int mouseX,int mouseY) {
        int x=leftPos,y=topPos;
        graphics.fill(x-2,y-2,x+178,y+194,0xFF55505A); graphics.fill(x,y,x+176,y+192,0xFF202329);
        for(var slot:menu.slots) {
            graphics.fill(x+slot.x-1,y+slot.y-1,x+slot.x+17,y+slot.y+17,0xFF55505A);
            graphics.fill(x+slot.x,y+slot.y,x+slot.x+16,y+slot.y+16,0xFF343840);
        }
        graphics.fill(x+68,y+49,x+96,y+63,0xFF343840);
        int width=Math.clamp(menu.progress(),0,CrystallizerBlockEntity.PROCESS_TICKS)*28/CrystallizerBlockEntity.PROCESS_TICKS;
        graphics.fill(x+68,y+49,x+68+width,y+63,0xFF8AB4C4);
        graphics.drawCenteredString(font,">",x+82,y+51,0xFFE9E0D5);
        graphics.fill(x+61,y+66,x+66,y+80,0xFF343840);
        int height=menu.totalBurnTime()>0?Math.clamp(menu.burnTime(),0,menu.totalBurnTime())*14/menu.totalBurnTime():0;
        graphics.fill(x+61,y+80-height,x+66,y+80,0xFFC7985F);
        graphics.fill(x+6,y+95,x+170,y+96,0xFF55505A);
    }
    @Override protected void renderLabels(GuiGraphics graphics,int mouseX,int mouseY) {
        graphics.drawString(font,title,8,6,0xFFE9E0D5,false);
        graphics.drawString(font,playerInventoryTitle,8,98,0xFFA9A7AC,false);
        graphics.drawCenteredString(font,Component.translatable("screen.abyssalport.machine.input"),48,21,0xFFA9A7AC);
        graphics.drawCenteredString(font,Component.translatable("screen.abyssalport.machine.fuel"),48,85,0xFFA9A7AC);
        graphics.drawCenteredString(font,Component.translatable("screen.abyssalport.machine.outputs"),127,36,0xFFA9A7AC);
    }
}
