package io.github.junjun1z.whathitme;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.github.junjun1z.whathitme.data.DamageTracker;
import io.github.junjun1z.whathitme.net.WhmNetwork;
import io.github.junjun1z.whathitme.server.ServerCombatHandler;
import io.github.junjun1z.whathitme.server.ServerCommandWhm;
import io.github.junjun1z.whathitme.server.ServerNet;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;

/**
 * What Hit Me? - damage history, death recap and debuff list. Works on the client only, on the server only or on
 * both (acceptableRemoteVersions = "*"); client classes are reached only through {@link CommonProxy}.
 */
@Mod(modid = WhatHitMeMod.MODID, name = "What Hit Me?", version = Tags.VERSION, acceptableRemoteVersions = "*", acceptedMinecraftVersions = "[1.12,1.12.2]")
public class WhatHitMeMod {

    public static final String MODID = "whathitme";
    public static final Logger LOG = LogManager.getLogger("WhatHitMe");

    @SidedProxy(clientSide = "io.github.junjun1z.whathitme.client.ClientProxy", serverSide = "io.github.junjun1z.whathitme.CommonProxy")
    public static CommonProxy proxy;

    private static final ServerNet SERVER_NET = new ServerNet();
    private static final ServerCombatHandler COMBAT = new ServerCombatHandler();

    public static ServerNet serverNet() {
        return SERVER_NET;
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
        WhmNetwork.init();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(COMBAT);
        proxy.init(event);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new ServerCommandWhm());
    }

    @Mod.EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        DamageTracker.SERVER.clear();
        COMBAT.clear();
    }
}
