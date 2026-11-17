package io.github.junjun1z.whathitme.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The player died; the recap is ready but not sent yet. Name, hint, badges and hidden fields change the killing blow;
 * summary lines are added to the respawn summary.
 */
public final class DeathEvent extends RecordChanges {

    private final WhmDeath death;
    private final List<String> lines = new ArrayList<>();

    public DeathEvent(WhmPlayer player, WhmDeath death) {
        super(player);
        this.death = death;
    }

    public WhmDeath getDeath() {
        return death;
    }

    /** Shortcut for getDeath().getKillingBlow(), null when unknown */
    public WhmDamage getKillingBlow() {
        return death.getKillingBlow();
    }

    /** A line added to the respawn summary (&amp; colors, lang:keys, template variables) */
    public void addSummaryLine(String line) {
        if (line != null) lines.add(line);
    }

    public List<String> getSummaryLines() {
        return Collections.unmodifiableList(lines);
    }

    @Override
    public boolean changed() {
        return super.changed() || !lines.isEmpty();
    }
}
