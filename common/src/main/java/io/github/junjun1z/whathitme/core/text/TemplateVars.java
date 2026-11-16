package io.github.junjun1z.whathitme.core.text;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.config.WhmConfig.Flag;
import io.github.junjun1z.whathitme.core.data.DamageRecord;
import io.github.junjun1z.whathitme.core.data.Entry;
import io.github.junjun1z.whathitme.core.data.Gear;
import io.github.junjun1z.whathitme.core.data.Snapshot;

/**
 * The template variables of one record (and its snapshot). A variable whose display switch is off for the reader is
 * empty, so a template never shows what the server or the player disabled. Death triggers use {@link #unmasked()}.
 */
public final class TemplateVars implements Template.Vars {

    /** Every variable with its group, in the order the editor lists them */
    public static final Map<String, String> KNOWN = new LinkedHashMap<>();

    private static void add(String group, String... names) {
        for (String n : names) KNOWN.put(n, group);
    }

    static {
        add("damage", "damage", "damage_hp", "raw_damage", "mitigation", "absorbed", "health_after", "max_health", "max_hp_percent", "hits");
        add("type", "name", "damage_name", "damage_id", "damage_mod", "properties", "death_message");
        add("attacker", "attacker_name", "attacker_id", "attacker_mod", "attacker_distance", "attacker_owner", "direct", "weapon", "weapon_id", "weapon_enchants");
        add("context", "environment", "dimension", "x", "y", "z", "time_ago", "time");
        add("badges", "badges", "killing", "tip", "player");
        add("summary", "killing_blow", "total", "total_hits", "total_seconds", "recap_link", "toggle_notice", "alert_kind", "details_link");
        add("copy", "timeline", "equipment", "effects", "recap");
    }

    public static boolean isKnown(String canonicalName) {
        return KNOWN.containsKey(canonicalName);
    }

    private final RecapFormatter f;
    private final Msg m;
    private final DamageRecord r;
    private final Snapshot s;
    private final long ref;
    private String player = "";
    private double[] pos;
    private boolean masked = true;
    private int alertIndex = 1;
    private final Map<String, Txt> extra = new HashMap<>();

    /**
     * @param r the record (summary / alert / copy: the killing blow or trigger hit), may be null
     * @param s the snapshot, may be null
     * @param ref time "time_ago" is measured from
     */
    public TemplateVars(RecapFormatter f, DamageRecord r, Snapshot s, long ref) {
        this.f = f;
        this.m = f.m;
        this.r = r;
        this.s = s;
        this.ref = ref;
        String p = f.o.player;
        if (p != null) player = p;
    }

    public TemplateVars player(String name) {
        player = name == null ? "" : name;
        return this;
    }

    public TemplateVars pos(double[] p) {
        pos = p;
        return this;
    }

    /** Every variable filled, whatever the display switches say (death triggers, server side only) */
    public TemplateVars unmasked() {
        masked = false;
        return this;
    }

    public TemplateVars alertIndex(int i) {
        alertIndex = i;
        return this;
    }

    public TemplateVars put(String name, Txt value) {
        extra.put(name, value);
        return this;
    }

    private boolean on(Flag fl) {
        return !masked || f.o.on(fl);
    }

    private static Txt str(String s) {
        return s == null || s.isEmpty() ? null : Txt.lit(s);
    }

    public static String number(double v, int decimals, int def) {
        if (Double.isNaN(v) || Double.isInfinite(v) || Math.abs(v) >= 100000) return "∞";
        int d = decimals < 0 ? def : decimals;
        return String.format(Locale.ROOT, "%." + d + "f", v);
    }

    private static Txt num(double v, int dec, int def) {
        return Txt.lit(number(v, dec, def));
    }

    private double[] position() {
        if (pos != null && pos.length >= 3) return pos;
        Gear g = s == null ? null : s.gear;
        if (g != null && g.hasPosition()) return new double[] {g.x, g.y, g.z};
        return null;
    }

    private static Txt join(List<Txt> parts, String sep) {
        if (parts.isEmpty()) return null;
        Txt c = Txt.empty();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) c.append(sep);
            c.append(parts.get(i));
        }
        return c;
    }

    @Override
    public Txt get(String name, int dec) {
        if (extra.containsKey(name)) return extra.get(name);
        Txt v = snapshotVar(name, dec);
        if (v != null || isSnapshotVar(name)) return v;
        if (name.equals("player")) return str(player);
        if (name.equals("death_message")) return s == null || s.deathMessage.isEmpty() ? null : Txt.json(s.deathMessage);
        if (r == null) return null;
        return recordVar(name, dec);
    }

    private static boolean isSnapshotVar(String name) {
        String g = KNOWN.get(name);
        return "summary".equals(g) || "copy".equals(g);
    }

    private Txt recordVar(String name, int dec) {
        boolean armor = r.has(DamageRecord.ARMOR_BREAK);
        switch (name) {
            case "name":
                return f.displayName(r);
            case "damage":
                return on(Flag.DAMAGE_AMOUNT) && !armor ? num(r.taken() / 2f, dec, 1) : null;
            case "damage_hp":
                return on(Flag.DAMAGE_AMOUNT) && !armor ? num(r.taken(), dec, 1) : null;
            case "max_hp_percent":
                return on(Flag.DAMAGE_AMOUNT) && on(Flag.MAX_HP_PERCENT) && r.maxHealth > 0f && r.taken() > 0f && !armor ? num(r.maxHpPercent(), dec, 0) : null;
            case "raw_damage":
                return on(Flag.MITIGATION_DETAILS) && r.raw >= 0 ? num(r.raw / 2f, dec, 1) : null;
            case "mitigation": {
                if (!on(Flag.MITIGATION_DETAILS) || r.raw <= 0) return null;
                float pct = (r.raw - r.actual - r.absorbed) / r.raw * 100f;
                return num(Math.max(0f, Math.min(100f, pct)), dec, 0);
            }
            case "absorbed":
                return on(Flag.MITIGATION_DETAILS) && r.absorbed > 0 ? num(r.absorbed / 2f, dec, 1) : null;
            case "health_after":
                return on(Flag.MITIGATION_DETAILS) && r.maxHealth > 0 ? num(r.healthAfter / 2f, dec, 1) : null;
            case "max_health":
                return on(Flag.MITIGATION_DETAILS) && r.maxHealth > 0 ? num(r.maxHealth / 2f, dec, 1) : null;
            case "hits":
                return on(Flag.HIT_COUNT) && r.hits > 1 ? Txt.lit(String.valueOf(r.hits)) : null;
            case "damage_name": {
                String ren = f.o.rename(r);
                return ren != null ? f.custom(ren) : f.typeName(r.damageType);
            }
            case "damage_id":
                return on(Flag.INTERNAL_DAMAGE_ID) ? str(r.damageId) : null;
            case "damage_mod":
                return on(Flag.SOURCE_MOD_NAME) ? str(r.modName) : null;
            case "properties":
                return on(Flag.DAMAGE_PROPERTIES) && (r.flags & DamageRecord.PROPERTY_MASK) != 0 ? f.properties(r.flags) : null;
            case "attacker_name":
                return on(Flag.ATTACKER_NAME) && !r.attackerName.isEmpty() ? f.attacker(r) : null;
            case "attacker_id":
                return on(Flag.ATTACKER_NAME) ? str(r.attackerId) : null;
            case "attacker_mod": {
                if (!on(Flag.ATTACKER_NAME) || !on(Flag.SOURCE_MOD_NAME) || r.attackerId.indexOf(':') <= 0) return null;
                return str(Platform.modName(r.attackerId.substring(0, r.attackerId.indexOf(':'))));
            }
            case "attacker_distance":
                return on(Flag.ATTACKER_DISTANCE) && r.distance >= 0 ? num(r.distance, dec, 1) : null;
            case "attacker_owner":
                return on(Flag.ENTITY_OWNER) ? str(r.ownerName) : null;
            case "direct":
                return on(Flag.ATTACKER_NAME) && !r.directName.equals(r.attackerName) ? str(r.directName) : null;
            case "weapon":
                return on(Flag.ATTACKER_WEAPON) && !r.weaponName.isEmpty() ? m.name(r.weaponKey, r.weaponName) : null;
            case "weapon_id":
                return on(Flag.ATTACKER_WEAPON) ? str(r.weaponId) : null;
            case "weapon_enchants":
                return on(Flag.ATTACKER_WEAPON) && !r.weaponName.isEmpty() ? str(f.enchantments(r)) : null;
            case "environment":
                return masked ? join(f.environment(r), " | ") : join(new RecapFormatter(m, f.o.withAll()).environment(r), " | ");
            case "dimension": {
                if (!on(Flag.EQUIPMENT_IN_COPY)) return null;
                String d = !r.dimension.isEmpty() ? r.dimension : s != null && s.gear != null ? s.gear.dimension : "";
                return str(d);
            }
            case "x":
            case "y":
            case "z": {
                if (!on(Flag.EQUIPMENT_IN_COPY) || !on(Flag.COORDS_IN_COPY)) return null;
                double[] p = position();
                if (p == null) return null;
                double c = p[name.charAt(0) - 'x'];
                return dec <= 0 ? Txt.lit(String.valueOf((long) Math.floor(c))) : num(c, dec, 0);
            }
            case "time_ago":
                return on(Flag.TIMESTAMP) ? Txt.lit(RecapFormatter.age(ref - r.time)) : null;
            case "time":
                return on(Flag.TIMESTAMP) ? Txt.lit(new SimpleDateFormat("HH:mm:ss", Locale.ROOT).format(new Date(r.time))) : null;
            case "badges":
                return badges();
            case "killing":
                return r.has(DamageRecord.KILLING) ? Txt.lit("☠") : null;
            case "tip":
                return on(Flag.HINTS_AND_TIPS) ? f.tip(r) : null;
            default:
                return null;
        }
    }

    private Txt badges() {
        List<Txt> b = new java.util.ArrayList<>();
        if (r.has(DamageRecord.ESTIMATED)) b.add(m.tr(Fmt.GOLD, "whathitme.badge.estimated"));
        if (r.has(DamageRecord.TOTEM)) b.add(m.tr(Fmt.GOLD, "whathitme.badge.totem"));
        if (r.has(DamageRecord.HEAVY)) b.add(m.tr(Fmt.RED, "whathitme.badge.heavy"));
        if (on(Flag.REFLECTED_BADGE) && r.has(DamageRecord.REFLECTED)) b.add(m.tr(Fmt.YELLOW, "whathitme.badge.reflected"));
        if (on(Flag.ATTACKER_INVISIBILITY) && r.has(DamageRecord.INVISIBLE)) b.add(m.tr(Fmt.LIGHT_PURPLE, "whathitme.badge.invisible"));
        Txt v = f.vital(r);
        if (v != null) b.add(v);
        return join(b, " ");
    }

    private Txt snapshotVar(String name, int dec) {
        if (s == null) return null;
        DamageRecord k = s.key();
        switch (name) {
            case "killing_blow": {
                if (k == null) {
                    Txt dm = f.deathMessage(s);
                    return dm != null ? dm : m.tr("whathitme.unknown");
                }
                return Txt.empty().append(f.displayName(k)).hover(f.hover(k));
            }
            case "total":
                return num(f.totals(s.records, s.time)[0] / 2f, dec, 1);
            case "total_hits":
                return Txt.lit(String.valueOf((int) f.totals(s.records, s.time)[1]));
            case "total_seconds":
                return Txt.lit(String.valueOf(f.o.summarySeconds));
            case "recap_link":
                return f.recapLink();
            case "toggle_notice":
                return on(Flag.TOGGLE_CHAT_NOTICE) ? f.toggleNotice() : null;
            case "alert_kind":
                return f.kindName(s.kind);
            case "details_link":
                return f.detailsLink(alertIndex);
            case "timeline": {
                List<Txt> lines = new java.util.ArrayList<>();
                RecapFormatter plain = new RecapFormatter(m, f.o.without(Flag.ACTION_BUTTONS));
                for (DamageRecord x : f.visible(s.records, 40)) lines.add(plain.line(x, s.time));
                return join(lines, "\n");
            }
            case "equipment":
                return on(Flag.EQUIPMENT_IN_COPY) ? str(f.equipmentBlock(s, m.lang()).trim()) : null;
            case "effects": {
                if (!on(Flag.EQUIPMENT_IN_COPY) || s.gear == null || s.gear.effects.isEmpty()) return null;
                StringBuilder sb = new StringBuilder();
                for (Entry e : s.gear.effects) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(Fmt.strip(f.entryName(e).toPlain(m.lang())));
                    if (e.amplifier > 0) sb.append(' ').append(e.amplifier + 1);
                    sb.append(" (").append(RecapFormatter.duration(e.duration)).append(')');
                }
                return str(sb.toString());
            }
            case "recap":
                return str(RecapFormatter.plain(f.builtinRecap(s, ref), m.lang()).trim());
            default:
                return null;
        }
    }
}
