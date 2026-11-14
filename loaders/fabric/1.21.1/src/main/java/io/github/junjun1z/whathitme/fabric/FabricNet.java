package io.github.junjun1z.whathitme.fabric;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.net.Packets;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * The "whathitme:main" payload: the core packet bytes (type byte + body). Fabric API only sends it to a side that
 * registered the channel, so clients and servers without the mod are never sent anything.
 */
public final class FabricNet {

    /** Core packet bytes */
    public record Payload(byte[] data) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<Payload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Platform.MODID, Packets.CHANNEL_PATH));
        public static final StreamCodec<FriendlyByteBuf, Payload> CODEC = CustomPacketPayload.codec((p, buf) -> buf.writeBytes(p.data), buf -> {
            byte[] b = new byte[buf.readableBytes()];
            buf.readBytes(b);
            return new Payload(b);
        });

        @Override
        public CustomPacketPayload.Type<Payload> type() {
            return TYPE;
        }
    }

    private FabricNet() {}

    static void registerTypes() {
        PayloadTypeRegistry.playC2S().register(Payload.TYPE, Payload.CODEC);
        PayloadTypeRegistry.playS2C().register(Payload.TYPE, Payload.CODEC);
    }

    static void sendTo(ServerPlayer p, Packets.Packet pkt) {
        if (ServerPlayNetworking.canSend(p, Payload.TYPE)) ServerPlayNetworking.send(p, new Payload(Packets.encode(pkt)));
    }
}
