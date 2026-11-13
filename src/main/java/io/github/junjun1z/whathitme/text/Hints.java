package io.github.junjun1z.whathitme.text;

import io.github.junjun1z.whathitme.data.DamageRecord;

/** Picks the automatic tip (whathitme.hint.&lt;key&gt;) for a damage record from its type and properties. */
public final class Hints {

    private Hints() {}

    public static String autoKey(DamageRecord r) {
        switch (r.damageType) {
            case "fall":
                return "fall";
            case "flyIntoWall":
                return "elytra";
            case "drown":
                return "drown";
            case "lava":
            case "inFire":
            case "onFire":
            case "hotFloor":
                return "fire";
            case "outOfWorld":
                return "void";
            case "starve":
                return "starve";
            case "inWall":
            case "cramming":
                return "suffocation";
            case "cactus":
                return "cactus";
            case "wither":
                return "wither";
            case "magic":
            case "indirectMagic":
                return "magic";
            case "lightningBolt":
                return "lightning";
            case "anvil":
            case "fallingBlock":
                return "falling_block";
            case "dragonBreath":
                return "dragon_breath";
            case "thorns":
                return "thorns";
            default:
                break;
        }
        if (r.has(DamageRecord.ABSOLUTE)) return "absolute";
        if (r.has(DamageRecord.EXPLOSION)) return "explosion";
        if (r.has(DamageRecord.BYPASS_ARMOR)) return "bypasses_armor";
        if (r.has(DamageRecord.FIRE)) return "fire";
        if (r.has(DamageRecord.PROJECTILE)) return "projectile";
        if (r.has(DamageRecord.MAGIC)) return "magic";
        if (r.has(DamageRecord.INVISIBLE)) return "invisible";
        if (r.damageId.startsWith("mob:minecraft:player")) return "player";
        if (r.damageId.startsWith("mob:")) return "melee";
        return "generic";
    }
}
