package io.github.junjun1z.whathitme.core.data;

/**
 * What the version adapter read from an attacking / nearby entity. Plain data, filled once per recorded hit (never per
 * tick) and never kept: {@link DamageRecord} copies the strings.
 */
public final class EntityFacts {

    /** Display name as the recording side sees it */
    public String name = "";
    /** Translation key of the default name ("" for players, custom names and names a mod builds itself) */
    public String nameKey = "";
    /** Registry id, "minecraft:player" for players, "unknown:&lt;class&gt;" when unknown */
    public String id = "";
    public String uuid = "";
    /** Owner of a pet / turret / summon ("" = none) */
    public String owner = "";
    public boolean player;
    public boolean invisible;
    /** Distance to the victim in meters, -1 = unknown */
    public float distance = -1f;
    /** Held weapon ("" = none) */
    public String weaponName = "";
    public String weaponKey = "";
    /** "translationKey|level|English name" separated by ';' */
    public String weaponEnchants = "";

    /** Mod id of the entity: the namespace of its id, "minecraft" when there is none */
    public String modId() {
        int c = id.indexOf(':');
        return c > 0 ? id.substring(0, c) : "minecraft";
    }
}
