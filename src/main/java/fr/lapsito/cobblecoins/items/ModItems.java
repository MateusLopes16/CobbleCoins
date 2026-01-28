package fr.lapsito.cobblecoins.items;

import fr.lapsito.cobblecoins.CobbleCoinsMod;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registers mod items. */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CobbleCoinsMod.MODID);

    public static final DeferredItem<Item> COBBLE_COIN = ITEMS.register("cobblecoin",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> COBBLE_NOTE = ITEMS.register("cobblenote",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> COBBLE_WAD = ITEMS.register("cobblewad",
            () -> new Item(new Item.Properties()));

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
