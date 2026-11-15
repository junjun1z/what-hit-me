package io.github.junjun1z.whathitme.core.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Environment of a hit without an attacker (block / fluid at the feet and at the body centre, held items) or the piece
 * of a broken-armor line, stored in {@link DamageRecord#context} as "slot|translationKey|name|modId" parts joined by ';'.
 */
public final class Context {

    public static final String FEET = "feet";
    public static final String BODY = "body";
    public static final String MAIN = "main";
    public static final String OFF = "off";
    public static final String ARMOR = "armor";

    /** One part of a context */
    public static final class Part {
        public final String slot;
        public final String key;
        public final String name;
        public final String modId;

        Part(String slot, String key, String name, String modId) {
            this.slot = slot;
            this.key = key;
            this.name = name;
            this.modId = modId;
        }
    }

    private final StringBuilder sb = new StringBuilder();

    private static String clean(String s) {
        return s == null ? "" : s.replace('|', '/').replace(';', ',').replace('\n', ' ');
    }

    /** Adds a part; ignored when the name is empty */
    public Context add(String slot, String key, String name, String modId) {
        if (name == null || name.isEmpty()) return this;
        if (sb.length() > 0) sb.append(';');
        sb.append(clean(slot)).append('|').append(clean(key)).append('|').append(clean(name)).append('|').append(clean(modId));
        return this;
    }

    @Override
    public String toString() {
        return sb.toString();
    }

    public static List<Part> parse(String ctx) {
        List<Part> out = new ArrayList<>();
        if (ctx == null || ctx.isEmpty()) return out;
        for (String p : ctx.split(";")) {
            String[] f = p.split("\\|", -1);
            if (f.length < 4) continue;
            out.add(new Part(f[0], f[1], f[2], f[3]));
        }
        return out;
    }

    public static Part find(String ctx, String slot) {
        for (Part p : parse(ctx)) if (p.slot.equals(slot)) return p;
        return null;
    }
}
