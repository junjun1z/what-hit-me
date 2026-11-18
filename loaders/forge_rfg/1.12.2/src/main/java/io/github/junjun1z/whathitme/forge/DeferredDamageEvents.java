package io.github.junjun1z.whathitme.forge;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.github.junjun1z.whathitme.core.server.PlayerHandle;
import io.github.junjun1z.whathitme.core.server.ServerCore;
import io.github.junjun1z.whathitme.core.server.ServerPort;
import io.github.junjun1z.whathitme.mc.Facts;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.DamageSource;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/**
 * Forge for 1.12 / 1.12.1 has no LivingDamageEvent (damage after armor and absorption). As on 1.11.2 and older, the hit
 * seen by LivingHurtEvent is kept until the very next thing that happens to that player (its next hurt event, its death,
 * or a one-off task queued for the next server tick, whichever comes first); by then Minecraft has applied it, and the health /
 * absorption left are read from the player ({@link ServerCore#onDamageAfter}; heals in between are added back). A Totem of Undying that saved the player
 * is recognised by the totem it used.
 * <p>
 * Registered <b>before</b> {@link ServerEvents}, so that each handler here runs before the one of the same priority there.
 */
public final class DeferredDamageEvents {

    private static final class Hit {
        final EntityPlayerMP p;
        final DamageSource src;
        final long tick;
        final int totems;
        /** Health given back between the hit and its read */
        float healed;

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
        PlayerHandle handle = new PlayerHandle112(p);
        // a totem saved the player: the hit was lethal (the totem set health to 1 and gave absorption)
        if (h.totems > 0 && !p.isDead && health <= 1.001f && PlayerHandle112.totems(p) < h.totems) {
            health = 0f;
            absorption = 0f;
            final int before = h.totems;
            handle = new PlayerHandle112(p) {
                @Override
                public int totems() {
                    return before;
                }
            };
        }
        ServerCore.onDamageAfter(handle, h.tick, h.src, health, absorption, () -> Facts.hit(p, h.src));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onHurt(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer) || pending.isEmpty()) return;
        EntityPlayerMP p = tracked(event.getEntityLiving());
        if (p != null) flush(p.getUniqueID());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onHurtLast(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer) || event.getAmount() <= 0) return;
        EntityPlayerMP p = tracked(event.getEntityLiving());
        if (p == null) return;
        UUID id = p.getUniqueID();
        boolean queued = pending.containsKey(id);
        pending.put(id, new Hit(p, event.getSource(), p.world.getTotalWorldTime(), PlayerHandle112.totems(p)));
        ServerPort srv = ServerCore.port;
        if (!queued && srv != null) srv.schedule(() -> flush(id));
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

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDeath(LivingDeathEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer) || pending.isEmpty()) return;
        EntityPlayerMP p = tracked(event.getEntityLiving());
        if (p != null) flush(p.getUniqueID());
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        pending.remove(event.player.getUniqueID());
    }
}
