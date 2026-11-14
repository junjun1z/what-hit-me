package io.github.junjun1z.whathitme.forge;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import io.github.junjun1z.whathitme.core.net.Packets;
import io.github.junjun1z.whathitme.core.server.ServerCore;
import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.network.ConnectionType;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

/**
 * The "whathitme:main" channel: one message carrying the core packet bytes (type byte + body). Both sides accept any
 * remote state (missing, vanilla, other versions), so vanilla / unmodded clients and servers can connect. Old Forge
 * builds have neither acceptMissingOr nor SimpleChannel#isRemotePresent: the server remembers which players sent a
 * packet, and the client says hello to every Forge server (an unknown channel is ignored there).
 */
public final class ForgeNet {

    private static final String VERSION = String.valueOf(Packets.PROTOCOL);
    static SimpleChannel channel;
    /** Players whose client has the mod (they sent a packet); cleared on logout */
    static final java.util.Set<java.util.UUID> MOD_CLIENTS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private ForgeNet() {}

    /** Core packet bytes */
    public static final class Raw {
        final byte[] data;

        Raw(byte[] data) {
            this.data = data;
        }
    }

    static void init() {
        channel = NetworkRegistry.newSimpleChannel(new ResourceLocation(io.github.junjun1z.whathitme.core.Platform.MODID, Packets.CHANNEL_PATH), () -> VERSION,
            v -> true, v -> true);
        channel.messageBuilder(Raw.class, 0)
            .encoder((m, buf) -> buf.writeBytes(m.data))
            .decoder(buf -> {
                byte[] b = new byte[buf.readableBytes()];
                buf.readBytes(b);
                return new Raw(b);
            })
            .consumer((BiConsumer<Raw, Supplier<NetworkEvent.Context>>) (m, ctx) -> {
                ctx.get().enqueueWork(() -> handle(m, ctx));
                ctx.get().setPacketHandled(true);
            })
            .add();
    }

    private static void handle(Raw msg, Supplier<NetworkEvent.Context> ctx) {
        Packets.Packet p;
        try {
            p = Packets.decode(msg.data);
        } catch (RuntimeException e) {
            return; // malformed: ignore
        }
        NetworkEvent.Context c = ctx.get();
        if (c.getDirection() == NetworkDirection.PLAY_TO_SERVER) {
            ServerPlayer sp = c.getSender();
            if (sp != null && Packets.serverbound(p.type())) {
                MOD_CLIENTS.add(sp.getUUID());
                ServerCore.onPacket(new ForgePlayer(sp), p);
            }
        } else if (!Packets.serverbound(p.type())) {
            ForgeClient.onPacket(p);
        }
    }

    static void sendTo(ServerPlayer p, Packets.Packet pkt) {
        if (!MOD_CLIENTS.contains(p.getUUID())) return;
        channel.send(PacketDistributor.PLAYER.with(() -> p), new Raw(Packets.encode(pkt)));
    }

    /** A Forge server (it may or may not have the mod: it answers the hello if it has) */
    static boolean serverPresent(Connection c) {
        return c != null && NetworkHooks.getConnectionType(() -> c) == ConnectionType.MODDED;
    }

    static void sendToServer(Packets.Packet pkt) {
        channel.sendToServer(new Raw(Packets.encode(pkt)));
    }
}
