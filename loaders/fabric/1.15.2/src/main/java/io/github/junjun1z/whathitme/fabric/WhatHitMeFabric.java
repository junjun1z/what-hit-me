package io.github.junjun1z.whathitme.fabric;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.Safe;
import io.github.junjun1z.whathitme.core.data.CatalogCache;
import io.github.junjun1z.whathitme.core.config.CfgFile;
import io.github.junjun1z.whathitme.core.config.WhmConfig;
import io.github.junjun1z.whathitme.core.net.Packets;
import io.github.junjun1z.whathitme.core.server.ServerCore;
import io.github.junjun1z.whathitme.mc.Brig;
import io.github.junjun1z.whathitme.mc.Facts;
import io.github.junjun1z.whathitme.mc.McText;
import io.github.junjun1z.whathitme.mc.Registries115;
import io.github.junjun1z.whathitme.mc.ServerPortMc;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.registry.CommandRegistry;
import net.fabricmc.fabric.api.event.server.ServerStartCallback;
import net.fabricmc.fabric.api.event.server.ServerStopCallback;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

/**
 * What Hit Me? on Fabric 1.15.2. Optional on both sides (works on the client only, the server only or both). This Fabric
 * API has no living-entity damage, respawn or disconnect events: tiny mixins on Player#actuallyHurt (before armor / after
 * the damage), Player#attack, ServerPlayer#die and PlayerList#respawn / #remove forward to {@link #onHurt}, {@link #afterHurt},
 * {@link #onDie}, {@link #onRespawn} and {@link #onLogout}; everything else uses Fabric API.
 */
public final class WhatHitMeFabric implements ModInitializer {

    static final Logger LOG = LogManager.getLogger("WhatHitMe");
    /** Vanilla invulnerability ticks of a fresh hit */
    private static final int MAX_INVUL = 20;
    private static ServerPortMc port;

    @Override
    public void onInitialize() {
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
                return FabricLoader.getInstance().getModContainer(modId).map(c -> c.getMetadata().getName()).orElse(modId);
            }

            @Override
            public Collection<String> modIds() {
                List<String> out = new ArrayList<>();
                for (ModContainer c : FabricLoader.getInstance().getAllMods()) out.add(c.getMetadata().getId());
                return out;
            }

            @Override
            public String version(String modId) {
                return FabricLoader.getInstance().getModContainer(modId).map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("");
            }
        };
        Facts.modOfSource = WhatHitMeFabric::modOfSource;
        Registries115.modFiles = WhatHitMeFabric::modFiles;
        Platform.serverLang = McText.LANG;
        Platform.configDir = FabricLoader.getInstance().getConfigDir().toFile();
        WhmConfig.init(Platform.configDir, FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT, CfgFile::new);
        ServerPortMc.replacesDeathMessagesFlag = true;
        ServerPortMc.requireClientModFlag = true;
        ServerPortMc.loaderName = "Fabric";
        ServerSidePacketRegistry.INSTANCE.register(FabricNet.ID, (ctx, buf) -> {
            Packets.Packet p = FabricNet.read(buf);
            if (p == null || !Packets.serverbound(p.type()) || !(ctx.getPlayer() instanceof ServerPlayer)) return;
            ServerPlayer player = (ServerPlayer) ctx.getPlayer();
            FabricNet.MOD_CLIENTS.add(player.getUUID());
            ctx.getTaskQueue().execute(() -> ServerCore.onPacket(new FabricPlayer(player), p));
        });

        ServerStartCallback.EVENT.register(server -> {
            port = new ServerPortMc(server, FabricPlayer::new);
            ServerCore.serverStarted(port);
            Safe.run("catalog cache", () -> CatalogCache.init(port.registries()));
            LOG.info("What Hit Me? catalog cache: " + CatalogCache.status + " in " + CatalogCache.initMillis + " ms, damage id scan: " + CatalogCache.scanStatus);
        });
        ServerStopCallback.EVENT.register(server -> {
            ServerCore.serverStopped();
            port = null;
        });
        CommandRegistry.INSTANCE.register(false, dispatcher -> Brig.register(dispatcher, (s, args) -> {
            if (port != null) ServerCore.execute(port.source(s), args);
        }, (s, args) -> port == null ? java.util.Collections.<String>emptyList() : ServerCore.complete(port.source(s), args)));
    }

    private static ServerPlayer tracked(Player e) {
        return e instanceof ServerPlayer && !e.level.isClientSide ? (ServerPlayer) e : null;
    }

    /** Player#attack, first line (reflected damage) */
    public static void onAttack(Player e, net.minecraft.world.entity.Entity target) {
        ServerPlayer p = tracked(e);
        if (p == null || target == null) return;
        ServerCore.onPlayerAttack(p.getUUID(), target.getUUID().toString());
    }

    /** Player#actuallyHurt, first line: damage before armor (a fresh hit has just set invulnerableTime to its maximum) */
    public static void onHurt(Player e, DamageSource source, float amount) {
        ServerPlayer p = tracked(e);
        if (p == null || amount <= 0) return;
        ServerCore.onHurt(new FabricPlayer(p), p.level.getGameTime(), source, amount, p.getAbsorptionAmount(), p.getHealth(), p.isBlocking(),
            p.invulnerableTime >= MAX_INVUL, MAX_INVUL);
    }

    /** Player#actuallyHurt, last line: the damage after armor / absorption is applied (lethal hits too) */
    public static void afterHurt(Player e, DamageSource source) {
        ServerPlayer p = tracked(e);
        if (p == null) return;
        ServerCore.onDamageAfter(new FabricPlayer(p), p.level.getGameTime(), source, p.getHealth(), p.getAbsorptionAmount(), () -> Facts.hit(p, source));
    }

    /** Custom death message of a dying player, from onDie to the death message mixin */
    private static final Map<java.util.UUID, net.minecraft.network.chat.Component> CUSTOM_DM = new java.util.concurrent.ConcurrentHashMap<>();

    /** From the death message mixin: the pack's custom death message (decided in onDie), or the vanilla one */
    public static net.minecraft.network.chat.Component deathMessage(ServerPlayer p, net.minecraft.network.chat.Component vanilla) {
        net.minecraft.network.chat.Component c = CUSTOM_DM.remove(p.getUUID());
        return c != null ? c : vanilla;
    }

    /** ServerPlayer#die, first line (the combat tracker still has the death message) */
    public static void onDie(ServerPlayer p, DamageSource source) {
        if (p.level.isClientSide) return;
        String msg = "";
        CUSTOM_DM.remove(p.getUUID());
        try {
            io.github.junjun1z.whathitme.core.text.Txt t = ServerCore.customDeathMessage(new FabricPlayer(p), () -> Facts.hit(p, source));
            if (t != null) {
                net.minecraft.network.chat.Component c = McText.of(t);
                CUSTOM_DM.put(p.getUUID(), c);
                msg = McText.toJson(c);
            } else {
                msg = McText.toJson(p.getCombatTracker().getDeathMessage());
            }
        } catch (Throwable ignored) {
            // no message
        }
        ServerCore.onDeath(new FabricPlayer(p), () -> Facts.hit(p, source), msg);
    }

    /** PlayerList#respawn, on return (alive = coming back from the End) */
    public static void onRespawn(ServerPlayer newPlayer, boolean alive) {
        if (!alive && newPlayer != null) ServerCore.onRespawn(new FabricPlayer(newPlayer));
    }

    /** PlayerList#remove, first line */
    /** PlayerList#placeNewPlayer, end. requireClientMod: a client without the mod sends no hello; checked a few seconds after joining */
    public static void onLogin(ServerPlayer p) {
        if (p != null) ServerCore.onLogin(new FabricPlayer(p));
    }

    public static void onLogout(ServerPlayer p) {
        if (p == null) return;
        FabricNet.MOD_CLIENTS.remove(p.getUUID());
        ServerCore.onLogout(p.getUUID());
    }

    private static volatile Map<File, String> sources;

    /** Mod jars / directories with their mod id (without minecraft, the loader, Fabric API) */
    private static Map<File, String> sources() {
        Map<File, String> map = sources;
        if (map == null) {
            map = new HashMap<>();
            for (ModContainer c : FabricLoader.getInstance().getAllMods()) {
                String id = c.getMetadata().getId();
                if ("minecraft".equals(id) || "java".equals(id) || "fabricloader".equals(id) || "fabric".equals(id) || id.startsWith("fabric-")) continue;
                try {
                    for (Path p : c.getOrigin().getPaths()) map.putIfAbsent(p.toFile().getCanonicalFile(), id);
                } catch (Throwable t) {
                    // old loader / nested jar: unknown
                }
            }
            sources = map;
        }
        return map;
    }

    /** Mod id of a jar / directory (for damage types of mods) */
    static String modOfSource(File f) {
        return sources().get(f);
    }

    /** Mod files for the damage id scanner (without What Hit Me? itself) */
    static Map<File, String> modFiles() {
        Map<File, String> out = new java.util.LinkedHashMap<>(sources());
        out.values().removeIf(Platform.MODID::equals);
        return out;
    }
}
