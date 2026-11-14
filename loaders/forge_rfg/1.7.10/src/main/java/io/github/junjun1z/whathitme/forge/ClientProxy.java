package io.github.junjun1z.whathitme.forge;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.client.ClientCore;
import io.github.junjun1z.whathitme.core.net.Packets;
import io.github.junjun1z.whathitme.mc.McText;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {

    @Override
    public boolean isClient() {
        return true;
    }

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        Platform.clientLang = McText.LANG;
        ClientCore.port = new ClientPort17();
    }

    @Override
    public void init(FMLInitializationEvent event) {
        ClientCommandHandler.instance.registerCommand(new ClientCommandWhm());
        ClientEvents ev = new ClientEvents();
        MinecraftForge.EVENT_BUS.register(ev);
        cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(ev);
        DevHook.start();
    }

    @Override
    public void onClientPacket(Packets.Packet p) {
        ClientCore.onPacket(p); // 1.7.10 handles packets on the client thread
    }
}
