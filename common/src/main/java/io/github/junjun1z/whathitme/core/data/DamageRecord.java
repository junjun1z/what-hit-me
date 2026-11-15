package io.github.junjun1z.whathitme.core.data;

import io.github.junjun1z.whathitme.core.net.WireIn;
import io.github.junjun1z.whathitme.core.net.WireOut;

/**
 * One entry of the damage history. Immutable and made only of primitives and Strings: it never keeps a reference to an
 * Entity, a World or a DamageSource, so a stored history cannot leak memory.
 */
public final class DamageRecord {

    // property flags (DamageSource / situation)
    public static final int BYPASS_ARMOR = 1;
    public static final int ABSOLUTE = 1 << 1;
    public static final int MAGIC = 1 << 2;
    public static final int FIRE = 1 << 3;
    public static final int EXPLOSION = 1 << 4;
    public static final int PROJECTILE = 1 << 5;
    public static final int THROUGH_SHIELD = 1 << 6;
    public static final int IGNORES_IFRAMES = 1 << 7;
    public static final int BYPASS_CREATIVE = 1 << 8;
    /** All flags above (the "Properties" list) */
    public static final int PROPERTY_MASK = (1 << 9) - 1;
    public static final int INVISIBLE = 1 << 9;
    // markers
    public static final int KILLING = 1 << 16;
    public static final int TOTEM = 1 << 17;
    public static final int HEAVY = 1 << 18;
    public static final int ESTIMATED = 1 << 19;
    /** Died with health left (a localized / vital hit of a health-overhaul mod) */
    public static final int VITAL = 1 << 20;
    /** Thorns, or damage from an entity the player hit a moment before */
    public static final int REFLECTED = 1 << 21;
    /** Not damage: a timeline line "armor piece broke" (the piece is in {@link #context}) */
    public static final int ARMOR_BREAK = 1 << 22;

    /** Damage id of armor break lines */
    public static final String ARMOR_BREAK_ID = "whathitme:armor_broken";

    /** Wall clock time of the (last merged) hit, in milliseconds */
    public final long time;
    /** Normalized id, e.g. minecraft:fall, mob:minecraft:zombie */
    public final String damageId;
    /** DamageSource#getDamageType() */
    public final String damageType;
    /** Attacker display name ("" = none) as known on the recording side */
    public final String attackerName;
    /** Translation key of the attacker name ("" = use attackerName) */
    public final String attackerKey;
    /** Registry name of the attacker entity ("" = none) */
    public final String attackerId;
    /** Name of the projectile / direct entity when it differs from the attacker ("" = none) */
    public final String directName;
    public final String modId;
    public final String modName;
    /** Damage before armor / potions / absorption, -1 = unknown */
    public final float raw;
    /** Health actually lost */
    public final float actual;
    /** Absorption (yellow hearts) used up */
    public final float absorbed;
    public final int flags;
    /** Distance to the attacker in meters, -1 = unknown */
    public final float distance;
    public final String weaponName;
    public final String weaponKey;
    /** "translationKey|level|English name" separated by ';' */
    public final String weaponEnchants;
    public final String ownerName;
    public final int hits;
    public final float healthAfter;
    public final float maxHealth;
    /** Identity of the source used by smart merge (attacker UUID or ""); never sent over the network */
    public final String sourceTag;
    /** Registry id of the attacker's main hand item ("" = none) */
    public final String weaponId;
    /** Dimension id of the victim ("" = unknown) */
    public final String dimension;
    /** Environment context / broken armor piece, see {@link Context} ("" = none) */
    public final String context;

    public DamageRecord(long time, String damageId, String damageType, String attackerName, String attackerKey, String attackerId,
                        String directName, String modId, String modName, float raw, float actual, float absorbed, int flags, float distance,
                        String weaponName, String weaponKey, String weaponEnchants, String ownerName, int hits, float healthAfter,
                        float maxHealth, String sourceTag) {
        this(time, damageId, damageType, attackerName, attackerKey, attackerId, directName, modId, modName, raw, actual, absorbed, flags, distance,
            weaponName, weaponKey, weaponEnchants, ownerName, hits, healthAfter, maxHealth, sourceTag, "", "", "");
    }

    public DamageRecord(long time, String damageId, String damageType, String attackerName, String attackerKey, String attackerId,
                        String directName, String modId, String modName, float raw, float actual, float absorbed, int flags, float distance,
                        String weaponName, String weaponKey, String weaponEnchants, String ownerName, int hits, float healthAfter,
                        float maxHealth, String sourceTag, String weaponId, String dimension, String context) {
        this.time = time;
        this.damageId = s(damageId);
        this.damageType = s(damageType);
        this.attackerName = s(attackerName);
        this.attackerKey = s(attackerKey);
        this.attackerId = s(attackerId);
        this.directName = s(directName);
        this.modId = s(modId);
        this.modName = s(modName);
        this.raw = raw;
        this.actual = actual;
        this.absorbed = absorbed;
        this.flags = flags;
        this.distance = distance;
        this.weaponName = s(weaponName);
        this.weaponKey = s(weaponKey);
        this.weaponEnchants = s(weaponEnchants);
        this.ownerName = s(ownerName);
        this.hits = hits;
        this.healthAfter = healthAfter;
        this.maxHealth = maxHealth;
        this.sourceTag = s(sourceTag);
        this.weaponId = s(weaponId);
        this.dimension = s(dimension);
        this.context = s(context);
    }

    private static String s(String v) {
        return v == null ? "" : v;
    }

    /** Health + absorption lost */
    public float taken() {
        return actual + absorbed;
    }

    public boolean has(int flag) {
        return (flags & flag) != 0;
    }

    public boolean sameSource(DamageRecord o) {
        return damageId.equals(o.damageId) && sourceTag.equals(o.sourceTag) && attackerName.equals(o.attackerName);
    }

    /** Smart merge: this record followed by {@code next} from the same source (the first hit's environment is kept) */
    public DamageRecord merge(DamageRecord next) {
        float r = raw < 0 || next.raw < 0 ? -1 : raw + next.raw;
        return new DamageRecord(next.time, damageId, damageType, next.attackerName, next.attackerKey, attackerId, next.directName, modId,
            modName, r, actual + next.actual, absorbed + next.absorbed, flags | next.flags, next.distance, next.weaponName, next.weaponKey,
            next.weaponEnchants, next.ownerName, hits + next.hits, next.healthAfter, next.maxHealth, sourceTag, next.weaponId,
            next.dimension.isEmpty() ? dimension : next.dimension, context.isEmpty() ? next.context : context);
    }

    public DamageRecord withFlags(int add) {
        return copy(time, flags | add, healthAfter, context);
    }

    public DamageRecord withTime(long t) {
        return copy(t, flags, healthAfter, context);
    }

    public DamageRecord withHealthAfter(float h) {
        return copy(time, flags, h, context);
    }

    public DamageRecord withContext(String c) {
        return copy(time, flags, healthAfter, c);
    }

    /** A copy without the parts a server switched off (sent to clients) */
    public DamageRecord stripped(boolean weapon, boolean distanceOn, boolean owner, boolean env) {
        if (weapon && distanceOn && owner && (env || context.isEmpty() || has(ARMOR_BREAK))) return this;
        return new DamageRecord(time, damageId, damageType, attackerName, attackerKey, attackerId, directName, modId, modName, raw, actual,
            absorbed, flags, distanceOn ? distance : -1f, weapon ? weaponName : "", weapon ? weaponKey : "", weapon ? weaponEnchants : "",
            owner ? ownerName : "", hits, healthAfter, maxHealth, sourceTag, weapon ? weaponId : "", dimension,
            env || has(ARMOR_BREAK) ? context : "");
    }

    private DamageRecord copy(long t, int f, float hAfter, String ctx) {
        return new DamageRecord(t, damageId, damageType, attackerName, attackerKey, attackerId, directName, modId, modName, raw, actual,
            absorbed, f, distance, weaponName, weaponKey, weaponEnchants, ownerName, hits, hAfter, maxHealth, sourceTag, weaponId, dimension, ctx);
    }

    /** Health + absorption lost as a percentage of max health, -1 = unknown */
    public float maxHpPercent() {
        return maxHealth > 0f ? taken() / maxHealth * 100f : -1f;
    }

    /** Writes the record with its age (clocks of client and server differ) */
    public void write(WireOut buf, long now) {
        buf.writeLong(Math.max(0, now - time));
        buf.writeString(damageId);
        buf.writeString(damageType);
        buf.writeString(attackerName);
        buf.writeString(attackerKey);
        buf.writeString(attackerId);
        buf.writeString(directName);
        buf.writeString(modId);
        buf.writeString(modName);
        buf.writeFloat(raw);
        buf.writeFloat(actual);
        buf.writeFloat(absorbed);
        buf.writeInt(flags);
        buf.writeFloat(distance);
        buf.writeString(weaponName);
        buf.writeString(weaponKey);
        buf.writeString(weaponEnchants);
        buf.writeString(ownerName);
        buf.writeInt(hits);
        buf.writeFloat(healthAfter);
        buf.writeFloat(maxHealth);
    }

    /** Protocol 2 part, written after {@link #write} */
    public void writeExtra(WireOut buf) {
        buf.writeString(weaponId);
        buf.writeString(dimension);
        buf.writeString(context);
    }

    public DamageRecord readExtra(WireIn buf) {
        String w = buf.readString();
        String d = buf.readString();
        String c = buf.readString();
        return new DamageRecord(time, damageId, damageType, attackerName, attackerKey, attackerId, directName, modId, modName, raw, actual,
            absorbed, flags, distance, weaponName, weaponKey, weaponEnchants, ownerName, hits, healthAfter, maxHealth, sourceTag, w, d, c);
    }

    public static DamageRecord read(WireIn buf, long now) {
        long age = buf.readLong();
        return new DamageRecord(now - age, buf.readString(), buf.readString(),
            buf.readString(), buf.readString(), buf.readString(),
            buf.readString(), buf.readString(), buf.readString(), buf.readFloat(),
            buf.readFloat(), buf.readFloat(), buf.readInt(), buf.readFloat(), buf.readString(),
            buf.readString(), buf.readString(), buf.readString(), buf.readInt(),
            buf.readFloat(), buf.readFloat(), "");
    }
}
