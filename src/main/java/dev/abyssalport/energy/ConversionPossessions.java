package dev.abyssalport.energy;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** Transactional return of possessions whose original owner is being replaced.
 * The reference kills horses to drop equipment; other conversions silently erase
 * cargo. Return exact native stacks instead, and retain the owner if any join is
 * denied. This is not death loot and does not invent species rewards.
 */
final class ConversionPossessions implements AutoCloseable {
    private final Mob source;
    private final Vec3 position;
    private final Container inventory;
    private final List<ItemStack> contents = new ArrayList<>();
    private final EnumMap<EquipmentSlot, ItemStack> equipment = new EnumMap<>(EquipmentSlot.class);
    private final List<ItemEntity> drops = new ArrayList<>();
    private final boolean saddle, chest;
    private boolean committed;

    ConversionPossessions(Mob source) {
        this.source = source;
        position = source.position();
        inventory = inventory(source);
        if (inventory != null) {
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) contents.add(inventory.getItem(slot).copy());
        }
        for (var slot : EquipmentSlot.values()) equipment.put(slot, source.getItemBySlot(slot).copy());
        saddle = separateSaddle(source);
        chest = source instanceof AbstractChestedHorse horse && horse.hasChest();
    }

    private static Container inventory(Mob mob) {
        if (mob instanceof AbstractHorse horse) return horse.getInventory();
        return mob instanceof InventoryCarrier carrier ? carrier.getInventory() : null;
    }

    private static boolean separateSaddle(Mob mob) {
        // Horse saddles are real inventory stacks, not an additional boolean
        // possession. Pig/Strider saddles must be returned exactly once as well.
        return !(mob instanceof AbstractHorse) && mob instanceof Saddleable mount && mount.isSaddled();
    }

    boolean unchanged(ServerLevel level) {
        if (!source.isAlive() || source.level() != level || !source.position().equals(position)
                || source.isPassenger() || source.isVehicle() || source.isLeashed()
                || inventory(source) != inventory
                || saddle != separateSaddle(source)
                || chest != (source instanceof AbstractChestedHorse horse && horse.hasChest())) return false;
        if (inventory != null) {
            if (inventory.getContainerSize() != contents.size()) return false;
            for (int slot = 0; slot < contents.size(); slot++) {
                if (!ItemStack.matches(contents.get(slot), inventory.getItem(slot))) return false;
            }
        }
        for (var slot : EquipmentSlot.values()) {
            if (!ItemStack.matches(equipment.get(slot), source.getItemBySlot(slot))) return false;
        }
        return true;
    }

    boolean admit(ServerLevel level) {
        var stacks = new ArrayList<>(contents);
        stacks.addAll(equipment.values());
        if (saddle) stacks.add(new ItemStack(Items.SADDLE));
        if (chest) stacks.add(new ItemStack(Items.CHEST));
        for (var stack : stacks) {
            if (stack.isEmpty()) continue;
            var drop = new ItemEntity(level, position.x, position.y + .25, position.z, stack.copy());
            drop.setDefaultPickUpDelay();
            drop.setDeltaMovement(Vec3.ZERO);
            drops.add(drop);
            if (!DisruptionEntities.clear(level, drop, drop.getBoundingBox())
                    || !level.addFreshEntity(drop) || !valid(drop, stack) || !unchanged(level)) return false;
        }
        // Later join listeners can also modify a previously admitted drop.
        int index = 0;
        for (var stack : stacks) if (!stack.isEmpty() && !valid(drops.get(index++), stack)) return false;
        return unchanged(level);
    }

    private static boolean valid(ItemEntity drop, ItemStack expected) {
        return drop.isAddedToLevel() && !drop.isRemoved() && ItemStack.matches(drop.getItem(), expected);
    }

    void commit() {
        if (inventory != null) inventory.clearContent();
        for (var slot : EquipmentSlot.values()) source.setItemSlot(slot, ItemStack.EMPTY);
        if (source instanceof AbstractChestedHorse horse) horse.setChest(false);
        source.discard();
        committed = true;
    }

    @Override public void close() {
        if (!committed) drops.forEach(ItemEntity::discard);
    }
}
