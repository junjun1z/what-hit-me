package io.github.junjun1z.whathitme.neoforge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.config.CfgFile;
import io.github.junjun1z.whathitme.core.config.WhmConfig;
import io.github.junjun1z.whathitme.mc.McText;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforgespi.language.IModInfo;

/**
 * What Hit Me? on NeoForge 1.21.8. Works on the client only, on the server only or on both: the payload is optional
 * and the display test ignores versions. Standard NeoForge events only (no mixins / coremods).
 */
@Mod(Platform.MODID)
public class WhatHitMeNeoForge {

    static final Logger LOG = LoggerFactory.getLogger("WhatHitMe");

    public WhatHitMeNeoForge(IEventBus modBus) {
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
                return ModList.get().getModContainerById(modId).map(c -> c.getModInfo().getDisplayName()).orElse(modId);
            }

            @Override
            public Collection<String> modIds() {
                List<String> out = new ArrayList<>();
                for (IModInfo i : ModList.get().getMods()) out.add(i.getModId());
                return out;
            }
        };
        Platform.serverLang = McText.LANG;
        Platform.configDir = FMLPaths.CONFIGDIR.get().toFile();
        // NeoForge 1.21.2+ has no effect cures any more: milk (vanilla) removes every effect (Registries121 default)
        boolean client = FMLEnvironment.dist.isClient();
        WhmConfig.init(Platform.configDir, client, CfgFile::new);
        modBus.addListener(NeoNet::register);
        NeoForge.EVENT_BUS.register(new NeoServerEvents());
        if (client) NeoClient.init();
    }
}
