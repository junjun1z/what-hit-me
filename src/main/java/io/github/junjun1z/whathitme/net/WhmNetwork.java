package io.github.junjun1z.whathitme.net;

import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/** The mod channel. Packets are only sent on demand (death, totem / heavy hit, commands), never per hit. */
public final class WhmNetwork {

    public static final int PROTOCOL = 1;
    public static SimpleNetworkWrapper channel;

    private WhmNetwork() {}

    public static void init() {
        channel = NetworkRegistry.INSTANCE.newSimpleChannel("whathitme");
        int id = 0;
        channel.registerMessage(PacketHello.Handler.class, PacketHello.class, id++, Side.SERVER);
        channel.registerMessage(PacketRequest.Handler.class, PacketRequest.class, id++, Side.SERVER);
        channel.registerMessage(PacketServerInfo.Handler.class, PacketServerInfo.class, id++, Side.CLIENT);
        channel.registerMessage(PacketRecap.Handler.class, PacketRecap.class, id++, Side.CLIENT);
        channel.registerMessage(PacketEntries.Handler.class, PacketEntries.class, id++, Side.CLIENT);
        channel.registerMessage(PacketMessage.Handler.class, PacketMessage.class, id++, Side.CLIENT);
    }
}
