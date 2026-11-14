package io.github.junjun1z.whathitme.forge;

import io.github.junjun1z.whathitme.core.server.ServerCore;
import io.github.junjun1z.whathitme.mc.Facts;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Damage after armor and absorption, from LivingDamageEvent (Forge 1.12.2, 14.23+). Its own class because the event
 * class does not exist in Forge for 1.12 / 1.12.1: there {@link DeferredDamageEvents} is registered instead.
 */
public final class DamageEvents {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDamage(LivingDamageEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        if (!(event.getEntityLiving() instanceof EntityPlayerMP) || event.getEntityLiving() instanceof FakePlayer) return;
        EntityPlayerMP p = (EntityPlayerMP) event.getEntityLiving();
        if (p.world.isRemote) return;
        ServerCore.onDamage(new PlayerHandle112(p), p.world.getTotalWorldTime(), event.getSource(), event.getAmount(), p.getAbsorptionAmount(), p.getHealth(),
            () -> Facts.hit(p, event.getSource()));
    }
}
