package io.github.junjun1z.whathitme.core;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Silent-fail guard: a bad DamageSource / Entity / ItemStack / text of another mod must never crash the game. Every
 * event hook, packet handler, scan and UI draw goes through here; an error is logged once per place (then counted).
 */
public final class Safe {

    private static final Map<String, Integer> SEEN = new HashMap<>();

    private Safe() {}

    public static void warn(String where, Throwable t) {
        int n;
        synchronized (SEEN) {
            Integer c = SEEN.get(where);
            n = c == null ? 1 : c + 1;
            SEEN.put(where, n);
        }
        if (n == 1) Platform.log.warn("What Hit Me? ignored an error in " + where + " (further errors there are not logged)", t);
    }

    public static void run(String where, Runnable r) {
        try {
            r.run();
        } catch (Throwable t) {
            warn(where, t);
        }
    }

    public static <T> T get(String where, Supplier<T> s, T fallback) {
        try {
            T v = s.get();
            return v == null ? fallback : v;
        } catch (Throwable t) {
            warn(where, t);
            return fallback;
        }
    }

    /** Errors counted so far (self-test) */
    public static int count(String where) {
        synchronized (SEEN) {
            Integer c = SEEN.get(where);
            return c == null ? 0 : c;
        }
    }
}
