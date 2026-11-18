package io.github.junjun1z.whathitme.forge;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import com.google.common.util.concurrent.ListenableFutureTask;

import io.github.junjun1z.whathitme.core.data.Registries;
import io.github.junjun1z.whathitme.core.server.PlayerHandle;
import io.github.junjun1z.whathitme.core.server.ServerPort;
import io.github.junjun1z.whathitme.mc.Registries112;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

/** The running 1.12 server for the core. */
final class ServerPort112 implements ServerPort {

    private final MinecraftServer server;

    ServerPort112(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public PlayerHandle player(UUID id) {
        EntityPlayerMP p = server.getPlayerList().getPlayerByUUID(id);
        return p == null ? null : new PlayerHandle112(p);
    }

    @Override
    public PlayerHandle playerByName(String name) {
        EntityPlayerMP p = server.getPlayerList().getPlayerByUsername(name);
        return p == null ? null : new PlayerHandle112(p);
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
        return Registries112.INSTANCE;
    }

    /** MinecraftServer#addScheduledTask runs at once on the server thread; the task queue itself runs at the next tick */
    @SuppressWarnings("unchecked")
    @Override
    public boolean schedule(Runnable task) {
        Queue<FutureTask<?>> q = ObfuscationReflectionHelper.getPrivateValue(MinecraftServer.class, server, "futureTaskQueue", "field_175589_i");
        if (q == null) return false;
        synchronized (q) {
            q.add(ListenableFutureTask.create(Executors.callable(task)));
        }
        return true;
    }

    @Override
    public boolean runCommand(String command) {
        return server.getCommandManager().executeCommand(server, command) > 0;
    }

    private static final java.util.Set<String> CONDITIONS = new java.util.HashSet<>(Arrays.asList("advancement", "tag", "score"));

    @Override
    public List<String> conditionValues(String prefix) {
        java.util.Set<String> out = new java.util.TreeSet<>();
        switch (prefix) {
            case "advancement":
                for (net.minecraft.advancements.Advancement a : server.getAdvancementManager().getAdvancements()) {
                    if (a.getDisplay() != null) out.add(a.getId().toString());
                }
                break;
            case "score":
                for (net.minecraft.scoreboard.ScoreObjective o : server.getWorld(0).getScoreboard().getScoreObjectives()) out.add(o.getName());
                break;
            case "tag":
                for (net.minecraft.entity.player.EntityPlayerMP p : server.getPlayerList().getPlayers()) out.addAll(p.getTags());
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
        return "Forge 1.12.2";
    }

    /** Hello timeout of ServerCore.onLogin (FML's handshake rejection cannot carry our message) */
    @Override
    public boolean canRequireClientMod() {
        return true;
    }
}
