package dev.abyssalport.block.entity;

import dev.abyssalport.content.*;
import dev.abyssalport.menu.TransmutatorMenu;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Source-informed fuel machine with native sided inventory and atomic input/output mutation. */
public final class TransmutatorBlockEntity extends FuelProcessingBlockEntity {
    public TransmutatorBlockEntity(BlockPos pos,BlockState state) { super(WastelandProcessingContent.ENTITY.get(),pos,state,1); }
    public static int fuelTicks(ItemStack stack) {
        if(stack.isEmpty()) return 0;
        var item=stack.getItem();
        if(item==WastelandMobsContent.PLAGUED_FLESH.get()) return 100;
        if(item==WastelandProcessingContent.BRICK.get() || item==CoraliumContent.GEM.get()) return 200;
        var clusters=java.util.List.of(CoraliumContent.CLUSTER_2,CoraliumContent.CLUSTER_3,CoraliumContent.CLUSTER_4,CoraliumContent.CLUSTER_5,
                CoraliumContent.CLUSTER_6,CoraliumContent.CLUSTER_7,CoraliumContent.CLUSTER_8,CoraliumContent.CLUSTER_9);
        for(int index=0;index<clusters.size();index++) if(item==clusters.get(index).get()) return (index+2)*200;
        if(item==CoraliumContent.PEARL.get()) return 2000;
        if(item==CoraliumContent.TRANSMUTATION_GEM.get()) return 10000;
        if(item==WastelandMobsContent.CORALIUM_CHUNK.get()) return 16200;
        if(item==WastelandContent.CORALIUM_BUCKET.get()) return 20000;
        if(item==Items.BLAZE_POWDER) return 1200;
        return item==Items.BLAZE_ROD?2400:0;
    }
    public static void serverTick(Level world,BlockPos pos,BlockState state,TransmutatorBlockEntity be) {
        tickMachine(world,pos,state,be);
    }
    @Override protected ProcessingPlan resolve(ServerLevel level,ItemStack stack) {
        var input=new SingleRecipeInput(stack);
        var holder=level.getRecipeManager().getRecipeFor(WastelandProcessingContent.TRANSMUTATION.get(),input,level).orElse(null);
        return holder==null?null:new ProcessingPlan(holder.id().toString(),java.util.List.of(holder.value().assemble(input,level.registryAccess())),holder.value().experience());
    }
    @Override protected int fuelDuration(ItemStack stack) { return fuelTicks(stack); }
    @Override protected ItemStack fuelRemainder(ItemStack stack) {
        return stack.is(WastelandContent.CORALIUM_BUCKET.get())?new ItemStack(Items.BUCKET):ItemStack.EMPTY;
    }
    @Override public int[] getSlotsForFace(Direction face) { return face==Direction.UP?new int[]{0}:face==Direction.DOWN?new int[]{2,1}:new int[]{1}; }
    @Override public Component getDisplayName() { return Component.translatable("block.abyssalport.transmutator"); }
    @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player) { return new TransmutatorMenu(id,inventory,this); }
}
