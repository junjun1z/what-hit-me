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
}
