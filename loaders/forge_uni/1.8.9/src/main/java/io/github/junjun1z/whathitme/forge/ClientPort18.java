package io.github.junjun1z.whathitme.forge;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.github.junjun1z.whathitme.core.client.ClientPort;
import io.github.junjun1z.whathitme.core.data.Entry;
import io.github.junjun1z.whathitme.core.data.Gear;
import io.github.junjun1z.whathitme.core.data.Registries;
import io.github.junjun1z.whathitme.core.net.Packets;
import io.github.junjun1z.whathitme.core.text.Txt;
import io.github.junjun1z.whathitme.mc.Facts;
import io.github.junjun1z.whathitme.mc.McText;
import io.github.junjun1z.whathitme.mc.Registries18;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** The 1.8 client for the core. */
@SideOnly(Side.CLIENT)
final class ClientPort18 implements ClientPort {

    private static Minecraft mc() {
        return Minecraft.getMinecraft();
    }

    @Override
    public void print(Txt line) {
        if (mc().ingameGUI != null) mc().ingameGUI.getChatGUI().printChatMessage(McText.of(line));
    }

    @Override
    public void send(Packets.Packet packet) {
        WhmNetwork.channel.sendToServer(WhmNetwork.wrap(packet));
    }

    @Override
    public boolean connected() {
        return mc().getNetHandler() != null;
    }

    @Override
    public void sendServerCommand(String commandWithoutSlash) {
        // EntityPlayerSP#sendChatMessage goes straight to the server (client commands are only run from the chat screen)
        mc().thePlayer.sendChatMessage("/" + commandWithoutSlash);
    }

    @Override
    public void setClipboard(String text) {
        GuiScreen.setClipboardString(text);
    }

    @Override
    public String getClipboard() {
        return GuiScreen.getClipboardString();
    }

    @Override
    public boolean isSingleplayer() {
        return mc().isSingleplayer();
    }

    @Override
    public boolean hasOpPermission() {
        if (mc().thePlayer == null) return false;
        // a client before 1.9 is not told its op level: single player asks the integrated server, on a server the server decides
        net.minecraft.server.integrated.IntegratedServer s = mc().getIntegratedServer();
        if (s == null) return true;
        net.minecraft.entity.player.EntityPlayerMP p = s.getConfigurationManager().getPlayerByUsername(mc().thePlayer.getName());
        return p != null && p.canCommandSenderUseCommand(2, "whm");
    }

    @Override
    public boolean knowsOpLevel() {
        return mc().getIntegratedServer() != null;
    }

    @Override
    public List<String> onlinePlayerNames() {
        List<String> names = new ArrayList<>();
        if (mc().getNetHandler() != null) {
            for (NetworkPlayerInfo i : mc().getNetHandler().getPlayerInfoMap()) names.add(i.getGameProfile().getName());
        }
        return names;
    }

    @Override
    public List<Entry> localEffects() {
        return mc().thePlayer == null ? new ArrayList<>() : Registries18.effects(mc().thePlayer);
    }

    @Override
    public Registries registries() {
        return Registries18.INSTANCE;
    }

    @Override
    public File gameDir() {
        return mc().mcDataDir;
    }

    @Override
    public boolean playerDead() {
        return mc().thePlayer == null || mc().thePlayer.getHealth() <= 0f || mc().thePlayer.isDead || mc().currentScreen instanceof GuiGameOver;
    }

    /** Set by /whm config, opened by {@link ClientEvents} on the next client tick (the chat screen closes after the command) */
    static volatile boolean openConfig;

    @Override
    public boolean openConfigScreen() {
        openConfig = true;
        return true;
    }

    /** Set by /whm template, opened by {@link ClientEvents} on the next client tick */
    static volatile boolean openEditor;

    @Override
    public boolean openTemplateEditor() {
        openEditor = true;
        return true;
    }

    /** Set by /whm pack, opened by {@link ClientEvents} on the next client tick (1 = pack, 2 = personal) */
    static volatile int openPack;

    @Override
    public boolean openPackEditor(boolean personal) {
        openPack = personal ? 2 : 1;
        return true;
    }

    @Override
    public String playerName() {
        return mc().thePlayer == null ? "" : mc().thePlayer.getName();
    }

    @Override
    public Gear localGear() {
        net.minecraft.entity.player.EntityPlayer p = mc().thePlayer;
        if (p == null) return null;
        return new Gear(Facts.dimension(p.worldObj), p.posX, p.posY, p.posZ, p.getMaxHealth(), Facts.armor(p), Registries18.effects(p));
    }

    @Override
    public String[] parseDeathMessage(String json) {
        try {
            IChatComponent c = IChatComponent.Serializer.jsonToComponent(json);
            if (!(c instanceof ChatComponentTranslation)) return null;
            ChatComponentTranslation t = (ChatComponentTranslation) c;
            Object[] args = t.getFormatArgs();
            String attacker = args.length > 1 ? (args[1] instanceof IChatComponent ? ((IChatComponent) args[1]).getUnformattedText() : String.valueOf(args[1])) : "";
            return new String[] {t.getKey(), attacker};
        } catch (Exception e) {
            return null;
        }
    }
}
