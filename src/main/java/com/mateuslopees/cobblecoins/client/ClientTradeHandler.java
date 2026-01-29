package com.mateuslopees.cobblecoins.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.UUID;

/**
 * Handles trade requests on the client side.
 */
public class ClientTradeHandler {
    private static UUID pendingTradeFrom = null;
    private static String pendingTradeFromName = null;
    private static long pendingTradeTime = 0;
    private static final long TRADE_TIMEOUT = 60000; // 60 seconds

    public static void handleTradeRequest(UUID fromPlayer, String fromPlayerName) {
        pendingTradeFrom = fromPlayer;
        pendingTradeFromName = fromPlayerName;
        pendingTradeTime = System.currentTimeMillis();

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(
                    Component.translatable("message.cobblecoins.trade_request", fromPlayerName)
                            .withStyle(ChatFormatting.GOLD),
                    false);
            mc.player.displayClientMessage(
                    Component.translatable("message.cobblecoins.trade_accept_hint")
                            .withStyle(ChatFormatting.GRAY),
                    false);
        }
    }

    public static boolean hasPendingTrade() {
        if (pendingTradeFrom != null && System.currentTimeMillis() - pendingTradeTime > TRADE_TIMEOUT) {
            clearPendingTrade();
        }
        return pendingTradeFrom != null;
    }

    public static UUID getPendingTradeFrom() {
        return pendingTradeFrom;
    }

    public static String getPendingTradeFromName() {
        return pendingTradeFromName;
    }

    public static void clearPendingTrade() {
        pendingTradeFrom = null;
        pendingTradeFromName = null;
        pendingTradeTime = 0;
    }
}
