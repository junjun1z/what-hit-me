package io.github.junjun1z.whathitme.core.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import io.github.junjun1z.whathitme.core.config.WhmConfig;

/**
 * A composite match key of renameMap, hintMap, ignoredDamageIds and deathTriggers. Conditions in any order, separated by
 * spaces or '&amp;': a damage id ({@code minecraft:lava}, {@code mob:minecraft:zombie}), {@code @entity_id} or
 * {@code @mob:entity_id} (the attacker), {@code @item:modid:item_id} (attacker main hand), {@code @dim:dimension_id}.
 * Every condition may use * wildcards. All conditions must match; when several keys match, the one with the most
 * conditions wins.
 */
public final class MatchKey {

    /** What a key is matched against */
    public static final class Target {
        public final String damageId;
        public final String entityId;
        public final String itemId;
        public final String dimension;

        public Target(String damageId, String entityId, String itemId, String dimension) {
            this.damageId = damageId == null ? "" : damageId;
            this.entityId = entityId == null ? "" : entityId;
            this.itemId = itemId == null ? "" : itemId;
            this.dimension = dimension == null ? "" : dimension;
        }

        public static Target of(DamageRecord r) {
            String ent = r.attackerId;
            if (ent.isEmpty() && r.damageId.startsWith("mob:")) ent = r.damageId.substring(4);
            return new Target(r.damageId, ent, r.weaponId, r.dimension);
        }

        public static Target id(String damageId) {
            return new Target(damageId, damageId != null && damageId.startsWith("mob:") ? damageId.substring(4) : "", "", "");
        }
    }

    private static final int DAMAGE = 0;
    private static final int ENTITY = 1;
    private static final int ITEM = 2;
    private static final int DIM = 3;

    public final String text;
    private final int[] kinds;
    private final Pattern[] patterns;
    private final String[] exact;

    private MatchKey(String text, List<Integer> kinds, List<String> values) {
        this.text = text;
        int n = kinds.size();
        this.kinds = new int[n];
        this.patterns = new Pattern[n];
        this.exact = new String[n];
        for (int i = 0; i < n; i++) {
            this.kinds[i] = kinds.get(i);
            String v = values.get(i);
            if (v.indexOf('*') >= 0 || v.indexOf('?') >= 0) patterns[i] = WhmConfig.glob(v);
            else exact[i] = v.toLowerCase(Locale.ROOT);
        }
    }

    public static MatchKey parse(String key) {
        List<Integer> kinds = new ArrayList<>();
        List<String> values = new ArrayList<>();
        String t = key == null ? "" : key.trim();
        for (String part : t.split("[\\s&]+")) {
            if (part.isEmpty()) continue;
            String low = part.toLowerCase(Locale.ROOT);
            if (low.startsWith("@item:")) {
                kinds.add(ITEM);
                values.add(part.substring(6));
            } else if (low.startsWith("@dim:")) {
                kinds.add(DIM);
                values.add(part.substring(5));
            } else if (low.startsWith("@mob:")) {
                kinds.add(ENTITY);
                values.add(part.substring(5));
            } else if (low.startsWith("@entity:")) {
                kinds.add(ENTITY);
                values.add(part.substring(8));
            } else if (part.startsWith("@")) {
                kinds.add(ENTITY);
                values.add(part.substring(1));
            } else {
                kinds.add(DAMAGE);
                values.add(part);
            }
        }
        return new MatchKey(t, kinds, values);
    }

    /** Number of conditions (the specificity) */
    public int size() {
        return kinds.length;
    }

    private boolean test(int i, String v) {
        if (patterns[i] != null) return patterns[i].matcher(v).matches();
        if (exact[i].equals(v.toLowerCase(Locale.ROOT))) return true;
        // a dimension id without namespace ("the_nether") or a 1.12 name ("DIM-1") also matches
        if (kinds[i] == DIM) {
            String lv = v.toLowerCase(Locale.ROOT);
            int c = lv.indexOf(':');
            return c >= 0 && lv.substring(c + 1).equals(exact[i]);
        }
        return false;
    }

    public boolean matches(Target t) {
        if (kinds.length == 0) return false;
        for (int i = 0; i < kinds.length; i++) {
            String v;
            switch (kinds[i]) {
                case ENTITY:
                    v = t.entityId;
                    break;
                case ITEM:
                    v = t.itemId;
                    break;
                case DIM:
                    v = t.dimension;
                    break;
                default:
                    v = t.damageId;
            }
            if (v.isEmpty() || !test(i, v)) return false;
        }
        return true;
    }

    /** The value of the best (most specific, then first) matching key of a map, or null */
    public static String best(Map<String, String> map, List<MatchKey> parsed, Target t) {
        if (map == null || map.isEmpty()) return null;
        String direct = map.get(t.damageId);
        MatchKey bestKey = null;
        int bestSize = direct != null ? 1 : 0;
        for (MatchKey k : parsed) {
            if (k.size() > bestSize && k.matches(t)) {
                bestKey = k;
                bestSize = k.size();
            }
        }
        if (bestKey != null) return map.get(bestKey.text);
        if (direct != null) return direct;
        return null;
    }

    public static List<MatchKey> parseAll(Iterable<String> keys) {
        List<MatchKey> out = new ArrayList<>();
        if (keys == null) return out;
        for (String k : keys) {
            if (k == null || k.trim().isEmpty()) continue;
            out.add(parse(k));
        }
        return Collections.unmodifiableList(out);
    }

    public static boolean any(List<MatchKey> keys, Target t) {
        for (MatchKey k : keys) if (k.matches(t)) return true;
        return false;
    }
}
