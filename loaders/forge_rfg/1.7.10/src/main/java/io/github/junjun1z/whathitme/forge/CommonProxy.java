package io.github.junjun1z.whathitme.forge;

import io.github.junjun1z.whathitme.core.net.Packets;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/** Dedicated server side. Client packets are ignored here. */
public class CommonProxy {

    public boolean isClient() {
        return false;
    }

    public void preInit(FMLPreInitializationEvent event) {}

    public void init(FMLInitializationEvent event) {}

    /** A server -> client packet arrived (network thread) */
    public void onClientPacket(Packets.Packet p) {}
}
