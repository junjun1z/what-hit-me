package io.github.junjun1z.whathitme.mc;

import io.github.junjun1z.whathitme.core.client.FallbackEnv;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.MobEffects;

/** The own player for the client fallback on 1.10. */
public final class FallbackEnv110 implements FallbackEnv {

    private final Minecraft mc;
    private final EntityPlayerSP p;

    public FallbackEnv110(Minecraft mc) {
        this.mc = mc;
        this.p = mc.player;
    }

    @Override
    public float health() {
        return p.getHealth();
    }

    @Override
    public float absorption() {
        return p.getAbsorptionAmount();
    }

    @Override
    public float maxHealth() {
        return p.getMaxHealth();
    }

    @Override
    public float fallDistance() {
        return p.fallDistance;
    }

    @Override
    public boolean onGround() {
        return p.onGround;
    }

    @Override
    public int totems() {
        return 0; // no Totem of Undying before 1.11
    }

    @Override
    public boolean hurtNow() {
        return p.hurtTime > 0 && p.hurtTime >= p.maxHurtTime - 1;
    }

    @Override
    public boolean hasRegeneration() {
        return p.isPotionActive(MobEffects.REGENERATION);
    }

    @Override
    public boolean hasAbsorptionEffect() {
        return p.isPotionActive(MobEffects.ABSORPTION);
    }

    @Override
    public boolean inVoid() {
        return p.posY < -60;
    }

    @Override
    public boolean inLava() {
        return p.isInLava();
    }

    @Override
    public boolean burning() {
        return p.isBurning() && !p.isPotionActive(MobEffects.FIRE_RESISTANCE);
    }

    @Override
    public boolean drowning() {
        return p.isInsideOfMaterial(Material.WATER) && p.getAir() <= 0;
    }

    @Override
    public boolean inWall() {
        return p.isEntityInsideOpaqueBlock();
    }

    @Override
    public Projectile nearestProjectile() {
        Entity best = null;
        double bd = 9.0;
        for (Entity e : mc.world.getEntitiesWithinAABBExcludingEntity(p, p.getEntityBoundingBox().expandXyz(3.0))) {
            if (!(e instanceof IProjectile)) continue;
            double d = e.getDistanceSqToEntity(p);
            if (d < bd) {
                bd = d;
                best = e;
            }
        }
        if (best == null) return null;
        Projectile r = new Projectile();
        r.name = Facts.entityName(best);
        r.arrow = best instanceof EntityArrow;
        Entity shooter = r.arrow ? ((EntityArrow) best).shootingEntity : null;
        r.shooter = shooter == null ? null : Facts.entity(shooter, p, true);
        return r;
    }

    @Override
    public Explosive nearestExplosive() {
        for (Entity e : mc.world.getEntitiesWithinAABBExcludingEntity(p, p.getEntityBoundingBox().expandXyz(8.0))) {
            if (e instanceof EntityTNTPrimed) return new Explosive();
            if (e instanceof EntityCreeper && ((EntityCreeper) e).getCreeperFlashIntensity(1f) > 0.5f) {
                Explosive x = new Explosive();
                x.creeper = Facts.entity(e, p, true);
                return x;
            }
        }
        return null;
    }

    @Override
    public Mob nearestMob() {
        EntityLivingBase best = null;
        double bd = 36.0;
        for (EntityLivingBase e : mc.world.getEntitiesWithinAABB(EntityLivingBase.class, p.getEntityBoundingBox().expandXyz(6.0))) {
            if (e == p || !e.isEntityAlive()) continue;
            double d = e.getDistanceSqToEntity(p);
            // hostile mobs and swinging entities first
            if (e instanceof IMob || e.swingProgress > 0f || e instanceof EntityPlayer) d *= 0.25;
            if (d < bd) {
                bd = d;
                best = e;
            }
        }
        if (best == null) return null;
        Mob m = new Mob();
        m.facts = Facts.entity(best, p, true);
        m.creeper = best instanceof EntityCreeper;
        return m;
    }

    @Override
    public boolean hasWither() {
        return p.isPotionActive(MobEffects.WITHER);
    }

    @Override
    public boolean hasPoison() {
        return p.isPotionActive(MobEffects.POISON);
    }

    @Override
    public boolean starving() {
        return p.getFoodStats().getFoodLevel() <= 0;
    }
}
