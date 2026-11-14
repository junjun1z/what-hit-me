package io.github.junjun1z.whathitme.core.text;

/** Chat colors, the same 16 as every Minecraft version, plus their legacy section-sign codes. */
public enum Fmt {
    BLACK('0'),
    DARK_BLUE('1'),
    DARK_GREEN('2'),
    DARK_AQUA('3'),
    DARK_RED('4'),
    DARK_PURPLE('5'),
    GOLD('6'),
    GRAY('7'),
    DARK_GRAY('8'),
    BLUE('9'),
    GREEN('a'),
    AQUA('b'),
    RED('c'),
    LIGHT_PURPLE('d'),
    YELLOW('e'),
    WHITE('f');

    public static final String BOLD = "§l";
    public static final String UNDERLINE = "§n";
    public static final String RESET = "§r";

    public final char code;

    Fmt(char code) {
        this.code = code;
    }

    /** "§c" */
    @Override
    public String toString() {
        return "§" + code;
    }

    /** Removes section-sign formatting codes */
    public static String strip(String s) {
        if (s == null || s.indexOf('§') < 0) return s == null ? "" : s;
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '§' && i + 1 < s.length()) {
                i++;
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
