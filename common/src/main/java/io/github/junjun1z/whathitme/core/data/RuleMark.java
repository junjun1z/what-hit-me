package io.github.junjun1z.whathitme.core.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.github.junjun1z.whathitme.core.net.WireIn;
import io.github.junjun1z.whathitme.core.net.WireOut;

/**
 * The result of a progression rule for one record, decided by the server for one player and sent with the record
 * (protocol 3). The hidden fields are already removed from the record; the mark tells the client what to show instead.
 * Immutable.
 */
public final class RuleMark {

    /** The rule overrides the player's personal renames, tips and templates */
    public static final int ENFORCE = 1;
    /** {@link #name} is set */
    public static final int NAME = 1 << 1;
    /** {@link #hint} is set ("" = no tip) */
    public static final int HINT = 1 << 2;
    /** Script variables / badges / summary lines follow on the wire */
    public static final int EXTRA = 1 << 3;

    // hidden fields
    public static final int H_DAMAGE_ID = 1 << 8;
    public static final int H_ATTACKER = 1 << 9;
    public static final int H_MOD = 1 << 10;
    public static final int H_WEAPON = 1 << 11;
    public static final int H_DEATH_MESSAGE = 1 << 12;
    public static final int H_TIP = 1 << 13;
    public static final int H_PROPERTIES = 1 << 14;
    public static final int H_ENVIRONMENT = 1 << 15;
    public static final int H_OWNER = 1 << 16;
    public static final int H_DISTANCE = 1 << 17;
    public static final int H_POSITION = 1 << 18;

    /** Every hide bit */
    public static final int HIDE_MASK = ((H_POSITION << 1) - 1) & ~(H_DAMAGE_ID - 1);

    /** Names of the "hide" list, in the order of the bits above */
    public static final String[] HIDE_NAMES = {"damage_id", "attacker", "mod", "weapon", "death_message", "tip", "properties", "environment", "owner",
        "distance", "position"};

    /** Damage id sent instead of a hidden one */
    public static final String HIDDEN_ID = "whathitme:hidden";

    public final int bits;
    public final String name;
    public final String hint;
    /** Script variable values by lower-case name (server computed for this reader) */
    public final Map<String, String> vars;
    /** Badge texts added by scripts */
    public final List<String> badges;
    /** Lines scripts add to the respawn summary */
    public final List<String> lines;

    public RuleMark(int bits, String name, String hint) {
        this(bits, name, hint, null, null, null);
    }

    public RuleMark(int bits, String name, String hint, Map<String, String> vars, List<String> badges, List<String> lines) {
        this.vars = vars == null || vars.isEmpty() ? Collections.<String, String>emptyMap() : Collections.unmodifiableMap(new LinkedHashMap<>(vars));
        this.badges = badges == null || badges.isEmpty() ? Collections.<String>emptyList() : Collections.unmodifiableList(new ArrayList<>(badges));
        this.lines = lines == null || lines.isEmpty() ? Collections.<String>emptyList() : Collections.unmodifiableList(new ArrayList<>(lines));
        int b = bits & ~EXTRA;
        if (!this.vars.isEmpty() || !this.badges.isEmpty() || !this.lines.isEmpty()) b |= EXTRA;
        this.bits = b;
        this.name = name == null ? "" : name;
        this.hint = hint == null ? "" : hint;
    }

    /** A rule (or a script listener) changed name, tip, enforcement or hidden fields (not only extras) */
    public boolean isRule() {
        return (bits & ~EXTRA) != 0;
    }

    /** The same mark with these variable values (null / empty = none) */
    public RuleMark withVars(Map<String, String> v) {
        return new RuleMark(bits, name, hint, v, badges, lines);
    }

    /** Only the variables, badges and lines of this mark (null when it has none) */
    public RuleMark extrasOnly() {
        return has(EXTRA) ? new RuleMark(0, "", "", vars, badges, lines) : null;
    }

    /**
     * A rule result over the mark scripts gave the record: the rule's name and tip win, hidden fields add up, the
     * script's badges and summary lines stay.
     */
    public static RuleMark combine(RuleMark rule, RuleMark base) {
        if (base == null) return rule;
        if (rule == null) return base;
        int b = rule.bits | (base.bits & (HIDE_MASK | ENFORCE));
        String n = rule.has(NAME) ? rule.name : base.has(NAME) ? base.name : "";
        if (base.has(NAME)) b |= NAME;
        String h = rule.has(HINT) ? rule.hint : base.has(HINT) ? base.hint : "";
        if (base.has(HINT)) b |= HINT;
        Map<String, String> v = new LinkedHashMap<>(base.vars);
        v.putAll(rule.vars);
        List<String> bd = new ArrayList<>(base.badges);
        bd.addAll(rule.badges);
        List<String> ln = new ArrayList<>(base.lines);
        ln.addAll(rule.lines);
        return new RuleMark(b, n, h, v, bd, ln);
    }

    public boolean has(int bit) {
        return (bits & bit) != 0;
    }

    public boolean hides(int bit) {
        return (bits & bit) != 0;
    }

    public boolean enforced() {
        return has(ENFORCE);
    }

    /** Bit of a hide name ("hint" is an alias of "tip"), 0 = unknown */
    public static int hideBit(String name) {
        String n = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
        if (n.equals("hint")) n = "tip";
        if (n.equals("coords") || n.equals("dimension")) n = "position";
        for (int i = 0; i < HIDE_NAMES.length; i++) if (HIDE_NAMES[i].equals(n)) return H_DAMAGE_ID << i;
        return 0;
    }

    /** Names of the hidden fields */
    public List<String> hiddenNames() {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < HIDE_NAMES.length; i++) if (hides(H_DAMAGE_ID << i)) out.add(HIDE_NAMES[i]);
        return out;
    }

    /** The record with the hidden fields removed (the mark itself is attached by the caller) */
    public DamageRecord strip(DamageRecord r) {
        if ((bits & HIDE_MASK) == 0) return r;
        boolean id = hides(H_DAMAGE_ID);
        boolean atk = hides(H_ATTACKER);
        boolean mod = hides(H_MOD);
        boolean wpn = hides(H_WEAPON);
        boolean props = hides(H_PROPERTIES);
        boolean env = hides(H_ENVIRONMENT);
        boolean own = atk || hides(H_OWNER);
        boolean dist = atk || hides(H_DISTANCE);
        boolean pos = hides(H_POSITION);
        int flags = props ? r.flags & ~DamageRecord.PROPERTY_MASK : r.flags;
        if (atk) flags &= ~DamageRecord.INVISIBLE;
        String type = r.damageType;
        // melee ids name the attacker
        String damageId = id || (atk && r.damageId.startsWith("mob:")) ? HIDDEN_ID : r.damageId;
        if (atk && ("mob".equals(type) || "player".equals(type))) type = "mob";
        return new DamageRecord(r.time, damageId, type, atk ? "" : r.attackerName, atk ? "" : r.attackerKey, atk ? "" : r.attackerId,
            atk ? "" : r.directName, mod ? "" : r.modId, mod ? "" : r.modName, r.raw, r.actual, r.absorbed, flags, dist ? -1f : r.distance,
            wpn ? "" : r.weaponName, wpn ? "" : r.weaponKey, wpn ? "" : r.weaponEnchants, own ? "" : r.ownerName, r.hits, r.healthAfter, r.maxHealth,
            r.sourceTag, wpn ? "" : r.weaponId, pos ? "" : r.dimension, env && !r.has(DamageRecord.ARMOR_BREAK) ? "" : r.context);
    }

    public void write(WireOut o) {
        o.writeInt(bits);
        o.writeString(name);
        o.writeString(hint);
        if (!has(EXTRA)) return;
        o.writeVarInt(vars.size());
        for (Map.Entry<String, String> e : vars.entrySet()) {
            o.writeString(e.getKey());
            o.writeString(e.getValue());
        }
        o.writeVarInt(badges.size());
        for (String b : badges) o.writeString(b);
        o.writeVarInt(lines.size());
        for (String l : lines) o.writeString(l);
    }

    public static RuleMark read(WireIn in) {
        int bits = in.readInt();
        String name = in.readString();
        String hint = in.readString();
        if ((bits & EXTRA) == 0) return new RuleMark(bits, name, hint);
        Map<String, String> v = new LinkedHashMap<>();
        int n = in.readVarInt();
        for (int i = 0; i < n; i++) v.put(in.readString(), in.readString());
        List<String> b = new ArrayList<>();
        n = in.readVarInt();
        for (int i = 0; i < n; i++) b.add(in.readString());
        List<String> l = new ArrayList<>();
        n = in.readVarInt();
        for (int i = 0; i < n; i++) l.add(in.readString());
        return new RuleMark(bits, name, hint, v, b, l);
    }
}
