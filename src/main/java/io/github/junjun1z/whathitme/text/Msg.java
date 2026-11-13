package io.github.junjun1z.whathitme.text;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

/**
 * Text for one recipient. {@link #LOCAL}: translation components (the recipient has the mod and our lang files).
 * {@link #ENGLISH}: plain English text read from our en_us.lang, for vanilla clients, the server console and command
 * blocks, which do not have our lang files.
 */
public final class Msg {

    public static final Msg LOCAL = new Msg(true);
    public static final Msg ENGLISH = new Msg(false);

    private static final Pattern FORMAT = Pattern.compile("%(?:(\\d+)\\$)?([A-Za-z%]|$)");
    private static volatile Map<String, String> english;

    public final boolean localized;

    private Msg(boolean localized) {
        this.localized = localized;
    }

    public static Msg of(boolean localized) {
        return localized ? LOCAL : ENGLISH;
    }

    private static Map<String, String> english() {
        Map<String, String> m = english;
        if (m == null) {
            m = new HashMap<>();
            try (InputStream in = Msg.class.getResourceAsStream("/assets/whathitme/lang/en_us.lang")) {
                if (in != null) {
                    BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                    String line;
                    while ((line = r.readLine()) != null) {
                        if (line.isEmpty() || line.charAt(0) == '#') continue;
                        int eq = line.indexOf('=');
                        if (eq > 0) m.put(line.substring(0, eq), line.substring(eq + 1));
                    }
                }
            } catch (Exception ignored) {
                // keys are shown as-is
            }
            english = m;
        }
        return m;
    }

    /** English text of one of our keys (the key itself when unknown) */
    public static String englishRaw(String key) {
        String v = english().get(key);
        return v == null ? key : v;
    }

    /** Does a (vanilla, mod or our) translation key exist for this recipient? */
    @SuppressWarnings("deprecation")
    public boolean has(String key) {
        if (key == null || key.isEmpty()) return false;
        if (localized) return net.minecraft.util.text.translation.I18n.canTranslate(key);
        return english().containsKey(key);
    }

    public ITextComponent tr(String key, Object... args) {
        if (localized) return new TextComponentTranslation(key, args);
        return build(englishRaw(key), args);
    }

    public ITextComponent tr(TextFormatting color, String key, Object... args) {
        ITextComponent c = tr(key, args);
        c.getStyle().setColor(color);
        return c;
    }

    /** Plain string (no styles) */
    @SuppressWarnings("deprecation")
    public String text(String key, Object... args) {
        if (localized) {
            Object[] a = new Object[args.length];
            for (int i = 0; i < args.length; i++) a[i] = args[i] instanceof ITextComponent ? ((ITextComponent) args[i]).getUnformattedText() : args[i];
            return net.minecraft.util.text.translation.I18n.translateToLocalFormatted(key, a);
        }
        return build(englishRaw(key), args).getUnformattedText();
    }

    /** A name from a translation key when the recipient knows it, otherwise the fallback text */
    public ITextComponent name(String key, String fallback) {
        if (localized && key != null && !key.isEmpty() && has(key)) return new TextComponentTranslation(key);
        return new TextComponentString(fallback == null || fallback.isEmpty() ? (key == null ? "" : key) : fallback);
    }

    public static ITextComponent str(String s) {
        return new TextComponentString(s == null ? "" : s);
    }

    public static ITextComponent str(TextFormatting color, String s) {
        ITextComponent c = str(s);
        c.getStyle().setColor(color);
        return c;
    }

    /** Same placeholder rules as TextComponentTranslation (%s, %1$s, %%), keeping the style of component arguments */
    static ITextComponent build(String pattern, Object[] args) {
        TextComponentString root = new TextComponentString("");
        Matcher m = FORMAT.matcher(pattern);
        int pos = 0;
        int auto = 0;
        StringBuilder text = new StringBuilder();
        while (m.find(pos)) {
            text.append(pattern, pos, m.start());
            String type = m.group(2);
            if ("%".equals(type)) {
                text.append('%');
            } else if ("s".equals(type) || "d".equals(type)) {
                int idx = m.group(1) != null ? Integer.parseInt(m.group(1)) - 1 : auto++;
                Object a = idx >= 0 && idx < args.length ? args[idx] : null;
                if (a instanceof ITextComponent) {
                    if (text.length() > 0) {
                        root.appendSibling(new TextComponentString(text.toString()));
                        text.setLength(0);
                    }
                    root.appendSibling(((ITextComponent) a).createCopy());
                } else {
                    text.append(a == null ? "null" : String.valueOf(a));
                }
            } else {
                text.append(m.group());
            }
            pos = m.end();
            if (m.end() == m.start()) break;
        }
        if (pos < pattern.length()) text.append(pattern, pos, pattern.length());
        if (text.length() > 0) root.appendSibling(new TextComponentString(text.toString()));
        return root;
    }
}
