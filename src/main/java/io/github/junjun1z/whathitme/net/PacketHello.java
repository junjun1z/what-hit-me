package io.github.junjun1z.whathitme.net;

import io.github.junjun1z.whathitme.WhatHitMeMod;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Client -> server, once per connection: "this client has What Hit Me?" */
public class PacketHello implements IMessage {

    public int protocol = WhmNetwork.PROTOCOL;

    @Override
    public void fromBytes(ByteBuf buf) {
        protocol = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(protocol);
    }

    public static class Handler implements IMessageHandler<PacketHello, IMessage> {
        @Override
        public IMessage onMessage(PacketHello msg, MessageContext ctx) {
            WhatHitMeMod.serverNet().onHello(ctx.getServerHandler().player, msg);
            return null;
        }
    }
}
