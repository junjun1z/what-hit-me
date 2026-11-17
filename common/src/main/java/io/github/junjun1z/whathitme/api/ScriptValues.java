package io.github.junjun1z.whathitme.api;

import java.util.List;

import io.github.junjun1z.whathitme.core.script.ScriptRegistry;

/** Helpers for bindings: script engine values (their own map, list and string types) as plain Java data. */
public final class ScriptValues {

    private ScriptValues() {}

    /** Maps, lists (also arrays), strings, numbers (as Double), booleans; other objects as their text */
    public static Object plain(Object value) {
        return ScriptRegistry.plain(value);
    }

    /** Template lines: a list / array of lines or one text with line breaks */
    public static List<String> lines(Object value) {
        return ScriptRegistry.lines(value);
    }

    /** A script return value as text ("" for null / undefined) */
    public static String text(Object value) {
        Object p = plain(value);
        if (p == null) return "";
        if (p instanceof Double) {
            double d = (Double) p;
            if (d == Math.rint(d) && !Double.isInfinite(d) && Math.abs(d) < 1e15) return String.valueOf((long) d);
        }
        String s = p.toString();
        return "undefined".equals(s) ? "" : s;
    }
}
