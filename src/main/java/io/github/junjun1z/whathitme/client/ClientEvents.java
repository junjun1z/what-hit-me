package io.github.junjun1z.whathitme.client;

import io.github.junjun1z.whathitme.config.WhmConfig.Flag;
import io.github.junjun1z.whathitme.net.PacketHello;
import io.github.junjun1z.whathitme.net.WhmNetwork;
import io.github.junjun1z.whathitme.text.RecapFormatter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Client events: hello, respawn summary, client fallback, death screen panel. */
@SideOnly(Side.CLIENT)
public final class ClientEvents {

    @SubscribeEvent
    public void onConfigChanged(net.minecraftforge.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent event) {
        if (!io.github.junjun1z.whathitme.WhatHitMeMod.MODID.equals(event.getModID())) return;
        io.github.junjun1z.whathitme.config.WhmConfig.client.saveAndReload();
        io.github.junjun1z.whathitme.config.WhmConfig.common.saveAndReload();
        // single player: the integrated server uses the same common file; tell the client side about it
        net.minecraft.server.integrated.IntegratedServer srv = Minecraft.getMinecraft().getIntegratedServer();
        if (srv != null) srv.addScheduledTask(io.github.junjun1z.whathitme.server.ServerNet::broadcastInfo);
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        Minecraft.getMinecraft().addScheduledTask(ClientState::reset);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null) return;
        if (!ClientState.helloSent && mc.getConnection() != null) {
            ClientState.helloSent = true;
            try {
                WhmNetwork.channel.sendToServer(new PacketHello());
            } catch (Throwable ignored) {
                // a server that does not know the channel simply ignores it
            }
        }
        if (!ClientState.serverHasMod && !mc.isSingleplayer()) ClientFallback.INSTANCE.tick(mc);
        if (ClientState.pendingSummary != null) checkRespawn(mc);
    }

    private static void checkRespawn(Minecraft mc) {
        boolean dead = mc.player.getHealth() <= 0f || mc.player.isDead || mc.currentScreen instanceof GuiGameOver;
        if (dead) {
            ClientState.sawDead = true;
            return;
        }
        if (!ClientState.sawDead) {
            // the recap arrived but no death followed (another mod saved the player)
            if (System.currentTimeMillis() - ClientState.pendingSince > 10000) ClientState.pendingSummary = null;
            return;
        }
        RecapFormatter f = ClientState.formatter();
        if (f.o.on(Flag.RESPAWN_CHAT_SUMMARY)) ClientState.print(f.respawnSummary(ClientState.pendingSummary));
        ClientState.pendingSummary = null;
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (event.getGui() instanceof GuiGameOver) {
            DeathPanel.INSTANCE.onOpen();
            ClientFallback.INSTANCE.onDeathScreen((GuiGameOver) event.getGui());
        }
    }

    @SubscribeEvent
    public void onDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.getGui() instanceof GuiGameOver)) return;
        DeathPanel.INSTANCE.render(event.getGui(), event.getMouseX(), event.getMouseY());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onMouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!(event.getGui() instanceof GuiGameOver)) return;
        if (DeathPanel.INSTANCE.mouse(event.getGui())) event.setCanceled(true);
    }
}
