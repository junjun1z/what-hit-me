package io.github.junjun1z.whathitme.api;

/** The player respawned after a death (before the summary is shown). */
public final class RespawnEvent extends WhmEvent {

    private final WhmDeath death;

    public RespawnEvent(WhmPlayer player, WhmDeath death) {
        super(player);
        this.death = death;
    }

    /** The death before this respawn */
    public WhmDeath getDeath() {
        return death;
    }
}
