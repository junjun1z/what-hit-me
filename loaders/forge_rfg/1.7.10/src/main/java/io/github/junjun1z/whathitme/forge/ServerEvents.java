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
import net.minecraft.util.IChatComponent;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;
import net.minecraftforge.event.world.WorldEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Forge 1.7 events of the logical server, forwarded to {@link ServerCore}. Every handler returns on its first line for
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

    /** Work for the server thread (1.7.10 has no scheduled tasks; normally empty) */
    static final java.util.Queue<Runnable> TASKS = new java.util.concurrent.ConcurrentLinkedQueue<>();

    /** Hits not yet applied / read, normally empty */
    private final Map<UUID, Hit> pending = new HashMap<>();

    private static EntityPlayerMP tracked(Object e) {
        if (!(e instanceof EntityPlayerMP) || e instanceof FakePlayer) return null;
        EntityPlayerMP p = (EntityPlayerMP) e;
        return p.worldObj.isRemote ? null : p;
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
        PlayerHandle handle = new PlayerHandle17(p);
        // a totem saved the player: the hit was lethal (the totem set health to 1 and gave absorption)
        if (h.totems > 0 && !p.isDead && health <= 1.001f && PlayerHandle17.totems(p) < h.totems) {
            health = 0f;
            absorption = 0f;
            final int before = h.totems;
            handle = new PlayerHandle17(p) {
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
        if (!(event.entityLiving instanceof EntityPlayer)) return;
        EntityPlayerMP p = tracked(event.entityLiving);
        if (p == null) return;
        flush(p.getUniqueID());
        if (event.ammount <= 0) return;
        // a fresh hit has just reset hurtResistantTime to its maximum
        ServerCore.onHurt(new PlayerHandle17(p), p.worldObj.getTotalWorldTime(), event.source, event.ammount, p.getAbsorptionAmount(), p.getHealth(),
            p.isBlocking(), p.hurtResistantTime >= p.maxHurtResistantTime, p.maxHurtResistantTime);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onHurtLast(LivingHurtEvent event) {
        if (!(event.entityLiving instanceof EntityPlayer)) return;
        EntityPlayerMP p = tracked(event.entityLiving);
        if (p == null) return;
        long tick = p.worldObj.getTotalWorldTime();
        if (event.isCanceled()) {
            ServerCore.onHurtCancelled(new PlayerHandle17(p), tick, event.source, event.ammount, () -> Facts.hit(p, event.source));
        } else if (event.ammount > 0) {
            UUID id = p.getUniqueID();
            boolean queued = pending.containsKey(id);
            pending.put(id, new Hit(p, event.source, tick, PlayerHandle17.totems(p)));
            ServerPort srv = ServerCore.port;
            if (!queued && srv != null) srv.schedule(() -> flush(id));
        }
    }

    /** Food regeneration (or any heal) between a hit and its read would hide the hit: count it */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onHeal(LivingHealEvent event) {
        if (pending.isEmpty() || event.amount <= 0) return;
        Hit h = pending.get(event.entityLiving.getUniqueID());
        if (h == null) return;
        float hp = h.p.getHealth();
        if (hp > 0f) h.healed += Math.min(event.amount, Math.max(0f, h.p.getMaxHealth() - hp));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onDeath(LivingDeathEvent event) {
        if (!(event.entityLiving instanceof EntityPlayer)) return;
        EntityPlayerMP p = tracked(event.entityLiving);
        if (p == null) return;
        if (event.isCanceled()) {
            ServerCore.onDeathCancelled(new PlayerHandle17(p), () -> Facts.hit(p, event.source));
            return;
        }
        flush(p.getUniqueID());
        String msg = "";
        try {
            msg = IChatComponent.Serializer.func_150696_a(p.func_110142_aN().func_151521_b());
        } catch (Throwable ignored) {
            // no message
        }
        ServerCore.onDeath(new PlayerHandle17(p), () -> Facts.hit(p, event.source), msg);
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (!(event.entityPlayer instanceof EntityPlayerMP)) return;
        EntityPlayerMP p = tracked(event.entityPlayer);
        if (p == null || event.target == null) return;
        ServerCore.onPlayerAttack(p.getUniqueID(), event.target.getUniqueID().toString());
    }

    @SubscribeEvent
    public void onItemBroken(PlayerDestroyItemEvent event) {
        if (!(event.entityPlayer instanceof EntityPlayerMP)) return;
        EntityPlayerMP p = tracked(event.entityPlayer);
        if (p == null) return;
        Safe.run("item break event", () -> {
            int slot = Facts.armorSlot(event.original);
            if (slot >= 0) ServerCore.onArmorBroken(new PlayerHandle17(p), Facts.item(event.original), slot);
        });
    }

    @SubscribeEvent
    public void onWorldSave(WorldEvent.Save event) {
        if (event.world.isRemote || event.world.provider.dimensionId != 0) return;
        ServerCore.onWorldSave();
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) return;
        ServerCore.onRespawn(new PlayerHandle17((EntityPlayerMP) event.player));
    }

    /** 1.7.10 has no server task queue: tasks of {@link ServerPort17#schedule} run here (an empty queue costs one check) */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (TASKS.isEmpty() || event.phase != TickEvent.Phase.END) return;
        Runnable r;
        while ((r = TASKS.poll()) != null) Safe.run("server task", r);
    }

    /** requireClientMod: a client without the mod sends no hello; checked a few seconds after joining */
    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP && !(event.player instanceof FakePlayer)) ServerCore.onLogin(new PlayerHandle17((EntityPlayerMP) event.player));
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        pending.remove(event.player.getUniqueID());
        ServerCore.onLogout(event.player.getUniqueID());
    }
}
