package io.github.junjun1z.whathitme;

import io.github.junjun1z.whathitme.config.WhmConfig;
import io.github.junjun1z.whathitme.net.PacketEntries;
import io.github.junjun1z.whathitme.net.PacketMessage;
import io.github.junjun1z.whathitme.net.PacketRecap;
import io.github.junjun1z.whathitme.net.PacketServerInfo;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/** Dedicated server side. Client packets are ignored here. */
public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        WhmConfig.init(event.getModConfigurationDirectory(), false);
    }

    public void init(FMLInitializationEvent event) {}

    public void onServerInfo(PacketServerInfo msg) {}

    public void onRecap(PacketRecap msg) {}

    public void onEntries(PacketEntries msg) {}

    public void onMessage(PacketMessage msg) {}
}
