package dev.abyssalport.ritual;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import dev.abyssalport.item.NecronomiconItem;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;

/** Datapack definitions under data/<namespace>/rituals/<name>.json. */
public final class RitualCatalog extends SimpleJsonResourceReloadListener {
    public static final RitualCatalog INSTANCE = new RitualCatalog();
    private static final Logger LOGGER = LogUtils.getLogger();
    private volatile Map<ResourceLocation, RitualDefinition> definitions = Map.of();

    private RitualCatalog() {
        super(new Gson(), "rituals");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> entries, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, RitualDefinition> loaded = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : new TreeMap<>(entries).entrySet()) {
            try {
                RitualDefinition definition = parse(entry.getKey(), GsonHelper.convertToJsonObject(entry.getValue(), "ritual"));
                if (loaded.values().stream().anyMatch(previous -> previous.offerings().equals(definition.offerings())
                        && previous.altarInput() == definition.altarInput())) {
                    throw new IllegalArgumentException("Another ritual uses the same offerings");
                }
                loaded.put(entry.getKey(), definition);
            } catch (RuntimeException exception) {
                LOGGER.error("Ignoring invalid ritual {}", entry.getKey(), exception);
            }
        }
        definitions = Map.copyOf(loaded);
        LOGGER.info("Loaded {} AbyssalPort ritual definitions", definitions.size());
    }

    public Optional<RitualDefinition> get(ResourceLocation id) {
        return Optional.ofNullable(definitions.get(id));
    }

    public Optional<RitualDefinition> find(ServerLevel level, BlockPos altarPos) {
        return definitions.values().stream().filter(definition -> definition.matches(level, altarPos)).findFirst();
    }

    private static RitualDefinition parse(ResourceLocation id, JsonObject json) {
        int cost = GsonHelper.getAsInt(json, "energy_cost");
        int duration = GsonHelper.getAsInt(json, "duration_ticks");
        ResourceKey<net.minecraft.world.level.Level> dimension = json.has("dimension")
                ? ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(GsonHelper.getAsString(json, "dimension"))) : null;
        RitualRequirements requirements = new RitualRequirements(GsonHelper.getAsInt(json, "book_tier", 0),
                GsonHelper.getAsInt(json, "altar_tier", 0), dimension);
        if (cost > requirements.bookCapacity() || duration > 1200) {
            throw new IllegalArgumentException("Ritual exceeds book capacity or 60-second duration limit");
        }
        Map<Item, Integer> offerings = new HashMap<>();
        for (JsonElement element : GsonHelper.getAsJsonArray(json, "offerings")) {
            Item item = item(GsonHelper.convertToString(element, "offering"));
            offerings.merge(item, 1, Integer::sum);
        }
        String outcome = GsonHelper.getAsString(json, "outcome", "item");
        if (!outcome.equals("item") && !outcome.equals("gateway") && !outcome.equals("summon")) throw new IllegalArgumentException("Unknown ritual outcome");
        var summon = outcome.equals("summon") ? entity(GsonHelper.getAsString(json, "entity")) : null;
        JsonObject output = summon == null ? GsonHelper.getAsJsonObject(json, "result") : null;
        Item result = output == null ? Items.AIR : item(GsonHelper.getAsString(output, "item"));
        int count = output == null ? 0 : GsonHelper.getAsInt(output, "count", 1);
        Item center = json.has("altar_input") ? item(GsonHelper.getAsString(json, "altar_input")) : Items.AIR;
        List<Item> layout = new ArrayList<>();
        if (json.has("offering_layout")) for (JsonElement element : GsonHelper.getAsJsonArray(json, "offering_layout"))
            layout.add(element.isJsonNull() ? Items.AIR : item(element.getAsString()));
        if (outcome.equals("gateway") && (!(center instanceof dev.abyssalport.item.GatewayKeyItem)
                || result != center || count != 1 || GsonHelper.getAsBoolean(json, "living_sacrifice", false)))
            throw new IllegalArgumentException("Gateway must return one selected key without a living sacrifice");
        return new RitualDefinition(id, cost, duration, offerings, result, count, center,
                GsonHelper.getAsBoolean(json, "living_sacrifice", false), layout, outcome.equals("gateway"),
                requirements, GsonHelper.getAsBoolean(json, "preserve_components", false), summon,
                GsonHelper.getAsBoolean(json, "preserve_energy", false));
    }

    private static net.minecraft.world.entity.EntityType<?> entity(String name) {
        var id = ResourceLocation.tryParse(name);
        if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) throw new IllegalArgumentException("Unknown summon: " + name);
        var type = BuiltInRegistries.ENTITY_TYPE.get(id);
        if (!SummoningRitual.supported(type)) throw new IllegalArgumentException("Unsupported ritual summon: " + name);
        return type;
    }

    private static Item item(String name) {
        ResourceLocation id = ResourceLocation.tryParse(name);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            throw new IllegalArgumentException("Unknown item: " + name);
        }
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == Items.AIR) {
            throw new IllegalArgumentException("Air cannot be a ritual ingredient or output");
        }
        return item;
    }
}
