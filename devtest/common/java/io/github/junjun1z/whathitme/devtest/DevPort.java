package io.github.junjun1z.whathitme.devtest;

import java.io.File;
import java.util.List;
import java.util.UUID;

/**
 * What the self-test needs from the game, implemented once per Minecraft version (devtest/&lt;code_version&gt;/).
 * Development only (compiled with -Pselftest).
 */
public interface DevPort {

    /** Actions on the integrated server (single player test), run on the server thread */
    enum Act {
        /** Normal difficulty, no daylight cycle / mob spawning, keepInventory, noon, survival, no effects, full health, empty inventory */
        SETUP,
        /** Zombie (3 blocks away, iron sword with Sharpness III), skeleton with a bow (8 away), creeper (4 away), all without AI */
        SPAWN_MOBS,
        /** Damage with amount: fall */
        FALL,
        /** Melee damage by the zombie */
        ZOMBIE,
        /** An arrow shot by the skeleton */
        ARROW,
        IN_FIRE,
        ON_FIRE,
        MAGIC,
        /** Explosion caused by the creeper */
        CREEPER,
        GENERIC,
        VOID,
        EXTINGUISH,
        /** Full health, absorption 4 and a diamond chestplate */
        ARMOR_ABSORB,
        REMOVE_CHEST,
        /** Full health, no absorption */
        HEAL,
        /** Full health and a Totem of Undying in the off hand */
        TOTEM,
        /** Poison II and Slowness for 20 s */
        POISON,
        CLEAR_EFFECTS
    }

    /** "1.12.2", "1.20.1", ... */
    String mcVersion();

    File gameDir();

    boolean atMainMenu();

    /** Creates (or loads) a flat single player world with cheats in saves/&lt;folder&gt; */
    void openWorld(String folder);

    /** In a single player world with the integrated server running and the player in it */
    boolean worldReady();

    /** Connected to a server (or world) with the own player present */
    boolean connected();

    boolean deathScreen();

    /** No screen open */
    boolean noScreen();

    float health();

    String playerName();

    UUID playerId();

    /** Respawns and closes the death screen */
    void respawn();

    void quit();

    /** Every chat line (plain text, full lines, not wrapped) */
    List<String> chat();

    void clearChat();

    /** Like typing in the chat screen: client commands first, otherwise sent to the server */
    void command(String cmd);

    /** Sends a chat line / "/command" straight to the server */
    void say(String text);

    boolean inPlayerList(String name);

    void screenshot(String name);

    String clipboard();

    /** Runs an action on the integrated server thread (single player) */
    void server(Act act, float amount);

    /** The integrated server is a dedicated one / none (multiplayer) */
    boolean dedicated();

    /** The version has the Totem of Undying (1.11+); the totem steps are skipped otherwise */
    default boolean hasTotem() {
        return true;
    }
}
