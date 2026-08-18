package com.awesomehippo.historicships.network;

import com.awesomehippo.historicships.entity.QuinqueremeEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record FireTowerStonePacket(int shipId) implements CustomPacketPayload {
    public static final Type<FireTowerStonePacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath("historicships", "fire_tower_stone"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FireTowerStonePacket> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, FireTowerStonePacket::shipId, FireTowerStonePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FireTowerStonePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            Entity entity = player.level().getEntity(packet.shipId());
            if (!(entity instanceof QuinqueremeEntity ship)) {
                return;
            }
            if (!ship.isConductor(player)) {
                return;
            }
            ship.serverFireTower(player);
        });
    }
}
