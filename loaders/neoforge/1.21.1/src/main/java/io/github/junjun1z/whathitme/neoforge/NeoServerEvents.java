package io.github.junjun1z.whathitme.neoforge;

import io.github.junjun1z.whathitme.core.Safe;
import io.github.junjun1z.whathitme.core.data.CatalogCache;
import io.github.junjun1z.whathitme.core.server.ServerCore;
import io.github.junjun1z.whathitme.mc.Brig;
import io.github.junjun1z.whathitme.mc.Facts;
import io.github.junjun1z.whathitme.mc.McText;
import io.github.junjun1z.whathitme.mc.ServerPortMc;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerDestroyItemEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * NeoForge 1.21.1 events of the logical server, forwarded to {@link ServerCore}. Every handler returns on its first
 * line for anything that is not a player. LivingIncomingDamageEvent comes before armor (like Fabric's ALLOW_DAMAGE),
 * LivingDamageEvent.Post after the damage was applied (lethal hits too, before the totem check).
 */
public final class NeoServerEvents {

    /** Vanilla invulnerability ticks of a fresh hit */
    private static final int MAX_INVUL = 20;
    private static ServerPortMc port;

    private static ServerPlayer tracked(Object e) {
        if (!(e instanceof ServerPlayer) || e instanceof FakePlayer) return null;
        ServerPlayer p = (ServerPlayer) e;
        return p.level().isClientSide ? null : p;
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        ServerPortMc.loaderName = "NeoForge";
        port = new ServerPortMc(event.getServer(), NeoPlayer::new);
        // dedicated server: JSON texts use its registries (the client sets its own supplier)
        if (!FMLEnvironment.dist.isClient()) McText.registries = event.getServer()::registryAccess;
        ServerCore.serverStarted(port);
        Safe.run("catalog cache", () -> CatalogCache.init(port.registries()));
        WhatHitMeNeoForge.LOG.info("What Hit Me? catalog cache: " + CatalogCache.status + " in " + CatalogCache.initMillis + " ms, damage id scan: " + CatalogCache.scanStatus);
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
    public void onIncoming(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer)) return;
        ServerPlayer p = tracked(event.getEntity());
        if (p == null || event.getAmount() <= 0) return;
        // a hit during the invulnerability frames only deals the difference: not a fresh hit
        boolean fresh = p.invulnerableTime <= 10 || event.getSource().is(DamageTypeTags.BYPASSES_COOLDOWN);
        ServerCore.onHurt(new NeoPlayer(p), p.level().getGameTime(), event.getSource(), event.getAmount(), p.getAbsorptionAmount(), p.getHealth(), p.isBlocking(),
            fresh, MAX_INVUL);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onIncomingCancelled(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer) || !event.isCanceled()) return;
        ServerPlayer p = tracked(event.getEntity());
        if (p == null) return;
        ServerCore.onHurtCancelled(new NeoPlayer(p), p.level().getGameTime(), event.getSource(), event.getAmount(), () -> Facts.hit(p, event.getSource()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDamagePost(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer)) return;
        ServerPlayer p = tracked(event.getEntity());
        if (p == null) return;
        ServerCore.onDamageAfter(new NeoPlayer(p), p.level().getGameTime(), event.getSource(), p.getHealth(), p.getAbsorptionAmount(),
            () -> Facts.hit(p, event.getSource()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer)) return;
        ServerPlayer p = tracked(event.getEntity());
        if (p == null) return;
        if (event.isCanceled()) {
            ServerCore.onDeathCancelled(new NeoPlayer(p), () -> Facts.hit(p, event.getSource()));
            return;
        }
        String msg = "";
        try {
            msg = McText.toJson(p.getCombatTracker().getDeathMessage());
        } catch (Throwable ignored) {
            // no message
        }
        ServerCore.onDeath(new NeoPlayer(p), () -> Facts.hit(p, event.getSource()), msg);
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
            if (slot >= 0) ServerCore.onArmorBroken(new NeoPlayer(p), Facts.item(event.getOriginal()), slot);
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
        ServerCore.onRespawn(new NeoPlayer((ServerPlayer) event.getEntity()));
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ServerCore.onLogout(event.getEntity().getUUID());
    }
}
