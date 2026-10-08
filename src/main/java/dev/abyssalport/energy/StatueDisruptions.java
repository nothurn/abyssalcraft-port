package dev.abyssalport.energy;

import dev.abyssalport.config.MobEcologyConfig;
import dev.abyssalport.block.entity.DeityStatueBlockEntity;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;

/** Source threshold/reset/selection from TileEntityStatue and DisruptionHandler.
 * Copyright (c) 2012-2026 Shinoow. NeoForge adaptation under LGPL-3.0.
 */
public final class StatueDisruptions {
    private StatueDisruptions() { }
    public static void trigger(ServerLevel level,DeityStatueBlockEntity statue,RandomSource random) {
        if(MobEcologyConfig.disruptionsDisabled()||statue.structureRangeAmplifier()>0
                ||level.dimension().location().equals(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("abyssalport","omothol")))return;
        var pos=statue.getBlockPos();
        triggerAt(level,pos,statue.deity(),random);
    }
    public static void triggerAt(ServerLevel level,net.minecraft.core.BlockPos pos,dev.abyssalport.content.StatueContent.Deity deity,RandomSource random) {
        if(MobEcologyConfig.disruptionsDisabled())return;
        var effect=DisruptionCatalogue.choose(deity,random);
        List<ServerPlayer> players=level.getEntitiesOfClass(ServerPlayer.class,new AABB(pos).inflate(16),player->player.isAlive()&&!player.isSpectator());
        var event=new DisruptionEvent(level,pos,deity,effect,players);
        if(NeoForge.EVENT_BUS.post(event).isCanceled())return;
        DisruptionEffects.lightning(level,pos.above().getCenter(),true);
        DisruptionEffects.apply(event,random);
        for(var player:players)player.displayClientMessage(Component.translatable("message.abyssalport.disruption",Component.translatable(effect.translationKey())).withStyle(ChatFormatting.DARK_RED),true);
    }
}
