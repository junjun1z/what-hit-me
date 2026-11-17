package io.github.junjun1z.whathitme.api;

import java.util.UUID;

/** An online player as the API sees it. */
public interface WhmPlayer {

    UUID getUuid();

    String getName();

    /** The version's own player object (ServerPlayer, EntityPlayerMP, ...), null when unknown */
    Object getNativePlayer();

    /**
     * Evaluates a rule condition for this player, e.g. "advancement:minecraft:story/mine_diamond", "tag:boss_ready",
     * "!script:veteran" (false when unknown or not supported)
     */
    boolean check(String condition);
}
