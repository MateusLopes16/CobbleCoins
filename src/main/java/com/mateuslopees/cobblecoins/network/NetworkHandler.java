package com.mateuslopees.cobblecoins.network;

import com.mateuslopees.cobblecoins.CobbleCoins;
import com.mateuslopees.cobblecoins.network.packet.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(NetworkHandler::registerPackets);
    }

    private static void registerPackets(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(CobbleCoins.MOD_ID)
                .versioned(PROTOCOL_VERSION);

        // Client-bound packets
        registrar.playToClient(
                SyncBalancePacket.TYPE,
                SyncBalancePacket.STREAM_CODEC,
                SyncBalancePacket::handle
        );

        registrar.playToClient(
                OpenShopPacket.TYPE,
                OpenShopPacket.STREAM_CODEC,
                OpenShopPacket::handle
        );

        registrar.playToClient(
                OpenPlayerShopPacket.TYPE,
                OpenPlayerShopPacket.STREAM_CODEC,
                OpenPlayerShopPacket::handle
        );

        // Server-bound packets
        registrar.playToServer(
                ShopPurchasePacket.TYPE,
                ShopPurchasePacket.STREAM_CODEC,
                ShopPurchasePacket::handle
        );

        registrar.playToServer(
                ShopSellPacket.TYPE,
                ShopSellPacket.STREAM_CODEC,
                ShopSellPacket::handle
        );

        registrar.playToServer(
                PlayerShopActionPacket.TYPE,
                PlayerShopActionPacket.STREAM_CODEC,
                PlayerShopActionPacket::handle
        );

        // Trade packets
        registrar.playToClient(
                TradeRequestPacket.TYPE,
                TradeRequestPacket.STREAM_CODEC,
                TradeRequestPacket::handle
        );

        registrar.playToServer(
                TradeResponsePacket.TYPE,
                TradeResponsePacket.STREAM_CODEC,
                TradeResponsePacket::handle
        );

        CobbleCoins.LOGGER.info("CobbleCoins network packets registered");
    }

    public static void sendToPlayer(CustomPacketPayload packet, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    public static void sendToServer(CustomPacketPayload packet) {
        PacketDistributor.sendToServer(packet);
    }

    public static void sendToAllPlayers(CustomPacketPayload packet) {
        PacketDistributor.sendToAllPlayers(packet);
    }
}
