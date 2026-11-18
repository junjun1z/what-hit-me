package io.github.junjun1z.whathitme.core.pack;

import java.util.Map;

import io.github.junjun1z.whathitme.core.data.Json;

/**
 * Sets one top-level key of a JSON file in its text, so comments and the layout of the rest of the file stay as they
 * are (order / enabled / memo of a pack category). Falls back to rewriting the whole file when the text is not a
 * plain object.
 */
public final class JsonPatch {

    private final String s;
    private int p;

    private JsonPatch(String s) {
        this.s = s;
    }

    /** The text with key set to value (null removes the key) */
    public static String set(String text, String key, Object value) {
        try {
            String out = new JsonPatch(text == null ? "" : text).apply(key, value);
            Map<String, Object> o = Json.obj(Json.parse(out));
            boolean ok = value == null ? !o.containsKey(key) : String.valueOf(Json.write(value)).equals(Json.write(o.get(key)));
            if (ok) return out;
        } catch (RuntimeException ignored) {
            // rewrite below
        }
        Map<String, Object> o;
        try {
            o = Json.obj(Json.parse(text == null ? "" : text));
        } catch (RuntimeException e) {
            o = new java.util.LinkedHashMap<>();
        }
        if (value == null) o.remove(key);
        else o.put(key, value);
        return Json.write(o);
    }

    private void ws() {
        while (p < s.length()) {
            char c = s.charAt(p);
            if (Character.isWhitespace(c) || c == '﻿') p++;
            else if (c == '/' && p + 1 < s.length() && s.charAt(p + 1) == '/') skipLine();
            else if (c == '#') skipLine();
            else if (c == '/' && p + 1 < s.length() && s.charAt(p + 1) == '*') {
                int e = s.indexOf("*/", p + 2);
                p = e < 0 ? s.length() : e + 2;
            } else return;
        }
    }

    private void skipLine() {
        while (p < s.length() && s.charAt(p) != '\n') p++;
    }

    private String string() {
        if (p >= s.length() || s.charAt(p) != '"') throw new IllegalArgumentException("string expected");
        int start = ++p;
        while (p < s.length()) {
            char c = s.charAt(p);
            if (c == '\\') p += 2;
            else if (c == '"') return s.substring(start, p++);
            else p++;
        }
        throw new IllegalArgumentException("unclosed string");
    }

    /** Skips one value (nested objects / arrays, strings with brackets, comments inside) */
    private void value() {
        ws();
        if (p >= s.length()) throw new IllegalArgumentException("value expected");
        char c = s.charAt(p);
        if (c == '"') {
            string();
            return;
        }
        if (c == '{' || c == '[') {
            int depth = 0;
            while (p < s.length()) {
                char d = s.charAt(p);
                if (d == '"') {
                    string();
                    continue;
                }
                if (d == '/' || d == '#') {
                    int before = p;
                    ws();
                    if (p != before) continue;
                }
                if (d == '{' || d == '[') depth++;
                else if (d == '}' || d == ']') {
                    depth--;
                    if (depth == 0) {
                        p++;
                        return;
                    }
                }
                p++;
            }
            throw new IllegalArgumentException("unclosed value");
        }
        while (p < s.length() && ",}]\n\r\t /#".indexOf(s.charAt(p)) < 0) p++;
    }

    private String apply(String key, Object value) {
        ws();
        if (p >= s.length() || s.charAt(p) != '{') throw new IllegalArgumentException("object expected");
        int open = p++;
        int entries = 0;
        int keyStart = -1;
        int valueStart = -1;
        int valueEnd = -1;
        int prevEnd = -1;
        int foundPrevEnd = -1;
        while (true) {
            ws();
            if (p >= s.length()) throw new IllegalArgumentException("unclosed object");
            if (s.charAt(p) == '}') break;
            int ks = p;
            String k = string();
            ws();
            if (p >= s.length() || s.charAt(p) != ':') throw new IllegalArgumentException("':' expected");
            p++;
            ws();
            int vs = p;
            value();
            int ve = p;
            if (k.equals(key) && keyStart < 0) {
                keyStart = ks;
                valueStart = vs;
                valueEnd = ve;
                foundPrevEnd = prevEnd;
            }
            entries++;
            ws();
            if (p < s.length() && s.charAt(p) == ',') p++;
            prevEnd = ve;
        }
        String json = value == null ? null : Json.write(value).trim().replace("\n", "\n  ");
        if (keyStart >= 0) {
            if (json != null) return s.substring(0, valueStart) + json + s.substring(valueEnd);
            // remove "key": value and one comma next to it
            int a = keyStart;
            int b = valueEnd;
            int q = b;
            while (q < s.length() && (s.charAt(q) == ' ' || s.charAt(q) == '\t')) q++;
            if (q < s.length() && s.charAt(q) == ',') b = q + 1;
            else if (foundPrevEnd >= 0) {
                int c = s.lastIndexOf(',', a);
                if (c >= foundPrevEnd) a = c;
            }
            while (a > 0 && (s.charAt(a - 1) == ' ' || s.charAt(a - 1) == '\t')) a--;
            if (a > 0 && s.charAt(a - 1) == '\n' && b < s.length() && s.charAt(b) == '\n') a--;
            return s.substring(0, a) + s.substring(b);
        }
        if (json == null) return s;
        String ins = "\n  \"" + key.replace("\\", "\\\\").replace("\"", "\\\"") + "\": " + json + (entries > 0 ? "," : "");
        if (entries == 0) {
            // an empty object (maybe only comments): keep the comments after the new key
            int close = p;
            String inner = s.substring(open + 1, close);
            String tail = inner.trim().isEmpty() ? "\n" : inner;
            return s.substring(0, open + 1) + ins + tail + s.substring(close);
        }
        return s.substring(0, open + 1) + ins + s.substring(open + 1);
    }
}
