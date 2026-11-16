package io.github.junjun1z.whathitme.core.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A chat text independent of the Minecraft version: literal text, a translation key with arguments, or a native JSON
 * text (a vanilla death message), with color / bold / underline, a click action, a hover text and appended siblings.
 * Unset style fields are inherited from the parent like in Minecraft. Each Minecraft version converts it to its own
 * component type ({@code McText} in versions/&lt;mc&gt;/).
 */
public final class Txt {

    public enum Kind { LITERAL, TRANSLATE, JSON }

    public enum Click { RUN_COMMAND, SUGGEST_COMMAND }

    private static final Pattern FORMAT = Pattern.compile("%(?:(\\d+)\\$)?([A-Za-z%]|$)");

    public final Kind kind;
    /** Literal text, translation key or JSON */
    public final String text;
    /** Translation arguments: Txt or plain values */
    private final Object[] args;
    private Fmt color;
    private Boolean bold;
    private Boolean underlined;
    private Click click;
    private String clickValue;
    private Txt hover;
    private List<Txt> siblings;
    /** Translation: text shown by clients that do not know the key (null = none) */
    private String fallback;

    private Txt(Kind kind, String text, Object[] args) {
        this.kind = kind;
        this.text = text == null ? "" : text;
        this.args = args == null ? new Object[0] : args;
    }

    // ------------------------------------------------------------ factories

    public static Txt lit(String s) {
        return new Txt(Kind.LITERAL, s, null);
    }

    public static Txt lit(Fmt color, String s) {
        return lit(s).color(color);
    }

    public static Txt tr(String key, Object... args) {
        return new Txt(Kind.TRANSLATE, key, args);
    }

    /**
     * A translation with an English fallback for clients without the key (1.19.4+ translatableWithFallback; versions without
     * it use the key). Only send it where {@code ServerPort.translationFallback()} is true.
     */
    public static Txt trFallback(String key, String fallback, Object... args) {
        Txt t = new Txt(Kind.TRANSLATE, key, args);
        t.fallback = fallback;
        return t;
    }

    /** A native JSON text component (e.g. the vanilla death message), converted by the Minecraft version */
    public static Txt json(String json) {
        return new Txt(Kind.JSON, json, null);
    }

    public static Txt empty() {
        return lit("");
    }

    // ------------------------------------------------------------ building

    public Txt append(Txt t) {
        if (siblings == null) siblings = new ArrayList<>(4);
        siblings.add(t);
        return this;
    }

    public Txt append(String s) {
        return append(lit(s));
    }

    public Txt color(Fmt c) {
        color = c;
        return this;
    }

    public Txt bold(boolean b) {
        bold = b;
        return this;
    }

    public Txt underline(boolean u) {
        underlined = u;
        return this;
    }

    /** Replaces the whole style by just this color (like setStyle(new Style().setColor(c))) */
    public Txt restyle(Fmt c) {
        color = c;
        bold = null;
        underlined = null;
        click = null;
        clickValue = null;
        hover = null;
        return this;
    }

    public Txt click(Click action, String value) {
        click = action;
        clickValue = value;
        return this;
    }

    public Txt hover(Txt h) {
        hover = h;
        return this;
    }

    // ------------------------------------------------------------ reading (for the version adapters)

    public Object[] args() {
        return args.clone();
    }

    /** Fallback text of a translation, null = none */
    public String fallback() {
        return fallback;
    }

    public Fmt color() {
        return color;
    }

    public Boolean bold() {
        return bold;
    }

    public Boolean underlined() {
        return underlined;
    }

    public Click clickAction() {
        return click;
    }

    public String clickValue() {
        return clickValue;
    }

    public Txt hoverText() {
        return hover;
    }

    public List<Txt> siblings() {
        return siblings == null ? Collections.<Txt>emptyList() : Collections.unmodifiableList(siblings);
    }

    // ------------------------------------------------------------ flattening

    /** Receives the text runs of a flattened Txt with their effective style */
    public interface Sink {
        void run(String text, Fmt color, boolean bold, boolean underlined);
    }

    public void visit(Lang lang, Sink sink) {
        visit(lang, null, false, false, sink);
    }

    private void visit(Lang lang, Fmt pc, boolean pb, boolean pu, Sink sink) {
        Fmt c = color != null ? color : pc;
        boolean b = bold != null ? bold : pb;
        boolean u = underlined != null ? underlined : pu;
        switch (kind) {
            case LITERAL:
                if (!text.isEmpty()) sink.run(text, c, b, u);
                break;
            case JSON: {
                String s = lang == null ? "" : lang.jsonText(text);
                if (!s.isEmpty()) sink.run(s, c, b, u);
                break;
            }
            default: {
                String p = lang == null ? null : lang.pattern(text);
                if (p == null) {
                    sink.run(fallback != null ? fallback : text, c, b, u);
                } else {
                    format(lang, p, c, b, u, sink);
                }
                break;
            }
        }
        if (siblings != null) for (Txt s : siblings) s.visit(lang, c, b, u, sink);
    }

    private void format(Lang lang, String pattern, Fmt c, boolean b, boolean u, Sink sink) {
        Matcher m = FORMAT.matcher(pattern);
        int pos = 0;
        int auto = 0;
        StringBuilder sb = new StringBuilder();
        while (m.find(pos)) {
            sb.append(pattern, pos, m.start());
            String type = m.group(2);
            if ("%".equals(type)) {
                sb.append('%');
            } else if ("s".equals(type) || "d".equals(type)) {
                int idx = m.group(1) != null ? Integer.parseInt(m.group(1)) - 1 : auto++;
                Object a = idx >= 0 && idx < args.length ? args[idx] : null;
                if (a instanceof Txt) {
                    if (sb.length() > 0) {
                        sink.run(sb.toString(), c, b, u);
                        sb.setLength(0);
                    }
                    ((Txt) a).visit(lang, c, b, u, sink);
                } else {
                    sb.append(a == null ? "null" : String.valueOf(a));
                }
            } else {
                sb.append(m.group());
            }
            pos = m.end();
            if (m.end() == m.start()) break;
        }
        if (pos < pattern.length()) sb.append(pattern, pos, pattern.length());
        if (sb.length() > 0) sink.run(sb.toString(), c, b, u);
    }

    /** Text without styles (section-sign codes inside literal text are kept) */
    public String toPlain(Lang lang) {
        final StringBuilder sb = new StringBuilder();
        visit(lang, (t, c, b, u) -> sb.append(t));
        return sb.toString();
    }

    /** Text with legacy section-sign codes, like 1.12's getFormattedText() (each run ends with a reset) */
    public String toLegacy(Lang lang) {
        final StringBuilder sb = new StringBuilder();
        visit(lang, (t, c, b, u) -> {
            if (c != null) sb.append(c);
            if (b) sb.append(Fmt.BOLD);
            if (u) sb.append(Fmt.UNDERLINE);
            sb.append(t).append(Fmt.RESET);
        });
        return sb.toString();
    }

    /**
     * Builds a text from a pattern with the same placeholder rules as Minecraft's translations (%s, %1$s, %%), keeping
     * Txt arguments as children (with their styles). Used for English text the server sends to clients without the mod.
     */
    public static Txt build(String pattern, Object[] args) {
        Txt root = lit("");
        Matcher m = FORMAT.matcher(pattern);
        int pos = 0;
        int auto = 0;
        StringBuilder sb = new StringBuilder();
        while (m.find(pos)) {
            sb.append(pattern, pos, m.start());
            String type = m.group(2);
            if ("%".equals(type)) {
                sb.append('%');
            } else if ("s".equals(type) || "d".equals(type)) {
                int idx = m.group(1) != null ? Integer.parseInt(m.group(1)) - 1 : auto++;
                Object a = idx >= 0 && idx < args.length ? args[idx] : null;
                if (a instanceof Txt) {
                    if (sb.length() > 0) {
                        root.append(lit(sb.toString()));
                        sb.setLength(0);
                    }
                    root.append((Txt) a);
                } else {
                    sb.append(a == null ? "null" : String.valueOf(a));
                }
            } else {
                sb.append(m.group());
            }
            pos = m.end();
            if (m.end() == m.start()) break;
        }
        if (pos < pattern.length()) sb.append(pattern, pos, pattern.length());
        if (sb.length() > 0) root.append(lit(sb.toString()));
        return root;
    }
}
