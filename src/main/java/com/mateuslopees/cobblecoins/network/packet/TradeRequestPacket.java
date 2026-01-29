package com.mateuslopees.cobblecoins.network.packet;

import com.mateuslopees.cobblecoins.CobbleCoins;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record TradeRequestPacket(UUID fromPlayer, String fromPlayerName) implements CustomPacketPayload {
    public static final Type<TradeRequestPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CobbleCoins.MOD_ID, "trade_request"));

    public static final StreamCodec<FriendlyByteBuf, TradeRequestPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.map(UUID::fromString, UUID::toString),
            TradeRequestPacket::fromPlayer,
            ByteBufCodecs.STRING_UTF8,
            TradeRequestPacket::fromPlayerName,
            TradeRequestPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TradeRequestPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist.isClient()) {
                com.mateuslopees.cobblecoins.client.ClientPacketHandler.handleTradeRequest(packet.fromPlayer(), packet.fromPlayerName());
            }
        });
    }
}
