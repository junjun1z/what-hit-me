package io.github.junjun1z.whathitme.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Changes a listener makes to a record: name, hint, badges, hidden fields. A progression rule that matches later still
 * wins for name and hint; hidden fields add up.
 */
public abstract class RecordChanges extends WhmEvent {

    private String name;
    private String hint;
    private final List<String> badges = new ArrayList<>();
    private final Set<String> hidden = new LinkedHashSet<>();

    protected RecordChanges(WhmPlayer player) {
        super(player);
    }

    /** Display name of the record (&amp; colors, lang:keys, template variables) */
    public void setName(String name) {
        this.name = name;
    }

    /** Tip of the record ("" = no tip) */
    public void setHint(String hint) {
        this.hint = hint == null ? "" : hint;
    }

    /** An extra badge text shown with the record (&amp; colors, lang:keys) */
    public void addBadge(String text) {
        if (text != null && !text.trim().isEmpty()) badges.add(text);
    }

    /**
     * Hides a field from the player (names of the rules "hide" list: damage_id, attacker, mod, weapon, death_message,
     * tip, properties, environment, owner, distance, position)
     */
    public void hide(String field) {
        if (field != null && !field.trim().isEmpty()) hidden.add(field.trim());
    }

    /** null = not set */
    public String getNewName() {
        return name;
    }

    /** null = not set */
    public String getNewHint() {
        return hint;
    }

    public List<String> getNewBadges() {
        return Collections.unmodifiableList(badges);
    }

    public Set<String> getHiddenFields() {
        return Collections.unmodifiableSet(hidden);
    }

    /** Anything changed? */
    public boolean changed() {
        return name != null || hint != null || !badges.isEmpty() || !hidden.isEmpty();
    }
}
