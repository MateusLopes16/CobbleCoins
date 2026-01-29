package com.mateuslopees.cobblecoins.registry;

import com.mateuslopees.cobblecoins.CobbleCoins;
import com.mateuslopees.cobblecoins.item.CobbleCoinItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CobbleCoins.MOD_ID);

    // CobbleCoin - the main currency item, stackable to 1000
    public static final DeferredItem<Item> COBBLECOIN = ITEMS.register("cobblecoin",
            () -> new CobbleCoinItem(new Item.Properties()
                    .stacksTo(1000)));

    // Silver CobbleCoin - worth 100 regular CobbleCoins
    public static final DeferredItem<Item> SILVER_COBBLECOIN = ITEMS.register("silver_cobblecoin",
            () -> new CobbleCoinItem(new Item.Properties()
                    .stacksTo(1000), 100));

    // Gold CobbleCoin - worth 1000 regular CobbleCoins
    public static final DeferredItem<Item> GOLD_COBBLECOIN = ITEMS.register("gold_cobblecoin",
            () -> new CobbleCoinItem(new Item.Properties()
                    .stacksTo(1000), 1000));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
