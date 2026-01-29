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

public record ShopPurchasePacket(String itemId, int amount) implements CustomPacketPayload {
    public static final Type<ShopPurchasePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CobbleCoins.MOD_ID, "shop_purchase"));

    public static final StreamCodec<FriendlyByteBuf, ShopPurchasePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            ShopPurchasePacket::itemId,
            ByteBufCodecs.INT,
            ShopPurchasePacket::amount,
            ShopPurchasePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ShopPurchasePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                ShopManager.handlePurchase(serverPlayer, packet.itemId(), packet.amount());
            }
        });
    }
}
