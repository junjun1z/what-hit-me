package io.github.junjun1z.whathitme.forge;

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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.forgespi.language.IModInfo;

/**
 * What Hit Me? on Forge 1.20.6. Works on the client only, on the server only or on both: the network channel accepts
 * a missing remote side and the display test ignores versions. Standard Forge events only (no mixins / coremods).
 */
@Mod(Platform.MODID)
public class WhatHitMeForge {

    static final Logger LOG = LoggerFactory.getLogger("WhatHitMe");

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
        };
        Platform.serverLang = McText.LANG;
        Platform.configDir = FMLPaths.CONFIGDIR.get().toFile();
        // Forge's curative items (milk does not cure every modded effect)
        Registries1206.cures = e -> {
            boolean milk = false;
            StringBuilder sb = new StringBuilder();
            for (ItemStack s : e.getEffect().value().getCurativeItems()) {
                if (s.isEmpty()) continue;
                if (s.is(Items.MILK_BUCKET)) milk = true;
                if (sb.length() > 0) sb.append(", ");
                sb.append(s.getHoverName().getString());
            }
            return new Registries1206.Cure(milk, sb.toString());
        };
        boolean client = FMLEnvironment.dist == Dist.CLIENT;
        WhmConfig.init(Platform.configDir, client, CfgFile::new);
        // the server list shows the mod as compatible whatever the other side has
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class,
            () -> new IExtensionPoint.DisplayTest(() -> IExtensionPoint.DisplayTest.IGNORESERVERONLY, (remote, isServer) -> true));
        ForgeNet.init();
        MinecraftForge.EVENT_BUS.register(new ForgeServerEvents());
        if (client) ForgeClient.init();
    }
}
