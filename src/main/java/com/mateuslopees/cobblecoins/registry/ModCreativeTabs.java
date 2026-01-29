package com.mateuslopees.cobblecoins.registry;

import com.mateuslopees.cobblecoins.CobbleCoins;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CobbleCoins.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> COBBLECOINS_TAB =
            CREATIVE_MODE_TABS.register("cobblecoins_tab", () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.COBBLECOIN.get()))
                    .title(Component.translatable("creativetab.cobblecoins"))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.COBBLECOIN.get());
                        output.accept(ModItems.SILVER_COBBLECOIN.get());
                        output.accept(ModItems.GOLD_COBBLECOIN.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
