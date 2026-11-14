package io.github.junjun1z.whathitme.forge;

import io.github.junjun1z.whathitme.core.net.Packets;
import io.github.junjun1z.whathitme.core.server.ServerCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 * The "whathitme" channel. One message class per packet type with the same discriminators and bodies as 1.0.x (the
 * bodies are encoded by the core). Packets are only sent on demand (death, totem / heavy hit, commands).
 */
public final class WhmNetwork {

    public static SimpleNetworkWrapper channel;

    private WhmNetwork() {}

    /** A packet body as raw bytes */
    public abstract static class Raw implements IMessage {
        byte[] body = new byte[0];

        abstract int type();

        @Override
        public void fromBytes(ByteBuf buf) {
            body = new byte[buf.readableBytes()];
            buf.readBytes(body);
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeBytes(body);
        }

        Packets.Packet packet() {
            return Packets.read(type(), body);
        }
    }

    public static class Hello extends Raw {
        @Override
        int type() {
            return Packets.HELLO;
        }
    }

    public static class Request extends Raw {
        @Override
        int type() {
            return Packets.REQUEST;
        }
    }

    public static class ServerInfo extends Raw {
        @Override
        int type() {
            return Packets.SERVER_INFO;
        }
    }

    public static class Recap extends Raw {
        @Override
        int type() {
            return Packets.RECAP;
        }
    }

    public static class Entries extends Raw {
        @Override
        int type() {
            return Packets.ENTRIES;
        }
    }

    public static class Message extends Raw {
        @Override
        int type() {
            return Packets.MESSAGE;
        }
    }

    public static Raw wrap(Packets.Packet p) {
        Raw r;
        switch (p.type()) {
            case Packets.HELLO:
                r = new Hello();
                break;
            case Packets.REQUEST:
                r = new Request();
                break;
            case Packets.SERVER_INFO:
                r = new ServerInfo();
                break;
            case Packets.RECAP:
                r = new Recap();
                break;
            case Packets.ENTRIES:
                r = new Entries();
                break;
            default:
                r = new Message();
        }
        r.body = Packets.body(p);
        return r;
    }

    /** Client -> server: handled on the server thread */
    public static class ServerHandler implements IMessageHandler<Raw, IMessage> {
        @Override
        public IMessage onMessage(Raw msg, MessageContext ctx) {
            EntityPlayerMP p = ctx.getServerHandler().playerEntity;
            Packets.Packet pkt;
            try {
                pkt = msg.packet();
            } catch (RuntimeException e) {
                return null; // malformed: ignore
            }
            p.getServerWorld().addScheduledTask(() -> ServerCore.onPacket(new PlayerHandle110(p), pkt));
            return null;
        }
    }

    /** Server -> client: handled by the client proxy on the client thread */
    public static class ClientHandler implements IMessageHandler<Raw, IMessage> {
        @Override
        public IMessage onMessage(Raw msg, MessageContext ctx) {
            try {
                WhatHitMeMod.proxy.onClientPacket(msg.packet());
            } catch (RuntimeException e) {
                WhatHitMeMod.LOG.warn("Bad What Hit Me? packet", e);
            }
            return null;
        }
    }

    public static void init() {
        channel = NetworkRegistry.INSTANCE.newSimpleChannel(io.github.junjun1z.whathitme.core.Platform.MODID);
        channel.registerMessage(new ServerHandler(), Hello.class, Packets.HELLO, Side.SERVER);
        channel.registerMessage(new ServerHandler(), Request.class, Packets.REQUEST, Side.SERVER);
        channel.registerMessage(new ClientHandler(), ServerInfo.class, Packets.SERVER_INFO, Side.CLIENT);
        channel.registerMessage(new ClientHandler(), Recap.class, Packets.RECAP, Side.CLIENT);
        channel.registerMessage(new ClientHandler(), Entries.class, Packets.ENTRIES, Side.CLIENT);
        channel.registerMessage(new ClientHandler(), Message.class, Packets.MESSAGE, Side.CLIENT);
    }
}
