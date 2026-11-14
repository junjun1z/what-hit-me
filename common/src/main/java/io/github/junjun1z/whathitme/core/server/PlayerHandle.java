package io.github.junjun1z.whathitme.core.server;

import java.util.List;
import java.util.UUID;

import io.github.junjun1z.whathitme.core.data.Entry;
import io.github.junjun1z.whathitme.core.net.Packets;
import io.github.junjun1z.whathitme.core.text.Txt;

/**
 * A server-side player as the core sees it. The version adapter wraps its player object in a small throw-away
 * implementation when an event or command needs it (never per tick).
 */
public interface PlayerHandle {

    UUID uuid();

    String name();

    /** Permission level 2, or the owner of a single player / LAN world */
    boolean isAdmin();

    /** Alive and in the world (not dead / removed) */
    boolean isAlive();

    float health();

    float maxHealth();

    /** Totems of Undying in both hands */
    int totems();

    /** Sends a chat line */
    void chat(Txt line);

    /** Sends a packet (only called for players whose client said hello) */
    void send(Packets.Packet packet);

    // ---- settings of players without the mod, kept across deaths and logins

    boolean chatOff();

    /** @return true when the respawn summary is on now */
    boolean toggleChat();

    List<String> personalIgnored();

    /** @return true when the id is ignored now */
    boolean togglePersonalIgnore(String id);

    /** Active effects as {@link Entry#ACTIVE_EFFECT} */
    List<Entry> effects();
}
