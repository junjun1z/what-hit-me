package io.github.junjun1z.whathitme.net;

import java.util.ArrayList;
import java.util.List;

import io.github.junjun1z.whathitme.WhatHitMeMod;
import io.github.junjun1z.whathitme.data.Entry;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Server -> client: search results, active effects or the damage catalog for a dump */
public class PacketEntries implements IMessage {

    public static final int SEARCH = 0;
    public static final int EFFECTS = 1;
    public static final int DUMP = 2;

    public int kind;
    public String query = "";
    public int total;
    public List<Entry> entries = new ArrayList<>();

    public PacketEntries() {}

    public PacketEntries(int kind, String query, int total, List<Entry> entries) {
        this.kind = kind;
        this.query = query == null ? "" : query;
        this.total = total;
        this.entries = entries;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        kind = buf.readByte();
        query = ByteBufUtils.readUTF8String(buf);
        total = buf.readInt();
        int n = buf.readInt();
        entries = new ArrayList<>(n);
        for (int i = 0; i < n; i++) entries.add(Entry.read(buf));
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(kind);
        ByteBufUtils.writeUTF8String(buf, query);
        buf.writeInt(total);
        buf.writeInt(entries.size());
        for (Entry e : entries) e.write(buf);
    }

    public static class Handler implements IMessageHandler<PacketEntries, IMessage> {
        @Override
        public IMessage onMessage(PacketEntries msg, MessageContext ctx) {
            WhatHitMeMod.proxy.onEntries(msg);
            return null;
        }
    }
}
