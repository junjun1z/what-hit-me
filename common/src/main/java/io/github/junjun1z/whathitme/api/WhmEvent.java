package io.github.junjun1z.whathitme.api;

/** Base of the API events: the player they are about. */
public abstract class WhmEvent {

    private final WhmPlayer player;

    protected WhmEvent(WhmPlayer player) {
        this.player = player;
    }

    public WhmPlayer getPlayer() {
        return player;
    }
}
