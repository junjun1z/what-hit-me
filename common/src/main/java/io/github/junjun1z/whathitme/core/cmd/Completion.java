package io.github.junjun1z.whathitme.core.cmd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.data.SearchEngine;

/** Argument splitting and tab completion helpers shared by every command implementation (CommandBase or Brigadier). */
public final class Completion {

    private Completion() {}

    /** Candidates starting with the last argument (case-insensitive), like 1.12's getListOfStringsMatchingLastWord */
    public static List<String> matching(String[] args, Collection<String> candidates) {
        String last = args.length == 0 ? "" : args[args.length - 1];
        List<String> out = new ArrayList<>();
        for (String c : candidates) {
            if (c != null && c.regionMatches(true, 0, last, 0, last.length())) out.add(c);
        }
        return out;
    }

    public static List<String> matching(String[] args, String... candidates) {
        return matching(args, Arrays.asList(candidates));
    }

    /**
     * Splits the text after "/whm " into arguments. A trailing space starts a new, empty argument (that is the one
     * being completed). Multiple spaces count as one.
     */
    public static String[] split(String input) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == ' ') {
                if (cur.length() > 0) {
                    out.add(cur.toString());
                    cur.setLength(0);
                }
            } else {
                cur.append(c);
            }
        }
        if (cur.length() > 0 || input.endsWith(" ") || input.isEmpty()) out.add(cur.toString());
        return out.toArray(new String[0]);
    }

    /** Arguments of a command to run (no empty trailing argument) */
    public static String[] args(String input) {
        String t = input == null ? "" : input.trim();
        return t.isEmpty() ? new String[0] : t.split(" +");
    }

    /** Offset of the last argument in the input (where suggestions are placed) */
    public static int lastArgStart(String input) {
        int i = input.lastIndexOf(' ');
        return i < 0 ? 0 : i + 1;
    }

    /** Arguments joined from index {@code from} (like CommandBase.buildString) */
    public static String join(String[] args, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < args.length; i++) {
            if (i > from) sb.append(' ');
            sb.append(args[i]);
        }
        return sb.toString();
    }

    /** #tags, @modids and damage ids for /whm search */
    public static List<String> searchSuggestions(List<String> ids) {
        List<String> out = new ArrayList<>(Arrays.asList(SearchEngine.TAGS));
        try {
            for (String m : Platform.mods.modIds()) out.add("@" + m.toLowerCase(java.util.Locale.ROOT));
        } catch (Throwable ignored) {
            // no mod list
        }
        out.addAll(ids);
        return out;
    }

    /** /whm test &lt;damage_id&gt; [entity_id] [item_id] [dim_id]: entity ids, "-" and dimensions */
    public static List<String> testArgs(String[] args, io.github.junjun1z.whathitme.core.data.Registries reg) {
        List<String> out = new ArrayList<>();
        out.add("-");
        if (args.length == 3) {
            for (io.github.junjun1z.whathitme.core.data.Entry e : reg.mobs()) out.add(e.id);
        } else if (args.length == 4) {
            out.add("minecraft:diamond_sword");
            out.add("minecraft:bow");
        } else if (args.length == 5) {
            out.add("minecraft:overworld");
            out.add("minecraft:the_nether");
            out.add("minecraft:the_end");
        }
        return matching(args, out);
    }

    public static int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }
}
