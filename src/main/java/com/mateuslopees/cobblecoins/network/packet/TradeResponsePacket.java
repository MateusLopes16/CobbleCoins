package com.mateuslopees.cobblecoins.network.packet;

import com.mateuslopees.cobblecoins.CobbleCoins;
import com.mateuslopees.cobblecoins.trade.TradeManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record TradeResponsePacket(UUID fromPlayer, boolean accepted) implements CustomPacketPayload {
    public static final Type<TradeResponsePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CobbleCoins.MOD_ID, "trade_response"));

    public static final StreamCodec<FriendlyByteBuf, TradeResponsePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.map(UUID::fromString, UUID::toString),
            TradeResponsePacket::fromPlayer,
            ByteBufCodecs.BOOL,
            TradeResponsePacket::accepted,
            TradeResponsePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TradeResponsePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                TradeManager.handleTradeResponse(serverPlayer, packet.fromPlayer(), packet.accepted());
            }
        });
    }
}
