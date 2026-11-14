package io.github.junjun1z.whathitme.forge;

import io.github.junjun1z.whathitme.core.server.ServerCore;
import io.github.junjun1z.whathitme.mc.Facts;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Forge 1.12 events of the logical server, forwarded to {@link ServerCore}. Every handler returns on its first line for
 * anything that is not a player, so mob fights cost nothing.
 */
public final class ServerEvents {

    private static EntityPlayerMP tracked(Object e) {
        if (!(e instanceof EntityPlayerMP) || e instanceof FakePlayer) return null;
        EntityPlayerMP p = (EntityPlayerMP) e;
        return p.world.isRemote ? null : p;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onHurt(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayerMP p = tracked(event.getEntityLiving());
        if (p == null || event.getAmount() <= 0) return;
        // a fresh hit has just reset hurtResistantTime to its maximum
        ServerCore.onHurt(p.getUniqueID(), p.world.getTotalWorldTime(), event.getSource(), event.getAmount(), p.getAbsorptionAmount(), p.getHealth(),
            p.isActiveItemStackBlocking(), p.hurtResistantTime >= p.maxHurtResistantTime, p.maxHurtResistantTime);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onHurtCancelled(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer) || !event.isCanceled()) return;
        EntityPlayerMP p = tracked(event.getEntityLiving());
        if (p == null) return;
        ServerCore.onHurtCancelled(new PlayerHandle112(p), p.world.getTotalWorldTime(), event.getSource(), event.getAmount(),
            () -> Facts.hit(p, event.getSource()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDamage(LivingDamageEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayerMP p = tracked(event.getEntityLiving());
        if (p == null) return;
        ServerCore.onDamage(new PlayerHandle112(p), p.world.getTotalWorldTime(), event.getSource(), event.getAmount(), p.getAbsorptionAmount(), p.getHealth(),
            () -> Facts.hit(p, event.getSource()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDeath(LivingDeathEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayerMP p = tracked(event.getEntityLiving());
        if (p == null) return;
        String msg = "";
        try {
            msg = ITextComponent.Serializer.componentToJson(p.getCombatTracker().getDeathMessage());
        } catch (Throwable ignored) {
            // no message
        }
        ServerCore.onDeath(new PlayerHandle112(p), () -> Facts.hit(p, event.getSource()), msg);
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.player instanceof EntityPlayerMP) || event.isEndConquered()) return;
        ServerCore.onRespawn(new PlayerHandle112((EntityPlayerMP) event.player));
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) ServerCore.onTickEnd();
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ServerCore.onLogout(event.player.getUniqueID());
    }
}
