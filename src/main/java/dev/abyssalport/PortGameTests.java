package dev.abyssalport;

import dev.abyssalport.item.NecronomiconItem;
import dev.abyssalport.block.entity.PrototypeEffigyBlockEntity;
import dev.abyssalport.block.entity.EnergyCollectorBlockEntity;
import dev.abyssalport.block.entity.EnergyRelayBlockEntity;
import dev.abyssalport.block.entity.EnergyContainerBlockEntity;
import dev.abyssalport.block.EnergyRelayBlock;
import dev.abyssalport.block.entity.RitualAltarBlockEntity;
import dev.abyssalport.block.entity.RitualPedestalBlockEntity;
import dev.abyssalport.ritual.BasicScrollRitual;
import dev.abyssalport.ritual.RitualCatalog;
import dev.abyssalport.ritual.RitualGroundFormation;
import dev.abyssalport.research.EarlyResearch;
import dev.abyssalport.research.DarklandsResearch;
import dev.abyssalport.research.ShadowMaterialResearch;
import dev.abyssalport.block.ShoggothOozeBlock;
import dev.abyssalport.entity.ShoggothEntity;
import dev.abyssalport.entity.ShadowCreatureEntity;
import dev.abyssalport.entity.ai.BuildMonolithGoal;
import dev.abyssalport.worldgen.MonolithBuilder;
import dev.abyssalport.worldgen.MonolithSiteRules;
import dev.abyssalport.worldgen.DarklandsWorldgen;
import dev.abyssalport.content.StatueContent;
import dev.abyssalport.content.ShoggothLairContent;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import terrablender.api.RegionType;
import terrablender.api.Regions;
import terrablender.api.SurfaceRuleManager;
import terrablender.worldgen.IExtendedParameterList;
import terrablender.worldgen.surface.NamespacedSurfaceRuleSource;

@GameTestHolder(AbyssalPort.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PortGameTests {
    private PortGameTests() {
    }

    @GameTest(template = "empty", batch = "darklands_biome")
    public static void darklandsBiomeLoadsWithSurfaceAndSurvivalContent(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess();
        var biome = registry.registryOrThrow(Registries.BIOME).getHolderOrThrow(DarklandsWorldgen.DARKLANDS).value();
        if (Regions.get(RegionType.OVERWORLD).stream().noneMatch(region ->
                region.getName().equals(DarklandsWorldgen.REGION) && region.getWeight() == 1)) {
            throw new AssertionError("Darklands Overworld region was not registered");
        }
        var mappings = new java.util.ArrayList<ResourceKey<net.minecraft.world.level.biome.Biome>>();
        var legacyClimatePoints = new java.util.ArrayList<Climate.ParameterPoint>();
        new dev.abyssalport.worldgen.DarklandsRegion(DarklandsWorldgen.REGION, 1)
                .addBiomes(registry.registryOrThrow(Registries.BIOME), pair -> {
                    mappings.add(pair.getSecond());
                    if (pair.getSecond().equals(DarklandsWorldgen.DARKLANDS)) legacyClimatePoints.add(pair.getFirst());
                });
        if (!mappings.contains(DarklandsWorldgen.DARKLANDS)) {
            throw new AssertionError("Darklands region does not map any climate points to its biome");
        }
        // The GameTest world is flat; its Overworld preset is never initialized by TerraBlender.
        // Initialize an isolated normal-world parameter list to verify positional selection.
        var normalPreset = new MultiNoiseBiomeSourceParameterList(
                MultiNoiseBiomeSourceParameterList.Preset.OVERWORLD,
                registry.registryOrThrow(Registries.BIOME).asLookup());
        @SuppressWarnings("unchecked")
        var selector = (IExtendedParameterList<Holder<Biome>>) (Object) normalPreset.parameters();
        selector.initializeForTerraBlender(registry, RegionType.OVERWORLD, 12345L);
        int darklandsRegion = Regions.getIndex(RegionType.OVERWORLD, DarklandsWorldgen.REGION);
        boolean selectable = false;
        for (int x = -256; x <= 256 && !selectable; x += 8) {
            for (int z = -256; z <= 256; z += 8) {
                if (selector.getUniqueness(x, 0, z) == darklandsRegion) {
                    // A zero climate point is not the humid legacy biome's climate.
                    // Test an actual mapped climate interior, not whichever new variant
                    // happens to be nearest to the origin of climate space.
                    for (var point : legacyClimatePoints) {
                        var target = new Climate.TargetPoint(midpoint(point.temperature()), midpoint(point.humidity()),
                                midpoint(point.continentalness()), midpoint(point.erosion()),
                                midpoint(point.depth()), midpoint(point.weirdness()));
                        if (selector.findValuePositional(target, x, 0, z).is(DarklandsWorldgen.DARKLANDS)) {
                            selectable = true;
                            break;
                        }
                    }
                    break;
                }
            }
        }
        if (!selectable) {
            throw new AssertionError("Darklands cannot be selected from TerraBlender's normal-world region map");
        }
        var surface = SurfaceRuleManager.getNamespacedRules(SurfaceRuleManager.RuleCategory.OVERWORLD,
                SurfaceRules.state(Blocks.STONE.defaultBlockState()));
        if (!(surface instanceof NamespacedSurfaceRuleSource namespaced)
                || !namespaced.sources().containsKey(AbyssalPort.MOD_ID)) {
            throw new AssertionError("Darklands surface rule is absent from TerraBlender's Overworld rules");
        }
        var monolith = registry.registryOrThrow(Registries.PLACED_FEATURE).get(
                ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID, "abandoned_monolith"));
        var darklandsVegetation = registry.registryOrThrow(Registries.PLACED_FEATURE).get(
                ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID, "darklands_oak"));
        if (monolith == null || darklandsVegetation == null
                || !biome.getGenerationSettings().hasFeature(monolith)
                || !biome.getGenerationSettings().hasFeature(darklandsVegetation)) {
            throw new AssertionError("Darklands vegetation or early monolith access is missing");
        }
        var monsters = biome.getMobSettings().getMobs(MobCategory.MONSTER).unwrap();
        if (monsters.stream().noneMatch(spawn -> spawn.type == AbyssalPort.SHADOW_CREATURE.get())
                || monsters.stream().noneMatch(spawn -> spawn.type == AbyssalPort.LESSER_SHOGGOTH.get())) {
            throw new AssertionError("Darklands prototype creatures are absent from the spawn table");
        }
        var soil = AbyssalPort.DARKLANDS_SOIL.get().defaultBlockState();
        if (!soil.is(BlockTags.DIRT) || !soil.is(BlockTags.ANIMALS_SPAWNABLE_ON)
                || !soil.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            throw new AssertionError("Darklands surface cannot support trees, passive animals or shovel mining");
        }
        helper.succeed();
    }

    private static long midpoint(Climate.Parameter parameter) {
        return (parameter.min() + parameter.max()) / 2;
    }

    @GameTest(template = "empty", batch = "darklands_biome")
    public static void darklandsDiscoveryIsBiomeSpecificAndPlayerScoped(GameTestHelper helper) {
        var biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        ServerPlayer visitor = helper.makeMockServerPlayerInLevel();
        ServerPlayer bystander = helper.makeMockServerPlayerInLevel();
        visitor.setData(AbyssalPort.EARLY_RESEARCH.get(), 0);
        bystander.setData(AbyssalPort.EARLY_RESEARCH.get(), 0);

        if (DarklandsResearch.discover(visitor, biomes.getHolderOrThrow(Biomes.PLAINS))
                || EarlyResearch.pageAvailable(7, EarlyResearch.discoveries(visitor))) {
            throw new AssertionError("Ordinary biome unlocked Darklands research");
        }
        if (!DarklandsResearch.discover(visitor, biomes.getHolderOrThrow(DarklandsWorldgen.DARKLANDS))
                || !EarlyResearch.pageAvailable(7, EarlyResearch.discoveries(visitor))
                || EarlyResearch.has(bystander, EarlyResearch.DARKLANDS)) {
            throw new AssertionError("Darklands discovery was not restricted to its visitor");
        }
        if (DarklandsResearch.discover(visitor, biomes.getHolderOrThrow(DarklandsWorldgen.DARKLANDS))
                || EarlyResearch.discoveries(visitor) != EarlyResearch.DARKLANDS) {
            throw new AssertionError("Repeat visit changed the Darklands discovery");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "darklands_stone")
    public static void darkstoneGeneratesOnlyInDarklandsAndReplacesStone(GameTestHelper helper) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID, "ore_darkstone");
        var registries = helper.getLevel().registryAccess();
        var configured = registries.registryOrThrow(Registries.CONFIGURED_FEATURE).get(id);
        var placed = registries.registryOrThrow(Registries.PLACED_FEATURE).get(id);
        if (configured == null || placed == null) {
            throw new AssertionError("Darkstone vein definition did not load");
        }
        var biomes = registries.registryOrThrow(Registries.BIOME);
        if (!biomes.getHolderOrThrow(DarklandsWorldgen.DARKLANDS).value()
                .getGenerationSettings().hasFeature(placed)
                || biomes.getHolderOrThrow(Biomes.PLAINS).value()
                .getGenerationSettings().hasFeature(placed)) {
            throw new AssertionError("Darkstone veins are not restricted to the Darklands biome");
        }

        BlockPos center = chunkSafeCenter(helper);
        Map<BlockPos, BlockState> original = new HashMap<>();
        try {
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-8, -6, -8), center.offset(8, 6, 8))) {
                original.put(pos.immutable(), helper.getLevel().getBlockState(pos));
                helper.getLevel().setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
            }
            helper.getLevel().setBlock(center, Blocks.DIRT.defaultBlockState(), 2);
            helper.getLevel().setBlock(center.west(), Blocks.GRANITE.defaultBlockState(), 2);
            // The flat GameTest level has not primed the worldgen heightmap used by ore placement.
            Heightmap.primeHeightmaps(helper.getLevel().getChunk(center), Set.of(Heightmap.Types.OCEAN_FLOOR_WG));
            if (!configured.place(helper.getLevel(), helper.getLevel().getChunkSource().getGenerator(),
                    RandomSource.create(17), center)) {
                throw new AssertionError("Darkstone vein did not replace stone");
            }
            int darkstone = 0;
            for (BlockPos pos : original.keySet()) {
                if (helper.getLevel().getBlockState(pos).is(AbyssalPort.DARKSTONE.get())) {
                    darkstone++;
                }
            }
            if (darkstone == 0 || !helper.getLevel().getBlockState(center).is(Blocks.DIRT)
                    || !helper.getLevel().getBlockState(center.west()).is(Blocks.GRANITE)) {
                throw new AssertionError("Darkstone did not form a mineable vein or replaced non-stone ground");
            }
        } finally {
            original.forEach((pos, state) -> helper.getLevel().setBlock(pos, state, 2));
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "darkstone_processing")
    public static void darkstoneMiningAndSmeltingPreserveTheMaterialLoop(GameTestHelper helper) {
        if (!AbyssalPort.DARKSTONE.get().defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE)
                || !AbyssalPort.DARKSTONE_COBBLESTONE.get().defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE)
                || AbyssalPort.DARKSTONE.get().defaultBlockState().is(BlockTags.NEEDS_STONE_TOOL)
                || AbyssalPort.DARKSTONE_COBBLESTONE.get().defaultBlockState().is(BlockTags.NEEDS_STONE_TOOL)) {
            throw new AssertionError("Darkstone material requires the wrong mining tool tier");
        }
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockState previous = helper.getLevel().getBlockState(pos);
        try {
            helper.getLevel().setBlock(pos, AbyssalPort.DARKSTONE.get().defaultBlockState(), 2);
            ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
            var ordinaryDrops = Block.getDrops(helper.getLevel().getBlockState(pos), helper.getLevel(),
                    pos, null, null, pickaxe);
            if (ordinaryDrops.size() != 1 || !ordinaryDrops.getFirst().is(AbyssalPort.DARKSTONE_COBBLESTONE_ITEM.get())) {
                throw new AssertionError("Ordinary mining must produce one Darkstone Cobblestone");
            }
            ItemStack silkTouch = new ItemStack(Items.IRON_PICKAXE);
            silkTouch.enchant(helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                    .getHolderOrThrow(Enchantments.SILK_TOUCH), 1);
            var silkDrops = Block.getDrops(helper.getLevel().getBlockState(pos), helper.getLevel(),
                    pos, null, null, silkTouch);
            if (silkDrops.size() != 1 || !silkDrops.getFirst().is(AbyssalPort.DARKSTONE_ITEM.get())) {
                throw new AssertionError("Silk Touch must preserve the original Darkstone block");
            }
            helper.getLevel().setBlock(pos, AbyssalPort.DARKSTONE_COBBLESTONE.get().defaultBlockState(), 2);
            var cobbleDrops = Block.getDrops(helper.getLevel().getBlockState(pos), helper.getLevel(),
                    pos, null, null, pickaxe);
            if (cobbleDrops.size() != 1 || !cobbleDrops.getFirst().is(AbyssalPort.DARKSTONE_COBBLESTONE_ITEM.get())) {
                throw new AssertionError("Darkstone Cobblestone must drop itself");
            }
        } finally {
            helper.getLevel().setBlock(pos, previous, 2);
        }
        SingleRecipeInput input = new SingleRecipeInput(new ItemStack(AbyssalPort.DARKSTONE_COBBLESTONE_ITEM.get()));
        var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.SMELTING, input,
                helper.getLevel()).orElseThrow(() -> new AssertionError("Darkstone smelting recipe missing"));
        if (!recipe.id().equals(ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID,
                "darkstone_from_cobblestone"))
                || !recipe.value().assemble(input, helper.getLevel().registryAccess())
                        .is(AbyssalPort.DARKSTONE_ITEM.get())) {
            throw new AssertionError("Smelting Darkstone Cobblestone did not restore Darkstone");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "darkstone_masonry")
    public static void darkstoneBricksCraftSmeltAndDropThemselves(GameTestHelper helper) {
        if (!AbyssalPort.DARKSTONE_BRICKS.get().defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE)
                || !AbyssalPort.CRACKED_DARKSTONE_BRICKS.get().defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            throw new AssertionError("Darkstone masonry must be pickaxe-mineable");
        }
        CraftingInput craft = CraftingInput.of(2, 2, java.util.List.of(
                new ItemStack(AbyssalPort.DARKSTONE_ITEM.get()),
                new ItemStack(AbyssalPort.DARKSTONE_ITEM.get()),
                new ItemStack(AbyssalPort.DARKSTONE_ITEM.get()),
                new ItemStack(AbyssalPort.DARKSTONE_ITEM.get())));
        var bricks = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, craft,
                helper.getLevel()).orElseThrow(() -> new AssertionError("Darkstone Bricks recipe missing"));
        ItemStack crafted = bricks.value().assemble(craft, helper.getLevel().registryAccess());
        if (!bricks.id().equals(ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID, "darkstone_bricks"))
                || !crafted.is(AbyssalPort.DARKSTONE_BRICKS_ITEM.get()) || crafted.getCount() != 4) {
            throw new AssertionError("Four Darkstone blocks must craft four Darkstone Bricks");
        }
        SingleRecipeInput furnace = new SingleRecipeInput(crafted.copyWithCount(1));
        var cracked = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.SMELTING, furnace,
                helper.getLevel()).orElseThrow(() -> new AssertionError("Cracked Darkstone Bricks recipe missing"));
        if (!cracked.id().equals(ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID,
                "cracked_darkstone_bricks"))
                || !cracked.value().assemble(furnace, helper.getLevel().registryAccess())
                        .is(AbyssalPort.CRACKED_DARKSTONE_BRICKS_ITEM.get())) {
            throw new AssertionError("Smelting Darkstone Bricks must yield Cracked Darkstone Bricks");
        }
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockState previous = helper.getLevel().getBlockState(pos);
        try {
            for (var block : java.util.List.of(AbyssalPort.DARKSTONE_BRICKS.get(),
                    AbyssalPort.CRACKED_DARKSTONE_BRICKS.get())) {
                helper.getLevel().setBlock(pos, block.defaultBlockState(), 2);
                var drops = Block.getDrops(helper.getLevel().getBlockState(pos), helper.getLevel(),
                        pos, null, null, new ItemStack(Items.IRON_PICKAXE));
                if (drops.size() != 1 || !drops.getFirst().is(block.asItem())) {
                    throw new AssertionError(block + " did not drop its own item");
                }
            }
        } finally {
            helper.getLevel().setBlock(pos, previous, 2);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "darkstone_shapes")
    public static void darkstoneShapesCraftAndDoubleSlabsDropTwo(GameTestHelper helper) {
        var level = helper.getLevel();
        for (var block : java.util.List.of(AbyssalPort.DARKSTONE_BRICK_SLAB.get(),
                AbyssalPort.DARKSTONE_BRICK_STAIRS.get(), AbyssalPort.CHISELED_DARKSTONE_BRICKS.get())) {
            if (!block.defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE)) {
                throw new AssertionError(block + " is not pickaxe-mineable");
            }
        }
        if (!(AbyssalPort.DARKSTONE_BRICK_STAIRS.get() instanceof StairBlock)
                || AbyssalPort.DARKSTONE_BRICK_STAIRS.get().getStateDefinition().getPossibleStates().size() != 80) {
            throw new AssertionError("Darkstone Brick Stairs must support 40 shapes and waterlogging");
        }
        ItemStack brick = new ItemStack(AbyssalPort.DARKSTONE_BRICKS_ITEM.get());
        CraftingInput slabInput = CraftingInput.of(3, 1, java.util.List.of(brick, brick, brick));
        var slabRecipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, slabInput, level)
                .orElseThrow(() -> new AssertionError("Darkstone Brick Slab recipe missing"));
        ItemStack slabs = slabRecipe.value().assemble(slabInput, level.registryAccess());
        if (!slabRecipe.id().equals(ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID,
                "darkstone_brick_slab")) || !slabs.is(AbyssalPort.DARKSTONE_BRICK_SLAB_ITEM.get())
                || slabs.getCount() != 6) {
            throw new AssertionError("Three Darkstone Bricks must craft six Slabs");
        }
        CraftingInput stairsInput = CraftingInput.of(3, 3, java.util.List.of(
                brick, ItemStack.EMPTY, ItemStack.EMPTY,
                brick, brick, ItemStack.EMPTY,
                brick, brick, brick));
        var stairsRecipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, stairsInput, level)
                .orElseThrow(() -> new AssertionError("Darkstone Brick Stairs recipe missing"));
        ItemStack stairs = stairsRecipe.value().assemble(stairsInput, level.registryAccess());
        if (!stairsRecipe.id().equals(ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID,
                "darkstone_brick_stairs")) || !stairs.is(AbyssalPort.DARKSTONE_BRICK_STAIRS_ITEM.get())
                || stairs.getCount() != 4) {
            throw new AssertionError("Six Darkstone Bricks must craft four Stairs");
        }
        CraftingInput chiseledInput = CraftingInput.of(1, 2, java.util.List.of(
                slabs.copyWithCount(1), slabs.copyWithCount(1)));
        var chiseledRecipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, chiseledInput, level)
                .orElseThrow(() -> new AssertionError("Chiseled Darkstone Bricks recipe missing"));
        if (!chiseledRecipe.id().equals(ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID,
                "chiseled_darkstone_bricks")) || !chiseledRecipe.value()
                        .assemble(chiseledInput, level.registryAccess())
                        .is(AbyssalPort.CHISELED_DARKSTONE_BRICKS_ITEM.get())) {
            throw new AssertionError("Two Darkstone Brick Slabs must craft Chiseled Bricks");
        }
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockState previous = level.getBlockState(pos);
        try {
            for (SlabType type : new SlabType[] {SlabType.BOTTOM, SlabType.TOP, SlabType.DOUBLE}) {
                level.setBlock(pos, AbyssalPort.DARKSTONE_BRICK_SLAB.get().defaultBlockState()
                        .setValue(SlabBlock.TYPE, type), 2);
                var drops = Block.getDrops(level.getBlockState(pos), level, pos, null, null,
                        new ItemStack(Items.IRON_PICKAXE));
                if (drops.size() != 1 || !drops.getFirst().is(AbyssalPort.DARKSTONE_BRICK_SLAB_ITEM.get())
                        || drops.getFirst().getCount() != (type == SlabType.DOUBLE ? 2 : 1)) {
                    throw new AssertionError(type + " Darkstone Brick Slab returned incorrect drops");
                }
            }
            for (var block : java.util.List.of(AbyssalPort.DARKSTONE_BRICK_STAIRS.get(),
                    AbyssalPort.CHISELED_DARKSTONE_BRICKS.get())) {
                level.setBlock(pos, block.defaultBlockState(), 2);
                var drops = Block.getDrops(level.getBlockState(pos), level, pos, null, null,
                        new ItemStack(Items.IRON_PICKAXE));
                if (drops.size() != 1 || !drops.getFirst().is(block.asItem())) {
                    throw new AssertionError(block + " did not drop itself");
                }
            }
        } finally {
            level.setBlock(pos, previous, 2);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "shadow_grove")
    public static void shadowGroveGeneratesAndRestrictsSpawns(GameTestHelper helper) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID, "shadow_grove");
        var registry = helper.getLevel().registryAccess();
        if (!registry.registryOrThrow(Registries.CONFIGURED_FEATURE).containsKey(id)
                || !registry.registryOrThrow(Registries.PLACED_FEATURE).containsKey(id)) {
            throw new AssertionError("Shadow grove worldgen data did not load");
        }
        var placed = registry.registryOrThrow(Registries.PLACED_FEATURE).get(id);
        for (var biome : java.util.List.of(Biomes.DARK_FOREST, Biomes.SWAMP)) {
            var value = registry.registryOrThrow(Registries.BIOME).getHolderOrThrow(biome).value();
            if (!value.getGenerationSettings().hasFeature(placed)) {
                throw new AssertionError("Shadow grove feature missing from " + biome);
            }
        }

        BlockPos center = chunkSafeCenter(helper);
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                BlockPos ground = center.offset(dx, 0, dz);
                helper.getLevel().setBlock(ground.below(), Blocks.DIRT.defaultBlockState(), 3);
                helper.getLevel().setBlock(ground,
                        dx == 2 && dz == 0 ? Blocks.STONE.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState(), 3);
                for (int dy = 1; dy <= 12; dy++) {
                    helper.getLevel().setBlock(ground.above(dy), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        FeaturePlaceContext<NoneFeatureConfiguration> context = new FeaturePlaceContext<>(Optional.empty(),
                helper.getLevel(), helper.getLevel().getChunkSource().getGenerator(), RandomSource.create(73),
                center.above(), NoneFeatureConfiguration.INSTANCE);
        if (!AbyssalPort.SHADOW_GROVE.get().place(context)
                || !helper.getLevel().getBlockState(center).is(AbyssalPort.DARKLANDS_SOIL.get())
                || !helper.getLevel().getBlockState(center.east(2)).is(Blocks.STONE)) {
            throw new AssertionError("Shadow grove failed to replace only suitable grass");
        }
        if (!ShadowCreatureEntity.canSpawnOnSoil(helper.getLevel(), center.above())
                || ShadowCreatureEntity.canSpawnOnSoil(helper.getLevel(), center.east(2).above())) {
            throw new AssertionError("Shadow Creature is not bound to its habitat");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "shadow_materials")
    public static void shadowCreatureFeedsTheMaterialChain(GameTestHelper helper) {
        for (var biome : java.util.List.of(Biomes.DARK_FOREST, Biomes.SWAMP)) {
            var settings = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME)
                    .getHolderOrThrow(biome).value().getMobSettings();
            if (settings.getMobs(MobCategory.MONSTER).unwrap().stream().noneMatch(spawn ->
                    spawn.type == AbyssalPort.SHADOW_CREATURE.get()
                            && spawn.minCount == 1 && spawn.maxCount == 2)) {
                throw new AssertionError("Provisional Shadow Creature spawn missing from " + biome);
            }
        }
        ShadowCreatureEntity creature = AbyssalPort.SHADOW_CREATURE.get().create(helper.getLevel());
        if (creature == null || !creature.fireImmune() || !creature.canBreatheUnderwater()
                || creature.canBeAffected(new MobEffectInstance(MobEffects.POISON, 100))) {
            throw new AssertionError("Shadow Creature source traits were not registered");
        }
        var table = helper.getLevel().getServer().reloadableRegistries().getLootTable(ResourceKey.create(
                Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID,
                        "entities/shadow_creature")));
        var loot = table.getRandomItems(new LootParams.Builder(helper.getLevel())
                .withParameter(LootContextParams.THIS_ENTITY, creature)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(new BlockPos(4, 1, 4))))
                .withParameter(LootContextParams.DAMAGE_SOURCE, creature.damageSources().generic())
                .create(LootContextParamSets.ENTITY));
        int fragments = loot.stream().filter(stack -> stack.is(AbyssalPort.SHADOW_FRAGMENT.get()))
                .mapToInt(ItemStack::getCount).sum();
        if (fragments < 1 || fragments > 3 || loot.stream().anyMatch(stack -> !stack.is(AbyssalPort.SHADOW_FRAGMENT.get()))) {
            throw new AssertionError("Shadow Creature loot did not produce 1–3 fragments: " + loot);
        }
        assertCrafting(helper, "shadow_shard", AbyssalPort.SHADOW_FRAGMENT.get(),
                AbyssalPort.SHADOW_SHARD.get(), 1);
        assertCrafting(helper, "shadow_gem", AbyssalPort.SHADOW_SHARD.get(),
                AbyssalPort.SHADOW_GEM.get(), 1);
        assertCrafting(helper, "shadow_shard_from_gem", AbyssalPort.SHADOW_GEM.get(),
                AbyssalPort.SHADOW_SHARD.get(), 4);
        assertCrafting(helper, "shadow_fragment_from_shard", AbyssalPort.SHADOW_SHARD.get(),
                AbyssalPort.SHADOW_FRAGMENT.get(), 4);
        var collectorInput = CraftingInput.of(3, 3, java.util.stream.IntStream.range(0, 9).mapToObj(slot ->
                new ItemStack(slot == 4 ? AbyssalPort.SHADOW_GEM.get()
                        : dev.abyssalport.content.ShoggothLairContent.MONOLITH_STONE.get().asItem())).toList());
        var collector = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,
                collectorInput, helper.getLevel()).orElseThrow(() -> new AssertionError("Shadow Gem collector recipe missing"));
        if (!collector.id().equals(ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID, "energy_collector"))) {
            throw new AssertionError("Shadow Gem did not select the Energy Collector recipe");
        }
        ServerPlayer finder = helper.makeMockServerPlayerInLevel();
        ServerPlayer crafter = helper.makeMockServerPlayerInLevel();
        if (EarlyResearch.pageAvailable(4, EarlyResearch.discoveries(finder))) {
            throw new AssertionError("Shadow materials page started unlocked");
        }
        Vec3 position = Vec3.atCenterOf(helper.absolutePos(new BlockPos(4, 1, 4)));
        ItemEntity dropped = new ItemEntity(helper.getLevel(), position.x, position.y, position.z,
                new ItemStack(Items.DIRT));
        NeoForge.EVENT_BUS.post(new ItemEntityPickupEvent.Post(finder, dropped, new ItemStack(Items.DIRT)));
        if (EarlyResearch.has(finder, EarlyResearch.SHADOW_MATERIALS)) {
            throw new AssertionError("Unrelated pickup unlocked shadow research");
        }
        NeoForge.EVENT_BUS.post(new ItemEntityPickupEvent.Post(finder, dropped,
                new ItemStack(AbyssalPort.SHADOW_FRAGMENT.get())));
        if (!EarlyResearch.has(finder, EarlyResearch.SHADOW_MATERIALS)
                || !EarlyResearch.pageAvailable(4, EarlyResearch.discoveries(finder))
                || EarlyResearch.has(crafter, EarlyResearch.SHADOW_MATERIALS)) {
            throw new AssertionError("Shadow pickup did not unlock only the finder");
        }
        NeoForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(crafter,
                new ItemStack(AbyssalPort.SHADOW_GEM.get()), new SimpleContainer(1)));
        if (!EarlyResearch.has(crafter, EarlyResearch.SHADOW_MATERIALS)
                || EarlyResearch.discoveries(finder) != EarlyResearch.SHADOW_MATERIALS) {
            throw new AssertionError("Crafting did not unlock independently of pickup");
        }
        finder.setData(AbyssalPort.EARLY_RESEARCH.get(), 0);
        finder.getInventory().setItem(0, new ItemStack(AbyssalPort.SHADOW_SHARD.get()));
        ShadowMaterialResearch.discoverFromInventory(finder);
        if (!EarlyResearch.has(finder, EarlyResearch.SHADOW_MATERIALS)) {
            throw new AssertionError("Existing material in inventory was not recognized");
        }
        helper.succeed();
    }

    private static void assertCrafting(GameTestHelper helper, String recipeName, Item inputItem,
            Item outputItem, int outputCount) {
        java.util.List<ItemStack> ingredients = new java.util.ArrayList<>();
        for (int i = 0; i < (outputCount == 1 ? 4 : 1); i++) {
            ingredients.add(new ItemStack(inputItem));
        }
        CraftingInput input = CraftingInput.of(outputCount == 1 ? 2 : 1,
                outputCount == 1 ? 2 : 1, ingredients);
        var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,
                input, helper.getLevel()).orElseThrow(() -> new AssertionError("Recipe missing: " + recipeName));
        ItemStack result = recipe.value().assemble(input, helper.getLevel().registryAccess());
        if (!recipe.id().equals(ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID, recipeName))
                || !result.is(outputItem) || result.getCount() != outputCount) {
            throw new AssertionError("Incorrect Shadow material conversion: " + recipeName + " -> " + result);
        }
    }

    @GameTest(template = "empty")
    public static void lesserShoggothSpawnDataAndOozeAreUsable(GameTestHelper helper) {
        for (var biome : java.util.List.of(Biomes.DARK_FOREST, Biomes.SWAMP)) {
            var settings = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME)
                    .getHolderOrThrow(biome).value().getMobSettings();
            if (settings.getMobs(MobCategory.MONSTER).unwrap().stream().noneMatch(spawn ->
                    spawn.type == AbyssalPort.LESSER_SHOGGOTH.get()
                            && spawn.minCount == 4 && spawn.maxCount == 4)) {
                throw new AssertionError("Lesser Shoggoth group spawn is missing from " + biome);
            }
        }
        if (SpawnPlacements.getPlacementType(AbyssalPort.LESSER_SHOGGOTH.get()) != SpawnPlacementTypes.ON_GROUND
                || SpawnPlacements.getHeightmapType(AbyssalPort.LESSER_SHOGGOTH.get())
                != Heightmap.Types.MOTION_BLOCKING_NO_LEAVES) {
            throw new AssertionError("Lesser Shoggoth lacks an on-ground spawn restriction");
        }
        BlockPos pos = helper.absolutePos(new BlockPos(4, 1, 4));
        java.util.Map<BlockPos, BlockState> oozeFixture = java.util.Map.of(
                pos.below(), helper.getLevel().getBlockState(pos.below()),
                pos, helper.getLevel().getBlockState(pos),
                pos.above(2), helper.getLevel().getBlockState(pos.above(2)));
        helper.getLevel().setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        ShoggothEntity shoggoth = AbyssalPort.LESSER_SHOGGOTH.get().create(helper.getLevel());
        if (shoggoth == null) {
            throw new AssertionError("Lesser Shoggoth was not registered");
        }
        try {
        shoggoth.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        helper.getLevel().addFreshEntity(shoggoth);
        shoggoth.aiStep();
        if (!helper.getLevel().getBlockState(pos).is(AbyssalPort.SHOGGOTH_OOZE.get())) {
            throw new AssertionError("A grounded Shoggoth did not leave ooze");
        }
        if (!helper.getLevel().getBlockTicks().hasScheduledTick(pos, AbyssalPort.SHOGGOTH_OOZE.get())) {
            throw new AssertionError("New Shoggoth ooze has no scheduled decay tick");
        }
        helper.getLevel().setBlock(pos, AbyssalPort.SHOGGOTH_OOZE.get().defaultBlockState()
                .setValue(ShoggothOozeBlock.LAYERS, 3), 3);
        helper.getLevel().setBlock(pos.above(2), Blocks.GLOWSTONE.defaultBlockState(), 3);
        if (helper.getLevel().getMaxLocalRawBrightness(pos.above()) < 13) {
            throw new AssertionError("Test ooze has insufficient light to exercise decay");
        }
        helper.getLevel().getBlockState(pos).tick(helper.getLevel(), pos, RandomSource.create());
        if (helper.getLevel().getBlockState(pos).getValue(ShoggothOozeBlock.LAYERS) != 2) {
            throw new AssertionError("Bright Shoggoth ooze did not shed one layer");
        }
        boolean original = helper.getLevel().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        try {
            helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING)
                    .set(false, helper.getLevel().getServer());
            helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            shoggoth.aiStep();
            if (!helper.getLevel().getBlockState(pos).is(Blocks.AIR)) {
                throw new AssertionError("Shoggoth changed terrain while mobGriefing was disabled");
            }
        } finally {
            helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING)
                    .set(original, helper.getLevel().getServer());
        }
        } finally {
            // The following autonomous test must have exactly its own four builders.
            shoggoth.discard();
            oozeFixture.forEach((fixturePos, previous) -> helper.getLevel().setBlock(fixturePos, previous, 3));
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fourShoggothsBuildOnlyOnSafeGround(GameTestHelper helper) {
        BlockPos center = helper.absolutePos(new BlockPos(4, 1, 80));
        for (int chunkX = (center.getX() - 20) >> 4; chunkX <= (center.getX() + 20) >> 4; chunkX++) {
            for (int chunkZ = (center.getZ() - 20) >> 4; chunkZ <= (center.getZ() + 20) >> 4; chunkZ++) {
                helper.getLevel().getChunk(chunkX, chunkZ);
            }
        }
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                helper.getLevel().setBlock(center.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 3);
                for (int y = 0; y <= 12; y++) {
                    helper.getLevel().setBlock(center.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }

        Set<BlockPos> existingMonoliths = monolithEffigiesNear(helper, center);
        java.util.List<ShoggothEntity> participants = new java.util.ArrayList<>(4);
        try {
            verifyManualShoggothBuild(helper, center, participants);
        } finally {
            participants.forEach(ShoggothEntity::discard);
            Set<BlockPos> createdMonoliths = monolithEffigiesNear(helper, center);
            createdMonoliths.removeAll(existingMonoliths);
            createdMonoliths.forEach(effigy -> clearTestMonolith(helper, effigy));
        }
        Set<BlockPos> remaining = monolithEffigiesNear(helper, center);
        remaining.removeAll(existingMonoliths);
        if (!remaining.isEmpty()) {
            throw new AssertionError("Manual Shoggoth test left a monolith in the shared GameTest world: " + remaining);
        }
        helper.succeed();
    }

    private static void verifyManualShoggothBuild(GameTestHelper helper, BlockPos center,
                                                   java.util.List<ShoggothEntity> participants) {
        ShoggothEntity leader = AbyssalPort.LESSER_SHOGGOTH.get().create(helper.getLevel());
        if (leader == null) {
            throw new AssertionError("Shoggoth entity registration failed");
        }
        leader.setPos(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
        helper.getLevel().addFreshEntity(leader);
        participants.add(leader);
        for (int i = 0; i < 3; i++) {
            ShoggothEntity assistant = AbyssalPort.LESSER_SHOGGOTH.get().create(helper.getLevel());
            if (assistant == null) {
                throw new AssertionError("Assistant entity creation failed");
            }
            assistant.setPos(center.getX() + 1.5, center.getY(), center.getZ() + 0.5 + i);
            helper.getLevel().addFreshEntity(assistant);
            participants.add(assistant);
            CompoundTag assistantData = new CompoundTag();
            assistant.addAdditionalSaveData(assistantData);
            assistantData.putInt("MonolithTimer", ShoggothEntity.MONOLITH_COOLDOWN_TICKS);
            assistant.readAdditionalSaveData(assistantData);
        }
        CompoundTag saved = new CompoundTag();
        leader.addAdditionalSaveData(saved);
        saved.putInt("MonolithTimer", ShoggothEntity.MONOLITH_COOLDOWN_TICKS);
        leader.readAdditionalSaveData(saved);
        if (leader.monolithTimer() != ShoggothEntity.MONOLITH_COOLDOWN_TICKS) {
            throw new AssertionError("Monolith cooldown did not survive loading");
        }
        BuildMonolithGoal goal = new BuildMonolithGoal(leader);
        if (!helper.getLevel().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            throw new AssertionError("Test requires vanilla mobGriefing default");
        }
        try {
            helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING)
                    .set(false, helper.getLevel().getServer());
            if (goal.canUse()) {
                throw new AssertionError("Four Shoggoths ignored disabled mobGriefing");
            }
        } finally {
            helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING)
                    .set(true, helper.getLevel().getServer());
        }
        boolean foundSite = false;
        for (int i = 0; i < 100 && !foundSite; i++) {
            leader.tickCount += 20;
            foundSite = goal.canUse();
        }
        if (!foundSite) {
            throw new AssertionError("Four Shoggoths could not find a clear supported site");
        }
        goal.start();
        goal.tick();
        if (leader.monolithTimer() != ShoggothEntity.MONOLITH_COOLDOWN_TICKS) {
            throw new AssertionError("Monolith was completed on the first assembly tick");
        }
        for (int i = 1; i < BuildMonolithGoal.ASSEMBLY_TICKS; i++) {
            goal.tick();
        }
        goal.stop();
        if (leader.monolithTimer() != 0 || leader.isBuilding()) {
            throw new AssertionError("Successful construction did not reset the leader");
        }
        for (ShoggothEntity shoggoth : helper.getLevel().getEntitiesOfClass(ShoggothEntity.class,
                new AABB(center).inflate(6))) {
            if (helper.getLevel().getBlockState(shoggoth.blockPosition()).is(ShoggothLairContent.MONOLITH_STONE.get())) {
                throw new AssertionError("Monolith enclosed a living Shoggoth");
            }
            if (shoggoth.monolithTimer() != 0) {
                throw new AssertionError("A participant can lead another monolith without a group cooldown");
            }
        }
        BlockPos statuePos = null;
        for (int x = -6; x <= 6; x++) {
            for (int z = -6; z <= 6; z++) {
                for (int y = 3; y <= 15; y++) {
                    BlockPos candidate = center.offset(x, y, z);
                    if (StatueContent.isStatue(helper.getLevel().getBlockState(candidate))) {
                        statuePos = candidate;
                    }
                }
            }
        }
        if (statuePos == null) {
            throw new AssertionError("Monolith was not built after assembly");
        }
        if (MonolithSiteRules.farFromExistingMonoliths(helper.getLevel(), statuePos)) {
            throw new AssertionError("Finished monolith is invisible to the spacing rule");
        }
        BlockPos foundation = statuePos.below(5);
        var previousFoundation=helper.getLevel().getBlockState(foundation);
        helper.getLevel().setBlock(foundation, Blocks.AIR.defaultBlockState(), 3);
        try {
            if (!MonolithSiteRules.farFromExistingMonoliths(helper.getLevel(), statuePos)) {
                throw new AssertionError("An incomplete pillar was mistaken for a finished monolith");
            }
        } finally {
            helper.getLevel().setBlock(foundation, previousFoundation, 3);
        }
        saved.putInt("MonolithTimer", ShoggothEntity.MONOLITH_COOLDOWN_TICKS);
        leader.readAdditionalSaveData(saved);
        for (int i = 0; i < 20; i++) {
            leader.tickCount += 20;
            if (goal.canUse()) {
                throw new AssertionError("A second monolith was planned next to the first");
            }
        }
    }

    private static void clearTestMonolith(GameTestHelper helper, BlockPos effigy) {
        helper.getLevel().setBlock(effigy, Blocks.AIR.defaultBlockState(), 3);
        for (int depth = 1; depth <= 12; depth++) {
            BlockPos center = effigy.below(depth);
            if (!helper.getLevel().getBlockState(center).is(AbyssalPort.DARKSTONE.get())
                    &&!helper.getLevel().getBlockState(center).is(ShoggothLairContent.MONOLITH_STONE.get())
                    &&!helper.getLevel().getBlockState(center).is(ShoggothLairContent.BIOMASS.get())) {
                break;
            }
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos pillar = center.offset(x, 0, z);
                    if (helper.getLevel().getBlockState(pillar).is(AbyssalPort.DARKSTONE.get())
                            ||helper.getLevel().getBlockState(pillar).is(ShoggothLairContent.MONOLITH_STONE.get())
                            ||helper.getLevel().getBlockState(pillar).is(ShoggothLairContent.BIOMASS.get())) {
                        helper.getLevel().setBlock(pillar, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    @GameTest(template = "empty", batch = "shoggoth_ai", timeoutTicks = 800)
    public static void fourShoggothsBuildAutonomously(GameTestHelper helper) {
        verifyAutonomousShoggothPhase(helper, 1);
    }

    @GameTest(template = "empty", batch = "shoggoth_ai_even", timeoutTicks = 800)
    public static void fourEvenIdShoggothsAlsoBuildThroughNativeGoalScheduling(GameTestHelper helper) {
        verifyAutonomousShoggothPhase(helper, 0);
    }

    private static void verifyAutonomousShoggothPhase(GameTestHelper helper, int parity) {
        BlockPos center = chunkSafeCenter(helper);
        java.util.List<ShoggothEntity> spawned = new java.util.ArrayList<>(4);
        java.util.List<ChunkPos> tickingChunks = new java.util.ArrayList<>();
        var previousModes = new java.util.HashMap<net.minecraft.server.level.ServerPlayer, GameType>();
        boolean previousGriefing = helper.getLevel().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        long previousDay = helper.getLevel().getDayTime();
        // A GameTest structure may be loaded while adjacent chunks (and their entities) do not tick.
        // Previous batches leave mock players online; hostile mobs must not chase them in this AI test.
        helper.getLevel().getServer().getPlayerList().getPlayers()
                .forEach(player -> { previousModes.put(player, player.gameMode.getGameModeForPlayer()); player.setGameMode(GameType.SPECTATOR); });
        helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(true, helper.getLevel().getServer());
        // FleeSunGoal has higher priority than building. Keep the autonomous
        // construction fixture at night rather than relying on other batches.
        helper.getLevel().setDayTime(18000);
        helper.getLevel().updateSkyBrightness();
        for (int chunkX = (center.getX() - 20) >> 4; chunkX <= (center.getX() + 20) >> 4; chunkX++) {
            for (int chunkZ = (center.getZ() - 20) >> 4; chunkZ <= (center.getZ() + 20) >> 4; chunkZ++) {
                helper.getLevel().getChunk(chunkX, chunkZ);
                ChunkPos ticking = new ChunkPos(chunkX, chunkZ);
                helper.getLevel().getChunkSource().addRegionTicket(TicketType.FORCED, ticking, 2, ticking);
                tickingChunks.add(ticking);
            }
        }
        for (int x = -18; x <= 18; x++) {
            for (int z = -18; z <= 18; z++) {
                helper.getLevel().setBlock(center.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 3);
                helper.getLevel().setBlock(center.offset(x, 18, z), Blocks.AIR.defaultBlockState(), 3);
                for (int y = 0; y <= 17; y++) {
                    helper.getLevel().setBlock(center.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        // A ready, nearby group on an open field is the actual precondition.
        // The former five-block-radius climbing cage obstructed the 3x3
        // footprint selected 4.5+ blocks away and introduced walls to climb.
        // Keep native goals/navigation; do not confine them with that obstacle.
        Set<BlockPos> existingMonoliths = monolithEffigiesNear(helper, center);
        for (int index = 0; index < 4; index++) {
            ShoggothEntity shoggoth = AbyssalPort.LESSER_SHOGGOTH.get().create(helper.getLevel());
            if (shoggoth == null) {
                throw new AssertionError("Autonomous Shoggoth test could not create a participant");
            }
            if ((shoggoth.getId() & 1) != parity) {
                shoggoth.discard();
                // Allocate one unused native ID to select the other AI phase,
                // retaining native multipart reservations without live-ID swaps.
                var unused = net.minecraft.world.entity.EntityType.COW.create(helper.getLevel());
                unused.discard();
                shoggoth = AbyssalPort.LESSER_SHOGGOTH.get().create(helper.getLevel());
            }
            helper.assertTrue((shoggoth.getId() & 1) == parity, "Native ID phase fixture was not selected");
            shoggoth.setPos(center.getX() + 0.5 + index, center.getY(), center.getZ() + 0.5);
            // Keep actual goal acquisition/site selection, but make this
            // regression's random input reproducible rather than timing luck.
            shoggoth.getRandom().setSeed(824300L + index);
            CompoundTag data = new CompoundTag();
            shoggoth.addAdditionalSaveData(data);
            data.putInt("MonolithTimer", ShoggothEntity.MONOLITH_COOLDOWN_TICKS);
            shoggoth.readAdditionalSaveData(data);
            helper.getLevel().addFreshEntity(shoggoth);
            spawned.add(shoggoth);
        }
        // Goal acquisition is throttled and randomized; leave room for a late valid site search.
        helper.runAfterDelay(700, () -> {
            Set<BlockPos> newMonoliths = monolithEffigiesNear(helper, center);
            newMonoliths.removeAll(existingMonoliths);
            boolean participantsBuilt = spawned.stream().allMatch(entity -> entity.monolithTimer() < 700);
            String failure = null;
            if (newMonoliths.size() != 1 || !participantsBuilt) {
                var participants = helper.getLevel().getEntitiesOfClass(ShoggothEntity.class,
                        new AABB(center).inflate(20));
                String state = participants.stream().map(entity -> "ticks=" + entity.tickCount
                        + ",timer=" + entity.monolithTimer() + ",building=" + entity.isBuilding()
                        + ",target=" + entity.getTarget())
                        .toList().toString();
                String spawnedState = spawned.stream().map(entity -> "ticks=" + entity.tickCount
                        + ",removed=" + entity.isRemoved() + ",pos=" + entity.blockPosition())
                        .toList().toString();
                failure = "Autonomous group should build one new monolith, found " + newMonoliths
                        + "; participantsBuilt=" + participantsBuilt
                        + "; preexisting=" + existingMonoliths
                        + "; difficulty=" + helper.getLevel().getDifficulty()
                        + "; phase=" + parity + "; day=" + helper.getLevel().getDayTime()
                        + "; spacing=" + MonolithSiteRules.farFromExistingMonoliths(helper.getLevel(), center)
                        + "; participants=" + participants.size() + " " + state
                        + "; spawned=" + spawnedState;
            }
            spawned.forEach(ShoggothEntity::discard);
            newMonoliths.forEach(effigy -> clearTestMonolith(helper, effigy));
            tickingChunks.forEach(chunk -> helper.getLevel().getChunkSource()
                    .removeRegionTicket(TicketType.FORCED, chunk, 2, chunk));
            previousModes.forEach((player, mode) -> player.setGameMode(mode));
            helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(previousGriefing, helper.getLevel().getServer());
            helper.getLevel().setDayTime(previousDay);
            helper.getLevel().updateSkyBrightness();
            if (failure != null) {
                helper.fail(failure);
            } else {
                helper.succeed();
            }
        });
    }

    private static Set<BlockPos> monolithEffigiesNear(GameTestHelper helper, BlockPos center) {
        Set<BlockPos> found = new HashSet<>();
        // The arena wall does not confine site selection; include the ticketed surroundings.
        for (int x = -20; x <= 20; x++) {
            for (int z = -20; z <= 20; z++) {
                for (int y = 3; y <= 15; y++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (StatueContent.isStatue(helper.getLevel().getBlockState(pos))) {
                        found.add(pos);
                    }
                }
            }
        }
        return found;
    }

    @GameTest(template = "empty")
    public static void monolithRespectsObstructionsAndOozeLayers(GameTestHelper helper) {
        BlockPos center = helper.absolutePos(new BlockPos(4, 1, 4));
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos base = center.offset(x, 0, z);
                helper.getLevel().setBlock(base.below(), Blocks.STONE.defaultBlockState(), 3);
                for (int y = 0; y <= 12; y++) {
                    helper.getLevel().setBlock(base.above(y), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        helper.getLevel().setBlock(center.above(1), Blocks.IRON_BLOCK.defaultBlockState(), 3);
        if (MonolithBuilder.build(helper.getLevel(), center, 5, 3)
                || !helper.getLevel().getBlockState(center).is(Blocks.AIR)) {
            throw new AssertionError("Monolith overwrote an obstructed site");
        }
        helper.getLevel().setBlock(center.above(1), Blocks.AIR.defaultBlockState(), 3);
        helper.getLevel().setBlock(center.above(7), Blocks.STONE.defaultBlockState(), 3);
        if (MonolithBuilder.build(helper.getLevel(), center, 5, 3)) {
            throw new AssertionError("Monolith built below a solid roof: sky="
                    + helper.getLevel().canSeeSky(center.above(6)) + ", surface="
                    + helper.getLevel().getHeight(Heightmap.Types.WORLD_SURFACE, center.getX(), center.getZ())
                    + ", roof=" + helper.getLevel().getBlockState(center.above(7)));
        }
        helper.getLevel().setBlock(center.above(7), Blocks.AIR.defaultBlockState(), 3);
        helper.getLevel().setBlock(center, AbyssalPort.SHOGGOTH_OOZE.get().defaultBlockState()
                .setValue(ShoggothOozeBlock.LAYERS, 6), 3);
        if (!MonolithBuilder.build(helper.getLevel(), center, 5, 3)
                || !helper.getLevel().getBlockState(center).is(ShoggothLairContent.BIOMASS.get())) {
            throw new AssertionError("Monolith could not replace its own ooze residue safely");
        }
        clearTestMonolith(helper, center.above(5));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void abandonedMonolithProvidesSurvivalEnergySource(GameTestHelper helper) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID, "abandoned_monolith");
        if (!helper.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).containsKey(id)
                || !helper.getLevel().registryAccess().registryOrThrow(Registries.PLACED_FEATURE).containsKey(id)) {
            throw new AssertionError("Abandoned monolith worldgen data did not load");
        }
        var placed = helper.getLevel().registryAccess().registryOrThrow(Registries.PLACED_FEATURE).get(id);
        var darkForest = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME)
                .getHolderOrThrow(Biomes.DARK_FOREST).value();
        if (!darkForest.getGenerationSettings().hasFeature(placed)) {
            throw new AssertionError("Biome modifier did not add monoliths to dark forests");
        }

        BlockPos center = chunkSafeCenter(helper);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos base = center.offset(x, 0, z);
                helper.getLevel().setBlock(base.below(), Blocks.STONE.defaultBlockState(), 3);
                for (int y = 0; y < 9; y++) {
                    helper.getLevel().setBlock(base.above(y), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        FeaturePlaceContext<NoneFeatureConfiguration> context = new FeaturePlaceContext<>(Optional.empty(),
                helper.getLevel(), helper.getLevel().getChunkSource().getGenerator(), RandomSource.create(7),
                center, NoneFeatureConfiguration.INSTANCE);
        helper.getLevel().setBlock(center.above(1), Blocks.STONE.defaultBlockState(), 3);
        if (!AbyssalPort.ABANDONED_MONOLITH.get().place(context)
                || !helper.getLevel().getBlockState(center.above(1)).is(Blocks.STONE)) {
            throw new AssertionError("Monolith did not find a safe fallback or damaged the obstructed site");
        }
        BlockPos fallbackEffigy = null;
        for (int x = 2; x <= 13; x++) {
            for (int z = 2; z <= 13; z++) {
                for (int y = center.getY() + 5; y <= center.getY() + 9; y++) {
                    BlockPos candidate = new BlockPos((center.getX() & ~15) + x, y,
                            (center.getZ() & ~15) + z);
                    if (StatueContent.isStatue(helper.getLevel().getBlockState(candidate))) {
                        fallbackEffigy = candidate;
                    }
                }
            }
        }
        if (fallbackEffigy == null || fallbackEffigy.getX() == center.getX()
                && fallbackEffigy.getZ() == center.getZ()) {
            throw new AssertionError("Fallback did not create an effigy away from the blocked origin");
        }
        clearTestMonolith(helper, fallbackEffigy);
        helper.getLevel().setBlock(center.above(1), Blocks.AIR.defaultBlockState(), 3);
        if (!AbyssalPort.ABANDONED_MONOLITH.get().place(context)) {
            throw new AssertionError("Monolith failed to generate on a clear supported site");
        }
        BlockPos statuePos = null;
        for (int y = 5; y <= 7; y++) {
            if (StatueContent.isStatue(helper.getLevel().getBlockState(center.above(y)))) {
                statuePos = center.above(y);
            }
        }
        if (!helper.getLevel().getBlockState(center).is(ShoggothLairContent.BIOMASS.get())
                || statuePos == null
                || !(helper.getLevel().getBlockEntity(statuePos) instanceof PrototypeEffigyBlockEntity effigy)) {
            throw new AssertionError("Monolith did not place biomass, monolith stone and a working deity statue");
        }
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ItemStack book = new ItemStack(AbyssalPort.NECRONOMICON.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, book);
        player.teleportTo(statuePos.getX() + 0.5, statuePos.getY() + 1, statuePos.getZ() + 0.5);
        for (int tick = 0; tick < 120; tick++) {
            PrototypeEffigyBlockEntity.serverTick(helper.getLevel(), statuePos,
                    helper.getLevel().getBlockState(statuePos), effigy);
        }
        if (NecronomiconItem.getPotentialEnergy(book) != 5) {
            throw new AssertionError("Naturally placed effigy did not provide usable PE");
        }
        if (!EarlyResearch.has(player, EarlyResearch.MONOLITH)
                || EarlyResearch.unlock(player, EarlyResearch.MONOLITH)) {
            throw new AssertionError("Monolith discovery was not granted exactly once");
        }
        CompoundTag playerSave = player.saveWithoutId(new CompoundTag());
        if (!playerSave.toString().contains("abyssalport:early_research")) {
            throw new AssertionError("Player discovery attachment was not written to save data");
        }
        player.setData(AbyssalPort.EARLY_RESEARCH.get(), 0);
        player.load(playerSave);
        if (!EarlyResearch.has(player, EarlyResearch.MONOLITH)) {
            throw new AssertionError("Player discovery attachment was not restored from save data");
        }
        clearTestMonolith(helper, statuePos);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void necronomiconFormsCompleteCobblestoneGround(GameTestHelper helper) {
        BlockPos center = chunkSafeCenter(helper);
        placeCobblestoneGround(helper, center);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ItemStack flesh = new ItemStack(Items.ROTTEN_FLESH);
        ItemStack iron = new ItemStack(Items.IRON_INGOT);
        CraftingInput bookInput = CraftingInput.of(3, 3, java.util.List.of(
                flesh, flesh, iron,
                flesh, new ItemStack(Items.BOOK), flesh,
                flesh, flesh, iron));
        var bookRecipe = helper.getLevel().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, bookInput, helper.getLevel())
                .orElseThrow(() -> new AssertionError("Survival Necronomicon recipe missing"));
        ItemStack book = bookRecipe.value().assemble(bookInput, helper.getLevel().registryAccess());
        if (!bookRecipe.id().equals(ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID, "necronomicon"))
                || !book.is(AbyssalPort.NECRONOMICON.get()) || book.getCount() != 1) {
            throw new AssertionError("Survival ingredients did not craft one Necronomicon");
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, book);
        UseOnContext click = new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(center), Direction.UP, center, false));
        InteractionResult formed = AbyssalPort.NECRONOMICON.get().useOn(click);
        if (!formed.consumesAction()
                || !helper.getLevel().getBlockState(center).is(AbyssalPort.RITUAL_ALTAR.get())
                || !EarlyResearch.has(player, EarlyResearch.RITUAL_GROUND)) {
            throw new AssertionError("Complete cobblestone ground did not form an altar: " + formed);
        }
        for (BlockPos offset : BasicScrollRitual.pedestalOffsets()) {
            if (!helper.getLevel().getBlockState(center.offset(offset)).is(AbyssalPort.RITUAL_PEDESTAL.get())) {
                throw new AssertionError("Formation omitted a pedestal at " + offset);
            }
        }
        for (int index = 0; index < 4; index++) {
            BlockPos pedestalPos = center.offset(BasicScrollRitual.pedestalOffsets().get(index));
            if (!(helper.getLevel().getBlockEntity(pedestalPos) instanceof RitualPedestalBlockEntity pedestal)) {
                throw new AssertionError("Formed pedestal has no block entity");
            }
            ItemStack offering = new ItemStack(Items.BOOK);
            player.setItemInHand(InteractionHand.MAIN_HAND, offering);
            var placed = helper.getLevel().getBlockState(pedestalPos).useItemOn(offering, helper.getLevel(), player,
                    InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(pedestalPos), Direction.UP, pedestalPos, false));
            if (!placed.consumesAction() || !offering.isEmpty() || !pedestal.getOffering().is(Items.BOOK)) {
                throw new AssertionError("Right-click did not place exactly one book on the pedestal");
            }
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, book);
        player.teleportTo(center.getX() + 0.5, center.getY() + 1, center.getZ() + 0.5);
        BlockPos effigyPos = center.above(4);
        BlockState previousEffigyState = helper.getLevel().getBlockState(effigyPos);
        try {
            helper.getLevel().setBlock(effigyPos, AbyssalPort.PROTOTYPE_EFFIGY.get().defaultBlockState(), 3);
            if (!(helper.getLevel().getBlockEntity(effigyPos) instanceof PrototypeEffigyBlockEntity effigy)) {
                throw new AssertionError("Survival PE source has no block entity");
            }
            for (int tick = 0; tick < 120 * (BasicScrollRitual.ENERGY_COST / 5); tick++) {
                PrototypeEffigyBlockEntity.serverTick(helper.getLevel(), effigyPos,
                        helper.getLevel().getBlockState(effigyPos), effigy);
            }
        } finally {
            helper.getLevel().setBlock(effigyPos, previousEffigyState, 3);
        }
        if (NecronomiconItem.getPotentialEnergy(book) != BasicScrollRitual.ENERGY_COST
                || !EarlyResearch.has(player, EarlyResearch.MONOLITH)) {
            throw new AssertionError("Effigy did not charge the crafted book for the first ritual");
        }
        Set<UUID> scrollsBefore = scrollIdsNear(helper, center);
        if (!(helper.getLevel().getBlockEntity(center) instanceof RitualAltarBlockEntity altar)) {
            throw new AssertionError("Formed altar has no block entity");
        }
        var started = helper.getLevel().getBlockState(center).useItemOn(book, helper.getLevel(), player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(center), Direction.UP, center, false));
        if (!started.consumesAction() || !altar.isRunning()) {
            throw new AssertionError("Right-click did not start the first ritual");
        }
        for (int tick = 0; tick < BasicScrollRitual.DURATION_TICKS; tick++) {
            RitualAltarBlockEntity.serverTick(helper.getLevel(), center,
                    helper.getLevel().getBlockState(center), altar);
        }
        Set<UUID> newScrolls = scrollIdsNear(helper, center);
        newScrolls.removeAll(scrollsBefore);
        if (altar.isRunning() || NecronomiconItem.getPotentialEnergy(book) != 0
                || newScrolls.size() != 1 || !EarlyResearch.has(player, EarlyResearch.BASIC_SCROLL)) {
            throw new AssertionError("Formed ground did not complete the first ritual");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void invalidGroundNeverChangesCobblestone(GameTestHelper helper) {
        BlockPos center = chunkSafeCenter(helper);
        placeCobblestoneGround(helper, center);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ItemStack book = new ItemStack(AbyssalPort.NECRONOMICON.get());
        BlockPos missing = center.offset(BasicScrollRitual.pedestalOffsets().get(0));
        helper.getLevel().setBlock(missing, Blocks.AIR.defaultBlockState(), 3);
        if (RitualGroundFormation.form(helper.getLevel(), center, player, book)
                != RitualGroundFormation.Result.INCOMPLETE) {
            throw new AssertionError("Incomplete ground was accepted");
        }
        helper.getLevel().setBlock(missing, Blocks.COBBLESTONE.defaultBlockState(), 3);
        BlockPos obstruction = missing.west();
        helper.getLevel().setBlock(obstruction, Blocks.COBBLESTONE.defaultBlockState(), 3);
        if (RitualGroundFormation.form(helper.getLevel(), center, player, book)
                != RitualGroundFormation.Result.OBSTRUCTED) {
            throw new AssertionError("Obstructed ground was accepted");
        }
        if (!helper.getLevel().getBlockState(center).is(Blocks.COBBLESTONE)) {
            throw new AssertionError("Failed formation changed the central block");
        }
        if (EarlyResearch.has(player, EarlyResearch.RITUAL_GROUND)) {
            throw new AssertionError("Failed formation unlocked the ritual discovery");
        }
        for (BlockPos offset : BasicScrollRitual.pedestalOffsets()) {
            if (!helper.getLevel().getBlockState(center.offset(offset)).is(Blocks.COBBLESTONE)) {
                throw new AssertionError("Failed formation changed a pedestal block");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void groundCrossingChunkBorderIsRejected(GameTestHelper helper) {
        BlockPos safe = chunkSafeCenter(helper);
        BlockPos center = new BlockPos((safe.getX() & ~15) + 1, safe.getY(), safe.getZ());
        placeCobblestoneGround(helper, center);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        if (RitualGroundFormation.form(helper.getLevel(), center, player,
                new ItemStack(AbyssalPort.NECRONOMICON.get()))
                != RitualGroundFormation.Result.CROSSES_CHUNK
                || !helper.getLevel().getBlockState(center).is(Blocks.COBBLESTONE)) {
            throw new AssertionError("Cross-chunk ground was formed or changed");
        }
        helper.succeed();
    }

    private static BlockPos chunkSafeCenter(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(4, 1, 4));
        return new BlockPos((origin.getX() & ~15) + 8, origin.getY(), (origin.getZ() & ~15) + 8);
    }

    private static void placeCobblestoneGround(GameTestHelper helper, BlockPos center) {
        Set<BlockPos> targets = new HashSet<>();
        targets.add(center);
        for (BlockPos offset : BasicScrollRitual.pedestalOffsets()) {
            targets.add(center.offset(offset));
        }
        for (BlockPos pedestal : targets) {
            if (pedestal.equals(center)) {
                continue;
            }
            for (BlockPos around : BlockPos.betweenClosed(pedestal.offset(-1, 0, -1),
                    pedestal.offset(1, 0, 1))) {
                if (!targets.contains(around)) {
                    helper.getLevel().setBlock(around.immutable(), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        for (BlockPos target : targets) {
            helper.getLevel().setBlock(target, Blocks.COBBLESTONE.defaultBlockState(), 3);
        }
    }

    @GameTest(template = "empty")
    public static void basicScrollAcceptsUnorderedFourBooks(GameTestHelper helper) {
        BlockPos altarPos = new BlockPos(4, 1, 4);
        helper.setBlock(altarPos, AbyssalPort.RITUAL_ALTAR.get());
        for (int index = 0; index < BasicScrollRitual.pedestalOffsets().size(); index++) {
            BlockPos pedestalPos = altarPos.offset(BasicScrollRitual.pedestalOffsets().get(index));
            helper.setBlock(pedestalPos, AbyssalPort.RITUAL_PEDESTAL.get());
            if (index >= 4) {
                ((RitualPedestalBlockEntity) helper.getBlockEntity(pedestalPos))
                        .placeOne(new ItemStack(Items.BOOK));
            }
        }
        var definition = RitualCatalog.INSTANCE.get(BasicScrollRitual.ID).orElseThrow();
        if (!definition.matches(helper.getLevel(), helper.absolutePos(altarPos))) {
            throw new AssertionError("Four diagonal books were rejected despite unordered upstream offerings");
        }
        definition.consumeOfferings(helper.getLevel(), helper.absolutePos(altarPos));
        for (BlockPos offset : BasicScrollRitual.pedestalOffsets()) {
            if (!((RitualPedestalBlockEntity) helper.getBlockEntity(altarPos.offset(offset)))
                    .getOffering().isEmpty()) {
                throw new AssertionError("Unordered book offering remained after consumption");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void energyCollectorStoresAndReturnsPEWithoutLoss(GameTestHelper helper) {
        if (helper.getLevel().getRecipeManager()
                .byKey(ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID, "energy_collector")).isEmpty()) {
            throw new AssertionError("Energy Collector recipe was not loaded");
        }
        BlockPos effigyPos = helper.absolutePos(new BlockPos(4, 1, 4));
        BlockPos collectorPos = effigyPos.offset(3, 0, 0);
        BlockPos tooClosePos = effigyPos.offset(1, 0, 0);
        BlockPos adjacentPos = collectorPos.offset(0, 0, 1);
        helper.getLevel().setBlock(effigyPos, AbyssalPort.PROTOTYPE_EFFIGY.get().defaultBlockState(), 3);
        helper.getLevel().setBlock(collectorPos, AbyssalPort.ENERGY_COLLECTOR.get().defaultBlockState(), 3);
        helper.getLevel().setBlock(tooClosePos, AbyssalPort.ENERGY_COLLECTOR.get().defaultBlockState(), 3);
        if (!(helper.getLevel().getBlockEntity(effigyPos) instanceof PrototypeEffigyBlockEntity effigy)
                || !(helper.getLevel().getBlockEntity(collectorPos) instanceof EnergyCollectorBlockEntity collector)
                || !(helper.getLevel().getBlockEntity(tooClosePos) instanceof EnergyCollectorBlockEntity tooClose)) {
            throw new AssertionError("PE source or collectors were not registered");
        }
        helper.getLevel().setBlock(adjacentPos, AbyssalPort.ENERGY_COLLECTOR.get().defaultBlockState(), 3);
        for (int tick = 0; tick < 120; tick++) {
            PrototypeEffigyBlockEntity.serverTick(helper.getLevel(), effigyPos,
                    helper.getLevel().getBlockState(effigyPos), effigy);
        }
        if (collector.energy() != 0) {
            throw new AssertionError("Adjacent collectors must not draw PE from the same effigy");
        }
        helper.getLevel().setBlock(adjacentPos, Blocks.AIR.defaultBlockState(), 3);
        for (int tick = 0; tick < 119; tick++) {
            PrototypeEffigyBlockEntity.serverTick(helper.getLevel(), effigyPos,
                    helper.getLevel().getBlockState(effigyPos), effigy);
        }
        if (collector.energy() != 0) {
            throw new AssertionError("Collector charged before the 120-tick interval");
        }
        PrototypeEffigyBlockEntity.serverTick(helper.getLevel(), effigyPos,
                helper.getLevel().getBlockState(effigyPos), effigy);
        if (collector.energy() != 5 || tooClose.energy() != 0) {
            throw new AssertionError("Effigy charged a collector outside the original distance ring");
        }
        collector.addEnergy(994);
        PrototypeEffigyBlockEntity.serverTick(helper.getLevel(), effigyPos,
                helper.getLevel().getBlockState(effigyPos), effigy);
        if (collector.energy() != 999 || collector.addEnergy(5) != 4 || collector.energy() != 1_000) {
            throw new AssertionError("Collector capacity or overflow is incorrect");
        }
        ItemStack book = new ItemStack(AbyssalPort.NECRONOMICON.get());
        if (collector.transferToBook(book) != 100 || collector.energy() != 900
                || NecronomiconItem.getPotentialEnergy(book) != 100
                || collector.transferFromBook(book) != 100 || collector.energy() != 1_000
                || NecronomiconItem.getPotentialEnergy(book) != 0) {
            throw new AssertionError("Collector/book transfer lost or duplicated PE");
        }
        var drops = Block.getDrops(helper.getLevel().getBlockState(collectorPos), helper.getLevel(),
                collectorPos, collector);
        if (drops.size() != 1 || !drops.getFirst().is(AbyssalPort.ENERGY_COLLECTOR_ITEM.get())) {
            throw new AssertionError("Mined collector did not drop exactly one collector item");
        }
        ItemStack drop = drops.getFirst();
        if (drop.get(DataComponents.CUSTOM_DATA) == null
                || drop.get(DataComponents.CUSTOM_DATA).copyTag().getFloat("PotEnergy") != 1_000) {
            throw new AssertionError("Mined collector lost its stored PE");
        }
        helper.getLevel().setBlock(collectorPos, Blocks.AIR.defaultBlockState(), 3);
        helper.getLevel().setBlock(collectorPos, AbyssalPort.ENERGY_COLLECTOR.get().defaultBlockState(), 3);
        AbyssalPort.ENERGY_COLLECTOR.get().setPlacedBy(helper.getLevel(), collectorPos,
                helper.getLevel().getBlockState(collectorPos), null, drop);
        if (!(helper.getLevel().getBlockEntity(collectorPos) instanceof EnergyCollectorBlockEntity replaced)
                || replaced.energy() != 1_000) {
            throw new AssertionError("Placed collector did not restore stored PE");
        }
        var saved = replaced.saveWithoutMetadata(helper.getLevel().registryAccess());
        EnergyCollectorBlockEntity restored = new EnergyCollectorBlockEntity(collectorPos,
                helper.getLevel().getBlockState(collectorPos));
        restored.loadWithComponents(saved, helper.getLevel().registryAccess());
        if (restored.energy() != 1_000) {
            throw new AssertionError("Collector PE did not persist through save/load");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void energyRelayMovesPEOnlyAlongClearUnpoweredPath(GameTestHelper helper) {
        for (String id : new String[] {"energy_relay", "energy_pedestal"}) {
            if (helper.getLevel().getRecipeManager()
                    .byKey(ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID, id)).isEmpty()) {
                throw new AssertionError(id + " recipe was not loaded");
            }
        }
        if (helper.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID, "energy_container")).isPresent()
                || dev.abyssalport.ritual.RitualCatalog.INSTANCE.get(ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID, "energy_container")).isEmpty()) {
            throw new AssertionError("Reservoir must use the canonical infusion, not the prototype chest craft");
        }
        BlockPos relayPos = helper.absolutePos(new BlockPos(4, 1, 4));
        BlockPos collectorPos = relayPos.west();
        BlockPos containerPos = relayPos.east(4);
        // Parts of the four-block beam lie outside the tiny template and can
        // contain scaffolding from an adjacent test. The simulation owns its path.
        java.util.Map<BlockPos, BlockState> originalBeam = new java.util.HashMap<>();
        for (int distance = 1; distance < 4; distance++) {
            BlockPos beam = relayPos.east(distance);
            originalBeam.put(beam, helper.getLevel().getBlockState(beam));
            helper.getLevel().setBlock(beam, Blocks.AIR.defaultBlockState(), 3);
            helper.assertTrue(helper.getLevel().hasChunkAt(beam), "Relay simulation beam chunk is not loaded: " + beam);
        }
        try {
            helper.getLevel().setBlock(collectorPos, AbyssalPort.ENERGY_COLLECTOR.get().defaultBlockState(), 3);
            helper.getLevel().setBlock(relayPos,
                    AbyssalPort.ENERGY_RELAY.get().defaultBlockState().setValue(EnergyRelayBlock.FACING, Direction.EAST), 3);
            helper.getLevel().setBlock(containerPos, AbyssalPort.ENERGY_CONTAINER.get().defaultBlockState(), 3);
            if (!(helper.getLevel().getBlockEntity(collectorPos) instanceof EnergyCollectorBlockEntity collector)
                    || !(helper.getLevel().getBlockEntity(relayPos) instanceof EnergyRelayBlockEntity)
                    || !(helper.getLevel().getBlockEntity(containerPos) instanceof EnergyContainerBlockEntity container)
                    || AbyssalPort.ENERGY_RELAY.get().getTicker(helper.getLevel(),
                        helper.getLevel().getBlockState(relayPos), AbyssalPort.ENERGY_RELAY_ENTITY.get()) == null) {
                throw new AssertionError("PE transfer blocks or server ticker are not registered");
            }
            // Use a fresh relay for exact manual tick assertions. The placed instance may
            // have received an ordinary world tick before this GameTest callback starts.
            EnergyRelayBlockEntity relay = new EnergyRelayBlockEntity(relayPos, helper.getLevel().getBlockState(relayPos));
            relay.setLevel(helper.getLevel());
            collector.addEnergy(100);
            tickRelay(helper, relayPos, relay, 19);
            if (collector.energy() != 100 || relay.energy() != 0 || container.energy() != 0) {
                throw new AssertionError("Relay transferred before 20 ticks");
            }
            tickRelay(helper, relayPos, relay, 1);
            if (collector.energy() != 90 || relay.energy() != 10 || container.energy() != 0) {
                throw new AssertionError("Relay failed to pull 10 PE on tick 20");
            }
            tickRelay(helper, relayPos, relay, 20);
            if (collector.energy() != 80 || relay.energy() != 0 || container.energy() != 20) {
                throw new AssertionError("Relay did not transfer the conserved 20 PE on tick 40: collector="
                        + collector.energy() + ", relay=" + relay.energy() + ", container=" + container.energy());
            }
            BlockPos obstruction = relayPos.east(2);
            helper.getLevel().setBlock(obstruction, Blocks.STONE.defaultBlockState(), 3);
            tickRelay(helper, relayPos, relay, 40);
            if (collector.energy() != 60 || relay.energy() != 20 || container.energy() != 20) {
                throw new AssertionError("Relay transferred through a solid obstruction");
            }
            helper.getLevel().setBlock(obstruction, Blocks.AIR.defaultBlockState(), 3);
            helper.getLevel().setBlock(relayPos.above(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
            tickRelay(helper, relayPos, relay, 40);
            if (collector.energy() != 60 || relay.energy() != 20 || container.energy() != 20) {
                throw new AssertionError("Redstone-powered relay did not pause");
            }
            helper.getLevel().setBlock(relayPos.above(), Blocks.AIR.defaultBlockState(), 3);
            tickRelay(helper, relayPos, relay, 40);
            if (collector.energy() != 40 || relay.energy() != 20 || container.energy() != 40) {
                throw new AssertionError("Relay did not resume after removing power or lost PE");
            }
            container.addEnergy(9_955);
            tickRelay(helper, relayPos, relay, 40);
            if (collector.energy() != 20 || relay.energy() != 35 || container.energy() != 10_000) {
                throw new AssertionError("Relay did not respect available container capacity");
            }
            tickRelay(helper, relayPos, relay, 40);
            if (collector.energy() != 0 || relay.energy() != 55 || container.energy() != 10_000) {
                throw new AssertionError("Full container lost energy or failed to stop overflow");
            }
        } finally {
            originalBeam.forEach((beam, state) -> helper.getLevel().setBlock(beam, state, 3));
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void energyRelayAndContainerKeepPEInItemAndSave(GameTestHelper helper) {
        BlockPos relayPos = helper.absolutePos(new BlockPos(3, 1, 3));
        BlockPos containerPos = relayPos.east(2);
        helper.getLevel().setBlock(relayPos, AbyssalPort.ENERGY_RELAY.get().defaultBlockState(), 3);
        helper.getLevel().setBlock(containerPos, AbyssalPort.ENERGY_CONTAINER.get().defaultBlockState(), 3);
        if (!(helper.getLevel().getBlockEntity(relayPos) instanceof EnergyRelayBlockEntity relay)
                || !(helper.getLevel().getBlockEntity(containerPos) instanceof EnergyContainerBlockEntity container)) {
            throw new AssertionError("Relay or container block entity is missing");
        }
        relay.addEnergy(430);
        container.addEnergy(7_650);
        assertEnergySurvivesDropAndPlacement(helper, relayPos, relay, AbyssalPort.ENERGY_RELAY_ITEM.get(), 430);
        assertEnergySurvivesDropAndPlacement(helper, containerPos, container,
                AbyssalPort.ENERGY_CONTAINER_ITEM.get(), 7_650);
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "container_inventory")
    public static void energyContainerChargesBooksAndKeepsInventory(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(3, 1, 3));
        helper.getLevel().setBlock(pos, AbyssalPort.ENERGY_CONTAINER.get().defaultBlockState(), 3);
        if (!(helper.getLevel().getBlockEntity(pos) instanceof EnergyContainerBlockEntity container)
                || AbyssalPort.ENERGY_CONTAINER.get().getTicker(helper.getLevel(),
                        helper.getLevel().getBlockState(pos), AbyssalPort.ENERGY_CONTAINER_ENTITY.get()) == null) {
            throw new AssertionError("Energy Container menu/ticker block entity is missing");
        }
        ItemStack input = new ItemStack(AbyssalPort.NECRONOMICON.get());
        NecronomiconItem.setPotentialEnergy(input, 30);
        container.items().setStackInSlot(0, input);
        EnergyContainerBlockEntity.serverTick(helper.getLevel(), pos,
                helper.getLevel().getBlockState(pos), container);
        if (container.energy() != 20 || NecronomiconItem.getPotentialEnergy(input) != 10) {
            throw new AssertionError("Input book did not charge the container by 20 PE");
        }
        EnergyContainerBlockEntity.serverTick(helper.getLevel(), pos,
                helper.getLevel().getBlockState(pos), container);
        if (container.energy() != 30 || NecronomiconItem.getPotentialEnergy(input) != 0) {
            throw new AssertionError("Input book lost PE at the lower boundary");
        }
        ItemStack output = new ItemStack(AbyssalPort.NECRONOMICON.get());
        NecronomiconItem.setPotentialEnergy(output, 4_995);
        container.items().setStackInSlot(1, output);
        EnergyContainerBlockEntity.serverTick(helper.getLevel(), pos,
                helper.getLevel().getBlockState(pos), container);
        if (container.energy() != 25 || NecronomiconItem.getPotentialEnergy(output) != 5_000) {
            throw new AssertionError("Output book overfilled or PE was lost");
        }
        var saved = container.saveWithoutMetadata(helper.getLevel().registryAccess());
        EnergyContainerBlockEntity restored = new EnergyContainerBlockEntity(pos,
                helper.getLevel().getBlockState(pos));
        restored.loadWithComponents(saved, helper.getLevel().registryAccess());
        if (restored.energy() != 25 || restored.items().getSlots() != 2
                || NecronomiconItem.getPotentialEnergy(restored.items().getStackInSlot(0)) != 0
                || NecronomiconItem.getPotentialEnergy(restored.items().getStackInSlot(1)) != 5_000) {
            throw new AssertionError("Container PE or book slots did not survive NBT roundtrip");
        }
        CompoundTag legacy = new CompoundTag();
        legacy.putFloat("PotEnergy", 77);
        restored.loadWithComponents(legacy, helper.getLevel().registryAccess());
        if (restored.energy() != 77 || !restored.items().getStackInSlot(0).isEmpty()
                || !restored.items().getStackInSlot(1).isEmpty()) {
            throw new AssertionError("Legacy PE-only container save was not loaded safely");
        }
        container.addEnergy(9_975);
        NecronomiconItem.setPotentialEnergy(input, 100);
        EnergyContainerBlockEntity.serverTick(helper.getLevel(), pos,
                helper.getLevel().getBlockState(pos), container);
        if (container.energy() != 10_000 || NecronomiconItem.getPotentialEnergy(input) != 100) {
            throw new AssertionError("Full container consumed an input book");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "container_live", timeoutTicks = 80)
    public static void placedEnergyContainerTicksItsBookSlots(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(3, 1, 3));
        ChunkPos chunk = new ChunkPos(pos);
        helper.getLevel().getChunkSource().addRegionTicket(TicketType.FORCED, chunk, 2, chunk);
        helper.getLevel().setBlock(pos, AbyssalPort.ENERGY_CONTAINER.get().defaultBlockState(), 3);
        if (!(helper.getLevel().getBlockEntity(pos) instanceof EnergyContainerBlockEntity container)) {
            helper.getLevel().getChunkSource().removeRegionTicket(TicketType.FORCED, chunk, 2, chunk);
            throw new AssertionError("Placed Energy Container is missing");
        }
        ItemStack book = new ItemStack(AbyssalPort.NECRONOMICON.get());
        NecronomiconItem.setPotentialEnergy(book, 60);
        container.items().setStackInSlot(0, book);
        helper.runAfterDelay(45, () -> {
            helper.getLevel().getChunkSource().removeRegionTicket(TicketType.FORCED, chunk, 2, chunk);
            if (container.energy() != 60 || NecronomiconItem.getPotentialEnergy(book) != 0) {
                helper.fail("Placed container did not tick its input book or lost PE");
            } else {
                helper.succeed();
            }
        });
    }

    @GameTest(template = "empty", batch = "container_inventory", timeoutTicks = 40)
    public static void energyContainerDropsStoredBooksWhenRemoved(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(3, 1, 3));
        helper.getLevel().setBlock(pos, AbyssalPort.ENERGY_CONTAINER.get().defaultBlockState(), 3);
        if (!(helper.getLevel().getBlockEntity(pos) instanceof EnergyContainerBlockEntity container)) {
            throw new AssertionError("Energy Container block entity is missing");
        }
        ItemStack book = new ItemStack(AbyssalPort.NECRONOMICON.get());
        NecronomiconItem.setPotentialEnergy(book, 123);
        container.items().setStackInSlot(0, book);
        helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        helper.runAfterDelay(2, () -> {
            long matching = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                    new AABB(pos).inflate(1.5)).stream()
                    .filter(item -> item.getItem().is(AbyssalPort.NECRONOMICON.get())
                            && NecronomiconItem.getPotentialEnergy(item.getItem()) == 123).count();
            if (matching != 1) {
                helper.fail("Stored book was lost or duplicated on block removal: " + matching);
            } else {
                helper.succeed();
            }
        });
    }

    @GameTest(template = "empty", batch = "research_devices")
    public static void bookStudiesCollectorAndNetworkPerPlayer(GameTestHelper helper) {
        BlockPos collector = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos relay = collector.east(2);
        helper.getLevel().setBlock(collector, AbyssalPort.ENERGY_COLLECTOR.get().defaultBlockState(), 3);
        helper.getLevel().setBlock(relay, AbyssalPort.ENERGY_RELAY.get().defaultBlockState(), 3);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ItemStack book = new ItemStack(AbyssalPort.NECRONOMICON.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, book);
        player.teleportTo(collector.getX() + 0.5, collector.getY() + 1, collector.getZ() + 0.5);
        helper.getLevel().getBlockState(collector).useItemOn(book, helper.getLevel(), player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(collector), Direction.UP, collector, false));
        if (!EarlyResearch.has(player, EarlyResearch.COLLECTOR)
                || EarlyResearch.has(player, EarlyResearch.NETWORK)) {
            throw new AssertionError("Studying Collector unlocked the wrong discovery");
        }
        helper.getLevel().getBlockState(relay).useItemOn(book, helper.getLevel(), player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(relay), Direction.UP, relay, false));
        if (!EarlyResearch.has(player, EarlyResearch.NETWORK)) {
            throw new AssertionError("Studying Relay did not unlock the PE network");
        }
        ServerPlayer other = helper.makeMockServerPlayerInLevel();
        if (EarlyResearch.has(other, EarlyResearch.COLLECTOR)
                || EarlyResearch.has(other, EarlyResearch.NETWORK)) {
            throw new AssertionError("Device discoveries leaked to a different player");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "relay_live", timeoutTicks = 80)
    public static void placedRelayTicksWithoutManualInvocation(GameTestHelper helper) {
        BlockPos relayPos = helper.absolutePos(new BlockPos(4, 1, 4));
        ChunkPos testChunk = new ChunkPos(relayPos);
        helper.getLevel().getChunkSource().addRegionTicket(TicketType.FORCED, testChunk, 2, testChunk);
        BlockPos collectorPos = relayPos.west();
        BlockPos containerPos = relayPos.east(2);
        BlockPos gapPos = relayPos.east();
        BlockState originalGap = helper.getLevel().getBlockState(gapPos);
        // Other GameTests share this world; an old fixture block must not obstruct this clear-path test.
        helper.getLevel().setBlock(gapPos, Blocks.AIR.defaultBlockState(), 3);
        helper.getLevel().setBlock(collectorPos, AbyssalPort.ENERGY_COLLECTOR.get().defaultBlockState(), 3);
        helper.getLevel().setBlock(relayPos,
                AbyssalPort.ENERGY_RELAY.get().defaultBlockState().setValue(EnergyRelayBlock.FACING, Direction.EAST), 3);
        helper.getLevel().setBlock(containerPos, AbyssalPort.ENERGY_CONTAINER.get().defaultBlockState(), 3);
        if (!(helper.getLevel().getBlockEntity(collectorPos) instanceof EnergyCollectorBlockEntity collector)
                || !(helper.getLevel().getBlockEntity(relayPos) instanceof EnergyRelayBlockEntity relay)
                || !(helper.getLevel().getBlockEntity(containerPos) instanceof EnergyContainerBlockEntity container)) {
            helper.getLevel().getChunkSource().removeRegionTicket(TicketType.FORCED, testChunk, 2, testChunk);
            helper.getLevel().setBlock(gapPos, originalGap, 3);
            throw new AssertionError("Placed PE chain is incomplete");
        }
        collector.addEnergy(100);
        helper.runAfterDelay(45, () -> {
            helper.getLevel().getChunkSource().removeRegionTicket(TicketType.FORCED, testChunk, 2, testChunk);
            helper.getLevel().setBlock(gapPos, originalGap, 3);
            if (container.energy() != 20 || collector.energy() + relay.energy() + container.energy() != 100) {
                helper.fail("Placed relay did not tick or PE was not conserved: collector=" + collector.energy()
                        + ", relay=" + relay.energy() + ", container=" + container.energy());
            } else {
                helper.succeed();
            }
        });
    }

    @GameTest(template = "empty")
    public static void relaySelectionShapeFollowsItsFacing(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        Direction[] directions = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        double[][] expected = {
                {2, 4, 14, 16}, {0, 2, 12, 14},
                {2, 0, 14, 12}, {4, 2, 16, 14}
        };
        for (int index = 0; index < directions.length; index++) {
            var state = AbyssalPort.ENERGY_RELAY.get().defaultBlockState()
                    .setValue(EnergyRelayBlock.FACING, directions[index]);
            AABB bounds = state.getShape(helper.getLevel(), pos).bounds();
            double[] target = expected[index];
            if (Math.abs(bounds.minX * 16 - target[0]) > 0.001
                    || Math.abs(bounds.minZ * 16 - target[1]) > 0.001
                    || Math.abs(bounds.maxX * 16 - target[2]) > 0.001
                    || Math.abs(bounds.maxZ * 16 - target[3]) > 0.001
                    || Math.abs(bounds.minY * 16 - 1) > 0.001
                    || Math.abs(bounds.maxY * 16 - 15) > 0.001) {
                helper.fail("Relay shape does not match " + directions[index] + ": " + bounds);
                return;
            }
        }
        helper.succeed();
    }

    private static void tickRelay(GameTestHelper helper, BlockPos pos, EnergyRelayBlockEntity relay, int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            EnergyRelayBlockEntity.serverTick(helper.getLevel(), pos, helper.getLevel().getBlockState(pos), relay);
        }
    }

    private static void assertEnergySurvivesDropAndPlacement(GameTestHelper helper, BlockPos pos,
            dev.abyssalport.energy.EnergyStorageBlockEntity storage, net.minecraft.world.item.Item expectedItem,
            int expectedEnergy) {
        var drops = Block.getDrops(helper.getLevel().getBlockState(pos), helper.getLevel(), pos, storage);
        if (drops.size() != 1 || !drops.getFirst().is(expectedItem)
                || drops.getFirst().get(DataComponents.CUSTOM_DATA) == null
                || drops.getFirst().get(DataComponents.CUSTOM_DATA).copyTag().getFloat("PotEnergy") != expectedEnergy) {
            throw new AssertionError("Mined PE device lost its energy or dropped the wrong item");
        }
        ItemStack dropped = drops.getFirst();
        var state = helper.getLevel().getBlockState(pos);
        var saved = storage.saveWithoutMetadata(helper.getLevel().registryAccess());
        helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        helper.getLevel().setBlock(pos, state, 3);
        state.getBlock().setPlacedBy(helper.getLevel(), pos, state, null, dropped);
        if (!(helper.getLevel().getBlockEntity(pos) instanceof dev.abyssalport.energy.EnergyStorageBlockEntity replaced)
                || replaced.energy() != expectedEnergy) {
            throw new AssertionError("Placed PE device lost its energy");
        }
        replaced.loadWithComponents(saved, helper.getLevel().registryAccess());
        if (replaced.energy() != expectedEnergy) {
            throw new AssertionError("PE device energy did not persist through save/load");
        }
    }

    @GameTest(template = "empty")
    public static void registeredContentAndEnergyTransfer(GameTestHelper helper) {
        if (helper.getLevel().getRecipeManager()
                .byKey(ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID, "necronomicon")).isEmpty()) {
            throw new AssertionError("Necronomicon recipe was not loaded");
        }
        if (AbyssalPort.DARKSTONE.get().asItem() != AbyssalPort.DARKSTONE_ITEM.get()) {
            throw new AssertionError("Darkstone block item is not registered correctly");
        }
        ItemStack book = new ItemStack(AbyssalPort.NECRONOMICON.get());
        if (NecronomiconItem.addPotentialEnergy(book, 5_200) != 200
                || NecronomiconItem.getPotentialEnergy(book) != 5_000) {
            throw new AssertionError("Potential energy did not cap at the first book tier");
        }
        if (NecronomiconItem.consumePotentialEnergy(book, 600) != 600
                || NecronomiconItem.getPotentialEnergy(book) != 4_400) {
            throw new AssertionError("Potential energy was not consumed correctly");
        }
        BlockPos effigyPos = new BlockPos(1, 1, 1);
        helper.setBlock(effigyPos, AbyssalPort.PROTOTYPE_EFFIGY.get());
        if (!(helper.getBlockEntity(effigyPos) instanceof PrototypeEffigyBlockEntity effigy)) {
            throw new AssertionError("PE source did not create a block entity");
        }
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ItemStack heldBook = new ItemStack(AbyssalPort.NECRONOMICON.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, heldBook);
        if (!PrototypeEffigyBlockEntity.chargeHeldBook(player)
                || NecronomiconItem.getPotentialEnergy(heldBook) != 5
                || !EarlyResearch.has(player, EarlyResearch.MONOLITH)) {
            throw new AssertionError("PE source did not charge a nearby held book");
        }
        BlockPos absoluteEffigyPos = helper.absolutePos(effigyPos);
        player.teleportTo(absoluteEffigyPos.getX() + 0.5, absoluteEffigyPos.getY() + 1,
                absoluteEffigyPos.getZ() + 0.5);
        ServerPlayer secondPlayer = helper.makeMockServerPlayerInLevel();
        ItemStack secondMain = new ItemStack(AbyssalPort.NECRONOMICON.get());
        ItemStack secondOffhand = new ItemStack(AbyssalPort.NECRONOMICON.get());
        secondPlayer.setItemInHand(InteractionHand.MAIN_HAND, secondMain);
        secondPlayer.setItemInHand(InteractionHand.OFF_HAND, secondOffhand);
        if (EarlyResearch.has(secondPlayer, EarlyResearch.MONOLITH)) {
            throw new AssertionError("Player discoveries leaked to another player");
        }
        secondPlayer.teleportTo(absoluteEffigyPos.getX() + 1.5, absoluteEffigyPos.getY() + 1,
                absoluteEffigyPos.getZ() + 0.5);
        for (int tick = 0; tick < 60; tick++) {
            PrototypeEffigyBlockEntity.serverTick(helper.getLevel(), absoluteEffigyPos,
                    helper.getBlockState(effigyPos), effigy);
        }
        var saved = effigy.saveWithoutMetadata(helper.getLevel().registryAccess());
        if (saved.getInt("ActiveTicks") != 60) {
            throw new AssertionError("PE source did not persist its charging progress");
        }
        PrototypeEffigyBlockEntity restored = new PrototypeEffigyBlockEntity(absoluteEffigyPos,
                helper.getBlockState(effigyPos));
        restored.loadWithComponents(saved, helper.getLevel().registryAccess());
        for (int tick = 0; tick < 60; tick++) {
            PrototypeEffigyBlockEntity.serverTick(helper.getLevel(), absoluteEffigyPos,
                    helper.getBlockState(effigyPos), restored);
        }
        if (NecronomiconItem.getPotentialEnergy(heldBook) != 10
                || NecronomiconItem.getPotentialEnergy(secondMain) != 5
                || NecronomiconItem.getPotentialEnergy(secondOffhand) != 5
                || !EarlyResearch.has(secondPlayer, EarlyResearch.MONOLITH)) {
            throw new AssertionError("PE source did not charge both players' held books after 120 ticks");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "ritual_scroll_completion")
    public static void basicScrollRitualCompletesWithoutDuplicatingOfferings(GameTestHelper helper) {
        var loadedDefinition = RitualCatalog.INSTANCE.get(BasicScrollRitual.ID)
                .orElseThrow(() -> new AssertionError("Basic Scroll datapack ritual was not loaded"));
        if (loadedDefinition.energyCost() != BasicScrollRitual.ENERGY_COST
                || loadedDefinition.durationTicks() != BasicScrollRitual.DURATION_TICKS
                || !loadedDefinition.signature().equals(BasicScrollRitual.legacySignature())) {
            throw new AssertionError("Basic Scroll datapack ritual differs from the legacy save definition");
        }
        BlockPos altarPos = new BlockPos(4, 1, 4);
        helper.setBlock(altarPos, AbyssalPort.RITUAL_ALTAR.get());
        BlockPos[] offsets = {
                new BlockPos(-3, 0, 0), new BlockPos(0, 0, -3),
                new BlockPos(3, 0, 0), new BlockPos(0, 0, 3),
                new BlockPos(-2, 0, 2), new BlockPos(-2, 0, -2),
                new BlockPos(2, 0, 2), new BlockPos(2, 0, -2)
        };
        for (int index = 0; index < offsets.length; index++) {
            BlockPos pedestalPos = altarPos.offset(offsets[index]);
            helper.setBlock(pedestalPos, AbyssalPort.RITUAL_PEDESTAL.get());
            if (index < 4) {
                RitualPedestalBlockEntity pedestal = (RitualPedestalBlockEntity) helper.getBlockEntity(pedestalPos);
                pedestal.placeOne(new ItemStack(Items.BOOK));
                if (index == 0) {
                    RitualPedestalBlockEntity restored = new RitualPedestalBlockEntity(
                            helper.absolutePos(pedestalPos), helper.getBlockState(pedestalPos));
                    restored.loadWithComponents(pedestal.saveWithoutMetadata(helper.getLevel().registryAccess()),
                            helper.getLevel().registryAccess());
                    if (!restored.getOffering().is(Items.BOOK)) {
                        throw new AssertionError("Pedestal offering did not survive NBT round-trip");
                    }
                }
            }
        }
        BlockPos absolutePos = helper.absolutePos(altarPos);
        RitualAltarBlockEntity altar = (RitualAltarBlockEntity) helper.getBlockEntity(altarPos);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.teleportTo(absolutePos.getX() + 3.5, absolutePos.getY() + 1,
                absolutePos.getZ() + 0.5);
        ItemStack book = new ItemStack(AbyssalPort.NECRONOMICON.get());
        NecronomiconItem.setPotentialEnergy(book, BasicScrollRitual.ENERGY_COST);
        player.setItemInHand(InteractionHand.MAIN_HAND, book);
        Set<UUID> scrollsBefore = scrollIdsNear(helper, absolutePos);
        if (!altar.start(helper.getLevel(), player) || !altar.isRunning()) {
            throw new AssertionError("Valid basic scroll ritual failed to start");
        }
        RitualAltarBlockEntity restoredAltar = new RitualAltarBlockEntity(absolutePos,
                helper.getBlockState(altarPos));
        restoredAltar.loadWithComponents(altar.saveWithoutMetadata(helper.getLevel().registryAccess()),
                helper.getLevel().registryAccess());
        if (!restoredAltar.isRunning() || restoredAltar.remainingTicks() != BasicScrollRitual.DURATION_TICKS) {
            throw new AssertionError("Ritual progress did not survive NBT round-trip");
        }
        CompoundTag altered = altar.saveWithoutMetadata(helper.getLevel().registryAccess());
        altered.putString("RitualSignature", "obsolete-definition");
        RitualAltarBlockEntity staleAltar = new RitualAltarBlockEntity(absolutePos, helper.getBlockState(altarPos));
        staleAltar.loadWithComponents(altered, helper.getLevel().registryAccess());
        RitualAltarBlockEntity.serverTick(helper.getLevel(), absolutePos, helper.getBlockState(altarPos), staleAltar);
        if (staleAltar.isRunning() || NecronomiconItem.getPotentialEnergy(book) != BasicScrollRitual.ENERGY_COST) {
            throw new AssertionError("Changed ritual definition was not canceled without charging PE");
        }
        CompoundTag legacy = altar.saveWithoutMetadata(helper.getLevel().registryAccess());
        legacy.remove("RitualId");
        legacy.remove("RitualSignature");
        RitualAltarBlockEntity oldAltar = new RitualAltarBlockEntity(absolutePos, helper.getBlockState(altarPos));
        oldAltar.loadWithComponents(legacy, helper.getLevel().registryAccess());
        RitualAltarBlockEntity.serverTick(helper.getLevel(), absolutePos, helper.getBlockState(altarPos), oldAltar);
        if (!oldAltar.isRunning() || oldAltar.remainingTicks() != BasicScrollRitual.DURATION_TICKS - 1) {
            throw new AssertionError("Valid pre-datapack ritual save did not resume");
        }
        for (int tick = 0; tick < BasicScrollRitual.DURATION_TICKS; tick++) {
            RitualAltarBlockEntity.serverTick(helper.getLevel(), absolutePos,
                    helper.getBlockState(altarPos), altar);
        }
        if (altar.isRunning() || NecronomiconItem.getPotentialEnergy(book) != 0) {
            throw new AssertionError("Ritual did not finish or consumed the wrong amount of PE");
        }
        for (int index = 0; index < 4; index++) {
            RitualPedestalBlockEntity pedestal = (RitualPedestalBlockEntity)
                    helper.getBlockEntity(altarPos.offset(offsets[index]));
            if (!pedestal.getOffering().isEmpty()) {
                throw new AssertionError("Ritual offering was not consumed");
            }
        }
        // Admission can precede entity-section accessibility on a newly loaded boundary chunk.
        // Wait for actual server integration; keep the player outside automatic pickup range.
        helper.runAfterDelay(2, () -> {
            Set<UUID> newScrolls = scrollIdsNear(helper, absolutePos);
            newScrolls.removeAll(scrollsBefore);
            if (newScrolls.size() != 1) {
                throw new AssertionError("Expected exactly one native, visible new basic scroll, got " + newScrolls.size());
            }
            if (!EarlyResearch.has(player, EarlyResearch.BASIC_SCROLL)) {
                throw new AssertionError("Completed ritual did not unlock Basic Scroll discovery");
            }
            if (altar.start(helper.getLevel(), player) || altar.isRunning()) {
                throw new AssertionError("Ritual restarted without offerings and energy");
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void interruptedRitualDoesNotConsumeEnergy(GameTestHelper helper) {
        BlockPos altarPos = new BlockPos(4, 1, 4);
        helper.setBlock(altarPos, AbyssalPort.RITUAL_ALTAR.get());
        BlockPos[] offsets = {
                new BlockPos(-3, 0, 0), new BlockPos(0, 0, -3),
                new BlockPos(3, 0, 0), new BlockPos(0, 0, 3),
                new BlockPos(-2, 0, 2), new BlockPos(-2, 0, -2),
                new BlockPos(2, 0, 2), new BlockPos(2, 0, -2)
        };
        for (int index = 0; index < offsets.length; index++) {
            BlockPos pedestalPos = altarPos.offset(offsets[index]);
            helper.setBlock(pedestalPos, AbyssalPort.RITUAL_PEDESTAL.get());
            if (index < 4) {
                ((RitualPedestalBlockEntity) helper.getBlockEntity(pedestalPos))
                        .placeOne(new ItemStack(Items.BOOK));
            }
        }
        BlockPos absolutePos = helper.absolutePos(altarPos);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.teleportTo(absolutePos.getX() + 0.5, absolutePos.getY() + 1,
                absolutePos.getZ() + 0.5);
        ItemStack book = new ItemStack(AbyssalPort.NECRONOMICON.get());
        NecronomiconItem.setPotentialEnergy(book, 150);
        player.setItemInHand(InteractionHand.MAIN_HAND, book);
        Set<UUID> scrollsBefore = scrollIdsNear(helper, absolutePos);
        RitualAltarBlockEntity altar = (RitualAltarBlockEntity) helper.getBlockEntity(altarPos);
        if (!altar.start(helper.getLevel(), player)) {
            throw new AssertionError("Valid ritual setup failed to start");
        }
        ((RitualPedestalBlockEntity) helper.getBlockEntity(altarPos.offset(offsets[0]))).removeOne();
        RitualAltarBlockEntity.serverTick(helper.getLevel(), absolutePos, helper.getBlockState(altarPos), altar);
        Set<UUID> newScrolls = scrollIdsNear(helper, absolutePos);
        newScrolls.removeAll(scrollsBefore);
        if (altar.isRunning() || NecronomiconItem.getPotentialEnergy(book) != 150
                || !newScrolls.isEmpty() || EarlyResearch.has(player, EarlyResearch.BASIC_SCROLL)) {
            throw new AssertionError("Interrupted ritual changed PE or produced a scroll");
        }
        helper.succeed();
    }

    private static Set<UUID> scrollIdsNear(GameTestHelper helper, BlockPos center) {
        Set<UUID> result = new HashSet<>();
        for (ItemEntity item : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(center).inflate(2))) {
            if (item.getItem().is(AbyssalPort.BASIC_SCROLL.get())) {
                result.add(item.getUUID());
            }
        }
        return result;
    }
}
