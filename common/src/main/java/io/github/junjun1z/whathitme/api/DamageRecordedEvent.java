package io.github.junjun1z.whathitme.api;

/** A hit is about to be stored in the player's history. Listeners may change or cancel it. */
public final class DamageRecordedEvent extends RecordChanges {

    private final WhmDamage damage;
    private boolean cancelled;

    public DamageRecordedEvent(WhmPlayer player, WhmDamage damage) {
        super(player);
        this.damage = damage;
    }

    public WhmDamage getDamage() {
        return damage;
    }

    /** The hit is not recorded (no history entry, no statistics) */
    public void cancel() {
        cancelled = true;
    }

    public boolean isCancelled() {
        return cancelled;
    }
}
