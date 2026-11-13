package io.github.junjun1z.whathitme.client;

import io.github.junjun1z.whathitme.CommonProxy;
import io.github.junjun1z.whathitme.config.WhmConfig;
import io.github.junjun1z.whathitme.net.PacketEntries;
import io.github.junjun1z.whathitme.net.PacketMessage;
import io.github.junjun1z.whathitme.net.PacketRecap;
import io.github.junjun1z.whathitme.net.PacketServerInfo;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        WhmConfig.init(event.getModConfigurationDirectory(), true);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        ClientCommandHandler.instance.registerCommand(new ClientCommandWhm());
        MinecraftForge.EVENT_BUS.register(new ClientEvents());
        DevHook.start();
    }

    private static void run(Runnable r) {
        Minecraft.getMinecraft().addScheduledTask(r);
    }

    @Override
    public void onServerInfo(PacketServerInfo msg) {
        run(() -> ClientState.onServerInfo(msg));
    }

    @Override
    public void onRecap(PacketRecap msg) {
        run(() -> ClientState.onRecap(msg));
    }

    @Override
    public void onEntries(PacketEntries msg) {
        run(() -> ClientState.onEntries(msg));
    }

    @Override
    public void onMessage(PacketMessage msg) {
        run(() -> ClientState.onMessage(msg));
    }
}
