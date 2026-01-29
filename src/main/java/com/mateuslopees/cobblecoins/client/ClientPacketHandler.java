package com.mateuslopees.cobblecoins.client;

import com.mateuslopees.cobblecoins.client.screen.ShopScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.UUID;

/**
 * Client-side packet handler to safely handle client-only operations.
 * This class should only ever be loaded on the client side.
 */
@OnlyIn(Dist.CLIENT)
public class ClientPacketHandler {
    
    public static void handleSyncBalance(long balance) {
        ClientBankData.setBalance(balance);
    }
    
    public static void handleOpenShop(String shopData) {
        Minecraft.getInstance().setScreen(new ShopScreen(shopData));
    }
    
    public static void handleTradeRequest(UUID fromPlayer, String fromPlayerName) {
        ClientTradeHandler.handleTradeRequest(fromPlayer, fromPlayerName);
    }
}
