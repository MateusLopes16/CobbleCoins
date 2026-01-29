package com.mateuslopees.cobblecoins.registry;

import com.mateuslopees.cobblecoins.CobbleCoins;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = 
            DeferredRegister.create(Registries.MENU, CobbleCoins.MOD_ID);

    // Menu types can be registered here when needed for container-based GUIs
    // For now, we're using simple screen-based GUIs

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
