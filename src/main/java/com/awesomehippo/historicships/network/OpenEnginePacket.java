package com.awesomehippo.historicships.network;

import com.awesomehippo.historicships.entity.NapoleonShipEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenEnginePacket(int shipId) implements CustomPacketPayload {
    public static final Type<OpenEnginePacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath("historicships", "open_engine"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenEnginePacket> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, OpenEnginePacket::shipId, OpenEnginePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenEnginePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            Entity entity = player.level().getEntity(packet.shipId());
            if (!(entity instanceof NapoleonShipEntity ship)) {
                return;
            }
            if (!ship.hasPassenger(player)) {
                return;
            }
            ship.openEngineMenu(player);
        });
    }
}
