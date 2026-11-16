package io.github.junjun1z.whathitme.core.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    /** Names of the "hide" list, in the order of the bits above */
    public static final String[] HIDE_NAMES = {"damage_id", "attacker", "mod", "weapon", "death_message", "tip", "properties", "environment", "owner",
        "distance", "position"};

    /** Damage id sent instead of a hidden one */
    public static final String HIDDEN_ID = "whathitme:hidden";

    public final int bits;
    public final String name;
    public final String hint;

    public RuleMark(int bits, String name, String hint) {
        this.bits = bits;
        this.name = name == null ? "" : name;
        this.hint = hint == null ? "" : hint;
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
        if ((bits & ~(ENFORCE | NAME | HINT)) == 0) return r;
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
    }

    public static RuleMark read(WireIn in) {
        return new RuleMark(in.readInt(), in.readString(), in.readString());
    }
}
