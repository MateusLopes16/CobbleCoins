package fr.lapsito.cobblecoins.items;

import fr.lapsito.cobblecoins.CobbleCoinsMod;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.registries.DeferredRegister;
import net.neoforged.registries.ForgeRegistries;
import net.neoforged.registries.RegistryObject;

/** Registers mod items. */
public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, CobbleCoinsMod.MODID);

    public static final RegistryObject<Item> COBBLE_COIN = ITEMS.register("cobblecoin",
            () -> new Item(new Item.Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(64)));

    public static final RegistryObject<Item> COBBLE_NOTE = ITEMS.register("cobblenote",
            () -> new Item(new Item.Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(64)));

    public static final RegistryObject<Item> COBBLE_WAD = ITEMS.register("cobblewad",
            () -> new Item(new Item.Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(64)));

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
