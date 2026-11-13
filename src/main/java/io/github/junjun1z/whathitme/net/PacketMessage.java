package io.github.junjun1z.whathitme.net;

import io.github.junjun1z.whathitme.WhatHitMeMod;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Server -> client: a short translated reply (errors, confirmations) */
public class PacketMessage implements IMessage {

    public String key = "";
    public String[] args = new String[0];
    public boolean error;

    public PacketMessage() {}

    public PacketMessage(boolean error, String key, String... args) {
        this.error = error;
        this.key = key;
        this.args = args;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        error = buf.readBoolean();
        key = ByteBufUtils.readUTF8String(buf);
        int n = buf.readByte();
        args = new String[n];
        for (int i = 0; i < n; i++) args[i] = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(error);
        ByteBufUtils.writeUTF8String(buf, key);
        buf.writeByte(args.length);
        for (String a : args) ByteBufUtils.writeUTF8String(buf, a);
    }

    public static class Handler implements IMessageHandler<PacketMessage, IMessage> {
        @Override
        public IMessage onMessage(PacketMessage msg, MessageContext ctx) {
            WhatHitMeMod.proxy.onMessage(msg);
            return null;
        }
    }
}
