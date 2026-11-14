package io.github.junjun1z.whathitme.forge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.config.WhmConfig;
import io.github.junjun1z.whathitme.core.server.ServerCore;
import io.github.junjun1z.whathitme.mc.McText;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;

/**
 * What Hit Me? on Forge 1.12.2 - damage history, death recap and debuff list. Works on the client only, on the server
 * only or on both (acceptableRemoteVersions = "*"); client classes are reached only through {@link CommonProxy}. All
 * logic is in the loader-independent core (common/); this package only wires Forge events, the network channel, the
 * commands and the config files to it.
 */
@Mod(modid = Platform.MODID, name = "What Hit Me?", useMetadata = true, acceptableRemoteVersions = "*", acceptedMinecraftVersions = "[1.12,1.12.2]",
    guiFactory = "io.github.junjun1z.whathitme.forge.WhmGuiFactory")
public class WhatHitMeMod {

    public static final Logger LOG = LogManager.getLogger("WhatHitMe");

    @SidedProxy(clientSide = "io.github.junjun1z.whathitme.forge.ClientProxy", serverSide = "io.github.junjun1z.whathitme.forge.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        Platform.log = new Platform.Log() {
            @Override
            public void info(String msg) {
                LOG.info(msg);
            }

            @Override
            public void warn(String msg, Throwable t) {
                LOG.warn(msg, t);
            }
        };
        Platform.mods = new Platform.Mods() {
            @Override
            public String modName(String modId) {
                ModContainer c = Loader.instance().getIndexedModList().get(modId);
                return c == null ? modId : c.getName();
            }

            @Override
            public java.util.Collection<String> modIds() {
                java.util.List<String> out = new java.util.ArrayList<>();
                for (ModContainer mc : Loader.instance().getActiveModList()) out.add(mc.getModId());
                return out;
            }
        };
        Platform.serverLang = McText.LANG;
        Platform.configDir = event.getModConfigurationDirectory();
        WhmConfig.init(event.getModConfigurationDirectory(), proxy.isClient(), ForgeCfgBackend::new);
        WhmNetwork.init();
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        boolean damageEvent = true;
        try {
            Class.forName("net.minecraftforge.event.entity.living.LivingDamageEvent");
        } catch (Throwable t) {
            damageEvent = false; // Forge for 1.12 / 1.12.1
        }
        // the deferred handlers must run before the ServerEvents handlers of the same priority: register them first
        if (!damageEvent) MinecraftForge.EVENT_BUS.register(new DeferredDamageEvents());
        MinecraftForge.EVENT_BUS.register(new ServerEvents());
        if (damageEvent) MinecraftForge.EVENT_BUS.register(new DamageEvents());
        proxy.init(event);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        ServerCore.serverStarted(new ServerPort112(event.getServer()));
        event.registerServerCommand(new ServerCommandWhm());
    }

    @Mod.EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        ServerCore.serverStopped();
    }
}
