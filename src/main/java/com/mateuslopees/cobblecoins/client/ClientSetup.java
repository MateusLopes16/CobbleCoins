package com.mateuslopees.cobblecoins.client;

import com.mateuslopees.cobblecoins.CobbleCoins;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.minecraft.resources.ResourceLocation;

@EventBusSubscriber(modid = CobbleCoins.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    
    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, 
                ResourceLocation.fromNamespaceAndPath(CobbleCoins.MOD_ID, "money_hud"),
                new MoneyHudOverlay());
        CobbleCoins.LOGGER.info("Registered CobbleCoins HUD overlay");
    }
}
