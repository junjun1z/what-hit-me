package io.github.junjun1z.whathitme.fabric;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.net.Packets;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * The "whathitme:main" channel: the core packet bytes (type byte + body). Fabric API only sends to a side that
 * registered the channel, so clients and servers without the mod are never sent anything.
 */
public final class FabricNet {

    public static final ResourceLocation ID = new ResourceLocation(Platform.MODID, Packets.CHANNEL_PATH);

    private FabricNet() {}

    static FriendlyByteBuf buf(Packets.Packet pkt) {
        FriendlyByteBuf b = new FriendlyByteBuf(Unpooled.buffer());
        b.writeBytes(Packets.encode(pkt));
        return b;
    }

    /** Decodes a received buffer (on the network thread), null when malformed */
    static Packets.Packet read(FriendlyByteBuf buf) {
        try {
            byte[] b = new byte[buf.readableBytes()];
            buf.readBytes(b);
            return Packets.decode(b);
        } catch (RuntimeException e) {
            return null;
        }
    }

    static void sendTo(ServerPlayer p, Packets.Packet pkt) {
        if (ServerPlayNetworking.canSend(p, ID)) ServerPlayNetworking.send(p, ID, buf(pkt));
    }
}
