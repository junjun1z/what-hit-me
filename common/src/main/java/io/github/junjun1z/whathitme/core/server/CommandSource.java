package io.github.junjun1z.whathitme.core.server;

import io.github.junjun1z.whathitme.core.cmd.Sink;

/** Who ran a server-side /whm: a player (with or without the mod), the console or a command block. */
public interface CommandSource extends Sink {

    /** The player, null for the console / command blocks */
    PlayerHandle player();

    /** Operator (level 2) or single player owner */
    boolean isAdmin();
}
