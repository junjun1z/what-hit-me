package io.github.junjun1z.whathitme.forge;

import io.github.junjun1z.whathitme.core.server.ServerCore;
import io.github.junjun1z.whathitme.mc.Brig;
import io.github.junjun1z.whathitme.mc.Facts;
import io.github.junjun1z.whathitme.mc.McText;
import io.github.junjun1z.whathitme.mc.ServerPortMc;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Forge 1.15.2 events of the logical server, forwarded to {@link ServerCore}. Every handler returns on its first line
 * for anything that is not a player.
 */
public final class ForgeServerEvents {

    /** Vanilla invulnerability ticks of a fresh hit */
    private static final int MAX_INVUL = 20;
    private static ServerPortMc port;

    private static ServerPlayer tracked(Object e) {
        if (!(e instanceof ServerPlayer) || e instanceof FakePlayer) return null;
        ServerPlayer p = (ServerPlayer) e;
        return p.level.isClientSide ? null : p;
    }

    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
        port = new ServerPortMc(event.getServer(), ForgePlayer::new);
        ServerCore.serverStarted(port);
        // no RegisterCommandsEvent on this Forge: commands are registered when the server starts
        Brig.register(event.getCommandDispatcher(), (s, args) -> {
            if (port != null) ServerCore.execute(port.source(s), args);
        }, (s, args) -> port == null ? java.util.Collections.<String>emptyList() : ServerCore.complete(port.source(s), args));
    }

    @SubscribeEvent
    public void onServerStopped(FMLServerStoppedEvent event) {
        ServerCore.serverStopped();
        port = null;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onHurt(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof ServerPlayer)) return;
        ServerPlayer p = tracked(event.getEntityLiving());
        if (p == null || event.getAmount() <= 0) return;
        // a fresh hit has just reset invulnerableTime to its maximum
        ServerCore.onHurt(p.getUUID(), p.level.getGameTime(), event.getSource(), event.getAmount(), p.getAbsorptionAmount(), p.getHealth(), p.isBlocking(),
            p.invulnerableTime >= MAX_INVUL, MAX_INVUL);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onHurtCancelled(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof ServerPlayer) || !event.isCanceled()) return;
        ServerPlayer p = tracked(event.getEntityLiving());
        if (p == null) return;
        ServerCore.onHurtCancelled(new ForgePlayer(p), p.level.getGameTime(), event.getSource(), event.getAmount(), () -> Facts.hit(p, event.getSource()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDamage(LivingDamageEvent event) {
        if (!(event.getEntityLiving() instanceof ServerPlayer)) return;
        ServerPlayer p = tracked(event.getEntityLiving());
        if (p == null) return;
        ServerCore.onDamage(new ForgePlayer(p), p.level.getGameTime(), event.getSource(), event.getAmount(), p.getAbsorptionAmount(), p.getHealth(),
            () -> Facts.hit(p, event.getSource()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDeath(LivingDeathEvent event) {
        if (!(event.getEntityLiving() instanceof ServerPlayer)) return;
        ServerPlayer p = tracked(event.getEntityLiving());
        if (p == null) return;
        String msg = "";
        try {
            msg = McText.toJson(p.getCombatTracker().getDeathMessage());
        } catch (Throwable ignored) {
            // no message
        }
        ServerCore.onDeath(new ForgePlayer(p), () -> Facts.hit(p, event.getSource()), msg);
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer) || event.isEndConquered()) return;
        ServerCore.onRespawn(new ForgePlayer((ServerPlayer) event.getPlayer()));
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) ServerCore.onTickEnd();
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ForgeNet.MOD_CLIENTS.remove(event.getPlayer().getUUID());
        ServerCore.onLogout(event.getPlayer().getUUID());
    }
}
