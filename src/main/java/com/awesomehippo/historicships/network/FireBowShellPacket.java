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

public record FireBowShellPacket(int shipId, int mode) implements CustomPacketPayload {
    public static final int FRONT = 0;
    public static final int LEFT = 1;
    public static final int RIGHT = 2;
    public static final Type<FireBowShellPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath("historicships", "fire_bow_shell"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FireBowShellPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FireBowShellPacket::shipId,
            ByteBufCodecs.VAR_INT, FireBowShellPacket::mode,
            FireBowShellPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FireBowShellPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            Entity entity = player.level().getEntity(packet.shipId());
            if (!(entity instanceof NapoleonShipEntity ship)) {
                return;
            }
            if (!ship.isConductor(player)) {
                return;
            }
            ship.serverFireBowShells(player, packet.mode());
        });
    }
}
