package io.github.junjun1z.whathitme.mc;

import io.github.junjun1z.whathitme.core.client.FallbackEnv;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** The own player for the client fallback on 1.16. */
public final class FallbackEnv116 implements FallbackEnv {

    private final Minecraft mc;
    private final LocalPlayer p;

    public FallbackEnv116(Minecraft mc) {
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
        return p.isOnGround();
    }

    @Override
    public int totems() {
        int n = 0;
        for (ItemStack s : new ItemStack[] {p.getMainHandItem(), p.getOffhandItem()}) {
            if (!s.isEmpty() && s.getItem() == Items.TOTEM_OF_UNDYING) n += s.getCount();
        }
        return n;
    }

    @Override
    public boolean hurtNow() {
        return p.hurtTime > 0 && p.hurtTime >= p.hurtDuration - 1;
    }

    @Override
    public boolean hasRegeneration() {
        return p.hasEffect(MobEffects.REGENERATION);
    }

    @Override
    public boolean hasAbsorptionEffect() {
        return p.hasEffect(MobEffects.ABSORPTION);
    }

    @Override
    public boolean inVoid() {
        return p.getY() < -60;
    }

    @Override
    public boolean inLava() {
        return p.isInLava();
    }

    @Override
    public boolean burning() {
        return p.isOnFire() && !p.hasEffect(MobEffects.FIRE_RESISTANCE);
    }

    @Override
    public boolean drowning() {
        return p.isEyeInFluid(FluidTags.WATER) && p.getAirSupply() <= 0;
    }

    @Override
    public boolean inWall() {
        return p.isInWall();
    }

    @Override
    public Projectile nearestProjectile() {
        Entity best = null;
        double bd = 9.0;
        for (Entity e : mc.level.getEntities(p, p.getBoundingBox().inflate(3.0))) {
            if (!(e instanceof net.minecraft.world.entity.projectile.Projectile)) continue;
            double d = e.distanceToSqr(p);
            if (d < bd) {
                bd = d;
                best = e;
            }
        }
        if (best == null) return null;
        Projectile r = new Projectile();
        r.name = Facts.entityName(best);
        r.arrow = best instanceof AbstractArrow;
        Entity shooter = r.arrow ? ((AbstractArrow) best).getOwner() : null;
        r.shooter = shooter == null ? null : Facts.entity(shooter, p);
        return r;
    }

    @Override
    public Explosive nearestExplosive() {
        for (Entity e : mc.level.getEntities(p, p.getBoundingBox().inflate(8.0))) {
            if (e instanceof PrimedTnt) return new Explosive();
            if (e instanceof Creeper && ((Creeper) e).getSwelling(1f) > 0.5f) {
                Explosive x = new Explosive();
                x.creeper = Facts.entity(e, p);
                return x;
            }
        }
        return null;
    }

    @Override
    public Mob nearestMob() {
        LivingEntity best = null;
        double bd = 36.0;
        for (LivingEntity e : mc.level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(6.0))) {
            if (e == p || !e.isAlive()) continue;
            double d = e.distanceToSqr(p);
            // hostile mobs and swinging entities first
            if (e instanceof Enemy || e.swinging || e instanceof Player) d *= 0.25;
            if (d < bd) {
                bd = d;
                best = e;
            }
        }
        if (best == null) return null;
        Mob m = new Mob();
        m.facts = Facts.entity(best, p);
        m.creeper = best instanceof Creeper;
        return m;
    }

    @Override
    public boolean hasWither() {
        return p.hasEffect(MobEffects.WITHER);
    }

    @Override
    public boolean hasPoison() {
        return p.hasEffect(MobEffects.POISON);
    }

    @Override
    public boolean starving() {
        return p.getFoodData().getFoodLevel() <= 0;
    }
}
