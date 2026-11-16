package io.github.junjun1z.whathitme.core.server;

import java.io.File;
import java.util.List;
import java.util.UUID;

import io.github.junjun1z.whathitme.core.data.Registries;

/** The running logical server, implemented by the loader. */
public interface ServerPort {

    /** Online player, null when not online */
    PlayerHandle player(UUID id);

    PlayerHandle playerByName(String name);

    List<String> onlinePlayerNames();

    /** Server directory (whathitme_dump.csv goes there) */
    File gameDir();

    Registries registries();

    // ---- 1.2: optional

    /**
     * Runs a task once on the server thread after the current one (next tick), never immediately. Used instead of a
     * tick handler to check the outcome of a lethal / heavy hit. false = not supported: the loader still calls
     * {@code ServerCore.onTickEnd()}.
     */
    default boolean schedule(Runnable task) {
        return false;
    }

    /** Runs a command with server console permission (death triggers); false = not supported */
    default boolean runCommand(String command) {
        return false;
    }

    // ---- 1.3: optional

    /**
     * The loader replaces the vanilla death message itself (Fabric mixin) with {@code ServerCore.customDeathMessage};
     * false = players with the mod get it from the server and replace the chat line themselves.
     */
    default boolean replacesDeathMessages() {
        return false;
    }

}
