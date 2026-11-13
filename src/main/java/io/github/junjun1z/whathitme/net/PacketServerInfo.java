package io.github.junjun1z.whathitme.net;

import java.util.ArrayList;
import java.util.List;

import io.github.junjun1z.whathitme.WhatHitMeMod;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Server -> client: the server has the mod, plus its common display limits and filters */
public class PacketServerInfo implements IMessage {

    public int mask;
    public int summarySeconds;
    public List<String> ignored = new ArrayList<>();
    public List<String> renames = new ArrayList<>();
    public List<String> hints = new ArrayList<>();

    private static void writeList(ByteBuf buf, List<String> l) {
        int n = Math.min(l.size(), 1000);
        buf.writeShort(n);
        for (int i = 0; i < n; i++) ByteBufUtils.writeUTF8String(buf, l.get(i));
    }

    private static List<String> readList(ByteBuf buf) {
        int n = buf.readShort();
        List<String> l = new ArrayList<>(n);
        for (int i = 0; i < n; i++) l.add(ByteBufUtils.readUTF8String(buf));
        return l;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        mask = buf.readInt();
        summarySeconds = buf.readInt();
        ignored = readList(buf);
        renames = readList(buf);
        hints = readList(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(mask);
        buf.writeInt(summarySeconds);
        writeList(buf, ignored);
        writeList(buf, renames);
        writeList(buf, hints);
    }

    public static class Handler implements IMessageHandler<PacketServerInfo, IMessage> {
        @Override
        public IMessage onMessage(PacketServerInfo msg, MessageContext ctx) {
            WhatHitMeMod.proxy.onServerInfo(msg);
            return null;
        }
    }
}
