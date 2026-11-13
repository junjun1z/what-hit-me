package io.github.junjun1z.whathitme.client;

import java.lang.reflect.Field;
import java.util.List;

import io.github.junjun1z.whathitme.config.WhmConfig;
import io.github.junjun1z.whathitme.config.WhmConfig.Flag;
import io.github.junjun1z.whathitme.data.DamageRecord;
import io.github.junjun1z.whathitme.data.Snapshot;
import io.github.junjun1z.whathitme.data.Sources;
import io.github.junjun1z.whathitme.text.RecapFormatter;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Client fallback for servers without the mod: watches the own health once per tick (a few float reads) and, only
 * when it drops, guesses the cause from the surroundings. Entries are marked [Client Est.].
 */
@SideOnly(Side.CLIENT)
final class ClientFallback {

    static final ClientFallback INSTANCE = new ClientFallback();

    private float prevHealth = -1f;
    private float prevAbsorption;
    private float prevFall;
    /** Highest fall distance of the current / last fall, kept a few ticks after landing (damage arrives later) */
    private float fallPeak;
    private int groundTicks;
    private int prevTotems;
    private boolean dead;
    private String deathMessage = "";

    void reset() {
        prevHealth = -1f;
        dead = false;
        deathMessage = "";
    }

    private static int totems(EntityPlayer p) {
        int n = 0;
        for (ItemStack s : new ItemStack[] {p.getHeldItemMainhand(), p.getHeldItemOffhand()}) {
            if (!s.isEmpty() && s.getItem() == Items.TOTEM_OF_UNDYING) n += s.getCount();
        }
        return n;
    }

    void tick(Minecraft mc) {
        EntityPlayerSP p = mc.player;
        float hp = p.getHealth();
        float abs = p.getAbsorptionAmount();
        int tot = totems(p);
        if (prevHealth < 0f) {
            prevHealth = hp;
            prevAbsorption = abs;
            prevFall = p.fallDistance;
            prevTotems = tot;
            dead = hp <= 0f;
            return;
        }
        if (p.fallDistance > 0f) {
            fallPeak = Math.max(fallPeak, p.fallDistance);
            groundTicks = 0;
        } else if (++groundTicks > 15) {
            fallPeak = 0f;
        }
        WhmConfig.Common c = WhmConfig.common;
        long now = System.currentTimeMillis();
        float lostHp = Math.max(0f, prevHealth - hp);
        float lostAbs = Math.max(0f, prevAbsorption - abs);
        boolean hurtNow = p.hurtTime > 0 && p.hurtTime >= p.maxHurtTime - 1;
        float taken = lostHp + (lostAbs > 0f && (lostHp > 0f || hurtNow) ? lostAbs : 0f);
        float threshold = Math.max(c.minDamageThreshold, WhmConfig.client.minDamageThreshold);
        if (!dead && taken > 0f && taken >= threshold) {
            DamageRecord r = estimate(mc, p, lostHp, taken - lostHp, hp, now);
            ClientState.FALLBACK.resize(WhmConfig.common.historySize);
            DamageRecord stored = ClientState.FALLBACK.add(r, (long) (c.mergeWindowSeconds * 1000f));
            ClientState.CATALOG.register(r);
            boolean totemPop = tot < prevTotems && hp > 0f && p.isPotionActive(MobEffects.REGENERATION);
            if (totemPop) {
                snapshot(Snapshot.TOTEM, stored, DamageRecord.TOTEM, now);
            } else if (hp > 0f && p.getMaxHealth() > 0f && taken >= Math.max(c.heavyHitThreshold, WhmConfig.client.heavyHitThreshold) * p.getMaxHealth()
                && ClientState.on(Flag.HEAVY_HIT_SNAPSHOT)) {
                snapshot(Snapshot.HEAVY, stored, DamageRecord.HEAVY, now);
            }
        } else if (tot < prevTotems && hp > 0f && p.isPotionActive(MobEffects.REGENERATION) && p.isPotionActive(MobEffects.ABSORPTION)) {
            DamageRecord last = ClientState.FALLBACK.last();
            if (last != null && now - last.time < 2000) snapshot(Snapshot.TOTEM, last, DamageRecord.TOTEM, now);
        }
        if (hp <= 0f && !dead) {
            dead = true;
            onDeath(now);
        } else if (hp > 0f && dead) {
            dead = false;
            deathMessage = "";
        }
        prevHealth = hp;
        prevAbsorption = abs;
        prevFall = p.fallDistance;
        prevTotems = tot;
    }

    private void snapshot(int kind, DamageRecord rec, int mark, long now) {
        DamageRecord marked = rec.withFlags(mark);
        if (!ClientState.FALLBACK.replace(rec, marked)) marked = rec;
        List<DamageRecord> recs = ClientState.FALLBACK.recent(now, WhmConfig.common.recapSeconds * 1000L, 30);
        int key = recs.indexOf(marked);
        Snapshot s = new Snapshot(kind, now, recs, key < 0 ? recs.size() - 1 : key, "");
        ClientState.FALLBACK.addSnapshot(s, WhmConfig.common.maxSnapshots);
        RecapFormatter f = ClientState.formatter();
        Flag fl = kind == Snapshot.HEAVY ? Flag.HEAVY_HIT_SNAPSHOT : Flag.TOTEM_CHAT_ALERT;
        if (f.o.on(fl)) ClientState.print(f.alert(s, 1));
    }

    private void onDeath(long now) {
        DamageRecord last = ClientState.FALLBACK.last();
        DamageRecord killing;
        if (last != null && now - last.time <= 1500 && !last.has(DamageRecord.KILLING)) {
            killing = last.withFlags(DamageRecord.KILLING);
            ClientState.FALLBACK.replace(last, killing);
        } else {
            killing = new DamageRecord(now, "unknown:unknown", "unknown", "", "", "", "", "unknown", "", -1f, 0f, 0f,
                DamageRecord.ESTIMATED | DamageRecord.KILLING, -1f, "", "", "", "", 1, 0f, 20f, "");
            ClientState.FALLBACK.add(killing, 0);
        }
        List<DamageRecord> recs = ClientState.FALLBACK.recent(now, WhmConfig.common.recapSeconds * 1000L, 30);
        int key = recs.indexOf(killing);
        if (key < 0) {
            recs.add(killing);
            key = recs.size() - 1;
        }
        Snapshot s = refine(new Snapshot(Snapshot.DEATH, now, recs, key, deathMessage));
        ClientState.FALLBACK.setLastDeath(s);
        ClientState.onDeath(s);
    }

    /** Reads the vanilla death message of the death screen (no access transformer: the only ITextComponent field) */
    void onDeathScreen(GuiGameOver gui) {
        if (ClientState.serverHasMod) return;
        try {
            for (Field f : GuiGameOver.class.getDeclaredFields()) {
                if (f.getType() == ITextComponent.class) {
                    f.setAccessible(true);
                    ITextComponent c = (ITextComponent) f.get(gui);
                    if (c != null) {
                        deathMessage = ITextComponent.Serializer.componentToJson(c);
                        Snapshot cur = ClientState.currentDeath;
                        if (cur != null && System.currentTimeMillis() - cur.time < 3000 && cur.deathMessage.isEmpty()) {
                            ClientState.currentDeath = refine(cur.withDeathMessage(deathMessage));
                            if (ClientState.pendingSummary == cur) ClientState.pendingSummary = ClientState.currentDeath;
                            if (ClientState.FALLBACK.lastDeath() == cur) ClientState.FALLBACK.setLastDeath(ClientState.currentDeath);
                        }
                    }
                    break;
                }
            }
        } catch (Throwable ignored) {
            // no message
        }
    }

    /**
     * The vanilla death message is a translation like death.attack.outOfWorld / death.attack.mob(.item): it tells the
     * real damage type of the killing blow, better than any guess.
     */
    private static Snapshot refine(Snapshot s) {
        DamageRecord k = s.key();
        if (k == null || !k.has(DamageRecord.ESTIMATED) || s.deathMessage.isEmpty()) return s;
        try {
            ITextComponent c = ITextComponent.Serializer.jsonToComponent(s.deathMessage);
            if (!(c instanceof net.minecraft.util.text.TextComponentTranslation)) return s;
            net.minecraft.util.text.TextComponentTranslation t = (net.minecraft.util.text.TextComponentTranslation) c;
            String key = t.getKey();
            String type;
            if (key.startsWith("death.fell.")) type = "fall";
            else if (key.startsWith("death.attack.")) {
                type = key.substring("death.attack.".length());
                if (type.endsWith(".item")) type = type.substring(0, type.length() - 5);
                if (type.endsWith(".player") && !type.equals("explosion.player")) type = type.substring(0, type.length() - 7);
            } else return s;
            Object[] args = t.getFormatArgs();
            String attacker = args.length > 1 ? (args[1] instanceof ITextComponent ? ((ITextComponent) args[1]).getUnformattedText() : String.valueOf(args[1])) : "";
            if (key.startsWith("death.fell.") && !key.startsWith("death.fell.accident")) attacker = "";
            boolean mob = "mob".equals(type) || "player".equals(type);
            String id = mob ? (k.damageId.startsWith("mob:") ? k.damageId : "mob:unknown") : "minecraft:" + type;
            String atk = attacker.isEmpty() ? k.attackerName : attacker;
            int flags = (k.flags & ~DamageRecord.PROPERTY_MASK) | typeFlags(type);
            DamageRecord nk = new DamageRecord(k.time, id, type, atk, attacker.isEmpty() ? k.attackerKey : "", k.attackerId, k.directName,
                mob ? k.modId : "minecraft", mob ? k.modName : "Minecraft", k.raw, k.actual, k.absorbed, flags, k.distance, k.weaponName, k.weaponKey,
                k.weaponEnchants, k.ownerName, k.hits, k.healthAfter, k.maxHealth, k.sourceTag);
            java.util.List<DamageRecord> recs = new java.util.ArrayList<>(s.records);
            recs.set(s.keyIndex, nk);
            ClientState.FALLBACK.replace(k, nk);
            ClientState.CATALOG.register(nk);
            return new Snapshot(s.kind, s.time, recs, s.keyIndex, s.deathMessage);
        } catch (Throwable e) {
            return s;
        }
    }

    private static int typeFlags(String type) {
        switch (type) {
            case "outOfWorld":
                return DamageRecord.BYPASS_ARMOR | DamageRecord.BYPASS_CREATIVE;
            case "fall":
            case "drown":
            case "inWall":
            case "cramming":
            case "flyIntoWall":
            case "generic":
                return DamageRecord.BYPASS_ARMOR;
            case "starve":
                return DamageRecord.BYPASS_ARMOR | DamageRecord.ABSOLUTE;
            case "magic":
            case "indirectMagic":
                return DamageRecord.BYPASS_ARMOR | DamageRecord.MAGIC;
            case "wither":
                return DamageRecord.BYPASS_ARMOR;
            case "onFire":
                return DamageRecord.BYPASS_ARMOR | DamageRecord.FIRE;
            case "inFire":
            case "lava":
            case "hotFloor":
                return DamageRecord.FIRE;
            case "explosion":
            case "explosion.player":
                return DamageRecord.EXPLOSION;
            case "arrow":
            case "thrown":
                return DamageRecord.PROJECTILE;
            case "fireball":
                return DamageRecord.PROJECTILE | DamageRecord.FIRE;
            default:
                return 0;
        }
    }

    // ------------------------------------------------------------------ guessing

    private DamageRecord estimate(Minecraft mc, EntityPlayerSP p, float actual, float absorbed, float hpAfter, long now) {
        String type;
        int flags = DamageRecord.ESTIMATED;
        Entity attacker = null;
        Entity direct = null;
        if (p.posY < -60) {
            type = "outOfWorld";
            flags |= DamageRecord.BYPASS_ARMOR | DamageRecord.ABSOLUTE | DamageRecord.BYPASS_CREATIVE;
        } else if (fallPeak > 3f && (p.onGround || p.fallDistance < prevFall)) {
            fallPeak = 0f;
            type = "fall";
            flags |= DamageRecord.BYPASS_ARMOR;
        } else if (p.isInLava()) {
            type = "lava";
            flags |= DamageRecord.FIRE;
        } else if (p.isBurning() && !p.isPotionActive(MobEffects.FIRE_RESISTANCE)) {
            type = "onFire";
            flags |= DamageRecord.FIRE | DamageRecord.BYPASS_ARMOR;
        } else if (p.isInsideOfMaterial(Material.WATER) && p.getAir() <= 0) {
            type = "drown";
            flags |= DamageRecord.BYPASS_ARMOR;
        } else if (p.isEntityInsideOpaqueBlock()) {
            type = "inWall";
            flags |= DamageRecord.BYPASS_ARMOR;
        } else {
            Entity proj = nearestProjectile(mc, p);
            EntityLivingBase mob = nearestMob(mc, p);
            Entity boom = nearestExplosive(mc, p);
            if (proj != null) {
                direct = proj;
                attacker = proj instanceof EntityArrow && ((EntityArrow) proj).shootingEntity != null ? ((EntityArrow) proj).shootingEntity : null;
                type = proj instanceof EntityArrow ? "arrow" : "thrown";
                flags |= DamageRecord.PROJECTILE;
            } else if (boom != null && actual + absorbed >= 4f) {
                attacker = boom instanceof EntityCreeper ? boom : null;
                type = attacker != null ? "explosion.player" : "explosion";
                flags |= DamageRecord.EXPLOSION;
            } else if (mob instanceof EntityCreeper) {
                // creepers never melee
                type = "explosion.player";
                attacker = mob;
                flags |= DamageRecord.EXPLOSION;
            } else if (mob != null) {
                attacker = mob;
                type = mob instanceof EntityPlayer ? "player" : "mob";
            } else if (p.isPotionActive(MobEffects.WITHER)) {
                type = "wither";
                flags |= DamageRecord.BYPASS_ARMOR;
            } else if (p.isPotionActive(MobEffects.POISON)) {
                type = "magic";
                flags |= DamageRecord.MAGIC | DamageRecord.BYPASS_ARMOR;
            } else if (p.getFoodStats().getFoodLevel() <= 0) {
                type = "starve";
                flags |= DamageRecord.BYPASS_ARMOR | DamageRecord.ABSOLUTE;
            } else {
                type = "unknown";
            }
        }
        String id;
        String modId;
        if (attacker != null && ("mob".equals(type) || "player".equals(type))) {
            id = "mob:" + Sources.entityId(attacker);
            modId = Sources.modOfEntity(attacker);
        } else if ("unknown".equals(type)) {
            id = "unknown:unknown";
            modId = "unknown";
        } else {
            id = "minecraft:" + type;
            modId = attacker != null && !(attacker instanceof EntityPlayer) ? Sources.modOfEntity(attacker) : "minecraft";
        }
        String atkName = "";
        String atkKey = "";
        String atkId = "";
        String owner = "";
        String tag = "";
        String wName = "";
        String wKey = "";
        String ench = "";
        float dist = -1f;
        if (attacker != null) {
            atkName = Sources.entityName(attacker);
            atkKey = Sources.entityNameKey(attacker);
            atkId = Sources.entityId(attacker);
            owner = Sources.ownerName(attacker);
            tag = attacker.getUniqueID().toString();
            dist = p.getDistance(attacker);
            if (attacker.isInvisible()) flags |= DamageRecord.INVISIBLE;
            ItemStack w = Sources.weapon(attacker);
            if (!w.isEmpty()) {
                wName = w.getDisplayName();
                wKey = w.hasDisplayName() ? "" : w.getTranslationKey() + ".name";
                ench = Sources.enchantments(w);
            }
        }
        String directName = direct != null && direct != attacker ? Sources.entityName(direct) : "";
        return new DamageRecord(now, id, type, atkName, atkKey, atkId, directName, modId, Sources.modName(modId), -1f, actual, absorbed, flags, dist,
            wName, wKey, ench, owner, 1, Math.max(0f, hpAfter), p.getMaxHealth(), tag);
    }

    private static Entity nearestProjectile(Minecraft mc, EntityPlayerSP p) {
        Entity best = null;
        double bd = 9.0;
        for (Entity e : mc.world.getEntitiesWithinAABBExcludingEntity(p, p.getEntityBoundingBox().grow(3.0))) {
            if (!(e instanceof IProjectile)) continue;
            double d = e.getDistanceSq(p);
            if (d < bd) {
                bd = d;
                best = e;
            }
        }
        return best;
    }

    private static EntityLivingBase nearestMob(Minecraft mc, EntityPlayerSP p) {
        EntityLivingBase best = null;
        double bd = 36.0;
        for (EntityLivingBase e : mc.world.getEntitiesWithinAABB(EntityLivingBase.class, p.getEntityBoundingBox().grow(6.0))) {
            if (e == p || !e.isEntityAlive()) continue;
            double d = e.getDistanceSq(p);
            // hostile mobs and swinging entities first
            if (e instanceof IMob || e.swingProgress > 0f || e instanceof EntityPlayer) d *= 0.25;
            if (d < bd) {
                bd = d;
                best = e;
            }
        }
        return best;
    }

    private static Entity nearestExplosive(Minecraft mc, EntityPlayerSP p) {
        for (Entity e : mc.world.getEntitiesWithinAABBExcludingEntity(p, p.getEntityBoundingBox().grow(8.0))) {
            if (e instanceof EntityTNTPrimed) return e;
            if (e instanceof EntityCreeper && ((EntityCreeper) e).getCreeperFlashIntensity(1f) > 0.5f) return e;
        }
        return null;
    }
}
