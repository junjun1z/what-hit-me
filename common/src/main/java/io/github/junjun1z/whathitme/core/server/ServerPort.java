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

    /** Built-in rule conditions this version supports ("advancement", "tag", "score"); empty = none */
    default java.util.Set<String> builtinConditions() {
        return java.util.Collections.emptySet();
    }

    /** "Forge 1.20.1" style name of loader and version for messages ("" = unknown) */
    default String platformName() {
        return "";
    }

    /**
     * Clients understand translations with an English fallback (Txt.trFallback; 1.19.4+), so a death message the server
     * replaces for everyone can be translated for players with the mod
     */
    default boolean translationFallback() {
        return false;
    }

    /** requireClientMod works on this loader (newer Forge / NeoForge: handshake; Fabric, old Forge: {@code ServerCore.onLogin} + disconnect) */
    default boolean canRequireClientMod() {
        return false;
    }

    /**
     * Values the pack editor offers for a built-in condition prefix: "advancement" (advancement ids), "score" (scoreboard
     * objectives), "tag" (tags of online players); empty = none known (the field still takes any text)
     */
    default List<String> conditionValues(String prefix) {
        return java.util.Collections.emptyList();
    }
}
