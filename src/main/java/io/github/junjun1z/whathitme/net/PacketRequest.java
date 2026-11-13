package io.github.junjun1z.whathitme.net;

import io.github.junjun1z.whathitme.WhatHitMeMod;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Client -> server: data for a /whm command */
public class PacketRequest implements IMessage {

    public static final int HISTORY = 0;
    public static final int LAST = 1;
    public static final int SNAPSHOT = 2;
    public static final int INSPECT = 3;
    public static final int EFFECTS = 4;
    public static final int SEARCH = 5;
    public static final int DUMP = 6;
    public static final int RELOAD = 7;

    public int kind;
    public String arg = "";

    public PacketRequest() {}

    public PacketRequest(int kind, String arg) {
        this.kind = kind;
        this.arg = arg == null ? "" : arg;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        kind = buf.readByte();
        arg = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(kind);
        ByteBufUtils.writeUTF8String(buf, arg.length() > 256 ? arg.substring(0, 256) : arg);
    }

    public static class Handler implements IMessageHandler<PacketRequest, IMessage> {
        @Override
        public IMessage onMessage(PacketRequest msg, MessageContext ctx) {
            WhatHitMeMod.serverNet().onRequest(ctx.getServerHandler().player, msg);
            return null;
        }
    }
}
