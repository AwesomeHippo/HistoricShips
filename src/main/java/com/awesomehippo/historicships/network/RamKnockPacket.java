package com.awesomehippo.historicships.network;

import com.awesomehippo.historicships.HistoricShips;
import com.awesomehippo.historicships.entity.StoredShipEntity;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RamKnockPacket(int shipId, float bowX, float bowZ, float push, float closing) implements CustomPacketPayload {
    public static final Type<RamKnockPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(HistoricShips.MODID, "ram_knock"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RamKnockPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RamKnockPacket::shipId,
            ByteBufCodecs.FLOAT, RamKnockPacket::bowX,
            ByteBufCodecs.FLOAT, RamKnockPacket::bowZ,
            ByteBufCodecs.FLOAT, RamKnockPacket::push,
            ByteBufCodecs.FLOAT, RamKnockPacket::closing,
            RamKnockPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RamKnockPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Entity entity = context.player().level().getEntity(packet.shipId());
            if (!(entity instanceof StoredShipEntity ship) || !ship.isLocalInstanceAuthoritative()) {
                return;
            }
            ship.applyRamKnock(packet.bowX(), packet.bowZ(), packet.push(), packet.closing());
        });
    }
}
