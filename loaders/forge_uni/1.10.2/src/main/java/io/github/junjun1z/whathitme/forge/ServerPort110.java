package io.github.junjun1z.whathitme.forge;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import io.github.junjun1z.whathitme.core.data.Registries;
import io.github.junjun1z.whathitme.core.server.PlayerHandle;
import io.github.junjun1z.whathitme.core.server.ServerPort;
import io.github.junjun1z.whathitme.mc.Registries110;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

/** The running 1.10 server for the core. */
final class ServerPort110 implements ServerPort {

    private final MinecraftServer server;

    ServerPort110(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public PlayerHandle player(UUID id) {
        EntityPlayerMP p = server.getPlayerList().getPlayerByUUID(id);
        return p == null ? null : new PlayerHandle110(p);
    }

    @Override
    public PlayerHandle playerByName(String name) {
        EntityPlayerMP p = server.getPlayerList().getPlayerByUsername(name);
        return p == null ? null : new PlayerHandle110(p);
    }

    @Override
    public List<String> onlinePlayerNames() {
        return Arrays.asList(server.getOnlinePlayerNames());
    }

    @Override
    public File gameDir() {
        return server.getDataDirectory();
    }

    @Override
    public Registries registries() {
        return Registries110.INSTANCE;
    }
}
