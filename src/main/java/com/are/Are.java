package com.are;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

@Mod(Are.MODID)
public class Are {
    public static final String MODID = "are";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Are(IEventBus modBus, ModContainer container) {
        ModSounds.SOUNDS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModEntities.ITEMS.register(modBus);
        modBus.addListener(ModEntities::onAttributes);
        modBus.addListener(ModEntities::onCreativeTab);
        modBus.addListener(Net::onRegisterPayloads);

        container.registerConfig(ModConfig.Type.COMMON, AreConfig.SPEC);

        NeoForge.EVENT_BUS.addListener(FearDirector::onServerTick);
        NeoForge.EVENT_BUS.addListener(FearDirector::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(FearDirector::onLogin);
        NeoForge.EVENT_BUS.addListener(FearDirector::onCommands);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
