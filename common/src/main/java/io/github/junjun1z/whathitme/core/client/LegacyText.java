package io.github.junjun1z.whathitme.core.client;

import java.util.ArrayList;
import java.util.List;

/** Trimming and word wrapping of strings with section-sign formatting codes, measured with a {@link Gfx}. */
public final class LegacyText {

    private LegacyText() {}

    /** Longest prefix that fits in width (never cuts a formatting code in two) */
    public static String trim(Gfx g, String s, int width) {
        if (g.width(s) <= width) return s;
        int lo = 0;
        int hi = s.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            int cut = safeCut(s, mid);
            if (g.width(s.substring(0, cut)) <= width) lo = mid;
            else hi = mid - 1;
        }
        return s.substring(0, safeCut(s, lo));
    }

    private static int safeCut(String s, int i) {
        if (i > 0 && i <= s.length() && s.charAt(i - 1) == '§') return i - 1;
        return i;
    }

    /** Formatting codes active at the end of s (last color and the styles after it) */
    public static String activeFormat(String s) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length() - 1; i++) {
            if (s.charAt(i) != '§') continue;
            char c = Character.toLowerCase(s.charAt(i + 1));
            if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || c == 'r') {
                out.setLength(0);
                if (c != 'r') out.append('§').append(c);
            } else if (c >= 'k' && c <= 'o') {
                out.append('§').append(c);
            }
            i++;
        }
        return out.toString();
    }

    /** Word wrap; each continued line starts with the formatting active at the break */
    public static List<String> wrap(Gfx g, String text, int width) {
        List<String> lines = new ArrayList<>();
        String rest = text;
        while (!rest.isEmpty()) {
            if (g.width(rest) <= width) {
                lines.add(rest);
                break;
            }
            String fit = trim(g, rest, width);
            int cut = fit.length();
            int space = fit.lastIndexOf(' ');
            if (space > 0 && cut < rest.length()) cut = space;
            if (cut <= 0) cut = Math.max(1, fit.length());
            String line = rest.substring(0, cut);
            lines.add(line);
            String next = rest.substring(cut);
            if (next.startsWith(" ")) next = next.substring(1);
            if (next.isEmpty()) break;
            rest = activeFormat(line) + next;
            if (lines.size() > 50) break;
        }
        if (lines.isEmpty()) lines.add("");
        return lines;
    }

    /** Word wrap to at most maxLines; the last line ends with "..." when cut */
    public static List<String> wrap(Gfx g, String text, int width, int maxLines) {
        List<String> l = wrap(g, text, width);
        if (l.size() > maxLines) {
            l = new ArrayList<>(l.subList(0, maxLines));
            String last = l.get(maxLines - 1);
            l.set(maxLines - 1, trim(g, last, width - g.width("...")) + "...");
        }
        return l;
    }
}
