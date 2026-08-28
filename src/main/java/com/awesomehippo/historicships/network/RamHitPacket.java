package com.awesomehippo.historicships.network;

import com.awesomehippo.historicships.entity.StoredShipEntity;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Consumer;

public record RamHitPacket(int attackerId, int targetId, float closing) implements CustomPacketPayload {
    public static final Type<RamHitPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath("historicships", "ram_hit"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RamHitPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RamHitPacket::attackerId,
            ByteBufCodecs.VAR_INT, RamHitPacket::targetId,
            ByteBufCodecs.FLOAT, RamHitPacket::closing,
            RamHitPacket::new);

    public static Consumer<RamHitPacket> clientSend;

    public static void send(int attackerId, int targetId, float closing) {
        if (clientSend != null) {
            clientSend.accept(new RamHitPacket(attackerId, targetId, closing));
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RamHitPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!(player.level() instanceof ServerLevel server)) {
                return;
            }
            Entity attackerEnt = server.getEntity(packet.attackerId());
            Entity targetEnt = server.getEntity(packet.targetId());
            if (!(attackerEnt instanceof StoredShipEntity attacker) || !(targetEnt instanceof StoredShipEntity target)) {
                return;
            }
            if (!attacker.isConductor(player)) {
                return;
            }
            attacker.serverRamHit(server, target, packet.closing());
        });
    }
}
