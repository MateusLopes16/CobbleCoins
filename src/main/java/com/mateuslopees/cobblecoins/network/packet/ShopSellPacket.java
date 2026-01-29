package com.mateuslopees.cobblecoins.network.packet;

import com.mateuslopees.cobblecoins.CobbleCoins;
import com.mateuslopees.cobblecoins.shop.ShopManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ShopSellPacket(int slot, int amount) implements CustomPacketPayload {
    public static final Type<ShopSellPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CobbleCoins.MOD_ID, "shop_sell"));

    public static final StreamCodec<FriendlyByteBuf, ShopSellPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            ShopSellPacket::slot,
            ByteBufCodecs.INT,
            ShopSellPacket::amount,
            ShopSellPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ShopSellPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                ShopManager.handleSell(serverPlayer, packet.slot(), packet.amount());
            }
        });
    }
}
