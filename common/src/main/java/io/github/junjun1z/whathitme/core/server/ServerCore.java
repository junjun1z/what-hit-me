package io.github.junjun1z.whathitme.core.server;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.Safe;
import io.github.junjun1z.whathitme.core.cmd.CommandText;
import io.github.junjun1z.whathitme.core.cmd.Completion;
import io.github.junjun1z.whathitme.core.cmd.TestPreview;
import io.github.junjun1z.whathitme.core.config.PackConfig;
import io.github.junjun1z.whathitme.core.config.WhmConfig;
import io.github.junjun1z.whathitme.core.config.WhmConfig.Flag;
import io.github.junjun1z.whathitme.core.data.CatalogCache;
import io.github.junjun1z.whathitme.core.data.Context;
import io.github.junjun1z.whathitme.core.data.DamageRecord;
import io.github.junjun1z.whathitme.core.data.DamageTracker;
import io.github.junjun1z.whathitme.core.data.DumpWriter;
import io.github.junjun1z.whathitme.core.data.Entry;
import io.github.junjun1z.whathitme.core.data.Gear;
import io.github.junjun1z.whathitme.core.data.HitFacts;
import io.github.junjun1z.whathitme.core.data.PlayerHistory;
import io.github.junjun1z.whathitme.core.data.SearchEngine;
import io.github.junjun1z.whathitme.core.data.Snapshot;
import io.github.junjun1z.whathitme.core.data.Stats;
import io.github.junjun1z.whathitme.core.net.Packets;
import io.github.junjun1z.whathitme.core.text.DisplayOptions;
import io.github.junjun1z.whathitme.core.text.Fmt;
import io.github.junjun1z.whathitme.core.text.Msg;
import io.github.junjun1z.whathitme.core.text.RecapFormatter;
import io.github.junjun1z.whathitme.core.text.Txt;

/**
 * Everything the logical server does, independent of the loader: recording damage taken by players, death recaps,
 * totem / heavy-hit snapshots, death triggers, statistics, the network protocol and the server-side /whm command. The
 * loader calls the on* methods from its events (players only: every event handler returns on its first line for other
 * entities) on the server thread. No packet is sent for normal hits, and nothing runs per tick: the outcome of a lethal
 * or heavy hit is checked by a one-off task ({@link ServerPort#schedule}). Every entry point is guarded ({@link Safe}).
 */
public final class ServerCore {

    /** Lethal / heavy hits whose outcome (death, totem) is known after the current tick */
    private static final class Pending {
        final UUID id;
        final DamageRecord record;
        final boolean lethal;
        final boolean heavy;
        final int totems;

        Pending(UUID id, DamageRecord record, boolean lethal, boolean heavy, int totems) {
            this.id = id;
            this.record = record;
            this.lethal = lethal;
            this.heavy = heavy;
            this.totems = totems;
        }
    }

    /** Damage from an entity the player hit at most this long ago counts as reflected */
    public static final long REFLECT_MS = 50;

    private static final List<Pending> PENDING = new ArrayList<>();
    /** "uuid|trigger" -> time it last ran */
    private static final Map<String, Long> TRIGGER_TIMES = new HashMap<>();
    /** The running server (null while none runs) */
    public static volatile ServerPort port;

    private ServerCore() {}

    public static DamageTracker tracker() {
        return DamageTracker.SERVER;
    }

    private static long mergeMs() {
        return (long) (WhmConfig.common.mergeWindowSeconds * 1000f);
    }

    private static boolean commonOn(Flag f) {
        return (WhmConfig.common.mask & f.bit()) != 0;
    }

    private static HitFacts facts(Supplier<HitFacts> s) {
        return Safe.get("reading a damage source", s, HitFacts.unknown());
    }

    // ================================================================== combat

    /** Like {@link #onHurt(PlayerHandle, long, Object, float, float, float, boolean, boolean, int)} without armor tracking */
    public static void onHurt(UUID id, long tick, Object source, float amount, float absorption, float health, boolean blocking, boolean freshHit, int maxInvul) {
        Safe.run("hurt event", () -> hurt(id, null, tick, source, amount, absorption, health, blocking, freshHit, maxInvul));
    }

    /**
     * Damage is about to be applied, before armor (Forge LivingHurtEvent at the highest priority, Fabric ALLOW_DAMAGE).
     *
     * @param tick server game time
     * @param source the damage source object (only its identity is used, to match the following onDamage)
     * @param amount damage before armor
     * @param absorption current absorption amount
     * @param health current health
     * @param blocking the player is blocking with a shield
     * @param freshHit this hit starts new invulnerability frames (not a partial hit during i-frames)
     * @param maxInvul invulnerability ticks of a fresh hit (20 in vanilla)
     */
    public static void onHurt(PlayerHandle p, long tick, Object source, float amount, float absorption, float health, boolean blocking, boolean freshHit,
                              int maxInvul) {
        Safe.run("hurt event", () -> hurt(p.uuid(), p, tick, source, amount, absorption, health, blocking, freshHit, maxInvul));
    }

    private static void hurt(UUID id, PlayerHandle p, long tick, Object source, float amount, float absorption, float health, boolean blocking,
                             boolean freshHit, int maxInvul) {
        if (amount <= 0) return;
        PlayerHistory h = tracker().get(id);
        h.pendingRaw = amount;
        h.pendingAbsorption = absorption;
        h.pendingHealth = health;
        h.pendingSourceHash = System.identityHashCode(source);
        h.pendingTick = tick;
        h.pendingBlocking = blocking;
        h.pendingArmor = p != null && commonOn(Flag.ARMOR_BREAK) ? Safe.get("reading armor", p::armor, null) : null;
        // a fresh hit shortly after the previous one: i-frames were bypassed
        long since = tick - h.lastHurtTick;
        h.pendingIFrameBypass = freshHit && since >= 1 && since < maxInvul / 2;
        h.lastHurtTick = tick;
    }

    /** The player attacked an entity (reflected damage detection) */
    public static void onPlayerAttack(UUID player, String targetUuid) {
        Safe.run("attack event", () -> {
            PlayerHistory h = tracker().get(player);
            h.lastAttackUuid = targetUuid == null ? "" : targetUuid;
            h.lastAttackTime = System.currentTimeMillis();
        });
    }

    /**
     * Some mods cancel the hurt event and apply the damage their own way, so no damage event follows. Record those hits
     * with the amount they took over (Forge: LivingHurtEvent, lowest priority, receiveCanceled).
     */
    public static void onHurtCancelled(PlayerHandle p, long tick, Object source, float amount, Supplier<HitFacts> facts) {
        Safe.run("cancelled hurt event", () -> {
            PlayerHistory h = tracker().peek(p.uuid());
            if (h == null || h.pendingTick != tick || h.pendingSourceHash != System.identityHashCode(source)) return;
            h.pendingTick = Long.MIN_VALUE;
            float a = Math.max(0f, amount);
            if (a < WhmConfig.common.minDamageThreshold || a <= 0f) return;
            DamageRecord r = facts(facts).build(System.currentTimeMillis(), h.pendingRaw, a, 0f, 0, p.health(), p.maxHealth(),
                Safe.get("dimension", p::dimension, ""), "");
            h.add(r, mergeMs());
            tracker().catalog.register(r);
        });
    }

    /**
     * Health was (or is about to be) reduced (Forge LivingDamageEvent at the lowest priority, Fabric AFTER_DAMAGE /
     * ALLOW_DEATH).
     *
     * @param actual health lost (after armor, potions and absorption)
     * @param absorptionAfter absorption left after this hit
     * @param healthBefore health before this hit
     * @param facts reads the damage source; only called when the hit is recorded
     */
    public static void onDamage(PlayerHandle p, long tick, Object source, float actual, float absorptionAfter, float healthBefore, Supplier<HitFacts> facts) {
        Safe.run("damage event", () -> damage(p, tick, source, actual, absorptionAfter, healthBefore, facts));
    }

    private static void damage(PlayerHandle p, long tick, Object source, float actualIn, float absorptionAfter, float healthBefore, Supplier<HitFacts> facts) {
        PlayerHistory h = tracker().get(p.uuid());
        boolean match = h.pendingTick == tick && h.pendingSourceHash == System.identityHashCode(source);
        h.pendingTick = Long.MIN_VALUE;
        Gear.Item[] armorBefore = match ? h.pendingArmor : null;
        h.pendingArmor = null;
        float actual = Math.max(0f, actualIn);
        float absorbed = match ? Math.max(0f, h.pendingAbsorption - absorptionAfter) : 0f;
        float taken = actual + absorbed;
        WhmConfig.Common c = WhmConfig.common;
        long now = System.currentTimeMillis();
        if (armorBefore != null) armorBreaks(p, h, armorBefore, now);
        if (taken < c.minDamageThreshold || taken <= 0) return;
        float raw = match ? h.pendingRaw : -1f;
        int extra = 0;
        if (match && h.pendingBlocking) extra |= DamageRecord.THROUGH_SHIELD;
        if (match && h.pendingIFrameBypass) extra |= DamageRecord.IGNORES_IFRAMES;
        float max = p.maxHealth();
        HitFacts hf = facts(facts);
        if ("thorns".equals(hf.damageType)) extra |= DamageRecord.REFLECTED;
        if (hf.attacker != null && !h.lastAttackUuid.isEmpty() && h.lastAttackUuid.equals(hf.attacker.uuid) && now - h.lastAttackTime <= REFLECT_MS) {
            extra |= DamageRecord.REFLECTED;
        }
        String ctx = hf.attacker == null && commonOn(Flag.ENVIRONMENT) ? environment(p, h, hf, now) : "";
        DamageRecord r = hf.build(now, raw, actual, absorbed, extra, Math.max(0f, healthBefore - actual), max, Safe.get("dimension", p::dimension, ""), ctx);
        DamageRecord stored = h.add(r, mergeMs());
        tracker().catalog.register(r);
        Stats.of(p.uuid()).addDamage(Math.min(taken, Math.max(0f, healthBefore) + absorbed)); // /kill deals Float.MAX_VALUE
        boolean lethal = healthBefore - actual <= 0f;
        boolean heavy = max > 0 && taken >= c.heavyHitThreshold * max;
        if (lethal || heavy) pending(new Pending(p.uuid(), stored, lethal, heavy, p.totems()));
    }

    /** Merged repeated damage reads the world only on its first hit */
    private static String environment(PlayerHandle p, PlayerHistory h, HitFacts hf, long now) {
        DamageRecord last = h.lastDamage();
        if (last != null && last.damageId.equals(hf.damageId()) && !last.context.isEmpty() && now - last.time <= Math.max(mergeMs(), 1000L)) return last.context;
        return Safe.get("reading the environment", p::environment, "");
    }

    private static void armorBreaks(PlayerHandle p, PlayerHistory h, Gear.Item[] before, long now) {
        Gear.Item[] after = Safe.get("reading armor", p::armor, null);
        if (after == null) return;
        for (int i = 0; i < Math.min(4, Math.min(before.length, after.length)); i++) {
            if (before[i] != null && (after[i] == null || !after[i].id.equals(before[i].id))) addArmorBreak(p, h, before[i], i, now);
        }
    }

    private static void addArmorBreak(PlayerHandle p, PlayerHistory h, Gear.Item it, int slot, long now) {
        if (!commonOn(Flag.ARMOR_BREAK)) return;
        String type = "armorBroken." + Gear.SLOTS[Math.max(0, Math.min(3, slot))];
        for (DamageRecord r : h.recent(now, 500, 10)) if (r.has(DamageRecord.ARMOR_BREAK) && r.damageType.equals(type)) return;
        String ctx = new Context().add(Context.ARMOR, it.key, it.name, it.modId).toString();
        h.add(new DamageRecord(now, DamageRecord.ARMOR_BREAK_ID, type, "", "", "", "", it.modId, Platform.modName(it.modId), -1f, 0f, 0f,
            DamageRecord.ARMOR_BREAK, -1f, "", "", it.enchants, "", 1, p.health(), p.maxHealth(), "armor" + slot, it.id,
            Safe.get("dimension", p::dimension, ""), ctx), 0);
    }

    /**
     * An item of the player broke (Forge PlayerDestroyItemEvent). Only armor is recorded.
     *
     * @param slot 0 head, 1 chest, 2 legs, 3 feet
     */
    public static void onArmorBroken(PlayerHandle p, Gear.Item item, int slot) {
        Safe.run("item break event", () -> {
            if (item == null) return;
            addArmorBreak(p, tracker().get(p.uuid()), item, slot, System.currentTimeMillis());
        });
    }

    /**
     * Like {@link #onDamage}, for loaders that only see health and absorption after the hit (Fabric AFTER_DAMAGE, and
     * ALLOW_DEATH for lethal hits): the health lost is computed from the health saved by {@link #onHurt}. Does nothing
     * when the hit does not match the pending one (already recorded, or no onHurt).
     */
    public static void onDamageAfter(PlayerHandle p, long tick, Object source, float healthNow, float absorptionNow, Supplier<HitFacts> facts) {
        Safe.run("damage event", () -> {
            PlayerHistory h = tracker().peek(p.uuid());
            if (h == null || h.pendingTick != tick || h.pendingSourceHash != System.identityHashCode(source)) return;
            float before = h.pendingHealth;
            damage(p, tick, source, Math.max(0f, before - Math.max(0f, healthNow)), absorptionNow, before, facts);
        });
    }

    private static void pending(Pending q) {
        ServerPort srv = port;
        if (srv != null && Safe.get("schedule", () -> srv.schedule(() -> Safe.run("hit outcome", () -> resolve(Collections.singletonList(q)))), false)) return;
        PENDING.add(q);
    }

    /**
     * The player died (Forge LivingDeathEvent at the lowest priority, Fabric AFTER_DEATH).
     *
     * @param facts reads the killing damage source (used when no hit was recorded just before)
     * @param deathMessageJson the vanilla death message as JSON ("" = none)
     */
    public static void onDeath(PlayerHandle p, Supplier<HitFacts> facts, String deathMessageJson) {
        Safe.run("death event", () -> death(p, facts, deathMessageJson));
    }

    private static void death(PlayerHandle p, Supplier<HitFacts> facts, String deathMessageJson) {
        UUID uid = p.uuid();
        PlayerHistory h = tracker().get(uid);
        long now = System.currentTimeMillis();
        DamageRecord last = h.lastDamage();
        final float healthAtDeath = Safe.get("health", p::health, 0f);
        float hpLeft = healthAtDeath;
        boolean recent = last != null && now - last.time <= 1500 && !last.has(DamageRecord.KILLING);
        // health-overhaul mods (First Aid) kill through a body part: health left at death, or the last hit was not lethal
        if (hpLeft <= 0.01f && recent && last.healthAfter > 0.01f && last.taken() > 0f) hpLeft = last.healthAfter;
        int vital = hpLeft > 0.01f ? DamageRecord.VITAL : 0;
        DamageRecord killing;
        if (recent) {
            killing = last.withFlags(DamageRecord.KILLING | vital);
            if (vital != 0) killing = killing.withHealthAfter(hpLeft);
            h.replace(last, killing);
        } else {
            // killed without a recorded hit (setHealth, a cancelled damage event, ...)
            HitFacts hf = facts(facts);
            String ctx = hf.attacker == null && commonOn(Flag.ENVIRONMENT) ? Safe.get("reading the environment", p::environment, "") : "";
            killing = hf.build(now, -1f, 0f, 0f, DamageRecord.KILLING | vital, vital != 0 ? hpLeft : 0f, p.maxHealth(), Safe.get("dimension", p::dimension, ""), ctx);
            h.add(killing, 0);
            tracker().catalog.register(killing);
        }
        WhmConfig.Common c = WhmConfig.common;
        List<DamageRecord> recs = h.recent(now, c.recapSeconds * 1000L, 30);
        int key = recs.indexOf(killing);
        if (key < 0) {
            recs.add(killing);
            key = recs.size() - 1;
        }
        Gear gear = Safe.get("reading equipment", () -> gear(p), null);
        Snapshot s = new Snapshot(Snapshot.DEATH, now, recs, key, deathMessageJson == null ? "" : deathMessageJson, gear);
        h.setLastDeath(s);
        h.summaryPending = true;
        h.lastHurtTick = Long.MIN_VALUE; // the respawned player starts without invulnerability frames
        for (int i = PENDING.size() - 1; i >= 0; i--) if (PENDING.get(i).id.equals(uid)) PENDING.remove(i);
        Stats st = Stats.of(uid);
        st.addDeath(killing);
        final DamageRecord kb = killing;
        Safe.run("death triggers", () -> runTriggers(p, kb, st, now));
        st.save();
        if (tracker().isModded(uid)) send(p, Packets.Recap.DEATH, "", 0, 0, s);
        Safe.run("death message", () -> customDeathMessageToClients(p, kb, s));
        final Snapshot dead = s;
        ServerPort srv = port;
        if (srv != null && healthAtDeath <= 0.01f) Safe.get("schedule", () -> srv.schedule(() -> Safe.run("revive check", () -> reviveCheck(uid, dead))), false);
    }


    /** The pack's death message template for a killing blow, null = vanilla message */
    public static String deathMessageTemplate(DamageRecord killing) {
        Map<String, String> dm = WhmConfig.pack.deathMessages;
        if (dm.isEmpty() || killing == null) return null;
        return io.github.junjun1z.whathitme.core.text.DeathMessages.find(dm, killing);
    }

    /**
     * Forge / NeoForge: no event can change the vanilla death message, so players with the mod get the custom message
     * (they replace the vanilla chat line); the console gets a log line. Fabric replaces it itself (see
     * {@link #customDeathMessage}).
     */
    private static void customDeathMessageToClients(PlayerHandle p, DamageRecord killing, Snapshot s) {
        ServerPort srv = port;
        if (srv == null || srv.replacesDeathMessages()) return;
        String tpl = deathMessageTemplate(killing);
        if (tpl == null) return;
        Txt msg = io.github.junjun1z.whathitme.core.text.DeathMessages.render(new RecapFormatter(Msg.ENGLISH, options(null, false)), tpl, killing, p.name(),
            s.deathMessage);
        Platform.log.info("[What Hit Me?] Death message: " + Fmt.strip(msg.toPlain(Msg.ENGLISH_LANG)));
        List<DamageRecord> one = new ArrayList<>();
        one.add(killing);
        Snapshot k = prepare(new Snapshot(Snapshot.DEATH, s.time, one, 0, s.deathMessage));
        for (UUID id : tracker().modded()) {
            if (tracker().protocol(id) < 3) continue;
            PlayerHandle t = srv.player(id);
            if (t != null) Safe.run("death message packet", () -> t.send(new Packets.Recap(Packets.Recap.DEATH_MESSAGE, p.name(), 0, 0, k, 3)));
        }
    }

    /**
     * Fabric: the custom death message (English, server side) used instead of the vanilla one for everyone, or null for
     * the vanilla message. Call where the vanilla message is built, after the lethal hit was recorded.
     */
    public static Txt customDeathMessage(PlayerHandle p, Supplier<HitFacts> facts) {
        return Safe.get("custom death message", () -> {
            if (WhmConfig.pack.deathMessages.isEmpty()) return null;
            PlayerHistory h = tracker().get(p.uuid());
            long now = System.currentTimeMillis();
            DamageRecord last = h.lastDamage();
            DamageRecord k;
            if (last != null && now - last.time <= 1500 && !last.has(DamageRecord.KILLING)) k = last.withFlags(DamageRecord.KILLING);
            else k = facts(facts).build(now, -1f, 0f, 0f, DamageRecord.KILLING, 0f, p.maxHealth(), Safe.get("dimension", p::dimension, ""), "");
            String tpl = deathMessageTemplate(k);
            if (tpl == null) return null;
            return io.github.junjun1z.whathitme.core.text.DeathMessages.render(new RecapFormatter(Msg.ENGLISH, options(null, false)), tpl, k, p.name(), "");
        }, null);
    }

    /** Health restored right after a death that was not cancelled: another mod revived the player */
    private static void reviveCheck(UUID uid, Snapshot dead) {
        ServerPort srv = port;
        PlayerHistory h = tracker().peek(uid);
        if (srv == null || h == null || h.lastDeath() != dead || !h.summaryPending) return;
        PlayerHandle p = srv.player(uid);
        if (p == null || !p.isAlive() || p.health() <= 0f) return;
        h.summaryPending = false;
        DamageRecord k = dead.key();
        if (k != null) prevented(p, h, k, Snapshot.PREVENTED, System.currentTimeMillis());
    }

    /**
     * LivingDeathEvent was cancelled by another mod (lowest priority, receiveCanceled): the player lives on. A lethal hit
     * just before becomes a "death prevented" snapshot when its outcome is checked; without one, the death source does.
     */
    public static void onDeathCancelled(PlayerHandle p, Supplier<HitFacts> facts) {
        Safe.run("cancelled death event", () -> {
            UUID uid = p.uuid();
            for (Pending q : PENDING) if (q.id.equals(uid) && q.lethal) return;
            PlayerHistory h = tracker().get(uid);
            long now = System.currentTimeMillis();
            DamageRecord last = h.lastDamage();
            if (last != null && now - last.time <= 1500 && (last.has(DamageRecord.TOTEM) || last.healthAfter <= 0f)) return;
            DamageRecord r = facts(facts).build(now, -1f, 0f, 0f, 0, p.health(), p.maxHealth(), Safe.get("dimension", p::dimension, ""), "");
            h.add(r, 0);
            tracker().catalog.register(r);
            prevented(p, h, r, Snapshot.PREVENTED, now);
        });
    }

    private static Gear gear(PlayerHandle p) {
        double[] pos = p.position();
        boolean hasPos = pos != null && pos.length >= 3;
        return new Gear(p.dimension(), hasPos ? pos[0] : Double.NaN, hasPos ? pos[1] : Double.NaN, hasPos ? pos[2] : Double.NaN, p.maxHealth(), p.armor(),
            p.effects());
    }

    private static void runTriggers(PlayerHandle p, DamageRecord k, Stats st, long now) {
        ServerPort srv = port;
        if (srv == null) return;
        for (PackConfig.DeathTrigger t : TestPreview.matching(WhmConfig.pack, k)) {
            String id = t.id();
            if (t.oncePerPlayer && st.triggersRun.contains(id)) continue;
            String ck = p.uuid() + "|" + id;
            Long prev = TRIGGER_TIMES.get(ck);
            if (t.cooldownSeconds > 0 && prev != null && now - prev < t.cooldownSeconds * 1000L) continue;
            String cmd = TestPreview.fill(t.command, p.name(), Safe.get("position", p::position, null), k);
            if (srv.runCommand(cmd)) {
                TRIGGER_TIMES.put(ck, now);
                if (t.oncePerPlayer) {
                    st.triggersRun.add(id);
                    st.markDirty();
                }
            }
        }
    }

    /** The player respawned after a death (not after leaving the End) */
    public static void onRespawn(PlayerHandle p) {
        Safe.run("respawn event", () -> {
            PlayerHistory h = tracker().peek(p.uuid());
            if (h == null || !h.summaryPending || h.lastDeath() == null) return;
            h.summaryPending = false;
            if (tracker().isModded(p.uuid())) return; // that client shows its own (translated) summary
            if ((WhmConfig.common.mask & Flag.RESPAWN_CHAT_SUMMARY.bit()) == 0 || p.chatOff()) return;
            RecapFormatter f = new RecapFormatter(Msg.ENGLISH, options(p, false));
            for (Txt line : f.respawnSummary(h.lastDeath())) p.chat(line);
        });
    }

    /**
     * End of a server tick, only for loaders whose {@link ServerPort#schedule} is not implemented: an empty-list check
     * unless a lethal / heavy hit happened this tick.
     */
    public static void onTickEnd() {
        if (PENDING.isEmpty()) return;
        List<Pending> list = new ArrayList<>(PENDING);
        PENDING.clear();
        Safe.run("hit outcome", () -> resolve(list));
    }

    private static void resolve(List<Pending> list) {
        ServerPort srv = port;
        if (srv == null) return;
        long now = System.currentTimeMillis();
        WhmConfig.Common c = WhmConfig.common;
        for (Pending q : list) {
            PlayerHandle p = srv.player(q.id);
            if (p == null || !p.isAlive() || p.health() <= 0f) continue;
            PlayerHistory h = tracker().get(q.id);
            if (h.lastDeath() != null && h.lastDeath().time >= q.record.time) continue;
            int kind;
            if (q.lethal) kind = p.totems() < q.totems ? Snapshot.TOTEM : Snapshot.PREVENTED;
            else if (q.heavy && (c.mask & Flag.HEAVY_HIT_SNAPSHOT.bit()) != 0) kind = Snapshot.HEAVY;
            else continue;
            prevented(p, h, q.record, kind, now);
        }
    }

    /** Marks the trigger record and saves / announces a totem, heavy hit or prevented death snapshot */
    private static void prevented(PlayerHandle p, PlayerHistory h, DamageRecord rec, int kind, long now) {
        WhmConfig.Common c = WhmConfig.common;
        int mark = kind == Snapshot.HEAVY ? DamageRecord.HEAVY : DamageRecord.TOTEM;
        DamageRecord marked = rec.withFlags(mark);
        if (!h.replace(rec, marked)) marked = h.lastDamage() == null ? marked : h.lastDamage();
        List<DamageRecord> recs = h.recent(now, c.recapSeconds * 1000L, 30);
        int key = recs.indexOf(marked);
        if (key < 0) key = recs.size() - 1;
        Snapshot s = new Snapshot(kind, now, recs, key, "");
        h.addSnapshot(s, c.maxSnapshots);
        if (tracker().isModded(p.uuid())) {
            send(p, Packets.Recap.ALERT, "", 1, h.snapshots().size(), s);
        } else {
            Flag f = kind == Snapshot.HEAVY ? Flag.HEAVY_HIT_SNAPSHOT : Flag.TOTEM_CHAT_ALERT;
            if ((c.mask & f.bit()) != 0) p.chat(new RecapFormatter(Msg.ENGLISH, options(p, false)).alert(s, 1));
        }
    }

    /** The player left: its buffer is dropped, its statistics saved */
    public static void onLogout(UUID id) {
        Safe.run("logout event", () -> {
            tracker().remove(id);
            Stats.unload(id);
            for (int i = PENDING.size() - 1; i >= 0; i--) if (PENDING.get(i).id.equals(id)) PENDING.remove(i);
        });
    }

    /** The world was saved: statistics of online players are written */
    public static void onWorldSave() {
        Safe.run("world save event", Stats::saveAll);
    }

    public static void serverStarted(ServerPort p) {
        port = p;
    }

    /** Server stopped (also the integrated server of single player) */
    public static void serverStopped() {
        Safe.run("server stop", Stats::unloadAll);
        tracker().clear();
        PENDING.clear();
        TRIGGER_TIMES.clear();
        port = null;
    }

    // ================================================================== network

    /** Removes what the server switched off before a snapshot leaves the server */
    static Snapshot prepare(Snapshot s) {
        if (s == null) return null;
        WhmConfig.Common c = WhmConfig.common;
        boolean weapon = commonOn(Flag.ATTACKER_WEAPON);
        boolean dist = commonOn(Flag.ATTACKER_DISTANCE);
        boolean owner = commonOn(Flag.ENTITY_OWNER);
        boolean env = commonOn(Flag.ENVIRONMENT);
        List<DamageRecord> list = new ArrayList<>(s.records.size());
        for (DamageRecord r : s.records) list.add(r.stripped(weapon, dist, owner, env));
        Gear g = s.gear;
        if (g != null && (c.mask & Flag.EQUIPMENT_IN_COPY.bit()) == 0) g = null;
        if (g != null && (c.mask & Flag.COORDS_IN_COPY.bit()) == 0) g = g.withoutPosition();
        return new Snapshot(s.kind, s.time, list, s.keyIndex, s.deathMessage, g);
    }

    private static void send(PlayerHandle p, int kind, String who, int index, int count, Snapshot s) {
        p.send(new Packets.Recap(kind, who, index, count, prepare(s), tracker().protocol(p.uuid())));
    }

    /** A packet from a client (call on the server thread) */
    public static void onPacket(PlayerHandle p, Packets.Packet pkt) {
        Safe.run("server packet", () -> {
            if (pkt instanceof Packets.Hello) {
                int proto = ((Packets.Hello) pkt).protocol;
                tracker().setModded(p.uuid(), proto);
                p.send(info(proto));
            } else if (pkt instanceof Packets.Request) {
                handle(p, (Packets.Request) pkt);
            }
        });
    }

    public static Packets.ServerInfo info() {
        return info(1);
    }

    /** Server info for a client of the given protocol; features the server switched off are left out */
    public static Packets.ServerInfo info(int protocol) {
        WhmConfig.Common c = WhmConfig.common;
        PackConfig pack = WhmConfig.pack;
        Packets.ServerInfo i = new Packets.ServerInfo();
        i.mask = c.mask;
        i.summarySeconds = c.summarySeconds;
        i.ignored = new ArrayList<>(c.ignoredDamageIds);
        for (String s : pack.ignored) if (!i.ignored.contains(s)) i.ignored.add(s);
        boolean custom = (c.mask & Flag.CUSTOM_HINTS.bit()) != 0;
        if (protocol >= 2) {
            i.extended = true;
            i.locked = new ArrayList<>(pack.locked);
            put(i.renames, c.renameMap);
            put(i.packRenames, pack.renameMap);
            if (protocol >= 3) {
                i.v3 = true;
                for (Map.Entry<String, io.github.junjun1z.whathitme.core.text.Template> e : pack.templates.entrySet()) {
                    i.templates.add(e.getKey() + "\n" + String.join("\n", e.getValue().lines));
                }
                put(i.deathMessages, pack.deathMessages);
            }
            if (custom) {
                put(i.hints, c.hintMap);
                put(i.packHints, pack.hintMap);
            }
        } else {
            Map<String, String> r = new LinkedHashMap<>(pack.renameMap);
            r.putAll(c.renameMap);
            put(i.renames, r);
            if (custom) {
                Map<String, String> h = new LinkedHashMap<>(pack.hintMap);
                h.putAll(c.hintMap);
                put(i.hints, h);
            }
        }
        return i;
    }

    private static void put(List<String> out, Map<String, String> m) {
        for (Map.Entry<String, String> e : m.entrySet()) out.add(e.getKey() + " = " + e.getValue());
    }

    /** After a reload / config change: tell every client with the mod */
    public static void broadcastInfo() {
        ServerPort srv = port;
        if (srv == null) return;
        for (UUID id : tracker().modded()) {
            PlayerHandle p = srv.player(id);
            if (p != null) Safe.run("server info", () -> p.send(info(tracker().protocol(id))));
        }
    }

    private static void handle(PlayerHandle p, Packets.Request r) {
        ServerPort srv = port;
        if (srv == null) return;
        PlayerHistory h = tracker().get(p.uuid());
        long now = System.currentTimeMillis();
        switch (r.kind) {
            case Packets.Request.HISTORY:
                send(p, Packets.Recap.HISTORY, "", 0, h.snapshots().size(), new Snapshot(Snapshot.HISTORY, now, h.records(), -1, ""));
                break;
            case Packets.Request.LAST:
                send(p, Packets.Recap.LAST, "", 0, 0, h.lastDeath());
                break;
            case Packets.Request.SNAPSHOT: {
                int n = Completion.parseInt(r.arg, 1);
                List<Snapshot> list = h.snapshots();
                Snapshot s = n >= 1 && n <= list.size() ? list.get(n - 1) : null;
                send(p, Packets.Recap.SNAPSHOT, "", n, list.size(), s);
                break;
            }
            case Packets.Request.INSPECT: {
                if (!p.isAdmin()) {
                    p.send(new Packets.Message(true, "whathitme.error.noperm"));
                    break;
                }
                PlayerHandle t = srv.playerByName(r.arg);
                if (t == null) {
                    p.send(new Packets.Message(true, "whathitme.inspect.notfound", r.arg));
                    break;
                }
                PlayerHistory th = tracker().get(t.uuid());
                send(p, Packets.Recap.INSPECT_DEATH, t.name(), 0, 0, th.lastDeath());
                send(p, Packets.Recap.INSPECT_HISTORY, t.name(), 0, 0, new Snapshot(Snapshot.HISTORY, now, th.records(), -1, ""));
                break;
            }
            case Packets.Request.EFFECTS: {
                List<Entry> e = p.effects();
                p.send(new Packets.Entries(Packets.Entries.EFFECTS, "", e.size(), e));
                break;
            }
            case Packets.Request.SEARCH: {
                List<Entry> all = SearchEngine.search(r.arg, tracker().catalog, srv.registries());
                List<Entry> part = all.size() > 40 ? new ArrayList<>(all.subList(0, 40)) : all;
                p.send(new Packets.Entries(Packets.Entries.SEARCH, r.arg, all.size(), part));
                break;
            }
            case Packets.Request.DUMP: {
                if (!p.isAdmin()) {
                    p.send(new Packets.Message(true, "whathitme.error.noperm"));
                    break;
                }
                List<Entry> all = SearchEngine.all(tracker().catalog, srv.registries());
                List<Entry> damage = new ArrayList<>();
                for (Entry e : all) if (e.type == Entry.DAMAGE) damage.add(e);
                p.send(new Packets.Entries(Packets.Entries.DUMP, "", damage.size(), damage.size() > 4000 ? new ArrayList<>(damage.subList(0, 4000)) : damage));
                break;
            }
            case Packets.Request.COMMAND: {
                final PlayerHandle who = p;
                execute(new CommandSource() {
                    @Override
                    public PlayerHandle player() {
                        return who;
                    }

                    @Override
                    public boolean isAdmin() {
                        return who.isAdmin();
                    }

                    @Override
                    public void send(Txt line) {
                        who.chat(line);
                    }
                }, Completion.args(r.arg));
                break;
            }
            case Packets.Request.RELOAD:
                if (!p.isAdmin()) break; // the client already reloaded its own files
                reload(srv);
                p.send(new Packets.Message(false, "whathitme.reload.server"));
                for (String w : WhmConfig.pack.warnings) p.send(new Packets.Message(true, "whathitme.reload.warning", w));

                break;
            default:
                break;
        }
    }

    /** /whm reload on the server: config files + pack folder, forced catalog rescan */
    private static void reload(ServerPort srv) {
        WhmConfig.reloadCommon();
        CatalogCache.rescan(srv.registries());
        broadcastInfo();
    }

    /** Display options for chat the server itself sends (players without the mod, console) */
    public static DisplayOptions options(PlayerHandle p, boolean clientActions) {
        WhmConfig.Common c = WhmConfig.common;
        PackConfig pack = WhmConfig.pack;
        DisplayOptions o = new DisplayOptions(c.mask, clientActions, c.summarySeconds).ignore(c.ignoredDamageIds).ignore(pack.ignored)
            .renames(pack.renameMap).renames(c.renameMap).hints(pack.hintMap).hints(c.hintMap).templates(pack.templates).deathMessages(pack.deathMessages);
        if (p != null) o.ignore(p.personalIgnored()).player(Safe.get("player name", p::name, ""));
        return o;
    }

    // ================================================================== /whm on the server

    private static boolean modded(CommandSource s) {
        return s.player() != null && tracker().isModded(s.player().uuid());
    }

    private static int historyLines() {
        return WhmConfig.client != null ? WhmConfig.client.chatHistoryLines : 15;
    }

    /**
     * /whm on the server. Players with the mod use the client command instead (it asks the server for data); this one
     * serves players without the mod (English text), the console, command blocks and "/whm server ..." from modded
     * clients.
     */
    public static void execute(CommandSource sender, String[] args) {
        Msg m = Msg.of(modded(sender));
        try {
            run(sender, m, args);
        } catch (CommandText.Failure f) {
            sender.fail(f.text);
        } catch (Throwable t) {
            Safe.warn("server command", t);
            sender.fail(m.tr(Fmt.RED, "whathitme.error.internal"));
        }
    }

    private static void requireAdmin(CommandSource sender, Msg m) {
        if (!sender.isAdmin()) throw CommandText.err(m, "whathitme.error.noperm");
    }

    private static void run(CommandSource sender, Msg m, String[] args) {
        ServerPort srv = port;
        PlayerHandle player = sender.player();
        RecapFormatter f = new RecapFormatter(m, options(player, m.localized));
        String sub = args.length == 0 ? "history" : args[0].toLowerCase(Locale.ROOT);
        String rest = args.length > 2 ? Completion.join(args, 2) : "";
        long now = System.currentTimeMillis();
        WhmConfig.Common c = WhmConfig.common;
        switch (sub) {
            case "history": {
                if (player == null) throw CommandText.err(m, "whathitme.error.player");
                PlayerHistory h = tracker().get(player.uuid());
                sender.send(f.history(h.records(), now, historyLines(), h.snapshots().size(), false, null));
                break;
            }
            case "last": {
                if (player == null) throw CommandText.err(m, "whathitme.error.player");
                Snapshot s = tracker().get(player.uuid()).lastDeath();
                if (s == null) sender.send(m.tr(Fmt.GRAY, "whathitme.last.none"));
                else sender.send(f.fullRecap(s, now, null));
                break;
            }
            case "snapshot": {
                if (player == null) throw CommandText.err(m, "whathitme.error.player");
                List<Snapshot> list = tracker().get(player.uuid()).snapshots();
                int n = args.length > 1 ? Completion.parseInt(args[1], 1) : 1;
                if (n < 1 || n > list.size()) sender.send(m.tr(Fmt.GRAY, "whathitme.snapshot.none"));
                else {
                    sender.send(f.fullRecap(list.get(n - 1), now, null));
                    sender.send(m.tr(Fmt.GRAY, "whathitme.snapshot.position", n, list.size()));
                }
                break;
            }
            case "effects": {
                if (player == null) throw CommandText.err(m, "whathitme.error.player");
                sender.send(f.effects(player.effects(), false));
                break;
            }
            case "stats": {
                if (player == null) throw CommandText.err(m, "whathitme.error.player");
                sender.send(CommandText.stats(m, f, Stats.of(player.uuid()), player.name()));
                break;
            }
            case "togglechat": {
                if (player == null) throw CommandText.err(m, "whathitme.error.player");
                boolean on = player.toggleChat();
                sender.send(m.tr(Fmt.YELLOW, on ? "whathitme.togglechat.on" : "whathitme.togglechat.off"));
                break;
            }
            case "ignore": {
                if (args.length < 2) throw CommandText.usage(m, "/whm ignore <damage_id>");
                boolean ign;
                if (sender.isAdmin()) {
                    ign = c.toggleIgnore(args[1]);
                    broadcastInfo();
                } else if (player != null) {
                    ign = player.togglePersonalIgnore(args[1]);
                } else {
                    throw CommandText.err(m, "whathitme.error.noperm");
                }
                sender.send(m.tr(Fmt.YELLOW, ign ? "whathitme.ignore.added" : "whathitme.ignore.removed", args[1]));
                break;
            }
            case "rename":
            case "sethint": {
                requireAdmin(sender, m);
                if (args.length < 2) throw CommandText.usage(m, "/whm " + sub + " <damage_id> <text...>");
                boolean rn = sub.equals("rename");
                c.putMapEntry(rn ? "renameMap" : "hintMap", args[1], rest);
                broadcastInfo();
                if (rest.isEmpty()) sender.send(m.tr(Fmt.YELLOW, rn ? "whathitme.rename.cleared" : "whathitme.sethint.cleared", args[1]));
                else sender.send(m.tr(Fmt.YELLOW, rn ? "whathitme.rename.set" : "whathitme.sethint.set", args[1], rest.replace('&', '§')));
                break;
            }
            case "config": {
                // everyone may look at the server settings; changing them needs operator permission
                if (args.length > 2) requireAdmin(sender, m);
                CommandText.config(sender, m, c, args);
                if (args.length > 2) broadcastInfo();
                break;
            }
            case "search": {
                if (args.length < 2) throw CommandText.usage(m, "/whm search <query | #tag | @modid>");
                if (srv == null) break;
                String q = Completion.join(args, 1);
                List<Entry> all = SearchEngine.search(q, tracker().catalog, srv.registries());
                sender.send(f.search(q, all.size() > 30 ? all.subList(0, 30) : all, all.size()));
                break;
            }
            case "inspect": {
                requireAdmin(sender, m);
                if (args.length < 2) throw CommandText.usage(m, "/whm inspect <player>");
                PlayerHandle t = srv == null ? null : srv.playerByName(args[1]);
                if (t == null) throw CommandText.err(m, "whathitme.inspect.notfound", args[1]);
                PlayerHistory th = tracker().get(t.uuid());
                // another player's records: no [Copy ID] / [Mute] buttons
                RecapFormatter fi = new RecapFormatter(m, f.o.without(Flag.ACTION_BUTTONS));
                if (th.lastDeath() == null) sender.send(m.tr(Fmt.GRAY, "whathitme.inspect.nodeath", t.name()));
                else sender.send(fi.fullRecap(th.lastDeath(), now, t.name()));
                sender.send(fi.history(th.records(), now, historyLines(), 0, false, t.name()));
                break;
            }
            case "test": {
                requireAdmin(sender, m);
                if (args.length < 2) throw CommandText.usage(m, "/whm test <damage_id> [entity_id] [item_id] [dim_id]");
                String name = player == null ? "Player" : player.name();
                double[] pos = player == null ? null : Safe.get("position", player::position, null);
                String dim = player == null ? "" : Safe.get("dimension", player::dimension, "");
                float max = player == null ? 20f : player.maxHealth();
                sender.send(TestPreview.lines(m, f, args, WhmConfig.pack, name, pos, dim, max));
                break;
            }
            case "dump": {
                requireAdmin(sender, m);
                if (srv == null) break;
                File file = new File(WhmConfig.cacheDir(), "whathitme_dump.csv");
                try {
                    List<Entry> all = SearchEngine.all(tracker().catalog, srv.registries());
                    int n = DumpWriter.write(file, all, c);
                    sender.send(m.tr(Fmt.GREEN, "whathitme.dump.done", n, file.getAbsolutePath()));
                    File tpl = new File(Platform.whmConfigDir(), "hint_template.cfg");
                    int ids = DumpWriter.writeTemplate(tpl, all, CatalogCache.view(srv.registries()));
                    sender.send(m.tr(Fmt.GREEN, "whathitme.dump.template", ids, tpl.getAbsolutePath()));
                } catch (Exception e) {
                    throw CommandText.err(m, "whathitme.dump.failed", String.valueOf(e.getMessage()));
                }
                break;
            }
            case "reload": {
                requireAdmin(sender, m);
                if (srv != null) reload(srv);
                else WhmConfig.reloadCommon();
                sender.send(m.tr(Fmt.GREEN, "whathitme.reload.server"));
                sender.send(m.tr(Fmt.GRAY, "whathitme.reload.pack", WhmConfig.pack.files.size(), WhmConfig.pack.errors.size()));
                for (String e : WhmConfig.pack.errors) sender.send(Msg.str(Fmt.RED, e));
                for (String w : WhmConfig.pack.warnings) sender.send(m.tr(Fmt.GOLD, "whathitme.reload.warning", w));
                break;
            }
            case "template": {
                if (player == null) throw CommandText.err(m, "whathitme.error.player");
                if (args.length < 3 || !"preview".equalsIgnoreCase(args[1])) throw CommandText.usage(m, "/whm template preview <" + String.join("|", io.github.junjun1z.whathitme.core.text.Template.OUTPUTS) + ">");
                Snapshot last = tracker().get(player.uuid()).lastDeath();
                sender.send(io.github.junjun1z.whathitme.core.cmd.TemplatePreview.lines(f, args[2], last, now));
                break;
            }
            case "help":
                CommandText.help(sender, m, false);
                break;
            default:
                sender.send(m.tr(Fmt.RED, "whathitme.error.unknown", sub));
                CommandText.help(sender, m, false);
        }
    }

    /** Tab completion of the server command */
    public static List<String> complete(CommandSource sender, String[] args) {
        try {
            return completeImpl(sender, args);
        } catch (Throwable t) {
            Safe.warn("server tab completion", t);
            return Collections.emptyList();
        }
    }

    private static List<String> completeImpl(CommandSource sender, String[] args) {
        if (modded(sender)) return Collections.emptyList(); // the client command completes itself
        if (args.length <= 1) return Completion.matching(args, CommandText.SERVER_SUBS);
        String sub = args[0].toLowerCase(Locale.ROOT);
        ServerPort srv = port;
        if (args.length == 2) {
            switch (sub) {
                case "ignore":
                case "rename":
                case "sethint":
                case "test": {
                    List<String> ids = new ArrayList<>(tracker().catalog.ids());
                    if (sub.equals("test") && srv != null) for (Entry e : CatalogCache.view(srv.registries()).damageTypeEntries()) ids.add(e.id);
                    ids.addAll(WhmConfig.common.ignoredDamageIds);
                    return Completion.matching(args, ids);
                }
                case "config":
                    return Completion.matching(args, WhmConfig.common.keys());
                case "template":
                    return Completion.matching(args, "preview");

                case "inspect":
                    return Completion.matching(args, srv == null ? Collections.<String>emptyList() : srv.onlinePlayerNames());
                case "snapshot":
                    return Completion.matching(args, "1", "2", "3", "4", "5");
                default:
                    break;
            }
        }
        if (sub.equals("test") && srv != null) return Completion.testArgs(args, CatalogCache.view(srv.registries()));
        if (sub.equals("search")) return Completion.matching(args, Completion.searchSuggestions(tracker().catalog.ids()));
        if (sub.equals("config") && args.length == 3) return Completion.matching(args, WhmConfig.common.values(args[1]));
        if (sub.equals("template") && args.length == 3) return Completion.matching(args, io.github.junjun1z.whathitme.core.text.Template.OUTPUTS);

        return Collections.emptyList();
    }
}
