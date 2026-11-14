package io.github.junjun1z.whathitme.core.text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import io.github.junjun1z.whathitme.core.config.WhmConfig;
import io.github.junjun1z.whathitme.core.config.WhmConfig.Flag;

/** What to show and how, for one recipient: effective display flags, ignore list, renames, tips. */
public final class DisplayOptions {

    public final int mask;
    private final List<Pattern> ignore = new ArrayList<>();
    private final Map<String, String> rename = new LinkedHashMap<>();
    private final Map<String, String> hint = new LinkedHashMap<>();
    /** The recipient has the mod: buttons run client commands (clipboard) */
    public final boolean clientActions;
    public final int summarySeconds;

    public DisplayOptions(int mask, boolean clientActions, int summarySeconds) {
        this.mask = mask;
        this.clientActions = clientActions;
        this.summarySeconds = summarySeconds;
    }

    public DisplayOptions ignore(Collection<String> globs) {
        for (String s : globs) {
            String t = s.trim();
            if (!t.isEmpty()) ignore.add(WhmConfig.glob(t));
        }
        return this;
    }

    public DisplayOptions renames(Map<String, String> m) {
        rename.putAll(m);
        return this;
    }

    public DisplayOptions hints(Map<String, String> m) {
        hint.putAll(m);
        return this;
    }

    /** A copy of these options with one flag turned off */
    public DisplayOptions without(Flag f) {
        DisplayOptions o = new DisplayOptions(mask & ~f.bit(), clientActions, summarySeconds);
        o.ignore.addAll(ignore);
        o.rename.putAll(rename);
        o.hint.putAll(hint);
        return o;
    }

    public boolean on(Flag f) {
        return (mask & f.bit()) != 0;
    }

    public boolean ignored(String id) {
        for (Pattern p : ignore) if (p.matcher(id).matches()) return true;
        return false;
    }

    public String rename(String id) {
        return lookup(rename, id);
    }

    public String hint(String id) {
        return lookup(hint, id);
    }

    private static String lookup(Map<String, String> m, String id) {
        String v = m.get(id);
        if (v != null) return v;
        for (Map.Entry<String, String> e : m.entrySet()) {
            if (e.getKey().indexOf('*') >= 0 && WhmConfig.glob(e.getKey()).matcher(id).matches()) return e.getValue();
        }
        return null;
    }
}
