package io.github.junjun1z.whathitme.core.data;

/**
 * What the version adapter read from a damage source. The core turns it into a {@link DamageRecord} ({@link #build}).
 *
 * <ul>
 * <li>{@code damageType}: 1.12 - 1.19.3 DamageSource#msgId ("fall", "outOfWorld", "mob"); 1.19.4+ the DamageType msgId
 * (the same names). Used for the automatic tip and the "whathitme.damage.&lt;type&gt;" name.</li>
 * <li>{@code typeId}: the damage id used when the hit is not melee: 1.12 - 1.19.3 "&lt;modid&gt;:&lt;msgId&gt;"
 * (see {@link TypeModResolver}), 1.19.4+ the DamageType registry key ("minecraft:out_of_world").</li>
 * <li>{@code melee}: a melee attack by an entity (msgId "mob" / "player", or the mob_attack / player_attack types):
 * the id becomes "mob:&lt;attacker id&gt;".</li>
 * </ul>
 */
public final class HitFacts {

    public String damageType = "";
    public String typeId = "unknown:unknown";
    public boolean melee;
    /** {@link DamageRecord} property flags of the source (BYPASS_ARMOR, FIRE, ...) */
    public int flags;
    /** Responsible entity (true source, or the shooter of a projectile); null = none or the victim itself */
    public EntityFacts attacker;
    /** Name of the projectile / direct entity when it is not the attacker ("" = none) */
    public String directName = "";

    /** The normalized damage id: mob:&lt;entity&gt; for melee, otherwise the type id */
    public String damageId() {
        if (attacker != null && melee) return "mob:" + attacker.id;
        return typeId;
    }

    public DamageRecord build(long time, float raw, float actual, float absorbed, int extraFlags, float healthAfter, float maxHealth) {
        String id = damageId();
        EntityFacts atk = attacker;
        String modId;
        if (atk != null && !atk.player && !"minecraft".equals(atk.modId())) modId = atk.modId();
        else if (id.startsWith("mob:") && atk != null) modId = atk.modId();
        else modId = id.indexOf(':') > 0 ? id.substring(0, id.indexOf(':')) : "unknown";
        int f = flags | extraFlags;
        String atkName = "";
        String atkKey = "";
        String atkId = "";
        String owner = "";
        String tag = "";
        String wName = "";
        String wKey = "";
        String ench = "";
        float dist = -1f;
        if (atk != null) {
            atkName = atk.name;
            atkKey = atk.nameKey;
            atkId = atk.id;
            owner = atk.owner;
            tag = atk.uuid;
            dist = atk.distance;
            if (atk.invisible) f |= DamageRecord.INVISIBLE;
            wName = atk.weaponName;
            wKey = atk.weaponKey;
            ench = atk.weaponEnchants;
        }
        return new DamageRecord(time, id, damageType, atkName, atkKey, atkId, directName, modId, io.github.junjun1z.whathitme.core.Platform.modName(modId), raw,
            actual, absorbed, f, dist, wName, wKey, ench, owner, 1, healthAfter, maxHealth, tag);
    }
}
