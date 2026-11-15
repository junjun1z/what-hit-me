package io.github.junjun1z.whathitme.core.client;

import java.io.File;
import java.util.List;

import io.github.junjun1z.whathitme.core.data.Entry;
import io.github.junjun1z.whathitme.core.data.Registries;
import io.github.junjun1z.whathitme.core.net.Packets;
import io.github.junjun1z.whathitme.core.text.Txt;

/** The local client, implemented per Minecraft version / loader. All calls happen on the client thread. */
public interface ClientPort {

    /** Prints a line in the own chat */
    void print(Txt line);

    /** Sends a packet to the server (ignored when the server cannot receive it) */
    void send(Packets.Packet packet);

    /** Can a hello be sent now (connected to a server / world)? */
    boolean connected();

    /** Sends "/&lt;command&gt;" to the server directly, bypassing client commands */
    void sendServerCommand(String commandWithoutSlash);

    void setClipboard(String text);

    String getClipboard();

    boolean isSingleplayer();

    /** The player may use level 2 commands on the server */
    boolean hasOpPermission();

    /** Names in the player list */
    List<String> onlinePlayerNames();

    /** Active effects of the own player as {@link Entry#ACTIVE_EFFECT} (client view) */
    List<Entry> localEffects();

    Registries registries();

    File gameDir();

    /** Own player exists, is dead (health 0 / removed) or the death screen is open */
    boolean playerDead();

    /** Parses a vanilla death message: {translation key, plain text of argument 2 (the attacker)}, or null */
    String[] parseDeathMessage(String json);

    // ---- 1.2: optional

    /** Position and equipment of the own player (client estimates, [Copy Recap]); null = unknown */
    default io.github.junjun1z.whathitme.core.data.Gear localGear() {
        return null;
    }

    /** Opens the config screen on the next client tick (/whm config without arguments); false = no config screen */
    default boolean openConfigScreen() {
        return false;
    }
}
