package dev.abyssalport.item;

import dev.abyssalport.content.DreadProcessingContent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Original chest/boot effects are finite, server-owned and only active while equipped. */
public final class DreadiumArmorEffects {
    private DreadiumArmorEffects() { }
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if(event.getEntity() instanceof ServerPlayer player && player.tickCount%10==0) apply(player);
    }
    public static void apply(ServerPlayer player) {
        if(player.getItemBySlot(EquipmentSlot.CHEST).is(DreadProcessingContent.CHESTPLATE.get()))
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,20,0,false,false,false));
        if(player.getItemBySlot(EquipmentSlot.FEET).is(DreadProcessingContent.BOOTS.get()))
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,20,0,false,false,false));
    }
}
