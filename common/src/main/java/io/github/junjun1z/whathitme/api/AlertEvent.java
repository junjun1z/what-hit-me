package io.github.junjun1z.whathitme.api;

/** A totem / heavy hit / prevented death alert is about to be shown. */
public final class AlertEvent extends WhmEvent {

    private final WhmDeath snapshot;

    public AlertEvent(WhmPlayer player, WhmDeath snapshot) {
        super(player);
        this.snapshot = snapshot;
    }

    /** "totem", "heavy" or "prevented" */
    public String getKind() {
        return snapshot.getKind();
    }

    /** The hit that caused the alert, null when unknown */
    public WhmDamage getDamage() {
        return snapshot.getKillingBlow();
    }

    public WhmDeath getSnapshot() {
        return snapshot;
    }
}
