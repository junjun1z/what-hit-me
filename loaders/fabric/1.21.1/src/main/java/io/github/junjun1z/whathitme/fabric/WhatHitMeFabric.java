package io.github.junjun1z.whathitme.fabric;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import io.github.junjun1z.whathitme.mc.ServerPortMc;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;

/**
 * What Hit Me? on Fabric 1.21.1. Optional on both sides (works on the client only, the server only or both). Damage is
 * read through Fabric API's living entity events (ALLOW_DAMAGE before armor, AFTER_DAMAGE / ALLOW_DEATH after it).
 */
public final class WhatHitMeFabric implements ModInitializer {

    static final Logger LOG = LoggerFactory.getLogger("WhatHitMe");
    /** Vanilla invulnerability ticks of a fresh hit */
    private static final int MAX_INVUL = 20;
    private static ServerPortMc port;
    /** Death message read before the death is processed (the combat tracker is reset afterwards) */
    private static final java.util.Map<java.util.UUID, String> DEATH_MSG = new java.util.HashMap<>();

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
        Platform.serverLang = McText.LANG;
        Platform.configDir = FabricLoader.getInstance().getConfigDir().toFile();
        WhmConfig.init(Platform.configDir, FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT, CfgFile::new);
        FabricNet.registerTypes();
        ServerPlayNetworking.registerGlobalReceiver(FabricNet.Payload.TYPE, (payload, ctx) -> {
            Packets.Packet p;
            try {
                p = Packets.decode(payload.data());
            } catch (RuntimeException e) {
                return; // malformed: ignore
            }
            if (Packets.serverbound(p.type())) ServerCore.onPacket(new FabricPlayer(ctx.player()), p);
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            port = new ServerPortMc(server, FabricPlayer::new);
            // dedicated server: JSON texts use its registries (the client sets its own supplier)
            if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) McText.registries = server::registryAccess;
            ServerCore.serverStarted(port);
            Safe.run("catalog cache", () -> CatalogCache.init(port.registries()));
            LOG.info("What Hit Me? catalog cache: " + CatalogCache.status + " in " + CatalogCache.initMillis + " ms, damage id scan: " + CatalogCache.scanStatus);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            ServerCore.serverStopped();
            port = null;
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> Brig.register(dispatcher, (s, args) -> {
            if (port != null) ServerCore.execute(port.source(s), args);
        }, (s, args) -> port == null ? java.util.Collections.<String>emptyList() : ServerCore.complete(port.source(s), args)));

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayer)) return true;
            ServerPlayer p = (ServerPlayer) entity;
            if (amount > 0) {
                // a hit during the invulnerability frames only deals the difference: not a fresh hit
                boolean fresh = p.invulnerableTime <= 10 || source.is(DamageTypeTags.BYPASSES_COOLDOWN);
                ServerCore.onHurt(new FabricPlayer(p), p.level().getGameTime(), source, amount, p.getAbsorptionAmount(), p.getHealth(), p.isBlocking(), fresh, MAX_INVUL);
            }
            return true;
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
            if (!(entity instanceof ServerPlayer)) return;
            DEATH_MSG.remove(entity.getUUID()); // survived (e.g. a totem)
            afterDamage((ServerPlayer) entity, source);
        });
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            // lethal: the damage was applied, the totem check comes next
            if (entity instanceof ServerPlayer) {
                ServerPlayer p = (ServerPlayer) entity;
                afterDamage(p, source);
                try {
                    DEATH_MSG.put(p.getUUID(), McText.toJson(p.getCombatTracker().getDeathMessage()));
                } catch (Throwable ignored) {
                    // no message
                }
            }
            return true;
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayer)) return;
            ServerPlayer p = (ServerPlayer) entity;
            String msg = DEATH_MSG.remove(p.getUUID());
            if (msg == null) {
                try {
                    msg = McText.toJson(p.getCombatTracker().getDeathMessage());
                } catch (Throwable ignored) {
                    msg = "";
                }
            }
            ServerCore.onDeath(new FabricPlayer(p), () -> Facts.hit(p, source), msg);
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (!alive) ServerCore.onRespawn(new FabricPlayer(newPlayer));
        });
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (player instanceof ServerPlayer && entity != null) ServerCore.onPlayerAttack(player.getUUID(), entity.getUUID().toString());
            return InteractionResult.PASS;
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            DEATH_MSG.remove(handler.getPlayer().getUUID());
            ServerCore.onLogout(handler.getPlayer().getUUID());
        });
    }

    private static void afterDamage(ServerPlayer p, DamageSource source) {
        ServerCore.onDamageAfter(new FabricPlayer(p), p.level().getGameTime(), source, p.getHealth(), p.getAbsorptionAmount(), () -> Facts.hit(p, source));
    }
}
