package io.github.junjun1z.whathitme.neoforge;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.net.Packets;
import io.github.junjun1z.whathitme.core.server.ServerCore;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/**
 * The "whathitme:main" payload: the core packet bytes (type byte + body), registered as optional, so vanilla /
 * unmodded clients and servers can connect. Only sent to a side that has the channel.
 */
public final class NeoNet {

    public static final ResourceLocation ID = new ResourceLocation(Platform.MODID, Packets.CHANNEL_PATH);

    /** Core packet bytes */
    public record Payload(byte[] data) implements CustomPacketPayload {
        static Payload read(FriendlyByteBuf buf) {
            byte[] b = new byte[buf.readableBytes()];
            buf.readBytes(b);
            return new Payload(b);
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeBytes(data);
        }

        @Override
        public ResourceLocation id() {
            return ID;
        }
    }

    private NeoNet() {}

    static void register(RegisterPayloadHandlerEvent event) {
        event.registrar(Platform.MODID).optional().play(ID, Payload::read, NeoNet::handle);
    }

    private static void handle(Payload payload, PlayPayloadContext ctx) {
        Packets.Packet p;
        try {
            p = Packets.decode(payload.data());
        } catch (RuntimeException e) {
            return; // malformed: ignore
        }
        ctx.workHandler().execute(() -> {
            if (Packets.serverbound(p.type())) {
                if (ctx.player().orElse(null) instanceof ServerPlayer sp) ServerCore.onPacket(new NeoPlayer(sp), p);
            } else if (!(ctx.player().orElse(null) instanceof ServerPlayer)) {
                NeoClient.onPacket(p);
            }
        });
    }

    static void sendTo(ServerPlayer p, Packets.Packet pkt) {
        if (!NetworkRegistry.getInstance().isConnected(p.connection, ID)) return;
        PacketDistributor.PLAYER.with(p).send(new Payload(Packets.encode(pkt)));
    }

    static boolean serverPresent() {
        Minecraft mc = Minecraft.getInstance();
        return mc.getConnection() != null && NetworkRegistry.getInstance().isConnected(mc.getConnection(), ID);
    }

    static void sendToServer(Packets.Packet pkt) {
        PacketDistributor.SERVER.noArg().send(new Payload(Packets.encode(pkt)));
    }
}
