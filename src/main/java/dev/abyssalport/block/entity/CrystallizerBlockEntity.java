package dev.abyssalport.block.entity;

import java.util.List;
import dev.abyssalport.content.*;
import dev.abyssalport.menu.CrystallizerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class CrystallizerBlockEntity extends FuelProcessingBlockEntity {
    public CrystallizerBlockEntity(BlockPos pos,BlockState state) { super(CrystallizationContent.ENTITY.get(),pos,state,2); }
    public static int fuelTicks(ItemStack stack) {
        if(stack.is(DreadlandsContent.FRAGMENT.get())) return 100;
        if(stack.is(DreadProcessingContent.SHARD.get())) return 1000;
        if(stack.is(Items.BLAZE_POWDER)) return 1200;
        if(stack.is(Items.BLAZE_ROD)) return 2400;
        if(stack.is(CrystallizationContent.CARBON_CLUSTER.get())) return 1600;
        if(stack.is(CrystallizationContent.DENSE_CARBON_CLUSTER.get())) return 12800;
        if(stack.is(CrystallizationContent.material("carbon").crystal().get())) return 200;
        return stack.is(CrystallizationContent.material("carbon").clusterItem().get())?1800:0;
    }
    @Override protected int fuelDuration(ItemStack fuel) { return fuelTicks(fuel); }
    @Override protected ProcessingPlan resolve(ServerLevel level,ItemStack stack) {
        var input=new SingleRecipeInput(stack);
        var holder=level.getRecipeManager().getRecipeFor(CrystallizationContent.TYPE.get(),input,level).orElse(null);
        return holder==null?null:new ProcessingPlan(holder.id().toString(),List.of(holder.value().assemble(input,level.registryAccess()),
                holder.value().secondaryResult()),holder.value().experience());
    }
    @Override protected ItemStack inputRemainder(ItemStack input) {
        if(input.is(Items.WATER_BUCKET)) return new ItemStack(Items.BUCKET);
        return input.is(Items.POTION)?new ItemStack(Items.GLASS_BOTTLE):ItemStack.EMPTY;
    }
    @Override protected boolean canExtractContainer(int slot,ItemStack stack) { return stack.is(Items.BUCKET) || stack.is(Items.GLASS_BOTTLE); }
    public static void serverTick(Level level,BlockPos pos,BlockState state,CrystallizerBlockEntity be) { tickMachine(level,pos,state,be); }
    @Override public Component getDisplayName() { return Component.translatable("block.abyssalport.crystallizer"); }
    @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player) { return new CrystallizerMenu(id,inventory,this); }
}
