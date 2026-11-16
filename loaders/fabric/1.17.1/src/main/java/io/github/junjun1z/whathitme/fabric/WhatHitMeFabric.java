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
import io.github.junjun1z.whathitme.mc.Registries117;
import io.github.junjun1z.whathitme.mc.ServerPortMc;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

/**
 * What Hit Me? on Fabric 1.17.1. Optional on both sides (works on the client only, the server only or both). This Fabric
 * API has no living-entity damage events: two tiny mixins on Player#actuallyHurt (before armor / after the damage) and
 * ServerPlayer#die forward to {@link #onHurt}, {@link #afterHurt} and {@link #onDie}; everything else uses Fabric API.
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
        Registries117.modFiles = WhatHitMeFabric::modFiles;
        Platform.serverLang = McText.LANG;
        Platform.configDir = FabricLoader.getInstance().getConfigDir().toFile();
        WhmConfig.init(Platform.configDir, FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT, CfgFile::new);
        ServerPortMc.replacesDeathMessagesFlag = true;
        ServerPlayNetworking.registerGlobalReceiver(FabricNet.ID, (server, player, handler, buf, sender) -> {
            Packets.Packet p = FabricNet.read(buf);
            if (p == null || !Packets.serverbound(p.type())) return;
            server.execute(() -> ServerCore.onPacket(new FabricPlayer(player), p));
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            port = new ServerPortMc(server, FabricPlayer::new);
            ServerCore.serverStarted(port);
            Safe.run("catalog cache", () -> CatalogCache.init(port.registries()));
            LOG.info("What Hit Me? catalog cache: " + CatalogCache.status + " in " + CatalogCache.initMillis + " ms, damage id scan: " + CatalogCache.scanStatus);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            ServerCore.serverStopped();
            port = null;
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> Brig.register(dispatcher, (s, args) -> {
            if (port != null) ServerCore.execute(port.source(s), args);
        }, (s, args) -> port == null ? java.util.Collections.<String>emptyList() : ServerCore.complete(port.source(s), args)));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (!alive) ServerCore.onRespawn(new FabricPlayer(newPlayer));
        });
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (player instanceof ServerPlayer && !world.isClientSide && entity != null) ServerCore.onPlayerAttack(player.getUUID(), entity.getUUID().toString());
            return InteractionResult.PASS;
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ServerCore.onLogout(handler.player.getUUID()));
    }

    private static ServerPlayer tracked(Player e) {
        return e instanceof ServerPlayer && !e.level.isClientSide ? (ServerPlayer) e : null;
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
