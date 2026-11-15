package io.github.junjun1z.whathitme.forge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.config.CfgFile;
import io.github.junjun1z.whathitme.core.config.WhmConfig;
import io.github.junjun1z.whathitme.mc.Facts;
import io.github.junjun1z.whathitme.mc.McText;
import io.github.junjun1z.whathitme.mc.Registries115;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import net.minecraftforge.forgespi.language.IModInfo;

/**
 * What Hit Me? on Forge 1.15.2. Works on the client only, on the server only or on both: the network channel accepts
 * a missing remote side and the display test ignores versions. Standard Forge events only (no mixins / coremods).
 */
@Mod(Platform.MODID)
public class WhatHitMeForge {

    static final Logger LOG = LogManager.getLogger("WhatHitMe");

    public WhatHitMeForge() {
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
        Facts.modOfSource = WhatHitMeForge::modOfSource;
        Registries115.modFiles = WhatHitMeForge::modFiles;
        Platform.serverLang = McText.LANG;
        Platform.configDir = FMLPaths.CONFIGDIR.get().toFile();
        // Forge's curative items (milk does not cure every modded effect)
        Registries115.cures = e -> {
            boolean milk = false;
            StringBuilder sb = new StringBuilder();
            for (ItemStack s : e.getCurativeItems()) {
                if (s.isEmpty()) continue;
                if (s.getItem() == Items.MILK_BUCKET) milk = true;
                if (sb.length() > 0) sb.append(", ");
                sb.append(s.getHoverName().getString());
            }
            return new Registries115.Cure(milk, sb.toString());
        };
        boolean client = FMLEnvironment.dist == Dist.CLIENT;
        WhmConfig.init(Platform.configDir, client, CfgFile::new);
        // the server list shows the mod as compatible whatever the other side has
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (remote, isServer) -> true));
        ForgeNet.init();
        MinecraftForge.EVENT_BUS.register(new ForgeServerEvents());
        if (client) ForgeClient.init();
    }

    private static volatile java.util.Map<java.io.File, String> sources;

    /** Mod jars / directories with their mod id (without minecraft, forge) */
    private static java.util.Map<java.io.File, String> sources() {
        java.util.Map<java.io.File, String> map = sources;
        if (map == null) {
            map = new java.util.HashMap<>();
            for (ModFileInfo fi : ModList.get().getModFiles()) {
                try {
                    if (fi.getMods().isEmpty()) continue;
                    String id = fi.getMods().get(0).getModId();
                    if ("minecraft".equals(id) || "forge".equals(id)) continue;
                    map.putIfAbsent(fi.getFile().getFilePath().toFile().getCanonicalFile(), id);
                } catch (Throwable t) {
                    // unknown
                }
            }
            sources = map;
        }
        return map;
    }

    /** Mod id of a jar / directory (for damage types of mods) */
    static String modOfSource(java.io.File f) {
        return sources().get(f);
    }

    /** Mod files for the damage id scanner (without What Hit Me? itself) */
    static java.util.Map<java.io.File, String> modFiles() {
        java.util.Map<java.io.File, String> out = new java.util.LinkedHashMap<>(sources());
        out.values().removeIf(Platform.MODID::equals);
        return out;
    }
}
