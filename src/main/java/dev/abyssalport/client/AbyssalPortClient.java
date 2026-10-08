package dev.abyssalport.client;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.network.BookOpenPayload;
import dev.abyssalport.content.WastelandContent;
import dev.abyssalport.content.WastelandMobsContent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class AbyssalPortClient {
    public static void registerParticles(net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(dev.abyssalport.energy.PEStreams.STREAM.get(), PEStreamParticle.Provider::new);
    }
    public static void receivePEStream(dev.abyssalport.network.PEStreamPayload payload, IPayloadContext context) {
        var client=Minecraft.getInstance(); if(client.level==null) return;
        int stride=switch(client.options.particles().get()) { case ALL -> 1; case DECREASED -> 2; case MINIMAL -> 4; };
        var from=payload.from().getCenter(); var to=payload.to().getCenter();
        var samples=dev.abyssalport.energy.PEStreamGeometry.line(
                new dev.abyssalport.energy.PEStreamGeometry.Point(from.x,from.y,from.z),
                new dev.abyssalport.energy.PEStreamGeometry.Point(to.x,to.y,to.z),stride);
        for(var sample:samples) client.level.addParticle(dev.abyssalport.energy.PEStreams.STREAM.get(),
                sample.position().x(),sample.position().y(),sample.position().z(),sample.velocity().x(),sample.velocity().y(),sample.velocity().z());
    }
    public static void setupCutouts(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            net.minecraft.client.renderer.item.ItemProperties.register(dev.abyssalport.content.StateTransformerContent.TABLET.get(),
                    ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID, "cursed"),
                    (stack, level, entity, seed) -> dev.abyssalport.item.StoneTabletItem.isCursed(stack) ? 1 : 0);
            var layer = net.minecraft.client.renderer.RenderType.cutoutMipped();
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(dev.abyssalport.content.DarklandsWoodContent.LEAVES.get(), layer);
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(dev.abyssalport.content.DarklandsWoodContent.SAPLING.get(), net.minecraft.client.renderer.RenderType.cutout());
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(dev.abyssalport.content.DarklandsWoodContent.DOOR.get(), net.minecraft.client.renderer.RenderType.cutout());
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(dev.abyssalport.content.MobEcologyContent.MIMIC_FIRE.get(),net.minecraft.client.renderer.RenderType.cutout());
        });
    }
    private AbyssalPortClient() {
    }

    public static void registerRenderers(RegisterRenderers event) {
        for(boolean demon:new boolean[]{false,true})for(var kind:dev.abyssalport.entity.CorruptedAnimalEntity.Kind.values())
            event.registerEntityRenderer(dev.abyssalport.content.MobEcologyContent.type(kind,demon),context->new CorruptedAnimalRenderer(context,kind));
        event.registerEntityRenderer(dev.abyssalport.content.MobEcologyContent.OFFSPRING.get(),ShubOffspringRenderer::new);
        event.registerBlockEntityRenderer(AbyssalPort.RITUAL_PEDESTAL_ENTITY.get(), RitualPedestalRenderer::new);
        event.registerBlockEntityRenderer(dev.abyssalport.content.EnergyDeviceContent.PEDESTAL_ENTITY.get(), EnergyPedestalRenderer::new);
        event.registerBlockEntityRenderer(dev.abyssalport.content.SacrificialAltarContent.ENTITY.get(), EnergyPedestalRenderer::new);
        event.registerBlockEntityRenderer(AbyssalPort.RITUAL_ALTAR_ENTITY.get(), RitualAltarRenderer::new);
        event.registerEntityRenderer(AbyssalPort.LESSER_SHOGGOTH.get(), ShoggothRenderer::new);
        event.registerEntityRenderer(dev.abyssalport.content.ShoggothEcologyContent.ACID_PROJECTILE.get(),net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        for(var habitat:dev.abyssalport.entity.ShoggothEntity.Habitat.values()) for(var kind:dev.abyssalport.entity.ShoggothEntity.Kind.values())
            if(habitat!=dev.abyssalport.entity.ShoggothEntity.Habitat.OVERWORLD||kind!=dev.abyssalport.entity.ShoggothEntity.Kind.LESSER)
                event.registerEntityRenderer(dev.abyssalport.content.ShoggothEcologyContent.type(kind,habitat),ShoggothRenderer::new);
        event.registerEntityRenderer(AbyssalPort.SHADOW_CREATURE.get(), ShadowCreatureRenderer::new);
        event.registerEntityRenderer(dev.abyssalport.content.ShadowFamilyContent.MONSTER.get(), context ->
                new ShadowCreatureRenderer(context, dev.abyssalport.entity.ShadowCreatureEntity.Kind.MONSTER));
        event.registerEntityRenderer(dev.abyssalport.content.ShadowFamilyContent.BEAST.get(), context ->
                new ShadowCreatureRenderer(context, dev.abyssalport.entity.ShadowCreatureEntity.Kind.BEAST));
        event.registerEntityRenderer(WastelandMobsContent.ABYSSAL_ZOMBIE.get(), AbyssalZombieRenderer::new);
        event.registerEntityRenderer(dev.abyssalport.content.GraveyardContent.GHOUL.get(), GhoulRenderer::new);
        event.registerEntityRenderer(dev.abyssalport.content.GraveyardContent.SHADOW_GHOUL.get(), GhoulRenderer::new);
        event.registerEntityRenderer(dev.abyssalport.content.GraveyardContent.DEPTHS_GHOUL.get(), GhoulRenderer::new);
        event.registerEntityRenderer(dev.abyssalport.content.GraveyardContent.DREADED_GHOUL.get(), GhoulRenderer::new);
        event.registerEntityRenderer(dev.abyssalport.content.DreadlandsContent.DREADLING.get(), DreadlingRenderer::new);
        event.registerEntityRenderer(dev.abyssalport.content.DreadProcessingContent.GUARD.get(), DreadguardRenderer::new);
        event.registerEntityRenderer(dev.abyssalport.content.DreadEcologyContent.SMALL.get(), DreadSpawnRenderer::new);
        event.registerEntityRenderer(dev.abyssalport.content.DreadEcologyContent.GREATER.get(), DreadSpawnRenderer::new);
        event.registerEntityRenderer(dev.abyssalport.content.DreadEcologyContent.BEAST.get(), DreadSpawnRenderer::new);
        event.registerEntityRenderer(dev.abyssalport.content.DreadEcologyContent.SLUG.get(), DreadSlugRenderer::new);
        event.registerEntityRenderer(dev.abyssalport.content.WastelandBossContent.ASORAH.get(), AbyssalDragonRenderer::new);
        event.registerEntityRenderer(dev.abyssalport.content.WastelandBossContent.SPECTRAL_DRAGON.get(), AbyssalDragonRenderer::new);
    }

    public static void registerLayers(RegisterLayerDefinitions event) {
        for(var kind:dev.abyssalport.entity.CorruptedAnimalEntity.Kind.values())event.registerLayerDefinition(CorruptedAnimalModel.layer(kind),()->CorruptedAnimalModel.createLayer(kind));
        event.registerLayerDefinition(CorruptedSheepSkinLayer.LAYER,CorruptedSheepSkinLayer::createLayer);
        event.registerLayerDefinition(ShubOffspringModel.LAYER,ShubOffspringModel::createLayer);
        event.registerLayerDefinition(ShoggothModel.LAYER, ShoggothModel::createBodyLayer);
        event.registerLayerDefinition(ShadowCreatureModel.LAYER, ShadowCreatureModel::createBodyLayer);
        event.registerLayerDefinition(ShadowCreatureModel.MONSTER_LAYER, () -> ShadowCreatureModel.createLargeLayer(false));
        event.registerLayerDefinition(ShadowCreatureModel.BEAST_LAYER, () -> ShadowCreatureModel.createLargeLayer(true));
        event.registerLayerDefinition(AbyssalZombieModel.LAYER, AbyssalZombieModel::createLayer);
        event.registerLayerDefinition(GhoulModel.LAYER, GhoulModel::createLayer);
        event.registerLayerDefinition(DreadlingModel.LAYER, DreadlingModel::createLayer);
        event.registerLayerDefinition(DreadguardModel.LAYER, DreadguardModel::createLayer);
        event.registerLayerDefinition(DreadSpawnModel.LAYER, DreadSpawnModel::createLayer);
        event.registerLayerDefinition(DreadSlugRenderer.LAYER, DreadSlugRenderer::createLayer);
        event.registerLayerDefinition(AbyssalDragonModel.LAYER, AbyssalDragonModel::createLayer);
    }

    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(AbyssalPort.ENERGY_CONTAINER_MENU.get(), EnergyContainerScreen::new);
        event.register(dev.abyssalport.content.WastelandProcessingContent.MENU.get(), TransmutatorScreen::new);
        event.register(dev.abyssalport.content.CrystallizationContent.MENU.get(), CrystallizerScreen::new);
        event.register(dev.abyssalport.content.CrystalBagContent.MENU.get(), CrystalBagScreen::new);
        event.register(dev.abyssalport.content.StateTransformerContent.MENU.get(), StateTransformerScreen::new);
        event.register(dev.abyssalport.content.EnergyDepositionerContent.MENU.get(), EnergyDepositionerScreen::new);
    }

    public static void registerFluidExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override public ResourceLocation getStillTexture() { return WastelandContent.id("block/coralium_still"); }
            @Override public ResourceLocation getFlowingTexture() { return WastelandContent.id("block/coralium_flow"); }
        }, WastelandContent.CORALIUM_TYPE.get());
    }

    public static void receiveBookOpen(BookOpenPayload payload, IPayloadContext context) {
        var client = Minecraft.getInstance();
        if (client.player == null || payload.hand() < 0 || payload.hand() > 1) {
            return;
        }
        ItemStack book = client.player.getItemInHand(payload.hand() == 0
                ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
        if (dev.abyssalport.item.NecronomiconItem.isBook(book)) {
            client.setScreen(new NecronomiconScreen(book, payload.discoveries()));
        }
    }
}
