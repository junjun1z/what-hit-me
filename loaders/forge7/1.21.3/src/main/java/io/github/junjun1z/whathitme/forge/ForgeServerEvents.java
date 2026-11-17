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
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Forge 1.21.3 events of the logical server, forwarded to {@link ServerCore}. Every handler returns on its first line
 * for anything that is not a player; there is no tick handler.
 */
public final class ForgeServerEvents {

    /** Vanilla invulnerability ticks of a fresh hit */
    private static final int MAX_INVUL = 20;
    private static ServerPortMc port;

    private static ServerPlayer tracked(Object e) {
        if (!(e instanceof ServerPlayer) || e.getClass().getSimpleName().contains("FakePlayer")) return null;
        ServerPlayer p = (ServerPlayer) e;
        return p.level().isClientSide ? null : p;
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        ServerPortMc.loaderName = "Forge";
        // hello timeout: the channel is optional, so the handshake cannot refuse with our message
        ServerPortMc.requireClientModFlag = true;
        port = new ServerPortMc(event.getServer(), ForgePlayer::new);
        ServerCore.serverStarted(port);
        Safe.run("catalog cache", () -> CatalogCache.init(port.registries()));
        WhatHitMeForge.LOG.info("What Hit Me? catalog cache: " + CatalogCache.status + " in " + CatalogCache.initMillis + " ms, damage id scan: " + CatalogCache.scanStatus);
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        ServerCore.serverStopped();
        port = null;
    }

    @SubscribeEvent
    public void onCommands(RegisterCommandsEvent event) {
        Brig.register(event.getDispatcher(), (s, args) -> {
            if (port != null) ServerCore.execute(port.source(s), args);
        }, (s, args) -> port == null ? java.util.Collections.<String>emptyList() : ServerCore.complete(port.source(s), args));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer)) return;
        ServerPlayer p = tracked(event.getEntity());
        if (p == null || event.getAmount() <= 0) return;
        // a fresh hit has just reset invulnerableTime to its maximum
        ServerCore.onHurt(new ForgePlayer(p), p.level().getGameTime(), event.getSource(), event.getAmount(), p.getAbsorptionAmount(), p.getHealth(), p.isBlocking(),
            p.invulnerableTime >= MAX_INVUL, MAX_INVUL);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onHurtCancelled(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer) || !event.isCanceled()) return;
        ServerPlayer p = tracked(event.getEntity());
        if (p == null) return;
        ServerCore.onHurtCancelled(new ForgePlayer(p), p.level().getGameTime(), event.getSource(), event.getAmount(), () -> Facts.hit(p, event.getSource()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer)) return;
        ServerPlayer p = tracked(event.getEntity());
        if (p == null) return;
        ServerCore.onDamage(new ForgePlayer(p), p.level().getGameTime(), event.getSource(), event.getAmount(), p.getAbsorptionAmount(), p.getHealth(),
            () -> Facts.hit(p, event.getSource()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer)) return;
        ServerPlayer p = tracked(event.getEntity());
        if (p == null) return;
        if (event.isCanceled()) {
            ServerCore.onDeathCancelled(new ForgePlayer(p), () -> Facts.hit(p, event.getSource()));
            return;
        }
        String msg = "";
        try {
            msg = McText.toJson(p.getCombatTracker().getDeathMessage());
        } catch (Throwable ignored) {
            // no message
        }
        ServerCore.onDeath(new ForgePlayer(p), () -> Facts.hit(p, event.getSource()), msg);
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer)) return;
        ServerPlayer p = tracked(event.getEntity());
        if (p == null || event.getTarget() == null) return;
        ServerCore.onPlayerAttack(p.getUUID(), event.getTarget().getUUID().toString());
    }

    @SubscribeEvent
    public void onItemBroken(PlayerDestroyItemEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer)) return;
        ServerPlayer p = tracked(event.getEntity());
        if (p == null) return;
        Safe.run("item break event", () -> {
            int slot = Facts.armorSlot(p, event.getOriginal());
            if (slot >= 0) ServerCore.onArmorBroken(new ForgePlayer(p), Facts.item(event.getOriginal()), slot);
        });
    }

    @SubscribeEvent
    public void onWorldSave(LevelEvent.Save event) {
        if (!(event.getLevel() instanceof ServerLevel)) return;
        if ("minecraft:overworld".equals(Facts.dimension((ServerLevel) event.getLevel()))) ServerCore.onWorldSave();
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer) || event.isEndConquered()) return;
        ServerCore.onRespawn(new ForgePlayer((ServerPlayer) event.getEntity()));
    }

    /** requireClientMod: a client without the mod sends no hello; checked a few seconds after joining */
    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        ServerPlayer p = tracked(event.getEntity());
        if (p != null) ServerCore.onLogin(new ForgePlayer(p));
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ServerCore.onLogout(event.getEntity().getUUID());
    }
}
