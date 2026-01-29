package com.mateuslopees.cobblecoins.network.packet;

import com.mateuslopees.cobblecoins.CobbleCoins;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncBalancePacket(long balance) implements CustomPacketPayload {
    public static final Type<SyncBalancePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CobbleCoins.MOD_ID, "sync_balance"));

    public static final StreamCodec<FriendlyByteBuf, SyncBalancePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG,
            SyncBalancePacket::balance,
            SyncBalancePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncBalancePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist.isClient()) {
                com.mateuslopees.cobblecoins.client.ClientPacketHandler.handleSyncBalance(packet.balance());
            }
        });
    }
}
