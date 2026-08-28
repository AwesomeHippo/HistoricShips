package com.awesomehippo.historicships.network;

import com.awesomehippo.historicships.NapoleonShipMod;
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

public record SailPaintPacket(int entityId, byte[] pixels) implements CustomPacketPayload {
    public static final Type<SailPaintPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(NapoleonShipMod.MODID, "sail_paint"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SailPaintPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SailPaintPacket::entityId,
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
            ship.applySailPaint(packet.pixels());
        });
    }

    public static void handleClient(SailPaintPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Entity entity = context.player().level().getEntity(packet.entityId());
            if (!(entity instanceof QuinqueremeEntity ship)) {
                return;
            }
            if (packet.pixels().length == 0) {
                ship.setSailPaintData(null);
                return;
            }
            if (SailPaint.isValid(packet.pixels())) {
                ship.setSailPaintData(packet.pixels().clone());
            }
        });
    }
}
