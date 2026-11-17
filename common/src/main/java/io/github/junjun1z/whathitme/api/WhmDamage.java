package io.github.junjun1z.whathitme.api;

import java.util.List;

/** One recorded hit (read only). Amounts in health points (2 = one heart) unless the name says hearts. */
public interface WhmDamage {

    /** Normalized id, e.g. "minecraft:fall", "mob:minecraft:zombie" */
    String getDamageId();

    /** The damage type / msgId, e.g. "fall", "mob", "arrow" */
    String getDamageType();

    /** Display name in English as a rename or rule shows it, else the damage type name */
    String getName();

    /** Attacker name ("" = none) */
    String getAttackerName();

    /** Registry id of the attacker entity ("" = none) */
    String getAttackerId();

    /** Projectile / direct entity name when it differs from the attacker ("" = none) */
    String getDirectName();

    String getModId();

    String getModName();

    /** Name of the attacker's weapon ("" = none) */
    String getWeaponName();

    /** Registry id of the attacker's weapon ("" = none) */
    String getWeaponId();

    /** Owner of a tamed attacker / of the projectile shooter ("" = none) */
    String getOwnerName();

    /** Health + absorption lost */
    float getAmount();

    /** {@link #getAmount()} in hearts */
    float getHearts();

    /** Before armor, -1 = unknown */
    float getRawAmount();

    float getAbsorbed();

    float getHealthAfter();

    float getMaxHealth();

    /** Distance to the attacker in blocks, -1 = unknown */
    float getDistance();

    /** Merged hits */
    int getHits();

    String getDimension();

    /** Wall clock time in milliseconds */
    long getTime();

    boolean isKillingBlow();

    boolean isTotem();

    boolean isHeavy();

    boolean isReflected();

    boolean isProjectile();

    boolean isFire();

    boolean isExplosion();

    boolean isMagic();

    boolean isBypassArmor();

    /** Property names: bypasses_armor, absolute, magic, fire, explosion, projectile, shield, iframes, creative */
    List<String> getProperties();

    /** Fields a rule or a script hides for the player (names of the rules "hide" list) */
    List<String> getHidden();

    /** Badge texts added by scripts */
    List<String> getBadges();

    /** The value of a script variable for this record ("" = none); only filled in outputs */
    String getVariable(String name);
}
