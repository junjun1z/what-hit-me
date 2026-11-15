package io.github.junjun1z.whathitme.forge;

import io.github.junjun1z.whathitme.core.net.Packets;
import io.github.junjun1z.whathitme.core.server.ServerCore;
import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

/**
 * The "whathitme:main" channel: one message carrying the core packet bytes (type byte + body). Both sides accept a
 * missing remote channel, so vanilla / unmodded clients and servers can connect.
 */
public final class ForgeNet {

    static SimpleChannel channel;

    private ForgeNet() {}

    /** Core packet bytes */
    public static final class Raw {
        final byte[] data;

        Raw(byte[] data) {
            this.data = data;
        }
    }

    static void init() {
        channel = ChannelBuilder.named(ResourceLocation.fromNamespaceAndPath(io.github.junjun1z.whathitme.core.Platform.MODID, Packets.CHANNEL_PATH))
            .networkProtocolVersion(Packets.PROTOCOL)
            .acceptedVersions(Channel.VersionTest.ACCEPT_MISSING.or(Channel.VersionTest.exact(Packets.PROTOCOL)))
            .simpleChannel();
        channel.messageBuilder(Raw.class, 0)
            .encoder((m, buf) -> buf.writeBytes(m.data))
            .decoder(buf -> {
                byte[] b = new byte[buf.readableBytes()];
                buf.readBytes(b);
                return new Raw(b);
            })
            .consumerMainThread(ForgeNet::handle)
            .add();
    }

    private static void handle(Raw msg, CustomPayloadEvent.Context c) {
        Packets.Packet p;
        try {
            p = Packets.decode(msg.data);
        } catch (RuntimeException e) {
            return; // malformed: ignore
        }
        if (c.isServerSide()) {
            ServerPlayer sp = c.getSender();
            if (sp != null && Packets.serverbound(p.type())) ServerCore.onPacket(new ForgePlayer(sp), p);
        } else if (!Packets.serverbound(p.type())) {
            ForgeClient.onPacket(p);
        }
    }

    static void sendTo(ServerPlayer p, Packets.Packet pkt) {
        if (p.connection == null || !channel.isRemotePresent(p.connection.getConnection())) return;
        channel.send(new Raw(Packets.encode(pkt)), PacketDistributor.PLAYER.with(p));
    }

    static boolean serverPresent(Connection c) {
        return c != null && channel.isRemotePresent(c);
    }

    static void sendToServer(Packets.Packet pkt) {
        channel.send(new Raw(Packets.encode(pkt)), PacketDistributor.SERVER.noArg());
    }
}
