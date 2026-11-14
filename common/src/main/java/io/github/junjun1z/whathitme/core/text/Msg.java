package io.github.junjun1z.whathitme.core.text;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import io.github.junjun1z.whathitme.core.Platform;

/**
 * Text for one recipient. {@link #LOCAL}: translation keys (the recipient has the mod and our language files).
 * {@link #ENGLISH}: plain English text read from our en_us file, for clients without the mod, the server console and
 * command blocks.
 */
public final class Msg {

    public static final Msg LOCAL = new Msg(true);
    public static final Msg ENGLISH = new Msg(false);

    private static volatile Map<String, String> english;

    /** Our English translations (plus the server's own translations for other keys) */
    public static final Lang ENGLISH_LANG = new Lang() {
        @Override
        public String pattern(String key) {
            String v = english().get(key);
            if (v != null) return v;
            Lang s = Platform.serverLang;
            return s == null ? null : s.pattern(key);
        }

        @Override
        public String jsonText(String json) {
            Lang s = Platform.serverLang != null ? Platform.serverLang : Platform.clientLang;
            return s == null ? "" : s.jsonText(json);
        }
    };

    public final boolean localized;

    private Msg(boolean localized) {
        this.localized = localized;
    }

    public static Msg of(boolean localized) {
        return localized ? LOCAL : ENGLISH;
    }

    /** The language this recipient reads (client language or English) */
    public Lang lang() {
        if (localized && Platform.clientLang != null) return Platform.clientLang;
        return ENGLISH_LANG;
    }

    private static Map<String, String> english() {
        Map<String, String> m = english;
        if (m == null) {
            m = new HashMap<>();
            String json = resource("/assets/whathitme/lang/en_us.json");
            if (json != null) {
                try {
                    m.putAll(parseJsonMap(json));
                } catch (RuntimeException ignored) {
                    // keys are shown as-is
                }
            } else {
                String lang = resource("/assets/whathitme/lang/en_us.lang");
                if (lang != null) m.putAll(parseLang(lang));
            }
            english = m;
        }
        return m;
    }

    private static String resource(String path) {
        try (InputStream in = Msg.class.getResourceAsStream(path)) {
            if (in == null) return null;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] b = new byte[8192];
            int n;
            while ((n = in.read(b)) > 0) out.write(b, 0, n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    /** key=value lines (1.12 .lang format) */
    public static Map<String, String> parseLang(String text) {
        Map<String, String> m = new LinkedHashMap<>();
        for (String line : text.split("\r?\n")) {
            if (line.isEmpty() || line.charAt(0) == '#') continue;
            int eq = line.indexOf('=');
            if (eq > 0) m.put(line.substring(0, eq), line.substring(eq + 1));
        }
        return m;
    }

    /** A flat JSON object of strings ({"key": "value", ...}), as used by the language files */
    public static Map<String, String> parseJsonMap(String s) {
        Map<String, String> m = new LinkedHashMap<>();
        int[] p = {0};
        skip(s, p);
        expect(s, p, '{');
        skip(s, p);
        if (peek(s, p) == '}') return m;
        while (true) {
            skip(s, p);
            String k = str(s, p);
            skip(s, p);
            expect(s, p, ':');
            skip(s, p);
            String v = str(s, p);
            m.put(k, v);
            skip(s, p);
            char c = s.charAt(p[0]++);
            if (c == '}') return m;
            if (c != ',') throw new IllegalArgumentException("bad json at " + p[0]);
        }
    }

    private static char peek(String s, int[] p) {
        return s.charAt(p[0]);
    }

    private static void skip(String s, int[] p) {
        while (p[0] < s.length() && (Character.isWhitespace(s.charAt(p[0])) || s.charAt(p[0]) == '﻿')) p[0]++;
    }

    private static void expect(String s, int[] p, char c) {
        if (s.charAt(p[0]++) != c) throw new IllegalArgumentException("expected " + c + " at " + p[0]);
    }

    private static String str(String s, int[] p) {
        expect(s, p, '"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = s.charAt(p[0]++);
            if (c == '"') return sb.toString();
            if (c == '\\') {
                char e = s.charAt(p[0]++);
                switch (e) {
                    case 'n':
                        sb.append('\n');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'u':
                        sb.append((char) Integer.parseInt(s.substring(p[0], p[0] + 4), 16));
                        p[0] += 4;
                        break;
                    default:
                        sb.append(e);
                }
            } else {
                sb.append(c);
            }
        }
    }

    /** English text of one of our keys (the key itself when unknown) */
    public static String englishRaw(String key) {
        String v = english().get(key);
        return v == null ? key : v;
    }

    /** Does a (vanilla, mod or our) translation key exist for this recipient? */
    public boolean has(String key) {
        if (key == null || key.isEmpty()) return false;
        if (localized) return Platform.clientLang != null && Platform.clientLang.has(key);
        return english().containsKey(key);
    }

    public Txt tr(String key, Object... args) {
        if (localized) return Txt.tr(key, args);
        return Txt.build(englishRaw(key), args);
    }

    public Txt tr(Fmt color, String key, Object... args) {
        return tr(key, args).color(color);
    }

    /** Plain string (no styles) */
    public String text(String key, Object... args) {
        return tr(key, args).toPlain(lang());
    }

    /** A name from a translation key when the recipient knows it, otherwise the fallback text */
    public Txt name(String key, String fallback) {
        if (localized && key != null && !key.isEmpty() && has(key)) return Txt.tr(key);
        return Txt.lit(fallback == null || fallback.isEmpty() ? (key == null ? "" : key) : fallback);
    }

    public static Txt str(String s) {
        return Txt.lit(s == null ? "" : s);
    }

    public static Txt str(Fmt color, String s) {
        return str(s).color(color);
    }
}
