package com.are;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Are.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Are.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<WatcherEntity>> WATCHER = ENTITIES.register("watcher",
            () -> EntityType.Builder.of(WatcherEntity::new, MobCategory.MONSTER)
                    .sized(0.7F, 2.9F).clientTrackingRange(12).fireImmune().build("watcher"));

    public static final DeferredHolder<EntityType<?>, EntityType<HollowEntity>> HOLLOW = ENTITIES.register("hollow",
            () -> EntityType.Builder.of(HollowEntity::new, MobCategory.MONSTER)
                    .sized(0.8F, 2.9F).clientTrackingRange(12).fireImmune().build("hollow"));

    public static final DeferredItem<DeferredSpawnEggItem> WATCHER_EGG = ITEMS.register("watcher_spawn_egg",
            () -> new DeferredSpawnEggItem(WATCHER, 0x050505, 0xEEEEEE, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> HOLLOW_EGG = ITEMS.register("hollow_spawn_egg",
            () -> new DeferredSpawnEggItem(HOLLOW, 0xA8AAB2, 0xFF0000, new Item.Properties()));

    public static void onAttributes(EntityAttributeCreationEvent e) {
        e.put(WATCHER.get(), WatcherEntity.createAttributes().build());
        e.put(HOLLOW.get(), HollowEntity.createAttributes().build());
    }

    public static void onCreativeTab(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            e.accept(WATCHER_EGG.get());
            e.accept(HOLLOW_EGG.get());
        }
    }
}
