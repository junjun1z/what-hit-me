package io.github.junjun1z.whathitme.fabric;

import java.util.List;

import io.github.junjun1z.whathitme.core.net.Packets;
import io.github.junjun1z.whathitme.core.server.FilePlayerPrefs;
import io.github.junjun1z.whathitme.mc.McPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

/** A Fabric 1.21.3 server player: packets through {@link FabricNet}, settings in &lt;world&gt;/whathitme/players/. */
final class FabricPlayer extends McPlayer {

    FabricPlayer(ServerPlayer p) {
        super(p);
    }

    private FilePlayerPrefs prefs() {
        return new FilePlayerPrefs(p.server.getWorldPath(LevelResource.ROOT).toFile(), p.getUUID());
    }

    @Override
    public void send(Packets.Packet packet) {
        FabricNet.sendTo(p, packet);
    }

    @Override
    public boolean chatOff() {
        return prefs().chatOff();
    }

    @Override
    public boolean toggleChat() {
        return prefs().toggleChat();
    }

    @Override
    public List<String> personalIgnored() {
        return prefs().ignored();
    }

    @Override
    public boolean togglePersonalIgnore(String id) {
        return prefs().toggleIgnore(id);
    }
}
