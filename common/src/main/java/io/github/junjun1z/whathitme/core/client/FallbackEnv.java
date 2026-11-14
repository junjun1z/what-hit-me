package io.github.junjun1z.whathitme.core.client;

import io.github.junjun1z.whathitme.core.data.EntityFacts;

/**
 * What the client fallback reads from its own player and surroundings (server without the mod). The per-tick values
 * are a few field reads; the "nearest ..." scans are only called when health just dropped.
 */
public interface FallbackEnv {

    // ---- every tick
    float health();

    float absorption();

    float maxHealth();

    float fallDistance();

    boolean onGround();

    /** Totems of Undying in both hands */
    int totems();

    /** The hurt animation just started (hurtTime at its maximum) */
    boolean hurtNow();

    boolean hasRegeneration();

    boolean hasAbsorptionEffect();

    // ---- only when health dropped (in this order of priority)
    /** Below the bottom of the world (void damage) */
    boolean inVoid();

    boolean inLava();

    /** Burning without Fire Resistance */
    boolean burning();

    /** In water without air */
    boolean drowning();

    boolean inWall();

    /** Nearest projectile within 3 blocks: its facts (attacker = shooter or null), or null */
    Projectile nearestProjectile();

    /** Primed TNT or a flashing creeper within 8 blocks, or null */
    Explosive nearestExplosive();

    /** Nearest living entity within 6 blocks (hostile / swinging ones first), or null */
    Mob nearestMob();

    boolean hasWither();

    boolean hasPoison();

    boolean starving();

    final class Projectile {
        /** Name of the projectile entity */
        public String name = "";
        public boolean arrow;
        /** Shooter, null when unknown */
        public EntityFacts shooter;
    }

    final class Explosive {
        /** Creeper facts, null for TNT */
        public EntityFacts creeper;
    }

    final class Mob {
        public EntityFacts facts;
        public boolean creeper;
    }
}
