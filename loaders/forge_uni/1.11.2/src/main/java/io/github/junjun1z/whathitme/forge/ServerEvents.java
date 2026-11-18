package io.github.junjun1z.whathitme.forge;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.github.junjun1z.whathitme.core.Safe;
import io.github.junjun1z.whathitme.core.data.PlayerHistory;
import io.github.junjun1z.whathitme.core.server.PlayerHandle;
import io.github.junjun1z.whathitme.core.server.ServerCore;
import io.github.junjun1z.whathitme.core.server.ServerPort;
import io.github.junjun1z.whathitme.mc.Facts;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/**
 * Forge 1.11 events of the logical server, forwarded to {@link ServerCore}. Every handler returns on its first line for
 * anything that is not a player, so mob fights cost nothing.
 * <p>
 * Forge before 1.12 has no LivingDamageEvent (damage after armor and absorption). The hit seen by LivingHurtEvent is
 * kept until the very next thing that happens to that player (its next hurt event, its death, or a one-off task queued
 * for the next server tick, whichever comes first); by then Minecraft has applied it, and the health / absorption lost
 * are read from the player ({@link ServerCore#onDamageAfter}; heals in between are added back). A Totem of Undying that saved the player is recognised
 * by the totem it used.
 */
public final class ServerEvents {

    private static final class Hit {
        final EntityPlayerMP p;
        final DamageSource src;
        final long tick;
        final int totems;
        /** Health given back between the hit and its read */
        float healed;
        final long millis = System.currentTimeMillis();

        Hit(EntityPlayerMP p, DamageSource src, long tick, int totems) {
            this.p = p;
            this.src = src;
            this.tick = tick;
            this.totems = totems;
        }
    }

    /** Hits not yet applied / read, normally empty */
    private final Map<UUID, Hit> pending = new HashMap<>();

    private static EntityPlayerMP tracked(Object e) {
        if (!(e instanceof EntityPlayerMP) || e instanceof FakePlayer) return null;
        EntityPlayerMP p = (EntityPlayerMP) e;
        return p.world.isRemote ? null : p;
    }

    private void flush(UUID id) {
        if (pending.isEmpty()) return;
        Hit h = pending.remove(id);
        if (h != null) apply(h);
    }

    private static void apply(Hit h) {
        EntityPlayerMP p = h.p;
        float health = Math.max(0f, p.getHealth() - h.healed);
        float absorption = p.getAbsorptionAmount();
        PlayerHandle handle = new PlayerHandle111(p);
        // a totem saved the player: the hit was lethal (the totem set health to 1 and gave absorption)
        if (h.totems > 0 && !p.isDead && health <= 1.001f && PlayerHandle111.totems(p) < h.totems) {
            health = 0f;
            absorption = 0f;
            final int before = h.totems;
            handle = new PlayerHandle111(p) {
                @Override
                public int totems() {
                    return before;
                }
            };
        }
        // the reflected damage window counts from the hurt event, not from this later read
        PlayerHistory ph = ServerCore.tracker().peek(p.getUniqueID());
        long delay = System.currentTimeMillis() - h.millis;
        if (ph != null) ph.lastAttackTime += delay;
        try {
            ServerCore.onDamageAfter(handle, h.tick, h.src, health, absorption, () -> Facts.hit(p, h.src));
        } finally {
            if (ph != null) ph.lastAttackTime -= delay;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onHurt(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayerMP p = tracked(event.getEntityLiving());
        if (p == null) return;
        flush(p.getUniqueID());
        if (event.getAmount() <= 0) return;
        // a fresh hit has just reset hurtResistantTime to its maximum
        ServerCore.onHurt(new PlayerHandle111(p), p.world.getTotalWorldTime(), event.getSource(), event.getAmount(), p.getAbsorptionAmount(), p.getHealth(),
            p.isActiveItemStackBlocking(), p.hurtResistantTime >= p.maxHurtResistantTime, p.maxHurtResistantTime);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onHurtLast(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayerMP p = tracked(event.getEntityLiving());
        if (p == null) return;
        long tick = p.world.getTotalWorldTime();
        if (event.isCanceled()) {
            ServerCore.onHurtCancelled(new PlayerHandle111(p), tick, event.getSource(), event.getAmount(), () -> Facts.hit(p, event.getSource()));
        } else if (event.getAmount() > 0) {
            UUID id = p.getUniqueID();
            boolean queued = pending.containsKey(id);
            pending.put(id, new Hit(p, event.getSource(), tick, PlayerHandle111.totems(p)));
            ServerPort srv = ServerCore.port;
            if (!queued && srv != null) srv.schedule(() -> flush(id));
        }
    }

    /** Food regeneration (or any heal) between a hit and its read would hide the hit: count it */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onHeal(LivingHealEvent event) {
        if (pending.isEmpty() || event.getAmount() <= 0) return;
        Hit h = pending.get(event.getEntityLiving().getUniqueID());
        if (h == null) return;
        float hp = h.p.getHealth();
        if (hp > 0f) h.healed += Math.min(event.getAmount(), Math.max(0f, h.p.getMaxHealth() - hp));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onDeath(LivingDeathEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayerMP p = tracked(event.getEntityLiving());
        if (p == null) return;
        if (event.isCanceled()) {
            ServerCore.onDeathCancelled(new PlayerHandle111(p), () -> Facts.hit(p, event.getSource()));
            return;
        }
        flush(p.getUniqueID());
        String msg = "";
        try {
            msg = ITextComponent.Serializer.componentToJson(p.getCombatTracker().getDeathMessage());
        } catch (Throwable ignored) {
            // no message
        }
        ServerCore.onDeath(new PlayerHandle111(p), () -> Facts.hit(p, event.getSource()), msg);
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (!(event.getEntityPlayer() instanceof EntityPlayerMP)) return;
        EntityPlayerMP p = tracked(event.getEntityPlayer());
        if (p == null || event.getTarget() == null) return;
        ServerCore.onPlayerAttack(p.getUniqueID(), event.getTarget().getUniqueID().toString());
    }

    @SubscribeEvent
    public void onItemBroken(PlayerDestroyItemEvent event) {
        if (!(event.getEntityPlayer() instanceof EntityPlayerMP)) return;
        EntityPlayerMP p = tracked(event.getEntityPlayer());
        if (p == null) return;
        Safe.run("item break event", () -> {
            int slot = Facts.armorSlot(event.getOriginal());
            if (slot >= 0) ServerCore.onArmorBroken(new PlayerHandle111(p), Facts.item(event.getOriginal()), slot);
        });
    }

    @SubscribeEvent
    public void onWorldSave(WorldEvent.Save event) {
        if (event.getWorld().isRemote || event.getWorld().provider.getDimension() != 0) return;
        ServerCore.onWorldSave();
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.player instanceof EntityPlayerMP) || event.isEndConquered()) return;
        ServerCore.onRespawn(new PlayerHandle111((EntityPlayerMP) event.player));
    }

    /** requireClientMod: a client without the mod sends no hello; checked a few seconds after joining */
    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP && !(event.player instanceof FakePlayer)) ServerCore.onLogin(new PlayerHandle111((EntityPlayerMP) event.player));
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        pending.remove(event.player.getUniqueID());
        ServerCore.onLogout(event.player.getUniqueID());
    }
}
