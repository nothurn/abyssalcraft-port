package dev.abyssalport.block.entity;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Shared transaction/persistence rules for machines with one input, one fuel and fixed output slots. */
public abstract class FuelProcessingBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {
    public static final int PROCESS_TICKS=200;
    private final NonNullList<ItemStack> items;
    private final int outputs;
    private int burnTime,totalBurnTime,progress;
    private float pendingExperience,activeExperience;
    private String activeRecipe="";
    private ItemStack activeInput=ItemStack.EMPTY;
    private final NonNullList<ItemStack> activeOutputs;

    protected FuelProcessingBlockEntity(BlockEntityType<?> type,BlockPos pos,BlockState state,int outputs) {
        super(type,pos,state); this.outputs=outputs;
        items=NonNullList.withSize(outputs+2,ItemStack.EMPTY);
        activeOutputs=NonNullList.withSize(outputs,ItemStack.EMPTY);
    }
    protected record ProcessingPlan(String id,List<ItemStack> results,float experience) { }
    protected abstract ProcessingPlan resolve(ServerLevel level,ItemStack input);
    protected abstract int fuelDuration(ItemStack fuel);
    protected ItemStack inputRemainder(ItemStack input) { return ItemStack.EMPTY; }
    protected ItemStack fuelRemainder(ItemStack fuel) { return ItemStack.EMPTY; }
    protected boolean canExtractContainer(int slot,ItemStack stack) { return stack.is(Items.BUCKET); }
    public int burnTime() { return burnTime; }
    public int totalBurnTime() { return totalBurnTime; }
    public int progress() { return progress; }
    public float pendingExperience() { return pendingExperience; }

    protected static void tickMachine(Level world,BlockPos pos,BlockState state,FuelProcessingBlockEntity be) {
        if(!(world instanceof ServerLevel level)) return;
        boolean wasLit=be.burnTime>0;
        if(be.burnTime>0) { be.burnTime--; be.setChanged(); }
        var plan=be.resolve(level,be.items.get(0));
        boolean changed=!(plan==null?"":plan.id).equals(be.activeRecipe)
                || !ItemStack.isSameItemSameComponents(be.activeInput,be.items.get(0)) || (plan==null?0:plan.experience)!=be.activeExperience;
        for(int index=0;!changed && index<be.outputs;index++)
            changed=!ItemStack.matches(be.activeOutputs.get(index),plan==null?ItemStack.EMPTY:plan.results.get(index));
        if(changed) {
            be.progress=0; be.activeRecipe=plan==null?"":plan.id;
            be.activeInput=be.items.get(0).copyWithCount(1); be.activeExperience=plan==null?0:plan.experience;
            for(int index=0;index<be.outputs;index++) be.activeOutputs.set(index,plan==null?ItemStack.EMPTY:plan.results.get(index).copy());
            be.setChanged();
        }
        var remainder=be.inputRemainder(be.items.get(0));
        // Modded stackable containers must not silently lose their returned container.
        boolean fits=plan!=null && !plan.results.getFirst().isEmpty() && (remainder.isEmpty() || be.items.get(0).getCount()==1);
        for(int index=0;fits && index<be.outputs;index++) {
            var output=plan.results.get(index); var stored=be.items.get(index+2);
            fits=output.isEmpty() || output.getCount()<=output.getMaxStackSize() && (stored.isEmpty()
                    || ItemStack.isSameItemSameComponents(stored,output) && stored.getCount()+output.getCount()<=output.getMaxStackSize());
        }
        if(fits && be.burnTime==0) {
            var fuel=be.items.get(1); int ticks=be.fuelDuration(fuel); var fuelRest=be.fuelRemainder(fuel);
            if(ticks>0 && (fuelRest.isEmpty() || fuel.getCount()==1)) {
                fuel.shrink(1); if(fuel.isEmpty()) be.items.set(1,fuelRest);
                be.burnTime=ticks; be.totalBurnTime=ticks; be.setChanged();
            }
        }
        if(fits && be.burnTime>0) {
            if(++be.progress>=PROCESS_TICKS) {
                for(int index=0;index<be.outputs;index++) {
                    var result=plan.results.get(index); if(result.isEmpty()) continue;
                    if(be.items.get(index+2).isEmpty()) be.items.set(index+2,result.copy()); else be.items.get(index+2).grow(result.getCount());
                }
                be.items.get(0).shrink(1); if(be.items.get(0).isEmpty()) be.items.set(0,remainder);
                be.progress=0; be.pendingExperience=Math.min(1000000F,be.pendingExperience+plan.experience);
            }
            be.setChanged();
        } else if(be.progress!=0) { be.progress=0; be.setChanged(); }
        if(wasLit!=(be.burnTime>0)) level.setBlock(pos,state.setValue(BlockStateProperties.LIT,be.burnTime>0),3);
    }
    public void payExperience() {
        if(!(level instanceof ServerLevel server)) return;
        int whole=(int)Math.floor(pendingExperience); if(whole==0) return;
        pendingExperience-=whole; ExperienceOrb.award(server,worldPosition.above().getCenter(),whole); setChanged();
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.saveAdditional(tag,registries); ContainerHelper.saveAllItems(tag,items,registries);
        tag.putInt("BurnTime",burnTime); tag.putInt("TotalBurnTime",totalBurnTime); tag.putInt("Progress",progress);
        tag.putFloat("PendingExperience",pendingExperience); tag.putString("ActiveRecipe",activeRecipe);
        if(!activeInput.isEmpty()) tag.put("ActiveInput",activeInput.save(registries));
        // Preserve the original Transmutator save key for its first output.
        for(int index=0;index<outputs;index++) if(!activeOutputs.get(index).isEmpty())
            tag.put(outputKey(index),activeOutputs.get(index).save(registries));
        tag.putFloat("ActiveExperience",activeExperience);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.loadAdditional(tag,registries); items.clear(); ContainerHelper.loadAllItems(tag,items,registries);
        burnTime=Math.clamp(tag.getInt("BurnTime"),0,20000); totalBurnTime=Math.clamp(tag.getInt("TotalBurnTime"),0,20000);
        progress=Math.clamp(tag.getInt("Progress"),0,PROCESS_TICKS-1);
        float experience=tag.getFloat("PendingExperience"); pendingExperience=Float.isFinite(experience)?Math.clamp(experience,0,1000000):0;
        activeRecipe=tag.getString("ActiveRecipe"); activeInput=readStack(tag,"ActiveInput",registries);
        for(int index=0;index<outputs;index++) activeOutputs.set(index,readStack(tag,outputKey(index),registries));
        activeExperience=tag.getFloat("ActiveExperience");
    }
    private static String outputKey(int index) { return index==0?"ActiveOutput":"ActiveOutput"+index; }
    private static ItemStack readStack(CompoundTag tag,String key,HolderLookup.Provider registries) {
        return tag.contains(key)?ItemStack.parse(registries,tag.getCompound(key)).orElse(ItemStack.EMPTY):ItemStack.EMPTY;
    }
    @Override public int getContainerSize() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot,int count) { var result=ContainerHelper.removeItem(items,slot,count); if(!result.isEmpty()) setChanged(); return result; }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items,slot); }
    @Override public void setItem(int slot,ItemStack stack) { items.set(slot,stack); stack.limitSize(stack.getMaxStackSize()); setChanged(); }
    @Override public void clearContent() { items.clear(); progress=0; setChanged(); }
    @Override public boolean stillValid(Player player) { return level!=null && level.getBlockEntity(worldPosition)==this && player.distanceToSqr(worldPosition.getCenter())<=64; }
    @Override public boolean canPlaceItem(int slot,ItemStack stack) { return slot==0 || slot==1 && fuelDuration(stack)>0; }
    @Override public int[] getSlotsForFace(Direction face) {
        if(face==Direction.UP) return new int[]{0};
        if(face!=Direction.DOWN) return new int[]{1};
        var slots=new int[outputs+2]; for(int index=0;index<outputs;index++) slots[index]=index+2;
        slots[outputs]=1; slots[outputs+1]=0; return slots;
    }
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction face) {
        // Bottom extraction slots must never become an extra insertion path.
        return face!=Direction.DOWN && canPlaceItem(slot,stack);
    }
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction face) {
        return slot>=2 || face==Direction.DOWN && canExtractContainer(slot,stack);
    }
}
