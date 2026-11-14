package io.github.junjun1z.whathitme.neoforge;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.net.Packets;
import io.github.junjun1z.whathitme.core.server.ServerCore;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The "whathitme:main" payload: the core packet bytes (type byte + body). Registered as optional, so vanilla /
 * unmodded clients and servers can connect; it is only sent to a side that has the channel.
 */
public final class NeoNet {

    /** Core packet bytes */
    public record Payload(byte[] data) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<Payload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Platform.MODID, Packets.CHANNEL_PATH));
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

    private NeoNet() {}

    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(String.valueOf(Packets.PROTOCOL)).optional().playBidirectional(Payload.TYPE, Payload.CODEC, NeoNet::handle);
    }

    private static void handle(Payload payload, IPayloadContext ctx) {
        Packets.Packet p;
        try {
            p = Packets.decode(payload.data());
        } catch (RuntimeException e) {
            return; // malformed: ignore
        }
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer) {
                if (Packets.serverbound(p.type())) ServerCore.onPacket(new NeoPlayer((ServerPlayer) ctx.player()), p);
            } else if (!Packets.serverbound(p.type())) {
                NeoClient.onPacket(p);
            }
        });
    }

    static void sendTo(ServerPlayer p, Packets.Packet pkt) {
        if (p.connection == null || !p.connection.hasChannel(Payload.TYPE)) return;
        PacketDistributor.sendToPlayer(p, new Payload(Packets.encode(pkt)));
    }
}
