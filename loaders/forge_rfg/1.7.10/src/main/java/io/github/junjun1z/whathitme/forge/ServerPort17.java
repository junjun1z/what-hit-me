package io.github.junjun1z.whathitme.forge;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import io.github.junjun1z.whathitme.core.data.Registries;
import io.github.junjun1z.whathitme.core.server.PlayerHandle;
import io.github.junjun1z.whathitme.core.server.ServerPort;
import io.github.junjun1z.whathitme.mc.Registries17;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

/** The running 1.7 server for the core. */
final class ServerPort17 implements ServerPort {

    private final MinecraftServer server;

    ServerPort17(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public PlayerHandle player(UUID id) {
        for (Object o : server.getConfigurationManager().playerEntityList) {
            EntityPlayerMP p = (EntityPlayerMP) o;
            if (p.getUniqueID().equals(id)) return new PlayerHandle17(p);
        }
        return null;
    }

    @Override
    public PlayerHandle playerByName(String name) {
        EntityPlayerMP p = server.getConfigurationManager().func_152612_a(name);
        return p == null ? null : new PlayerHandle17(p);
    }

    @Override
    public List<String> onlinePlayerNames() {
        return Arrays.asList(server.getAllUsernames());
    }

    @Override
    public File gameDir() {
        return server.getFile("").getAbsoluteFile();
    }

    @Override
    public Registries registries() {
        return Registries17.INSTANCE;
    }

    /** 1.7.10 has no server task queue: {@link ServerEvents} runs these at the end of the next server tick */
    @Override
    public boolean schedule(Runnable task) {
        ServerEvents.TASKS.add(task);
        return true;
    }

    @Override
    public boolean runCommand(String command) {
        return server.getCommandManager().executeCommand(server, command) > 0;
    }

    // no advancements before 1.12, no entity tags before 1.9
    private static final java.util.Set<String> CONDITIONS = new java.util.HashSet<>(Arrays.asList("score"));

    @Override
    public List<String> conditionValues(String prefix) {
        java.util.Set<String> out = new java.util.TreeSet<>();
        switch (prefix) {
            case "score":
                for (Object o : server.worldServerForDimension(0).getScoreboard().getScoreObjectives()) {
                    out.add(((net.minecraft.scoreboard.ScoreObjective) o).getName());
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
