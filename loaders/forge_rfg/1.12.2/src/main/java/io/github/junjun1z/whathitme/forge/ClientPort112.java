package io.github.junjun1z.whathitme.forge;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.github.junjun1z.whathitme.core.client.ClientPort;
import io.github.junjun1z.whathitme.core.data.Entry;
import io.github.junjun1z.whathitme.core.data.Registries;
import io.github.junjun1z.whathitme.core.net.Packets;
import io.github.junjun1z.whathitme.core.text.Txt;
import io.github.junjun1z.whathitme.mc.McText;
import io.github.junjun1z.whathitme.mc.Registries112;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** The 1.12 client for the core. */
@SideOnly(Side.CLIENT)
final class ClientPort112 implements ClientPort {

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
        return mc().getConnection() != null;
    }

    @Override
    public void sendServerCommand(String commandWithoutSlash) {
        // EntityPlayerSP#sendChatMessage goes straight to the server (client commands are only run from the chat screen)
        mc().player.sendChatMessage("/" + commandWithoutSlash);
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
        return mc().player != null && mc().player.canUseCommand(2, "whm");
    }

    @Override
    public List<String> onlinePlayerNames() {
        List<String> names = new ArrayList<>();
        if (mc().getConnection() != null) {
            for (NetworkPlayerInfo i : mc().getConnection().getPlayerInfoMap()) names.add(i.getGameProfile().getName());
        }
        return names;
    }

    @Override
    public List<Entry> localEffects() {
        return mc().player == null ? new ArrayList<>() : Registries112.effects(mc().player);
    }

    @Override
    public Registries registries() {
        return Registries112.INSTANCE;
    }

    @Override
    public File gameDir() {
        return mc().gameDir;
    }

    @Override
    public boolean playerDead() {
        return mc().player == null || mc().player.getHealth() <= 0f || mc().player.isDead || mc().currentScreen instanceof GuiGameOver;
    }

    @Override
    public String[] parseDeathMessage(String json) {
        try {
            ITextComponent c = ITextComponent.Serializer.jsonToComponent(json);
            if (!(c instanceof TextComponentTranslation)) return null;
            TextComponentTranslation t = (TextComponentTranslation) c;
            Object[] args = t.getFormatArgs();
            String attacker = args.length > 1 ? (args[1] instanceof ITextComponent ? ((ITextComponent) args[1]).getUnformattedText() : String.valueOf(args[1])) : "";
            return new String[] {t.getKey(), attacker};
        } catch (Exception e) {
            return null;
        }
    }
}
