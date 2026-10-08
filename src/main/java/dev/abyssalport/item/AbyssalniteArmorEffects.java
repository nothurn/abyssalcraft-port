package dev.abyssalport.item;

import dev.abyssalport.content.AbyssalniteContent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class AbyssalniteArmorEffects {
    private AbyssalniteArmorEffects() { }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 10 == 0) {
            apply(player);
        }
    }

    /** Original helmet/boot effects, restricted to equipped slots on the server. */
    public static void apply(ServerPlayer player) {
        if (player.getItemBySlot(EquipmentSlot.HEAD).is(AbyssalniteContent.HELMET.get())) {
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 20, 0, true, false, false));
        }
        if (player.getItemBySlot(EquipmentSlot.FEET).is(AbyssalniteContent.BOOTS.get())) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20, 0, true, false, false));
        }
    }
}
