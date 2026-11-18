package io.github.junjun1z.whathitme.forge;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import io.github.junjun1z.whathitme.core.data.Registries;
import io.github.junjun1z.whathitme.core.server.PlayerHandle;
import io.github.junjun1z.whathitme.core.server.ServerPort;
import io.github.junjun1z.whathitme.mc.Registries111;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

/** The running 1.11 server for the core. */
final class ServerPort111 implements ServerPort {

    private final MinecraftServer server;

    ServerPort111(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public PlayerHandle player(UUID id) {
        EntityPlayerMP p = server.getPlayerList().getPlayerByUUID(id);
        return p == null ? null : new PlayerHandle111(p);
    }

    @Override
    public PlayerHandle playerByName(String name) {
        EntityPlayerMP p = server.getPlayerList().getPlayerByUsername(name);
        return p == null ? null : new PlayerHandle111(p);
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
        return Registries111.INSTANCE;
    }

    private static java.lang.reflect.Field taskQueue;

    /** MinecraftServer#addScheduledTask runs at once on the server thread; the task queue itself runs at the next tick */
    @SuppressWarnings("unchecked")
    @Override
    public boolean schedule(Runnable task) {
        try {
            if (taskQueue == null) {
                for (java.lang.reflect.Field f : MinecraftServer.class.getDeclaredFields()) {
                    if (f.getType() == java.util.Queue.class) {
                        f.setAccessible(true);
                        taskQueue = f;
                        break;
                    }
                }
            }
            if (taskQueue == null) return false;
            java.util.Queue<java.util.concurrent.FutureTask<?>> q = (java.util.Queue<java.util.concurrent.FutureTask<?>>) taskQueue.get(server);
            synchronized (q) {
                q.add(com.google.common.util.concurrent.ListenableFutureTask.create(java.util.concurrent.Executors.callable(task)));
            }
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public boolean runCommand(String command) {
        return server.getCommandManager().executeCommand(server, command) > 0;
    }

    // no advancements before 1.12
    private static final java.util.Set<String> CONDITIONS = new java.util.HashSet<>(Arrays.asList("tag", "score"));

    @Override
    public List<String> conditionValues(String prefix) {
        java.util.Set<String> out = new java.util.TreeSet<>();
        switch (prefix) {
            case "score":
                for (Object o : server.getEntityWorld().getScoreboard().getScoreObjectives()) {
                    out.add(((net.minecraft.scoreboard.ScoreObjective) o).getName());
                }
                break;
            case "tag":
                for (String n : onlinePlayerNames()) {
                    EntityPlayerMP p = server.getPlayerList().getPlayerByUsername(n);
                    if (p != null) out.addAll(p.getTags());
                }
                break;
            default:
                break;
        }
        return new java.util.ArrayList<>(out);
    }

    @Override
    public java.util.Set<String> builtinConditions() {
        return CONDITIONS;
    }

    @Override
    public String platformName() {
        return "Forge " + server.getMinecraftVersion();
    }

    /** Hello timeout of ServerCore.onLogin (FML's handshake rejection cannot carry our message) */
    @Override
    public boolean canRequireClientMod() {
        return true;
    }
}
