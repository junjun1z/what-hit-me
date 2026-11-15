package io.github.junjun1z.whathitme.neoforge;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.net.Packets;
import io.github.junjun1z.whathitme.core.server.ServerCore;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The "whathitme:main" payload: the core packet bytes (type byte + body), registered as optional, so vanilla /
 * unmodded clients and servers can connect. Only sent to a side that has the channel.
 */
public final class NeoNet {

    /** Core packet bytes */
    public record Payload(byte[] data) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<Payload> TYPE =
            new CustomPacketPayload.Type<>(new ResourceLocation(Platform.MODID, Packets.CHANNEL_PATH));

        @Override
        public CustomPacketPayload.Type<Payload> type() {
            return TYPE;
        }
    }

    private NeoNet() {}

    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playBidirectional(Payload.TYPE, CODEC, NeoNet::handle);
    }

    private static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC = StreamCodec.of(
        (buf, p) -> buf.writeBytes(p.data()), buf -> {
            byte[] b = new byte[buf.readableBytes()];
            buf.readBytes(b);
            return new Payload(b);
        });

    /** On the main thread (the default handler thread) */
    private static void handle(Payload payload, IPayloadContext ctx) {
        Packets.Packet p;
        try {
            p = Packets.decode(payload.data());
        } catch (RuntimeException e) {
            return; // malformed: ignore
        }
        if (Packets.serverbound(p.type())) {
            if (ctx.player() instanceof ServerPlayer sp) ServerCore.onPacket(new NeoPlayer(sp), p);
        } else if (!(ctx.player() instanceof ServerPlayer)) {
            NeoClient.onPacket(p);
        }
    }

    static void sendTo(ServerPlayer p, Packets.Packet pkt) {
        if (!p.connection.hasChannel(Payload.TYPE)) return;
        PacketDistributor.sendToPlayer(p, new Payload(Packets.encode(pkt)));
    }

    static boolean serverPresent() {
        Minecraft mc = Minecraft.getInstance();
        return mc.getConnection() != null && mc.getConnection().hasChannel(Payload.TYPE);
    }

    static void sendToServer(Packets.Packet pkt) {
        PacketDistributor.sendToServer(new Payload(Packets.encode(pkt)));
    }
}
