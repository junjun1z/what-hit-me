package io.github.junjun1z.whathitme.fabric;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.config.CfgFile;
import io.github.junjun1z.whathitme.core.config.WhmConfig;
import io.github.junjun1z.whathitme.core.net.Packets;
import io.github.junjun1z.whathitme.core.server.ServerCore;
import io.github.junjun1z.whathitme.mc.Brig;
import io.github.junjun1z.whathitme.mc.Facts;
import io.github.junjun1z.whathitme.mc.McText;
import io.github.junjun1z.whathitme.mc.ServerPortMc;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

/**
 * What Hit Me? on Fabric 1.19.2. Optional on both sides (works on the client only, the server only or both). Damage is
 * read by two tiny mixins, as Fabric API for 1.19 / 1.19.1 has no living entity damage / death events (they came with
 * the 1.19.2 builds; the mixins keep one jar for 1.19 - 1.19.2): {@code PlayerMixin}
 * (start and end of Player#actuallyHurt: before armor, and the health left after it) and {@code ServerPlayerMixin}
 * (ServerPlayer#die).
 */
public final class WhatHitMeFabric implements ModInitializer {

    static final Logger LOG = LoggerFactory.getLogger("WhatHitMe");
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
        };
        Platform.serverLang = McText.LANG;
        Platform.configDir = FabricLoader.getInstance().getConfigDir().toFile();
        WhmConfig.init(Platform.configDir, FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT, CfgFile::new);
        ServerPlayNetworking.registerGlobalReceiver(FabricNet.ID, (server, player, handler, buf, sender) -> {
            Packets.Packet p = FabricNet.read(buf);
            if (p != null && Packets.serverbound(p.type())) server.execute(() -> ServerCore.onPacket(new FabricPlayer(player), p));
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            port = new ServerPortMc(server, FabricPlayer::new);
            ServerCore.serverStarted(port);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            ServerCore.serverStopped();
            port = null;
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> Brig.register(dispatcher, (s, args) -> {
            if (port != null) ServerCore.execute(port.source(s), args);
        }, (s, args) -> port == null ? java.util.Collections.<String>emptyList() : ServerCore.complete(port.source(s), args)));

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (!alive) ServerCore.onRespawn(new FabricPlayer(newPlayer));
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> ServerCore.onTickEnd());
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerCore.onLogout(handler.getPlayer().getUUID());
        });
    }

    /** Damage is about to be applied, before armor (start of Player#actuallyHurt, from PlayerMixin) */
    public static void beforeDamage(ServerPlayer p, DamageSource source, float amount) {
        if (amount <= 0) return;
        // a fresh hit has just reset invulnerableTime to its maximum (a hit during the i-frames only deals the difference)
        ServerCore.onHurt(p.getUUID(), p.level.getGameTime(), source, amount, p.getAbsorptionAmount(), p.getHealth(), p.isBlocking(),
            p.invulnerableTime >= MAX_INVUL, MAX_INVUL);
    }

    /** Health and absorption were reduced (end of Player#actuallyHurt, from PlayerMixin) */
    public static void afterDamage(ServerPlayer p, DamageSource source) {
        ServerCore.onDamageAfter(new FabricPlayer(p), p.level.getGameTime(), source, p.getHealth(), p.getAbsorptionAmount(), () -> Facts.hit(p, source));
    }

    /** The player died (start of ServerPlayer#die, from ServerPlayerMixin: the combat tracker still has the message) */
    public static void onDeath(ServerPlayer p, DamageSource source) {
        String msg;
        try {
            msg = McText.toJson(p.getCombatTracker().getDeathMessage());
        } catch (Throwable ignored) {
            msg = "";
        }
        ServerCore.onDeath(new FabricPlayer(p), () -> Facts.hit(p, source), msg);
    }
}
