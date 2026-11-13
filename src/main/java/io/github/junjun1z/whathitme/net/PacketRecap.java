package io.github.junjun1z.whathitme.net;

import io.github.junjun1z.whathitme.WhatHitMeMod;
import io.github.junjun1z.whathitme.data.Snapshot;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Server -> client: a history / death recap / snapshot (sent only on death, totem, heavy hit or a command) */
public class PacketRecap implements IMessage {

    public static final int HISTORY = 0;
    public static final int LAST = 1;
    public static final int DEATH = 2;
    public static final int ALERT = 3;
    public static final int SNAPSHOT = 4;
    public static final int INSPECT_DEATH = 5;
    public static final int INSPECT_HISTORY = 6;

    public int kind;
    public String who = "";
    /** Snapshot number (1 = newest) */
    public int index;
    /** Number of saved snapshots */
    public int count;
    /** null = nothing */
    public Snapshot snapshot;

    public PacketRecap() {}

    public PacketRecap(int kind, String who, int index, int count, Snapshot snapshot) {
        this.kind = kind;
        this.who = who == null ? "" : who;
        this.index = index;
        this.count = count;
        this.snapshot = snapshot;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        long now = System.currentTimeMillis();
        kind = buf.readByte();
        who = ByteBufUtils.readUTF8String(buf);
        index = buf.readInt();
        count = buf.readInt();
        snapshot = buf.readBoolean() ? Snapshot.read(buf, now) : null;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        long now = System.currentTimeMillis();
        buf.writeByte(kind);
        ByteBufUtils.writeUTF8String(buf, who);
        buf.writeInt(index);
        buf.writeInt(count);
        buf.writeBoolean(snapshot != null);
        if (snapshot != null) snapshot.write(buf, now);
    }

    public static class Handler implements IMessageHandler<PacketRecap, IMessage> {
        @Override
        public IMessage onMessage(PacketRecap msg, MessageContext ctx) {
            WhatHitMeMod.proxy.onRecap(msg);
            return null;
        }
    }
}
