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
import io.github.junjun1z.whathitme.mc.Registries1206;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.EffectCure;
import net.neoforged.neoforge.common.EffectCures;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforgespi.language.IModInfo;

/**
 * What Hit Me? on NeoForge 1.20.6. Works on the client only, on the server only or both: the payload is
 * optional and the display test ignores versions. Standard NeoForge events only (no mixins / coremods).
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

            @Override
            public String version(String modId) {
                return ModList.get().getModContainerById(modId).map(c -> c.getModInfo().getVersion().toString()).orElse("");
            }
        };
        Platform.serverLang = McText.LANG;
        Platform.configDir = FMLPaths.CONFIGDIR.get().toFile();
        // NeoForge's effect cures (milk does not cure every modded effect)
        Registries1206.cures = e -> {
            boolean milk = e.getCures().contains(EffectCures.MILK);
            StringBuilder sb = new StringBuilder();
            if (milk) sb.append("Milk Bucket");
            for (EffectCure c : e.getCures()) {
                if (c == EffectCures.MILK || c == EffectCures.PROTECTED_BY_TOTEM) continue;
                if (sb.length() > 0) sb.append(", ");
                sb.append(c.name());
            }
            return new Registries1206.Cure(milk, sb.toString());
        };
        boolean client = FMLEnvironment.dist == Dist.CLIENT;
        WhmConfig.init(Platform.configDir, client, CfgFile::new);
        modBus.addListener(NeoNet::register);
        NeoForge.EVENT_BUS.register(new NeoServerEvents());
        if (client) NeoClient.init();
    }
}
