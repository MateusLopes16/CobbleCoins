package com.mateuslopees.cobblecoins.trade;

import com.mateuslopees.cobblecoins.CobbleCoins;
import com.mateuslopees.cobblecoins.data.BankAccountManager;
import com.mateuslopees.cobblecoins.network.NetworkHandler;
import com.mateuslopees.cobblecoins.network.packet.TradeRequestPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TradeManager {
    // Pending trade requests: key = target player, value = requesting player
    private static final Map<UUID, TradeRequest> pendingRequests = new ConcurrentHashMap<>();
    // Active trade sessions
    private static final Map<UUID, TradeSession> activeTrades = new ConcurrentHashMap<>();
    
    private static final long REQUEST_TIMEOUT = 60000; // 60 seconds

    public static void requestTrade(ServerPlayer requester, ServerPlayer target) {
        if (requester.getUUID().equals(target.getUUID())) {
            requester.displayClientMessage(
                    Component.translatable("message.cobblecoins.cant_trade_self")
                            .withStyle(ChatFormatting.RED),
                    false);
            return;
        }

        // Check if either player is already in a trade
        if (activeTrades.containsKey(requester.getUUID()) || activeTrades.containsKey(target.getUUID())) {
            requester.displayClientMessage(
                    Component.translatable("message.cobblecoins.player_busy")
                            .withStyle(ChatFormatting.RED),
                    false);
            return;
        }

        // Remove old request if exists
        pendingRequests.entrySet().removeIf(entry -> 
                entry.getValue().requester.equals(requester.getUUID()) ||
                System.currentTimeMillis() - entry.getValue().timestamp > REQUEST_TIMEOUT);

        // Create new request
        TradeRequest request = new TradeRequest(requester.getUUID(), target.getUUID());
        pendingRequests.put(target.getUUID(), request);

        // Notify target
        NetworkHandler.sendToPlayer(
                new TradeRequestPacket(requester.getUUID(), requester.getName().getString()),
                target);

        requester.displayClientMessage(
                Component.translatable("message.cobblecoins.trade_request_sent", target.getName().getString())
                        .withStyle(ChatFormatting.GREEN),
                false);
    }

    public static void handleTradeResponse(ServerPlayer responder, UUID requesterUUID, boolean accepted) {
        TradeRequest request = pendingRequests.remove(responder.getUUID());
        
        if (request == null || !request.requester.equals(requesterUUID)) {
            responder.displayClientMessage(
                    Component.translatable("message.cobblecoins.no_pending_trade")
                            .withStyle(ChatFormatting.RED),
                    false);
            return;
        }

        if (System.currentTimeMillis() - request.timestamp > REQUEST_TIMEOUT) {
            responder.displayClientMessage(
                    Component.translatable("message.cobblecoins.trade_expired")
                            .withStyle(ChatFormatting.RED),
                    false);
            return;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        ServerPlayer requester = server.getPlayerList().getPlayer(requesterUUID);
        if (requester == null) {
            responder.displayClientMessage(
                    Component.translatable("message.cobblecoins.player_offline")
                            .withStyle(ChatFormatting.RED),
                    false);
            return;
        }

        if (accepted) {
            // Start trade session
            TradeSession session = new TradeSession(requesterUUID, responder.getUUID());
            activeTrades.put(requesterUUID, session);
            activeTrades.put(responder.getUUID(), session);

            requester.displayClientMessage(
                    Component.translatable("message.cobblecoins.trade_accepted", responder.getName().getString())
                            .withStyle(ChatFormatting.GREEN),
                    false);
            responder.displayClientMessage(
                    Component.translatable("message.cobblecoins.trade_started", requester.getName().getString())
                            .withStyle(ChatFormatting.GREEN),
                    false);

            // TODO: Open trade GUI for both players
            CobbleCoins.LOGGER.info("Trade started between {} and {}", requester.getName().getString(), responder.getName().getString());
        } else {
            requester.displayClientMessage(
                    Component.translatable("message.cobblecoins.trade_declined", responder.getName().getString())
                            .withStyle(ChatFormatting.YELLOW),
                    false);
            responder.displayClientMessage(
                    Component.translatable("message.cobblecoins.trade_declined_confirm")
                            .withStyle(ChatFormatting.GRAY),
                    false);
        }
    }

    public static void addItemToTrade(ServerPlayer player, ItemStack item) {
        TradeSession session = activeTrades.get(player.getUUID());
        if (session == null) {
            player.displayClientMessage(
                    Component.translatable("message.cobblecoins.not_in_trade")
                            .withStyle(ChatFormatting.RED),
                    false);
            return;
        }

        session.addItem(player.getUUID(), item.copy());
        session.setConfirmed(player.getUUID(), false);
    }

    public static void addMoneyToTrade(ServerPlayer player, long amount) {
        TradeSession session = activeTrades.get(player.getUUID());
        if (session == null) {
            player.displayClientMessage(
                    Component.translatable("message.cobblecoins.not_in_trade")
                            .withStyle(ChatFormatting.RED),
                    false);
            return;
        }

        if (BankAccountManager.getBalance(player.getUUID()) < amount) {
            player.displayClientMessage(
                    Component.translatable("message.cobblecoins.insufficient_funds")
                            .withStyle(ChatFormatting.RED),
                    false);
            return;
        }

        session.setMoney(player.getUUID(), amount);
        session.setConfirmed(player.getUUID(), false);
    }

    public static void confirmTrade(ServerPlayer player) {
        TradeSession session = activeTrades.get(player.getUUID());
        if (session == null) {
            player.displayClientMessage(
                    Component.translatable("message.cobblecoins.not_in_trade")
                            .withStyle(ChatFormatting.RED),
                    false);
            return;
        }

        session.setConfirmed(player.getUUID(), true);

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        UUID otherPlayerId = session.getOtherPlayer(player.getUUID());
        ServerPlayer otherPlayer = server.getPlayerList().getPlayer(otherPlayerId);

        if (otherPlayer != null) {
            otherPlayer.displayClientMessage(
                    Component.translatable("message.cobblecoins.other_confirmed", player.getName().getString())
                            .withStyle(ChatFormatting.GREEN),
                    false);
        }

        // Check if both confirmed
        if (session.bothConfirmed()) {
            executeTrade(session);
        }
    }

    private static void executeTrade(TradeSession session) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        ServerPlayer player1 = server.getPlayerList().getPlayer(session.player1);
        ServerPlayer player2 = server.getPlayerList().getPlayer(session.player2);

        if (player1 == null || player2 == null) {
            cancelTrade(session, "Player disconnected");
            return;
        }

        // Verify both players still have the items and money
        if (BankAccountManager.getBalance(session.player1) < session.player1Money ||
            BankAccountManager.getBalance(session.player2) < session.player2Money) {
            cancelTrade(session, "Insufficient funds");
            return;
        }

        // Execute money transfer
        if (session.player1Money > 0) {
            BankAccountManager.removeBalance(session.player1, session.player1Money);
            BankAccountManager.addBalance(session.player2, session.player1Money);
        }
        if (session.player2Money > 0) {
            BankAccountManager.removeBalance(session.player2, session.player2Money);
            BankAccountManager.addBalance(session.player1, session.player2Money);
        }

        // Transfer items
        for (ItemStack item : session.player1Items) {
            if (!player2.getInventory().add(item)) {
                player2.drop(item, false);
            }
        }
        for (ItemStack item : session.player2Items) {
            if (!player1.getInventory().add(item)) {
                player1.drop(item, false);
            }
        }

        // Complete trade
        activeTrades.remove(session.player1);
        activeTrades.remove(session.player2);

        player1.displayClientMessage(
                Component.translatable("message.cobblecoins.trade_complete")
                        .withStyle(ChatFormatting.GREEN),
                false);
        player2.displayClientMessage(
                Component.translatable("message.cobblecoins.trade_complete")
                        .withStyle(ChatFormatting.GREEN),
                false);
    }

    public static void cancelTrade(ServerPlayer player) {
        TradeSession session = activeTrades.remove(player.getUUID());
        if (session != null) {
            cancelTrade(session, "Cancelled by " + player.getName().getString());
        }
    }

    private static void cancelTrade(TradeSession session, String reason) {
        activeTrades.remove(session.player1);
        activeTrades.remove(session.player2);

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        ServerPlayer player1 = server.getPlayerList().getPlayer(session.player1);
        ServerPlayer player2 = server.getPlayerList().getPlayer(session.player2);

        Component message = Component.translatable("message.cobblecoins.trade_cancelled", reason)
                .withStyle(ChatFormatting.RED);

        if (player1 != null) {
            player1.displayClientMessage(message, false);
            // Return items
            for (ItemStack item : session.player1Items) {
                if (!player1.getInventory().add(item)) {
                    player1.drop(item, false);
                }
            }
        }
        if (player2 != null) {
            player2.displayClientMessage(message, false);
            // Return items
            for (ItemStack item : session.player2Items) {
                if (!player2.getInventory().add(item)) {
                    player2.drop(item, false);
                }
            }
        }
    }

    private static class TradeRequest {
        final UUID requester;
        final UUID target;
        final long timestamp;

        TradeRequest(UUID requester, UUID target) {
            this.requester = requester;
            this.target = target;
            this.timestamp = System.currentTimeMillis();
        }
    }

    private static class TradeSession {
        final UUID player1;
        final UUID player2;
        final List<ItemStack> player1Items = new ArrayList<>();
        final List<ItemStack> player2Items = new ArrayList<>();
        long player1Money = 0;
        long player2Money = 0;
        boolean player1Confirmed = false;
        boolean player2Confirmed = false;

        TradeSession(UUID player1, UUID player2) {
            this.player1 = player1;
            this.player2 = player2;
        }

        void addItem(UUID player, ItemStack item) {
            if (player.equals(player1)) {
                player1Items.add(item);
            } else {
                player2Items.add(item);
            }
        }

        void setMoney(UUID player, long amount) {
            if (player.equals(player1)) {
                player1Money = amount;
            } else {
                player2Money = amount;
            }
        }

        void setConfirmed(UUID player, boolean confirmed) {
            if (player.equals(player1)) {
                player1Confirmed = confirmed;
            } else {
                player2Confirmed = confirmed;
            }
        }

        boolean bothConfirmed() {
            return player1Confirmed && player2Confirmed;
        }

        UUID getOtherPlayer(UUID player) {
            return player.equals(player1) ? player2 : player1;
        }
    }
}
