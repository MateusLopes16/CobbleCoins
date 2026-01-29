package com.mateuslopees.cobblecoins.network.packet;

import com.mateuslopees.cobblecoins.CobbleCoins;
import com.mateuslopees.cobblecoins.data.PlayerShopManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PlayerShopActionPacket(Action action, String itemId, int amount, long price) implements CustomPacketPayload {
    public static final Type<PlayerShopActionPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CobbleCoins.MOD_ID, "player_shop_action"));

    public static final StreamCodec<FriendlyByteBuf, PlayerShopActionPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT.map(i -> Action.values()[i], Enum::ordinal),
            PlayerShopActionPacket::action,
            ByteBufCodecs.STRING_UTF8,
            PlayerShopActionPacket::itemId,
            ByteBufCodecs.INT,
            PlayerShopActionPacket::amount,
            ByteBufCodecs.VAR_LONG,
            PlayerShopActionPacket::price,
            PlayerShopActionPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PlayerShopActionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                switch (packet.action()) {
                    case LIST_ITEM -> PlayerShopManager.listItem(serverPlayer, packet.itemId(), packet.amount(), packet.price());
                    case REMOVE_ITEM -> PlayerShopManager.removeItem(serverPlayer, packet.itemId());
                    case BUY_FROM_PLAYER -> PlayerShopManager.buyFromPlayer(serverPlayer, packet.itemId(), packet.amount());
                }
            }
        });
    }

    public enum Action {
        LIST_ITEM,
        REMOVE_ITEM,
        BUY_FROM_PLAYER
    }
}
