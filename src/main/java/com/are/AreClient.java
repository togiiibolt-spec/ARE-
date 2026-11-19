package com.are;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Client-only setup. This class never loads on a dedicated server. */
@Mod(value = Are.MODID, dist = Dist.CLIENT)
public class AreClient {
    public AreClient(IEventBus modBus, ModContainer container) {
        modBus.addListener(AreClient::onLayers);
        modBus.addListener(AreClient::onRenderers);

        NeoForge.EVENT_BUS.addListener(ClientFx::onGui);
        NeoForge.EVENT_BUS.addListener(ClientFx::onTick);
        NeoForge.EVENT_BUS.addListener(ClientFx::onCamera);
        NeoForge.EVENT_BUS.addListener(ClientFx::onTitleRender);
        NeoForge.EVENT_BUS.addListener(ClientFx::onScreenOpening);
    }

    private static void onLayers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(HollowModel.LAYER, HollowModel::createBodyLayer);
    }

    private static void onRenderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(ModEntities.WATCHER.get(), ctx -> new GhostRenderer<WatcherEntity>(ctx, "watcher"));
        e.registerEntityRenderer(ModEntities.HOLLOW.get(), ctx -> new GhostRenderer<HollowEntity>(ctx, "hollow"));
    }
}
