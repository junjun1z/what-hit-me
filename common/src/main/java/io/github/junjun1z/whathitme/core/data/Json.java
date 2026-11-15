package io.github.junjun1z.whathitme.core.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Small lenient JSON reader / writer for the pack files and the caches: objects become LinkedHashMap, arrays ArrayList,
 * numbers Double. The reader accepts // and block comments and trailing commas, so pack authors can comment out lines.
 */
public final class Json {

    private final String s;
    private int p;

    private Json(String s) {
        this.s = s;
    }

    public static Object parse(String text) {
        Json j = new Json(text == null ? "" : text);
        j.ws();
        Object v = j.value();
        j.ws();
        if (j.p < j.s.length()) throw j.err("trailing data");
        return v;
    }

    private IllegalArgumentException err(String what) {
        int line = 1;
        for (int i = 0; i < p && i < s.length(); i++) if (s.charAt(i) == '\n') line++;
        return new IllegalArgumentException(what + " at line " + line);
    }

    private void ws() {
        while (p < s.length()) {
            char c = s.charAt(p);
            if (Character.isWhitespace(c) || c == '﻿') {
                p++;
            } else if (c == '/' && p + 1 < s.length() && s.charAt(p + 1) == '/') {
                while (p < s.length() && s.charAt(p) != '\n') p++;
            } else if (c == '/' && p + 1 < s.length() && s.charAt(p + 1) == '*') {
                int end = s.indexOf("*/", p + 2);
                p = end < 0 ? s.length() : end + 2;
            } else if (c == '#') {
                while (p < s.length() && s.charAt(p) != '\n') p++;
            } else {
                return;
            }
        }
    }

    private Object value() {
        if (p >= s.length()) throw err("unexpected end");
        char c = s.charAt(p);
        if (c == '{') return object();
        if (c == '[') return array();
        if (c == '"') return string();
        if (s.startsWith("true", p)) {
            p += 4;
            return Boolean.TRUE;
        }
        if (s.startsWith("false", p)) {
            p += 5;
            return Boolean.FALSE;
        }
        if (s.startsWith("null", p)) {
            p += 4;
            return null;
        }
        int start = p;
        while (p < s.length() && "+-0123456789.eE".indexOf(s.charAt(p)) >= 0) p++;
        if (start == p) throw err("unexpected '" + c + "'");
        try {
            return Double.valueOf(s.substring(start, p));
        } catch (NumberFormatException e) {
            throw err("bad number");
        }
    }

    private Map<String, Object> object() {
        Map<String, Object> m = new LinkedHashMap<>();
        p++;
        while (true) {
            ws();
            if (p >= s.length()) throw err("unclosed object");
            if (s.charAt(p) == '}') {
                p++;
                return m;
            }
            String k = string();
            ws();
            if (p >= s.length() || s.charAt(p) != ':') throw err("expected ':'");
            p++;
            ws();
            m.put(k, value());
            ws();
            if (p < s.length() && s.charAt(p) == ',') p++;
            else if (p < s.length() && s.charAt(p) != '}') throw err("expected ',' or '}'");
        }
    }

    private List<Object> array() {
        List<Object> l = new ArrayList<>();
        p++;
        while (true) {
            ws();
            if (p >= s.length()) throw err("unclosed array");
            if (s.charAt(p) == ']') {
                p++;
                return l;
            }
            l.add(value());
            ws();
            if (p < s.length() && s.charAt(p) == ',') p++;
            else if (p < s.length() && s.charAt(p) != ']') throw err("expected ',' or ']'");
        }
    }

    private String string() {
        if (p >= s.length() || s.charAt(p) != '"') throw err("expected a string");
        p++;
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (p >= s.length()) throw err("unclosed string");
            char c = s.charAt(p++);
            if (c == '"') return sb.toString();
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            if (p >= s.length()) throw err("unclosed string");
            char e = s.charAt(p++);
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
                    if (p + 4 > s.length()) throw err("bad escape");
                    sb.append((char) Integer.parseInt(s.substring(p, p + 4), 16));
                    p += 4;
                    break;
                default:
                    sb.append(e);
            }
        }
    }

    // ---------------------------------------------------------------- writing

    public static String write(Object v) {
        StringBuilder sb = new StringBuilder();
        write(sb, v, 0);
        return sb.append('\n').toString();
    }

    private static void indent(StringBuilder sb, int n) {
        for (int i = 0; i < n; i++) sb.append("  ");
    }

    @SuppressWarnings("unchecked")
    private static void write(StringBuilder sb, Object v, int depth) {
        if (v == null) {
            sb.append("null");
        } else if (v instanceof Map) {
            Map<String, Object> m = (Map<String, Object>) v;
            if (m.isEmpty()) {
                sb.append("{}");
                return;
            }
            sb.append("{\n");
            int i = 0;
            for (Map.Entry<String, Object> e : m.entrySet()) {
                indent(sb, depth + 1);
                quote(sb, e.getKey());
                sb.append(": ");
                write(sb, e.getValue(), depth + 1);
                if (++i < m.size()) sb.append(',');
                sb.append('\n');
            }
            indent(sb, depth);
            sb.append('}');
        } else if (v instanceof List) {
            List<Object> l = (List<Object>) v;
            if (l.isEmpty()) {
                sb.append("[]");
                return;
            }
            boolean simple = true;
            for (Object o : l) simple &= !(o instanceof Map) && !(o instanceof List);
            if (simple && l.size() <= 8) {
                sb.append('[');
                for (int i = 0; i < l.size(); i++) {
                    if (i > 0) sb.append(", ");
                    write(sb, l.get(i), depth);
                }
                sb.append(']');
                return;
            }
            sb.append("[\n");
            for (int i = 0; i < l.size(); i++) {
                indent(sb, depth + 1);
                write(sb, l.get(i), depth + 1);
                if (i + 1 < l.size()) sb.append(',');
                sb.append('\n');
            }
            indent(sb, depth);
            sb.append(']');
        } else if (v instanceof Number) {
            double d = ((Number) v).doubleValue();
            if (d == Math.rint(d) && Math.abs(d) < 1e15) sb.append((long) d);
            else sb.append(d);
        } else if (v instanceof Boolean) {
            sb.append(v);
        } else {
            quote(sb, String.valueOf(v));
        }
    }

    private static void quote(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        sb.append('"');
    }

    // ---------------------------------------------------------------- typed access

    @SuppressWarnings("unchecked")
    public static Map<String, Object> obj(Object v) {
        return v instanceof Map ? (Map<String, Object>) v : new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    public static List<Object> list(Object v) {
        return v instanceof List ? (List<Object>) v : new ArrayList<>();
    }

    public static String str(Object v, String def) {
        return v == null ? def : v instanceof Double ? num(v) : String.valueOf(v);
    }

    private static String num(Object v) {
        double d = (Double) v;
        return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d);
    }

    public static double num(Object v, double def) {
        if (v instanceof Number) return ((Number) v).doubleValue();
        try {
            return v == null ? def : Double.parseDouble(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static boolean bool(Object v, boolean def) {
        if (v instanceof Boolean) return (Boolean) v;
        if (v == null) return def;
        return Boolean.parseBoolean(String.valueOf(v).trim());
    }
}
