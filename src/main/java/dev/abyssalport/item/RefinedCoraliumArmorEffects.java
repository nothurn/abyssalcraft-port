package dev.abyssalport.item;

import dev.abyssalport.content.WastelandMineralsContent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class RefinedCoraliumArmorEffects {
    private RefinedCoraliumArmorEffects() { }
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 10 == 0) apply(player);
    }
    public static void apply(ServerPlayer player) {
        if (player.getItemBySlot(EquipmentSlot.HEAD).is(WastelandMineralsContent.HELMET.get()))
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 20, 0, true, false, false));
        if (player.getItemBySlot(EquipmentSlot.CHEST).is(WastelandMineralsContent.CHESTPLATE.get()))
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20, 0, true, false, false));
        if (player.getItemBySlot(EquipmentSlot.FEET).is(WastelandMineralsContent.BOOTS.get()))
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20, 0, true, false, false));
    }
}
