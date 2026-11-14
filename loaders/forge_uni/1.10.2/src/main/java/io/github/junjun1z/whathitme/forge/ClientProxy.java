package io.github.junjun1z.whathitme.forge;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.client.ClientCore;
import io.github.junjun1z.whathitme.core.net.Packets;
import io.github.junjun1z.whathitme.mc.McText;
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
    public boolean isClient() {
        return true;
    }

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        Platform.clientLang = McText.LANG;
        ClientCore.port = new ClientPort110();
    }

    @Override
    public void init(FMLInitializationEvent event) {
        ClientCommandHandler.instance.registerCommand(new ClientCommandWhm());
        MinecraftForge.EVENT_BUS.register(new ClientEvents());
        DevHook.start();
    }

    @Override
    public void onClientPacket(Packets.Packet p) {
        Minecraft.getMinecraft().addScheduledTask(() -> ClientCore.onPacket(p));
    }
}
