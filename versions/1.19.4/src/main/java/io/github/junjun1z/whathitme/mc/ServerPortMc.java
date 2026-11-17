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

/** The running 1.19.4 server for the core; the loader supplies how a player is wrapped. */
public final class ServerPortMc implements ServerPort {

    private final MinecraftServer server;
    private final Function<ServerPlayer, PlayerHandle> wrap;
    private final Registries1194 registries;

    /** Set by a loader that replaces the vanilla death message itself (Fabric mixin) */
    public static volatile boolean replacesDeathMessagesFlag;

    @Override
    public boolean replacesDeathMessages() {
        return replacesDeathMessagesFlag;
    }

    /** "Fabric" / "Forge" / "NeoForge", set by the loader ("" = unknown) */
    public static volatile String loaderName = "";
    /** Set by a loader that checks requireClientMod (hello timeout through ServerCore.onLogin) */
    public static volatile boolean requireClientModFlag;

    private static final java.util.Set<String> CONDITIONS = new java.util.HashSet<>(Arrays.asList("advancement", "tag", "score"));

    @Override
    public java.util.Set<String> builtinConditions() {
        return CONDITIONS;
    }

    @Override
    public String platformName() {
        return loaderName.isEmpty() ? "" : loaderName + " " + net.minecraft.SharedConstants.getCurrentVersion().getName();
    }

    @Override
    public boolean translationFallback() {
        return true;
    }

    @Override
    public boolean canRequireClientMod() {
        return requireClientModFlag;
    }

    public ServerPortMc(MinecraftServer server, Function<ServerPlayer, PlayerHandle> wrap) {
        this.server = server;
        this.wrap = wrap;
        this.registries = new Registries1194(server::registryAccess);
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
                s.sendSystemMessage(McText.of(line));
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
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
        return true;
    }
}
