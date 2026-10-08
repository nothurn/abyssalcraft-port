package dev.abyssalport.block;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;

/** Source reduced-fire algorithm adapted from Shinoow's BlockMimicFire (2012-2026, LGPL-3.0). Native Minecraft visuals. */
public final class MimicFireBlock extends FireBlock {
    public MimicFireBlock(BlockBehaviour.Properties properties){super(properties);}
    @Override public int getBurnOdds(BlockState state){return ((FireBlock)Blocks.FIRE).getBurnOdds(state);}
    @Override public int getIgniteOdds(BlockState state){return ((FireBlock)Blocks.FIRE).getIgniteOdds(state);}
    @Override protected void onPlace(BlockState state,Level level,BlockPos pos,BlockState previous,boolean moving){
        super.onPlace(state,level,pos,previous,moving);level.scheduleTick(pos,this,1);}
    private static boolean wet(ServerLevel level,BlockPos pos){if(level.isRainingAt(pos))return true;
        for(Direction dir:Direction.Plane.HORIZONTAL)if(level.hasChunkAt(pos.relative(dir))&&level.isRainingAt(pos.relative(dir)))return true;return false;}
    private static boolean neighborFuel(ServerLevel level,BlockPos pos){for(Direction dir:Direction.values()){
        var next=pos.relative(dir);if(level.hasChunkAt(next)&&level.getBlockState(next).isFlammable(level,next,dir.getOpposite()))return true;}return false;}
    private static int encouragement(ServerLevel level,BlockPos pos){if(!level.hasChunkAt(pos)||!level.isEmptyBlock(pos))return 0;int fuel=0;
        for(Direction dir:Direction.values()){var next=pos.relative(dir);if(level.hasChunkAt(next))fuel=Math.max(fuel,level.getBlockState(next).getFlammability(level,next,dir.getOpposite()));}return fuel;}
    private void burn(ServerLevel level,BlockPos pos,int chance,RandomSource random,int age,Direction face){if(!level.hasChunkAt(pos))return;
        var old=level.getBlockState(pos);if(random.nextInt(chance)>=old.getFlammability(level,pos,face))return;
        old.onCaughtFire(level,pos,face,null);
        if(random.nextInt(age+10)<5&&!level.isRainingAt(pos))level.setBlock(pos,defaultBlockState().setValue(AGE,Math.min(15,age+random.nextInt(5)/4)),3);
        else level.removeBlock(pos,false);
    }
    @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){
        if(!level.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK))return;
        if(!state.canSurvive(level,pos)||level.isRaining()&&wet(level,pos)){level.removeBlock(pos,false);return;}
        int age=state.getValue(AGE);if(age<4){state=state.setValue(AGE,age+random.nextInt(3)/2);level.setBlock(pos,state,4);}
        level.scheduleTick(pos,this,1+random.nextInt(10));
        if(!neighborFuel(level,pos)){if(!level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP)||age>3)level.removeBlock(pos,false);return;}
        if(!level.getBlockState(pos.below()).isFlammable(level,pos.below(),Direction.UP)&&age>=4&&random.nextInt(4)==0){level.removeBlock(pos,false);return;}
        if(random.nextFloat()>=.2F)return;
        // Modern FireBlock uses the native wet-biome tag instead of the removed humidity accessor.
        int humidity=level.getBiome(pos).is(net.minecraft.tags.BiomeTags.INCREASED_FIRE_BURNOUT)?-50:0;
        for(Direction dir:Direction.values())burn(level,pos.relative(dir),(dir.getAxis()==Direction.Axis.Y?250:300)+humidity,random,age,dir.getOpposite());
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)for(int y=-1;y<=4;y++)if(x!=0||y!=0||z!=0){var next=pos.offset(x,y,z);
            int fuel=encouragement(level,next),chance=100+(y>1?(y-1)*100:0),spread=(fuel+40+level.getDifficulty().getId()*7)/(age+30);
            if(humidity<0)spread/=2;if(fuel>0&&spread>0&&random.nextInt(chance)<=spread&&(!level.isRaining()||!wet(level,next)))
                level.setBlock(next,state.setValue(AGE,Math.min(4,age+random.nextInt(5)/4)),3);}
    }
}
