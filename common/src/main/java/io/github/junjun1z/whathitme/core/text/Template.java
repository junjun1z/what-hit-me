package io.github.junjun1z.whathitme.core.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * A user template: a list of lines with {variable} placeholders, &amp; color codes, {var|fallback} and {var:2} (decimals).
 * A line whose placeholders are all empty is dropped. A line "lang:key" (or a bare translation key) is translated first.
 * Immutable; the lines are parsed while rendering (cheap, and lang: lines depend on the reader's language).
 */
public final class Template {

    public static final String HOVER = "hover";
    public static final String LINE = "line";
    public static final String SUMMARY = "summary";
    public static final String ALERT = "alert";
    public static final String COPY = "copy";
    /** The templates of the six outputs (the /whm history line and hover use LINE and HOVER) */
    public static final String[] OUTPUTS = {HOVER, LINE, SUMMARY, ALERT, COPY};
    /** Editor pseudo output: the pack's deathMessages ("match = template" per line) */
    public static final String DEATH = "deathmessages";

    /** Values of the placeholders */
    public interface Vars {
        /**
         * @param name canonical variable name
         * @param decimals decimals given in the placeholder, -1 = the variable's default
         * @return the value, null or empty text when empty
         */
        Txt get(String name, int decimals);
    }

    public final List<String> lines;

    private Template(List<String> lines) {
        this.lines = Collections.unmodifiableList(new ArrayList<>(lines));
    }

    public static Template of(List<String> lines) {
        return new Template(lines == null ? Collections.<String>emptyList() : lines);
    }

    public static boolean isOutput(String s) {
        for (String o : OUTPUTS) if (o.equalsIgnoreCase(s)) return true;
        return false;
    }

    public boolean uses(String var) {
        for (String l : lines) {
            for (Placeholder p : placeholders(l)) if (p.name.equals(var)) return true;
        }
        return false;
    }

    /** mob_* are aliases of attacker_*; names are case-insensitive */
    public static String canonical(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        if (n.startsWith("mob_")) n = "attacker_" + n.substring(4);
        return n;
    }

    /** A {name[:decimals][|fallback]} placeholder found in a line */
    public static final class Placeholder {
        public final int start;
        public final int end;
        /** Canonical name */
        public final String name;
        public final String raw;
        public final int decimals;
        public final String fallback;

        Placeholder(int start, int end, String raw, int decimals, String fallback) {
            this.start = start;
            this.end = end;
            this.raw = raw;
            this.name = canonical(raw);
            this.decimals = decimals;
            this.fallback = fallback;
        }
    }

    /** The placeholder starting at i ('{'), or null when the brace is plain text */
    public static Placeholder placeholderAt(String line, int i) {
        if (i >= line.length() || line.charAt(i) != '{') return null;
        int end = line.indexOf('}', i);
        if (end < 0) return null;
        String spec = line.substring(i + 1, end);
        int bar = spec.indexOf('|');
        String fallback = bar >= 0 ? spec.substring(bar + 1) : null;
        String head = bar >= 0 ? spec.substring(0, bar) : spec;
        int colon = head.indexOf(':');
        String name = colon >= 0 ? head.substring(0, colon) : head;
        int dec = -1;
        if (colon >= 0) {
            String d = head.substring(colon + 1);
            if (d.isEmpty() || d.length() > 1 || !Character.isDigit(d.charAt(0))) return null;
            dec = d.charAt(0) - '0';
        }
        if (name.isEmpty()) return null;
        for (int k = 0; k < name.length(); k++) {
            char c = name.charAt(k);
            if (!(c >= 'a' && c <= 'z') && !(c >= 'A' && c <= 'Z') && !(c >= '0' && c <= '9') && c != '_') return null;
        }
        return new Placeholder(i, end + 1, name, dec, fallback);
    }

    public static List<Placeholder> placeholders(String line) {
        List<Placeholder> out = new ArrayList<>();
        int i = 0;
        while (i < line.length()) {
            Placeholder p = placeholderAt(line, i);
            if (p != null) {
                out.add(p);
                i = p.end;
            } else {
                i++;
            }
        }
        return out;
    }

    /** Variable names (as written) of these lines that are not known */
    public static Set<String> unknown(List<String> lines) {
        Set<String> out = new LinkedHashSet<>();
        for (String l : lines) {
            for (Placeholder p : placeholders(l)) if (!TemplateVars.isKnown(p.name)) out.add(p.raw);
        }
        return out;
    }

    /** Rendered lines (dropped lines left out) */
    public List<Txt> render(Msg m, Vars v) {
        List<Txt> out = new ArrayList<>();
        for (String l : lines) {
            Txt t = renderLine(m, l, v);
            if (t != null) out.add(t);
        }
        return out;
    }

    /** The text of a lang: line or of a bare translation key, otherwise the line itself */
    static String resolve(Msg m, String line) {
        String t = line.trim();
        if (t.startsWith("lang:")) {
            String key = t.substring(5).trim();
            Lang lang = m.lang();
            return lang.has(key) ? lang.translate(key) : key;
        }
        if (!t.isEmpty() && t.indexOf(' ') < 0 && t.indexOf('{') < 0 && t.indexOf('.') > 0 && m.has(t)) return m.lang().translate(t);
        return line;
    }

    private static boolean isCode(char c) {
        c = Character.toLowerCase(c);
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'k' && c <= 'o') || c == 'r';
    }

    private static Fmt color(char c) {
        c = Character.toLowerCase(c);
        for (Fmt f : Fmt.values()) if (f.code == c) return f;
        return null;
    }

    /** Current &amp; style while building a line */
    private static final class Style {
        Fmt color;
        boolean bold;
        boolean underline;
        String extra = "";

        Txt apply(Txt t) {
            if (color != null) t.color(color);
            if (bold) t.bold(true);
            if (underline) t.underline(true);
            return t;
        }

        void code(char c) {
            char k = Character.toLowerCase(c);
            Fmt f = color(k);
            if (f != null) {
                color = f;
                bold = false;
                underline = false;
                extra = "";
            } else if (k == 'r') {
                color = null;
                bold = false;
                underline = false;
                extra = "";
            } else if (k == 'l') {
                bold = true;
            } else if (k == 'n') {
                underline = true;
            } else {
                extra += "§" + k;
            }
        }
    }

    /** One line; null when all of its placeholders are empty */
    public static Txt renderLine(Msg m, String line, Vars v) {
        String text = resolve(m, line);
        Txt root = Txt.empty();
        Style st = new Style();
        StringBuilder lit = new StringBuilder();
        int vars = 0;
        int filled = 0;
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < text.length() && isCode(text.charAt(i + 1))) {
                flush(root, lit, st);
                st.code(text.charAt(i + 1));
                i += 2;
                continue;
            }
            if (c == '{') {
                Placeholder p = placeholderAt(text, i);
                if (p != null) {
                    flush(root, lit, st);
                    vars++;
                    Txt val = null;
                    try {
                        val = v.get(p.name, p.decimals);
                    } catch (RuntimeException e) {
                        val = null;
                    }
                    if (empty(val, m)) {
                        val = p.fallback != null && !p.fallback.isEmpty() ? Txt.lit(st.extra + p.fallback.replace('&', '§')) : null;
                    }
                    if (val != null) {
                        filled++;
                        root.append(st.apply(Txt.empty()).append(val));
                    }
                    i = p.end;
                    continue;
                }
            }
            lit.append(c);
            i++;
        }
        flush(root, lit, st);
        if (vars > 0 && filled == 0) return null;
        return root;
    }

    private static void flush(Txt root, StringBuilder lit, Style st) {
        if (lit.length() == 0) return;
        root.append(st.apply(Txt.lit(st.extra + lit)));
        lit.setLength(0);
    }

    static boolean empty(Txt t, Msg m) {
        return t == null || t.toPlain(m.lang()).trim().isEmpty();
    }

    /**
     * Fills only known placeholders of a plain text (death trigger commands); unknown braces stay as they are, no color
     * codes are processed.
     */
    public static String fillPlain(String text, Msg m, Vars v) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            Placeholder p = text.charAt(i) == '{' ? placeholderAt(text, i) : null;
            if (p != null && TemplateVars.isKnown(p.name)) {
                Txt val = null;
                try {
                    val = v.get(p.name, p.decimals);
                } catch (RuntimeException e) {
                    val = null;
                }
                String s = empty(val, m) ? (p.fallback == null ? "" : p.fallback) : Fmt.strip(val.toPlain(m.lang()));
                sb.append(s);
                i = p.end;
            } else {
                sb.append(text.charAt(i++));
            }
        }
        return sb.toString();
    }
}
