package io.github.junjun1z.whathitme.mc;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import io.github.junjun1z.whathitme.core.data.Registries;
import io.github.junjun1z.whathitme.core.server.CommandSource;
import io.github.junjun1z.whathitme.core.server.PlayerHandle;
import io.github.junjun1z.whathitme.core.server.ServerPort;
import io.github.junjun1z.whathitme.core.text.Txt;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** The running 1.14.4 server for the core; the loader supplies how a player is wrapped. */
public final class ServerPortMc implements ServerPort {

    private final MinecraftServer server;
    private final Function<ServerPlayer, PlayerHandle> wrap;
    private final Registries114 registries;

    public ServerPortMc(MinecraftServer server, Function<ServerPlayer, PlayerHandle> wrap) {
        this.server = server;
        this.wrap = wrap;
        this.registries = new Registries114();
    }

    @Override
    public PlayerHandle player(UUID id) {
        ServerPlayer p = server.getPlayerList().getPlayer(id);
        return p == null ? null : wrap.apply(p);
    }

    @Override
    public PlayerHandle playerByName(String name) {
        ServerPlayer p = server.getPlayerList().getPlayerByName(name);
        return p == null ? null : wrap.apply(p);
    }

    @Override
    public List<String> onlinePlayerNames() {
        return Arrays.asList(server.getPlayerNames());
    }

    @Override
    public File gameDir() {
        return server.getServerDirectory();
    }

    @Override
    public Registries registries() {
        return registries;
    }

    /** A Brigadier command source for the core */
    public CommandSource source(CommandSourceStack s) {
        final PlayerHandle player = s.getEntity() instanceof ServerPlayer ? wrap.apply((ServerPlayer) s.getEntity()) : null;
        return new CommandSource() {
            @Override
            public PlayerHandle player() {
                return player;
            }

            @Override
            public boolean isAdmin() {
                return player != null ? player.isAdmin() : s.hasPermission(2);
            }

            @Override
            public void send(Txt line) {
                s.sendSuccess(McText.of(line), false);
            }

            @Override
            public void fail(Txt line) {
                s.sendFailure(McText.of(line));
            }
        };
    }

    /** Queued for the next tick (execute would run it at once on the server thread) */
    @Override
    public boolean schedule(Runnable task) {
        server.tell(new net.minecraft.server.TickTask(server.getTickCount(), task));
        return true;
    }

    @Override
    public boolean runCommand(String command) {
        return server.getCommands().performCommand(server.createCommandSourceStack(), command) > 0;
    }
}
