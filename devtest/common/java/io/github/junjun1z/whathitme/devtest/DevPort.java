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
        CLEAR_EFFECTS,
        // ---- 1.2 (only used when v12() is true)
        /** Thorns damage caused by the zombie */
        THORNS,
        /** The player punches the zombie, which hurts the player in the same tick */
        REFLECT,
        /** Diamond helmet that breaks on the next armor damage */
        BRITTLE_HELMET,
        /** Cobweb at the feet and a stick in the main hand */
        ENV_SETUP,
        CACTUS,
        /** Removes the cobweb and the held item */
        ENV_CLEAR,
        /** Iron armor + poison: equipment for [Copy Recap] */
        GEAR,
        /** Death while health is 10 (health-overhaul part death), then health 0 so the player can respawn */
        VITAL_DEATH,
        /** Health set to 0 and a death without any hurt event before (magic source) */
        SETHEALTH_DEATH
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

    // ---- 1.2

    /** The 1.2 features are implemented by this version (enables the extra checks) */
    default boolean v12() {
        return false;
    }

    /** The client knows its op level (vanilla tells it since 1.9); before, client-only commands leave the check to the server */
    default boolean clientKnowsOp() {
        return true;
    }

    /** The What Hit Me? config screen is open */
    default boolean configScreenOpen() {
        return false;
    }

    default void closeScreen() {}

    /** The entry of a config key is shown greyed out in the config screen */
    default boolean configGuiLocked(String key) {
        return false;
    }

    // ---- 1.3

    /** The 1.3 features (templates, death messages, template editor host) are implemented by this version */
    default boolean v13() {
        return false;
    }

    /** The template editor screen is open */
    default boolean templateEditorOpen() {
        return false;
    }

    /** Opens the What Hit Me? config screen and presses its "Templates..." button */
    default void pressConfigTemplatesButton() {}

}
