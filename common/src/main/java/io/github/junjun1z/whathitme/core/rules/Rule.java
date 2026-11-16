package io.github.junjun1z.whathitme.core.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.github.junjun1z.whathitme.core.data.Json;
import io.github.junjun1z.whathitme.core.data.MatchKey;
import io.github.junjun1z.whathitme.core.data.RuleMark;

/** One entry of the pack "rules" list. Immutable. */
public final class Rule {

    public final String match;
    public final MatchKey key;
    public final Conds conds;
    /** null = not set */
    public final String name;
    /** null = not set, "" = no tip */
    public final String hint;
    /** RuleMark.H_* bits */
    public final int hide;
    public final List<String> commands;
    public final boolean enforce;
    public final boolean oncePerPlayer;
    public final int cooldownSeconds;
    /** "file #n" for messages */
    public final String source;

    private Rule(String match, Conds conds, String name, String hint, int hide, List<String> commands, boolean enforce, boolean oncePerPlayer,
                 int cooldownSeconds, String source) {
        this.match = match;
        this.key = MatchKey.parse(match);
        this.conds = conds;
        this.name = name;
        this.hint = hint;
        this.hide = hide;
        this.commands = Collections.unmodifiableList(new ArrayList<>(commands));
        this.enforce = enforce;
        this.oncePerPlayer = oncePerPlayer;
        this.cooldownSeconds = Math.max(0, cooldownSeconds);
        this.source = source;
    }

    /**
     * Reads one rule object.
     *
     * @param warnings unknown hide names are added here
     */
    public static Rule read(Map<String, Object> o, String source, List<String> warnings) {
        String match = Json.str(o.get("match"), "*").trim();
        if (match.isEmpty()) match = "*";
        int hide = 0;
        List<Object> hl = o.get("hide") instanceof String ? Collections.<Object>singletonList(o.get("hide")) : Json.list(o.get("hide"));
        for (Object h : hl) {
            String n = Json.str(h, "").trim();
            if (n.isEmpty()) continue;
            int bit = RuleMark.hideBit(n);
            if (bit == 0) warnings.add(source + ": unknown hide field \"" + n + "\" (known: " + String.join(", ", RuleMark.HIDE_NAMES) + ")");
            hide |= bit;
        }
        List<String> cmds = new ArrayList<>();
        Object cv = o.get("commands");
        if (cv == null) cv = o.get("command");
        List<Object> cl = cv instanceof String ? Collections.<Object>singletonList(cv) : Json.list(cv);
        for (Object c : cl) {
            String s = Json.str(c, "").trim();
            if (s.startsWith("/")) s = s.substring(1);
            if (!s.isEmpty()) cmds.add(s);
        }
        String name = o.containsKey("name") ? Json.str(o.get("name"), "") : null;
        String hint = o.containsKey("hint") ? Json.str(o.get("hint"), "") : null;
        if (name != null && name.trim().isEmpty()) name = null;
        // hiding a field enforces the rule unless it says "enforce": false
        boolean enforce = Json.bool(o.get("enforce"), hide != 0);
        return new Rule(match, Conds.read(o), name, hint, hide, cmds, enforce, Json.bool(o.get("oncePerPlayer"), false),
            (int) Json.num(o.get("cooldownSeconds"), 0), source);
    }

    public RuleMark mark() {
        int bits = hide;
        if (enforce) bits |= RuleMark.ENFORCE;
        if (name != null) bits |= RuleMark.NAME;
        if (hint != null) bits |= RuleMark.HINT;
        return new RuleMark(bits, name, hint);
    }

    /** Identity used for oncePerPlayer / cooldowns of the commands */
    public String id() {
        return "rule " + match + " -> " + String.join(" ; ", commands);
    }

    @Override
    public String toString() {
        return "\"" + match + "\" (" + source + ")";
    }
}
