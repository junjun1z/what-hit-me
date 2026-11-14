package io.github.junjun1z.whathitme.core.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.github.junjun1z.whathitme.core.text.RecapFormatter;

/** /whm search and /whm dump data. Runs on demand only. */
public final class SearchEngine {

    public static final String[] TAGS = {"#bypasses_armor", "#absolute", "#magic", "#fire", "#explosion", "#projectile", "#damage", "#mob", "#effect"};

    private SearchEngine() {}

    /** Every mob and effect of the registries plus the damage catalog */
    public static List<Entry> all(DamageCatalog catalog, Registries reg) {
        List<Entry> out = new ArrayList<>(catalog.entries());
        out.addAll(reg.mobs());
        out.addAll(reg.effects());
        return out;
    }

    /**
     * #tag (damage property or type), @modid, or text matched against ids and names. Several terms can be combined
     * (all must match).
     */
    public static List<Entry> search(String query, DamageCatalog catalog, Registries reg) {
        String[] terms = query.trim().toLowerCase(Locale.ROOT).split("\\s+");
        List<Entry> out = new ArrayList<>();
        for (Entry e : all(catalog, reg)) {
            boolean ok = true;
            for (String t : terms) {
                if (t.isEmpty()) continue;
                if (!matches(e, t, reg)) {
                    ok = false;
                    break;
                }
            }
            if (ok) out.add(e);
        }
        return out;
    }

    private static boolean matches(Entry e, String t, Registries reg) {
        if (t.startsWith("#")) {
            switch (t) {
                case "#bypasses_armor":
                case "#bypassesarmor":
                case "#unblockable":
                    return e.type == Entry.DAMAGE && (e.flags & DamageRecord.BYPASS_ARMOR) != 0;
                case "#absolute":
                case "#bypasses_magic":
                    return e.type == Entry.DAMAGE && (e.flags & DamageRecord.ABSOLUTE) != 0;
                case "#magic":
                    return e.type == Entry.DAMAGE && (e.flags & DamageRecord.MAGIC) != 0;
                case "#fire":
                    return e.type == Entry.DAMAGE && (e.flags & DamageRecord.FIRE) != 0;
                case "#explosion":
                    return e.type == Entry.DAMAGE && (e.flags & DamageRecord.EXPLOSION) != 0;
                case "#projectile":
                    return e.type == Entry.DAMAGE && (e.flags & DamageRecord.PROJECTILE) != 0;
                case "#damage":
                    return e.type == Entry.DAMAGE;
                case "#mob":
                    return e.type == Entry.MOB;
                case "#effect":
                case "#debuff":
                    return e.type == Entry.EFFECT && (!"#debuff".equals(t) || (e.flags & 1) != 0);
                default:
                    return false;
            }
        }
        if (t.startsWith("@")) {
            String mod = t.substring(1);
            return e.modId.toLowerCase(Locale.ROOT).equals(mod) || e.id.toLowerCase(Locale.ROOT).startsWith(mod + ":")
                || e.id.toLowerCase(Locale.ROOT).startsWith("mob:" + mod + ":");
        }
        if (e.id.toLowerCase(Locale.ROOT).contains(t)) return true;
        if (e.name.toLowerCase(Locale.ROOT).contains(t)) return true;
        if (e.type == Entry.DAMAGE && RecapFormatter.pretty(e.name).toLowerCase(Locale.ROOT).contains(t)) return true;
        String loc = e.nameKey.isEmpty() ? "" : reg.translate(e.nameKey);
        return loc != null && !loc.isEmpty() && loc.toLowerCase(Locale.ROOT).contains(t);
    }
}
