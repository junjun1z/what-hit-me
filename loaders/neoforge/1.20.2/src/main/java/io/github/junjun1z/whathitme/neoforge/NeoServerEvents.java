package io.github.junjun1z.whathitme.neoforge;

import io.github.junjun1z.whathitme.core.server.ServerCore;
import io.github.junjun1z.whathitme.mc.Brig;
import io.github.junjun1z.whathitme.mc.Facts;
import io.github.junjun1z.whathitme.mc.McText;
import io.github.junjun1z.whathitme.mc.ServerPortMc;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingHurtEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

/**
 * NeoForge 1.20.2 - 1.20.4 events of the logical server, forwarded to {@link ServerCore}. Every handler returns on its first line
 * for anything that is not a player.
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
        port = new ServerPortMc(event.getServer(), NeoPlayer::new);
        ServerCore.serverStarted(port);
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
        ServerCore.onHurt(p.getUUID(), p.level().getGameTime(), event.getSource(), event.getAmount(), p.getAbsorptionAmount(), p.getHealth(), p.isBlocking(),
            p.invulnerableTime >= MAX_INVUL, MAX_INVUL);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onHurtCancelled(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer) || !event.isCanceled()) return;
        ServerPlayer p = tracked(event.getEntity());
        if (p == null) return;
        ServerCore.onHurtCancelled(new NeoPlayer(p), p.level().getGameTime(), event.getSource(), event.getAmount(), () -> Facts.hit(p, event.getSource()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer)) return;
        ServerPlayer p = tracked(event.getEntity());
        if (p == null) return;
        ServerCore.onDamage(new NeoPlayer(p), p.level().getGameTime(), event.getSource(), event.getAmount(), p.getAbsorptionAmount(), p.getHealth(),
            () -> Facts.hit(p, event.getSource()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer)) return;
        ServerPlayer p = tracked(event.getEntity());
        if (p == null) return;
        String msg = "";
        try {
            msg = McText.toJson(p.getCombatTracker().getDeathMessage());
        } catch (Throwable ignored) {
            // no message
        }
        ServerCore.onDeath(new NeoPlayer(p), () -> Facts.hit(p, event.getSource()), msg);
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer) || event.isEndConquered()) return;
        ServerCore.onRespawn(new NeoPlayer((ServerPlayer) event.getEntity()));
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) ServerCore.onTickEnd();
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ServerCore.onLogout(event.getEntity().getUUID());
    }
}
