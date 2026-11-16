package io.github.junjun1z.whathitme.core.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.github.junjun1z.whathitme.core.data.Json;

/** One condition of a rule: "prefix:argument", "!" in front negates it. Immutable. */
public final class Condition {

    /** The text as written (with "!") */
    public final String raw;
    /** Lower-case prefix, "" = none */
    public final String prefix;
    public final String arg;
    public final boolean negate;

    private Condition(String raw, String prefix, String arg, boolean negate) {
        this.raw = raw;
        this.prefix = prefix;
        this.arg = arg;
        this.negate = negate;
    }

    public static Condition parse(String text) {
        String t = text == null ? "" : text.trim();
        boolean neg = t.startsWith("!");
        String body = neg ? t.substring(1).trim() : t;
        int c = body.indexOf(':');
        String prefix = c > 0 ? body.substring(0, c).trim().toLowerCase(Locale.ROOT) : "";
        String arg = c > 0 ? body.substring(c + 1).trim() : body;
        return new Condition(t, prefix, arg, neg);
    }

    /** The condition without "!" */
    public String positive() {
        return prefix.isEmpty() ? arg : prefix + ":" + arg;
    }

    /** A JSON value: one string or a list of strings */
    public static List<Condition> list(Object v) {
        List<Condition> out = new ArrayList<>();
        if (v instanceof String) {
            if (!((String) v).trim().isEmpty()) out.add(parse((String) v));
            return out;
        }
        for (Object o : Json.list(v)) {
            String s = Json.str(o, "").trim();
            if (!s.isEmpty()) out.add(parse(s));
        }
        return out;
    }

    @Override
    public String toString() {
        return raw;
    }
}
