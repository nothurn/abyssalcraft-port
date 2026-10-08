package dev.abyssalport.ritual;

import dev.abyssalport.block.entity.RitualPedestalBlockEntity;
import dev.abyssalport.block.entity.RitualAltarBlockEntity;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.TreeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Server-owned eight-pedestal ritual definition with explicit optional layout and access requirements. */
public record RitualDefinition(ResourceLocation id, int energyCost, int durationTicks,
                               Map<Item, Integer> offerings, Item result, int resultCount, Item altarInput,
                               boolean livingSacrifice, List<Item> offeringLayout, boolean gateway,
                               RitualRequirements requirements, boolean preserveComponents,
                               net.minecraft.world.entity.EntityType<?> summonType, boolean preserveEnergy) {
    public RitualDefinition(ResourceLocation id, int energyCost, int durationTicks,
            Map<Item, Integer> offerings, Item result, int resultCount, Item altarInput,
            boolean livingSacrifice, List<Item> offeringLayout, boolean gateway,
            RitualRequirements requirements, boolean preserveComponents, net.minecraft.world.entity.EntityType<?> summonType) {
        this(id, energyCost, durationTicks, offerings, result, resultCount, altarInput, livingSacrifice,
                offeringLayout, gateway, requirements, preserveComponents, summonType, false);
    }
    public RitualDefinition(ResourceLocation id, int energyCost, int durationTicks,
            Map<Item, Integer> offerings, Item result, int resultCount, Item altarInput,
            boolean livingSacrifice, List<Item> offeringLayout, boolean gateway,
            RitualRequirements requirements, boolean preserveComponents) {
        this(id, energyCost, durationTicks, offerings, result, resultCount, altarInput, livingSacrifice,
                offeringLayout, gateway, requirements, preserveComponents, null);
    }
    public RitualDefinition(ResourceLocation id, int energyCost, int durationTicks,
            Map<Item, Integer> offerings, Item result, int resultCount, Item altarInput,
            boolean livingSacrifice, List<Item> offeringLayout, boolean gateway) {
        this(id, energyCost, durationTicks, offerings, result, resultCount, altarInput, livingSacrifice,
                offeringLayout, gateway, RitualRequirements.BASIC, false);
    }
    public RitualDefinition(ResourceLocation id, int energyCost, int durationTicks,
            Map<Item, Integer> offerings, Item result, int resultCount) {
        this(id, energyCost, durationTicks, offerings, result, resultCount, Items.AIR, false, List.of(), false);
    }

    public RitualDefinition(ResourceLocation id, int energyCost, int durationTicks,
            Map<Item, Integer> offerings, Item result, int resultCount, Item altarInput) {
        this(id, energyCost, durationTicks, offerings, result, resultCount, altarInput, false, List.of(), false);
    }

    public RitualDefinition(ResourceLocation id, int energyCost, int durationTicks,
            Map<Item, Integer> offerings, Item result, int resultCount, Item altarInput, boolean livingSacrifice) {
        this(id, energyCost, durationTicks, offerings, result, resultCount, altarInput, livingSacrifice, List.of(), false);
    }

    public RitualDefinition {
        java.util.Objects.requireNonNull(requirements);
        if (energyCost <= 0 || durationTicks <= 0 || summonType == null
                && (result == Items.AIR || resultCount <= 0 || resultCount > result.getDefaultMaxStackSize())) {
            throw new IllegalArgumentException("Invalid ritual cost, duration or result count");
        }
        if (summonType != null && (!SummoningRitual.supported(summonType) || result != Items.AIR || resultCount != 0
                || altarInput != Items.AIR || preserveComponents || preserveEnergy || gateway)) throw new IllegalArgumentException("Invalid summoning outcome");
        if (energyCost > requirements.bookCapacity()) throw new IllegalArgumentException("Ritual exceeds its book tier capacity");
        if (preserveComponents && (altarInput == Items.AIR || resultCount != 1 || gateway
                || !(altarInput instanceof dev.abyssalport.item.RendingStaffItem)
                || !(result instanceof dev.abyssalport.item.RendingStaffItem)
                || ((dev.abyssalport.item.RendingStaffItem) result).tier() <= ((dev.abyssalport.item.RendingStaffItem) altarInput).tier()))
            throw new IllegalArgumentException("Component-preserving infusion requires one rending staff upgrade");
        // Original energyContainer.setTags("PotEnergy") copies that balance only,
        // not a whole item component patch or a hidden pedestal inventory.
        if (preserveEnergy && (resultCount != 1 || gateway || preserveComponents
                || !(altarInput instanceof dev.abyssalport.item.EnergyStorageItem from)
                || !(from.getBlock() instanceof dev.abyssalport.block.EnergyPedestalBlock)
                || !(result instanceof dev.abyssalport.item.EnergyStorageItem to)
                || !(to.getBlock() instanceof dev.abyssalport.block.EnergyContainerBlock)
                || ((dev.abyssalport.block.EnergyStorageBlock) from.getBlock()).tier() != dev.abyssalport.energy.EnergyDeviceTier.BASE
                || ((dev.abyssalport.block.EnergyStorageBlock) to.getBlock()).tier() != dev.abyssalport.energy.EnergyDeviceTier.BASE))
            throw new IllegalArgumentException("Energy-preserving infusion requires one base pedestal to reservoir");
        offerings = Map.copyOf(offerings);
        int total = 0;
        for (Map.Entry<Item, Integer> entry : offerings.entrySet()) {
            if (entry.getValue() <= 0) {
                throw new IllegalArgumentException("Ritual offering count must be positive");
            }
            total += entry.getValue();
        }
        if (total == 0 || total > BasicScrollRitual.pedestalOffsets().size()) {
            throw new IllegalArgumentException("Ritual requires between one and eight offerings");
        }
        offeringLayout = List.copyOf(offeringLayout);
        if (!offeringLayout.isEmpty()) {
            if (offeringLayout.size() != 8) throw new IllegalArgumentException("Ordered layout requires eight positions");
            Map<Item, Integer> ordered = new HashMap<>();
            offeringLayout.stream().filter(item -> item != Items.AIR).forEach(item -> ordered.merge(item, 1, Integer::sum));
            if (!ordered.equals(offerings)) throw new IllegalArgumentException("Ordered layout does not match offering counts");
        }
    }

    public boolean matches(ServerLevel level, BlockPos altarPos) {
        if (!requirements.matchesGround(level, altarPos)) return false;
        if (!(level.getBlockEntity(altarPos) instanceof RitualAltarBlockEntity altar)) return false;
        ItemStack center = altar.getInput();
        if (altarInput == Items.AIR ? !center.isEmpty() : !center.is(altarInput)) return false;
        if ((preserveComponents || preserveEnergy) && center.getCount() != 1) return false;
        if (gateway && !GatewayRitual.canComplete(level, altarPos, center)) return false;
        Map<Item, Integer> found = new HashMap<>();
        for (int index = 0; index < BasicScrollRitual.pedestalOffsets().size(); index++) {
            BlockPos offset = BasicScrollRitual.pedestalOffsets().get(index);
            if (!(level.getBlockEntity(altarPos.offset(offset)) instanceof RitualPedestalBlockEntity pedestal)) {
                return false;
            }
            ItemStack stack = pedestal.getOffering();
            if (!offeringLayout.isEmpty()) {
                Item expected = offeringLayout.get(index);
                if (expected == Items.AIR ? !stack.isEmpty() : !stack.is(expected)) return false;
            }
            if (!stack.isEmpty()) {
                found.merge(stack.getItem(), 1, Integer::sum);
            }
        }
        return found.equals(offerings);
    }

    /** Only call after matches() on the server thread. */
    public void consumeOfferings(ServerLevel level, BlockPos altarPos) {
        for (BlockPos offset : BasicScrollRitual.pedestalOffsets()) {
            RitualPedestalBlockEntity pedestal = (RitualPedestalBlockEntity) level.getBlockEntity(altarPos.offset(offset));
            if (!pedestal.getOffering().isEmpty()) {
                pedestal.removeOne();
            }
        }
        if (altarInput != Items.AIR) {
            ((RitualAltarBlockEntity) level.getBlockEntity(altarPos)).removeInput();
        }
    }

    public ItemStack resultStack() {
        return new ItemStack(result, resultCount);
    }

    public net.minecraft.network.chat.Component outcomeName() {
        return summonType == null ? resultStack().getHoverName() : summonType.getDescription();
    }

    public ItemStack resultStack(ItemStack center) {
        ItemStack stack = resultStack();
        if (preserveComponents) stack.applyComponents(center.getComponentsPatch());
        if (preserveEnergy) dev.abyssalport.item.EnergyStorageItem.addEnergy(stack,
                dev.abyssalport.item.EnergyStorageItem.storedEnergy(center));
        return stack;
    }

    /** Stable fingerprint prevents a datapack reload from changing a ritual already underway. */
    public String signature() {
        Map<String, Integer> sorted = new TreeMap<>();
        offerings.forEach((item, count) -> sorted.put(BuiltInRegistries.ITEM.getKey(item).toString(), count));
        String base = id + "|" + energyCost + "|" + durationTicks + "|" + sorted + "|"
                + BuiltInRegistries.ITEM.getKey(result) + "|" + resultCount;
        // Keep the original Creation fingerprint readable for pre-infusion saves.
        if (altarInput != Items.AIR) base += "|altar=" + BuiltInRegistries.ITEM.getKey(altarInput);
        if (livingSacrifice) base += "|living_sacrifice=true";
        if (!offeringLayout.isEmpty()) base += "|layout=" + offeringLayout.stream().map(BuiltInRegistries.ITEM::getKey).toList();
        if (gateway) base += "|outcome=gateway";
        if (!requirements.equals(RitualRequirements.BASIC)) base += "|requirements=" + requirements;
        if (preserveComponents) base += "|preserve_components=true";
        if (preserveEnergy) base += "|preserve_energy=true";
        if (summonType != null) base += "|summon=" + BuiltInRegistries.ENTITY_TYPE.getKey(summonType);
        return base;
    }
}
