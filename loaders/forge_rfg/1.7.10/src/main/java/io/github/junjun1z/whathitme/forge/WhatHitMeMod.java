package io.github.junjun1z.whathitme.forge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.Safe;
import io.github.junjun1z.whathitme.core.data.CatalogCache;
import io.github.junjun1z.whathitme.core.config.WhmConfig;
import io.github.junjun1z.whathitme.core.server.ServerCore;
import io.github.junjun1z.whathitme.mc.McText;
import io.github.junjun1z.whathitme.mc.Registries17;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;

/**
 * What Hit Me? on Forge 1.7.10 - damage history, death recap and debuff list. Works on the client only, on the server
 * only or on both (acceptableRemoteVersions = "*"); client classes are reached only through {@link CommonProxy}. All
 * logic is in the loader-independent core (common/); this package only wires Forge events, the network channel, the
 * commands and the config files to it.
 */
@Mod(modid = Platform.MODID, name = "What Hit Me?", useMetadata = true, acceptableRemoteVersions = "*", acceptedMinecraftVersions = "[1.7.10]",
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
            public String version(String modId) {
                ModContainer c = Loader.instance().getIndexedModList().get(modId);
                return c == null ? "" : c.getVersion();
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
        ServerEvents ev = new ServerEvents();
        MinecraftForge.EVENT_BUS.register(ev);
        cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(ev);
        proxy.init(event);
    }

    /** Every mod is loaded: load or build the catalog cache (damage types, entities, effects) */
    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        Safe.run("catalog cache", () -> CatalogCache.init(Registries17.INSTANCE));
        LOG.info("What Hit Me? catalog cache: " + CatalogCache.status + " in " + CatalogCache.initMillis + " ms, damage id scan: " + CatalogCache.scanStatus);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        ServerCore.serverStarted(new ServerPort17(event.getServer()));
        event.registerServerCommand(new ServerCommandWhm());
    }

    @Mod.EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        ServerCore.serverStopped();
    }
}
