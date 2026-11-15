package io.github.junjun1z.whathitme.forge;

import io.github.junjun1z.whathitme.core.Safe;
import io.github.junjun1z.whathitme.core.data.CatalogCache;
import io.github.junjun1z.whathitme.core.server.ServerCore;
import io.github.junjun1z.whathitme.mc.Brig;
import io.github.junjun1z.whathitme.mc.Facts;
import io.github.junjun1z.whathitme.mc.McText;
import io.github.junjun1z.whathitme.mc.ServerPortMc;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.listener.ObjBooleanBiConsumer;
import net.minecraftforge.eventbus.api.listener.Priority;

/**
 * Forge 1.21.6+ (EventBus 7) events of the logical server, forwarded to {@link ServerCore}. Every listener returns on
 * its first line for anything that is not a player and never cancels anything; there is no tick handler.
 */
public final class ForgeServerEvents {

    /** Vanilla invulnerability ticks of a fresh hit */
    private static final int MAX_INVUL = 20;
    private static ServerPortMc port;

    private ForgeServerEvents() {}

    private static ServerPlayer tracked(Object e) {
        if (!(e instanceof ServerPlayer) || e.getClass().getSimpleName().contains("FakePlayer")) return null;
        ServerPlayer p = (ServerPlayer) e;
        return p.level().isClientSide() ? null : p;
    }

    static void register() {
        ServerStartingEvent.BUS.addListener(event -> {
            port = new ServerPortMc(event.getServer(), ForgePlayer::new);
            ServerCore.serverStarted(port);
            Safe.run("catalog cache", () -> CatalogCache.init(port.registries()));
            WhatHitMeForge.LOG.info("What Hit Me? catalog cache: " + CatalogCache.status + " in " + CatalogCache.initMillis + " ms, damage id scan: " + CatalogCache.scanStatus);
        });
        ServerStoppedEvent.BUS.addListener(event -> {
            ServerCore.serverStopped();
            port = null;
        });
        RegisterCommandsEvent.BUS.addListener(event -> Brig.register(event.getDispatcher(), (s, args) -> {
            if (port != null) ServerCore.execute(port.source(s), args);
        }, (s, args) -> port == null ? java.util.Collections.<String>emptyList() : ServerCore.complete(port.source(s), args)));

        LivingHurtEvent.BUS.addListener(Priority.HIGHEST, (LivingHurtEvent event) -> {
            if (!(event.getEntity() instanceof ServerPlayer)) return false;
            ServerPlayer p = tracked(event.getEntity());
            if (p == null || event.getAmount() <= 0) return false;
            // a fresh hit has just reset invulnerableTime to its maximum
            ServerCore.onHurt(new ForgePlayer(p), p.level().getGameTime(), event.getSource(), event.getAmount(), p.getAbsorptionAmount(), p.getHealth(),
                p.isBlocking(), p.invulnerableTime >= MAX_INVUL, MAX_INVUL);
            return false;
        });
        // monitor: sees whether another mod cancelled the hit
        LivingHurtEvent.BUS.addListener((ObjBooleanBiConsumer<LivingHurtEvent>) (event, cancelled) -> {
            if (!cancelled || !(event.getEntity() instanceof ServerPlayer)) return;
            ServerPlayer p = tracked(event.getEntity());
            if (p == null) return;
            ServerCore.onHurtCancelled(new ForgePlayer(p), p.level().getGameTime(), event.getSource(), event.getAmount(), () -> Facts.hit(p, event.getSource()));
        });
        LivingDamageEvent.BUS.addListener(Priority.LOWEST, (LivingDamageEvent event) -> {
            if (!(event.getEntity() instanceof ServerPlayer)) return false;
            ServerPlayer p = tracked(event.getEntity());
            if (p == null) return false;
            ServerCore.onDamage(new ForgePlayer(p), p.level().getGameTime(), event.getSource(), event.getAmount(), p.getAbsorptionAmount(), p.getHealth(),
                () -> Facts.hit(p, event.getSource()));
            return false;
        });
        LivingDeathEvent.BUS.addListener(Priority.LOWEST, (LivingDeathEvent event) -> {
            if (!(event.getEntity() instanceof ServerPlayer)) return false;
            ServerPlayer p = tracked(event.getEntity());
            if (p == null) return false;
            String msg = "";
            try {
                msg = McText.toJson(p.getCombatTracker().getDeathMessage());
            } catch (Throwable ignored) {
                // no message
            }
            ServerCore.onDeath(new ForgePlayer(p), () -> Facts.hit(p, event.getSource()), msg);
            return false;
        });
        PlayerEvent.PlayerRespawnEvent.BUS.addListener(event -> {
            if (!(event.getEntity() instanceof ServerPlayer) || event.isEndConquered()) return;
            ServerCore.onRespawn(new ForgePlayer((ServerPlayer) event.getEntity()));
        });
        // monitor: a death cancelled by another mod (revive)
        LivingDeathEvent.BUS.addListener((ObjBooleanBiConsumer<LivingDeathEvent>) (event, cancelled) -> {
            if (!cancelled || !(event.getEntity() instanceof ServerPlayer)) return;
            ServerPlayer p = tracked(event.getEntity());
            if (p == null) return;
            ServerCore.onDeathCancelled(new ForgePlayer(p), () -> Facts.hit(p, event.getSource()));
        });
        AttackEntityEvent.BUS.addListener((AttackEntityEvent event) -> {
            if (!(event.getEntity() instanceof ServerPlayer)) return false;
            ServerPlayer p = tracked(event.getEntity());
            if (p != null && event.getTarget() != null) ServerCore.onPlayerAttack(p.getUUID(), event.getTarget().getUUID().toString());
            return false;
        });
        PlayerDestroyItemEvent.BUS.addListener(event -> {
            if (!(event.getEntity() instanceof ServerPlayer)) return;
            ServerPlayer p = tracked(event.getEntity());
            if (p == null) return;
            Safe.run("item break event", () -> {
                int slot = Facts.armorSlot(p, event.getOriginal());
                if (slot >= 0) ServerCore.onArmorBroken(new ForgePlayer(p), Facts.item(event.getOriginal()), slot);
            });
        });
        LevelEvent.Save.BUS.addListener(event -> {
            if (!(event.getLevel() instanceof ServerLevel)) return;
            if ("minecraft:overworld".equals(Facts.dimension((ServerLevel) event.getLevel()))) ServerCore.onWorldSave();
        });
        PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(event -> ServerCore.onLogout(event.getEntity().getUUID()));
    }
}
