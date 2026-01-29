package com.mateuslopees.cobblecoins;

import com.mateuslopees.cobblecoins.config.CobbleCoinsConfig;
import com.mateuslopees.cobblecoins.config.JsonConfigManager;
import com.mateuslopees.cobblecoins.data.BankAccountManager;
import com.mateuslopees.cobblecoins.data.PlayerShopManager;
import com.mateuslopees.cobblecoins.event.CobblemonEventHandler;
import com.mateuslopees.cobblecoins.network.NetworkHandler;
import com.mateuslopees.cobblecoins.registry.ModCreativeTabs;
import com.mateuslopees.cobblecoins.registry.ModItems;
import com.mateuslopees.cobblecoins.registry.ModMenuTypes;
import com.mateuslopees.cobblecoins.shop.ShopManager;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;

@Mod(CobbleCoins.MOD_ID)
public class CobbleCoins {
    public static final String MOD_ID = "cobblecoins";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CobbleCoins(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Initializing CobbleCoins - Economy mod for Cobblemon!");

        // Register mod event listeners
        modEventBus.addListener(this::commonSetup);

        // Register items, menus, and creative tabs
        ModItems.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        
        // Register network handler
        NetworkHandler.register(modEventBus);

        // Register config
        modContainer.registerConfig(ModConfig.Type.COMMON, CobbleCoinsConfig.SPEC, "cobblecoins-common.toml");
        modContainer.registerConfig(ModConfig.Type.CLIENT, CobbleCoinsConfig.CLIENT_SPEC, "cobblecoins-client.toml");
        
        // Initialize JSON config manager (creates config/cobblecoins folder and JSON files)
        JsonConfigManager.init();

        // Register NeoForge event handlers
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(new CobblemonEventHandler());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("CobbleCoins common setup complete!");
        
        event.enqueueWork(() -> {
            // Initialize managers
            BankAccountManager.init();
            PlayerShopManager.init();
        });
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        BankAccountManager.onServerStarting(server);
        PlayerShopManager.setServerPath(server);
        ShopManager.init(server);
        LOGGER.info("CobbleCoins server data loaded!");
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        BankAccountManager.onServerStopping();
        PlayerShopManager.saveShops();
        LOGGER.info("CobbleCoins server data saved!");
    }
}
