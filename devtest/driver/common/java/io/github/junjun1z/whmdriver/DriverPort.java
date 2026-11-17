package io.github.junjun1z.whmdriver;

import java.io.File;

/** The client that runs the driver (a client WITHOUT What Hit Me?). Implemented per Minecraft version / loader. */
public interface DriverPort {

    String mcVersion();

    File gameDir();

    /** Own player exists and the client is connected */
    boolean connected();

    boolean deathScreen();

    /** Respawns and closes the death screen */
    void respawn();

    /** Sends a chat line or "/command" to the server */
    void say(String text);

    void quit();

    /** Text of the disconnect screen when the client was refused / kicked ("" = not on that screen) */
    default String disconnectReason() {
        return "";
    }
}
