package io.github.junjun1z.whathitme.core.client;

import java.util.ArrayList;
import java.util.List;

import io.github.junjun1z.whathitme.core.config.WhmConfig;
import io.github.junjun1z.whathitme.core.config.WhmConfig.Flag;
import io.github.junjun1z.whathitme.core.data.DamageRecord;
import io.github.junjun1z.whathitme.core.data.EntityFacts;
import io.github.junjun1z.whathitme.core.data.HitFacts;
import io.github.junjun1z.whathitme.core.data.Snapshot;
import io.github.junjun1z.whathitme.core.text.RecapFormatter;

/**
 * Client fallback for servers without the mod: watches the own health once per tick (a few float reads) and, only
 * when it drops, guesses the cause from the surroundings. Entries are marked [Client Est.].
 */
public final class FallbackCore {

    public static final FallbackCore INSTANCE = new FallbackCore();

    private float prevHealth = -1f;
    private float prevAbsorption;
    private float prevFall;
    /** Highest fall distance of the current / last fall, kept a few ticks after landing (damage arrives later) */
    private float fallPeak;
    private int groundTicks;
    private int prevTotems;
    private boolean dead;
    private String deathMessage = "";

    public void reset() {
        prevHealth = -1f;
        dead = false;
        deathMessage = "";
    }

    public void tick(FallbackEnv p) {
        float hp = p.health();
        float abs = p.absorption();
        int tot = p.totems();
        float fall = p.fallDistance();
        if (prevHealth < 0f) {
            prevHealth = hp;
            prevAbsorption = abs;
            prevFall = fall;
            prevTotems = tot;
            dead = hp <= 0f;
            return;
        }
        if (fall > 0f) {
            fallPeak = Math.max(fallPeak, fall);
            groundTicks = 0;
        } else if (++groundTicks > 15) {
            fallPeak = 0f;
        }
        WhmConfig.Common c = WhmConfig.common;
        long now = System.currentTimeMillis();
        float lostHp = Math.max(0f, prevHealth - hp);
        float lostAbs = Math.max(0f, prevAbsorption - abs);
        boolean hurtNow = p.hurtNow();
        float taken = lostHp + (lostAbs > 0f && (lostHp > 0f || hurtNow) ? lostAbs : 0f);
        float threshold = Math.max(c.minDamageThreshold, WhmConfig.client.minDamageThreshold);
        if (!dead && taken > 0f && taken >= threshold) {
            DamageRecord r = estimate(p, lostHp, taken - lostHp, hp, now);
            ClientCore.FALLBACK.resize(WhmConfig.common.historySize);
            DamageRecord stored = ClientCore.FALLBACK.add(r, (long) (c.mergeWindowSeconds * 1000f));
            ClientCore.CATALOG.register(r);
            boolean totemPop = tot < prevTotems && hp > 0f && p.hasRegeneration();
            if (totemPop) {
                snapshot(Snapshot.TOTEM, stored, DamageRecord.TOTEM, now);
            } else if (hp > 0f && p.maxHealth() > 0f && taken >= Math.max(c.heavyHitThreshold, WhmConfig.client.heavyHitThreshold) * p.maxHealth()
                && ClientCore.on(Flag.HEAVY_HIT_SNAPSHOT)) {
                snapshot(Snapshot.HEAVY, stored, DamageRecord.HEAVY, now);
            }
        } else if (tot < prevTotems && hp > 0f && p.hasRegeneration() && p.hasAbsorptionEffect()) {
            DamageRecord last = ClientCore.FALLBACK.last();
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
        prevFall = fall;
        prevTotems = tot;
    }

    private void snapshot(int kind, DamageRecord rec, int mark, long now) {
        DamageRecord marked = rec.withFlags(mark);
        if (!ClientCore.FALLBACK.replace(rec, marked)) marked = rec;
        List<DamageRecord> recs = ClientCore.FALLBACK.recent(now, WhmConfig.common.recapSeconds * 1000L, 30);
        int key = recs.indexOf(marked);
        Snapshot s = new Snapshot(kind, now, recs, key < 0 ? recs.size() - 1 : key, "");
        ClientCore.FALLBACK.addSnapshot(s, WhmConfig.common.maxSnapshots);
        RecapFormatter f = ClientCore.formatter();
        Flag fl = kind == Snapshot.HEAVY ? Flag.HEAVY_HIT_SNAPSHOT : Flag.TOTEM_CHAT_ALERT;
        if (f.o.on(fl)) ClientCore.print(f.alert(s, 1));
    }

    private void onDeath(long now) {
        DamageRecord last = ClientCore.FALLBACK.last();
        DamageRecord killing;
        if (last != null && now - last.time <= 1500 && !last.has(DamageRecord.KILLING)) {
            killing = last.withFlags(DamageRecord.KILLING);
            ClientCore.FALLBACK.replace(last, killing);
        } else {
            killing = new DamageRecord(now, "unknown:unknown", "unknown", "", "", "", "", "unknown", "", -1f, 0f, 0f,
                DamageRecord.ESTIMATED | DamageRecord.KILLING, -1f, "", "", "", "", 1, 0f, 20f, "");
            ClientCore.FALLBACK.add(killing, 0);
        }
        List<DamageRecord> recs = ClientCore.FALLBACK.recent(now, WhmConfig.common.recapSeconds * 1000L, 30);
        int key = recs.indexOf(killing);
        if (key < 0) {
            recs.add(killing);
            key = recs.size() - 1;
        }
        Snapshot s = refine(new Snapshot(Snapshot.DEATH, now, recs, key, deathMessage));
        ClientCore.FALLBACK.setLastDeath(s);
        ClientCore.onDeath(s);
    }

    /** The death screen opened: its vanilla death message as JSON */
    public void onDeathScreen(String json) {
        if (ClientCore.serverHasMod || json == null || json.isEmpty()) return;
        deathMessage = json;
        Snapshot cur = ClientCore.currentDeath;
        if (cur != null && System.currentTimeMillis() - cur.time < 3000 && cur.deathMessage.isEmpty()) {
            ClientCore.currentDeath = refine(cur.withDeathMessage(deathMessage));
            if (ClientCore.pendingSummary == cur) ClientCore.pendingSummary = ClientCore.currentDeath;
            if (ClientCore.FALLBACK.lastDeath() == cur) ClientCore.FALLBACK.setLastDeath(ClientCore.currentDeath);
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
            String[] parsed = ClientCore.port.parseDeathMessage(s.deathMessage);
            if (parsed == null) return s;
            String key = parsed[0];
            String type;
            if (key.startsWith("death.fell.")) type = "fall";
            else if (key.startsWith("death.attack.")) {
                type = key.substring("death.attack.".length());
                if (type.endsWith(".item")) type = type.substring(0, type.length() - 5);
                if (type.endsWith(".player") && !type.equals("explosion.player")) type = type.substring(0, type.length() - 7);
            } else return s;
            String attacker = parsed.length > 1 && parsed[1] != null ? parsed[1] : "";
            if (key.startsWith("death.fell.") && !key.startsWith("death.fell.accident")) attacker = "";
            boolean mob = "mob".equals(type) || "player".equals(type);
            String id = mob ? (k.damageId.startsWith("mob:") ? k.damageId : "mob:unknown") : ClientCore.typeIdOfMsgId(type);
            String atk = attacker.isEmpty() ? k.attackerName : attacker;
            int flags = (k.flags & ~DamageRecord.PROPERTY_MASK) | typeFlags(type);
            DamageRecord nk = new DamageRecord(k.time, id, type, atk, attacker.isEmpty() ? k.attackerKey : "", k.attackerId, k.directName,
                mob ? k.modId : "minecraft", mob ? k.modName : "Minecraft", k.raw, k.actual, k.absorbed, flags, k.distance, k.weaponName, k.weaponKey,
                k.weaponEnchants, k.ownerName, k.hits, k.healthAfter, k.maxHealth, k.sourceTag);
            List<DamageRecord> recs = new ArrayList<>(s.records);
            recs.set(s.keyIndex, nk);
            ClientCore.FALLBACK.replace(k, nk);
            ClientCore.CATALOG.register(nk);
            return new Snapshot(s.kind, s.time, recs, s.keyIndex, s.deathMessage);
        } catch (Throwable e) {
            return s;
        }
    }

    /** Property flags of the vanilla damage types (by msgId) */
    public static int typeFlags(String type) {
        switch (type) {
            case "outOfWorld":
                return DamageRecord.BYPASS_ARMOR | DamageRecord.BYPASS_CREATIVE;
            case "fall":
            case "drown":
            case "inWall":
            case "cramming":
            case "flyIntoWall":
            case "generic":
            case "freeze":
            case "stalagmite":
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
            case "trident":
            case "mobProjectile":
                return DamageRecord.PROJECTILE;
            case "fireball":
                return DamageRecord.PROJECTILE | DamageRecord.FIRE;
            default:
                return 0;
        }
    }

    // ------------------------------------------------------------------ guessing

    private DamageRecord estimate(FallbackEnv p, float actual, float absorbed, float hpAfter, long now) {
        HitFacts f = new HitFacts();
        int flags = DamageRecord.ESTIMATED;
        String type;
        EntityFacts attacker = null;
        String directName = "";
        if (p.inVoid()) {
            type = "outOfWorld";
            flags |= DamageRecord.BYPASS_ARMOR | DamageRecord.ABSOLUTE | DamageRecord.BYPASS_CREATIVE;
        } else if (fallPeak > 3f && (p.onGround() || p.fallDistance() < prevFall)) {
            fallPeak = 0f;
            type = "fall";
            flags |= DamageRecord.BYPASS_ARMOR;
        } else if (p.inLava()) {
            type = "lava";
            flags |= DamageRecord.FIRE;
        } else if (p.burning()) {
            type = "onFire";
            flags |= DamageRecord.FIRE | DamageRecord.BYPASS_ARMOR;
        } else if (p.drowning()) {
            type = "drown";
            flags |= DamageRecord.BYPASS_ARMOR;
        } else if (p.inWall()) {
            type = "inWall";
            flags |= DamageRecord.BYPASS_ARMOR;
        } else {
            FallbackEnv.Projectile proj = p.nearestProjectile();
            FallbackEnv.Mob mob = p.nearestMob();
            FallbackEnv.Explosive boom = p.nearestExplosive();
            if (proj != null) {
                directName = proj.name;
                attacker = proj.arrow ? proj.shooter : null;
                type = proj.arrow ? "arrow" : "thrown";
                flags |= DamageRecord.PROJECTILE;
            } else if (boom != null && actual + absorbed >= 4f) {
                attacker = boom.creeper;
                type = attacker != null ? "explosion.player" : "explosion";
                flags |= DamageRecord.EXPLOSION;
            } else if (mob != null && mob.creeper) {
                // creepers never melee
                type = "explosion.player";
                attacker = mob.facts;
                flags |= DamageRecord.EXPLOSION;
            } else if (mob != null) {
                attacker = mob.facts;
                type = mob.facts.player ? "player" : "mob";
            } else if (p.hasWither()) {
                type = "wither";
                flags |= DamageRecord.BYPASS_ARMOR;
            } else if (p.hasPoison()) {
                type = "magic";
                flags |= DamageRecord.MAGIC | DamageRecord.BYPASS_ARMOR;
            } else if (p.starving()) {
                type = "starve";
                flags |= DamageRecord.BYPASS_ARMOR | DamageRecord.ABSOLUTE;
            } else {
                type = "unknown";
            }
        }
        f.damageType = type;
        f.melee = attacker != null && ("mob".equals(type) || "player".equals(type));
        f.typeId = "unknown".equals(type) ? "unknown:unknown" : ClientCore.typeIdOfMsgId(type);
        f.flags = flags;
        f.attacker = attacker;
        if (attacker != null && directName.equals(attacker.name)) directName = "";
        f.directName = directName;
        return f.build(now, -1f, actual, absorbed, 0, Math.max(0f, hpAfter), p.maxHealth());
    }
}
