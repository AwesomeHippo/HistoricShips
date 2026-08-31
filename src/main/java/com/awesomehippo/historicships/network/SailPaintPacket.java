package com.awesomehippo.historicships.network;

import com.awesomehippo.historicships.HistoricShips;
import com.awesomehippo.historicships.entity.QuinqueremeEntity;
import com.awesomehippo.historicships.entity.SailPaint;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SailPaintPacket(int entityId, int sail, byte[] pixels) implements CustomPacketPayload {
    public static final int MAIN = 0;
    public static final int FRONT = 1;
    public static final Type<SailPaintPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(HistoricShips.MODID, "sail_paint"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SailPaintPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SailPaintPacket::entityId,
            ByteBufCodecs.VAR_INT, SailPaintPacket::sail,
            ByteBufCodecs.BYTE_ARRAY, SailPaintPacket::pixels,
            SailPaintPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServer(SailPaintPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                return;
            }
            Entity entity = serverPlayer.level().getEntity(packet.entityId());
            if (!(entity instanceof QuinqueremeEntity ship)) {
                return;
            }
            if (!ship.canEditSail(serverPlayer)) {
                return;
            }
            if (packet.pixels().length != 0 && !SailPaint.isValid(packet.pixels())) {
                return;
            }
            if (packet.sail() != MAIN && packet.sail() != FRONT) {
                return;
            }
            ship.applySailPaint(packet.sail(), packet.pixels());
        });
    }

    public static void handleClient(SailPaintPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Entity entity = context.player().level().getEntity(packet.entityId());
            if (!(entity instanceof QuinqueremeEntity ship)) {
                return;
            }
            byte[] data = packet.pixels().length == 0 ? null : packet.pixels();
            if (data != null && !SailPaint.isValid(data)) {
                return;
            }
            if (packet.sail() == FRONT) {
                ship.setFrontSailPaintData(data == null ? null : data.clone());
            } else if (packet.sail() == MAIN) {
                ship.setSailPaintData(data == null ? null : data.clone());
            }
        });
    }
}
