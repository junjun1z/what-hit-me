package io.github.junjun1z.whathitme.core.server;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.cmd.CommandText;
import io.github.junjun1z.whathitme.core.cmd.Completion;
import io.github.junjun1z.whathitme.core.config.WhmConfig;
import io.github.junjun1z.whathitme.core.config.WhmConfig.Flag;
import io.github.junjun1z.whathitme.core.data.DamageRecord;
import io.github.junjun1z.whathitme.core.data.DamageTracker;
import io.github.junjun1z.whathitme.core.data.DumpWriter;
import io.github.junjun1z.whathitme.core.data.Entry;
import io.github.junjun1z.whathitme.core.data.HitFacts;
import io.github.junjun1z.whathitme.core.data.PlayerHistory;
import io.github.junjun1z.whathitme.core.data.SearchEngine;
import io.github.junjun1z.whathitme.core.data.Snapshot;
import io.github.junjun1z.whathitme.core.net.Packets;
import io.github.junjun1z.whathitme.core.text.DisplayOptions;
import io.github.junjun1z.whathitme.core.text.Fmt;
import io.github.junjun1z.whathitme.core.text.Msg;
import io.github.junjun1z.whathitme.core.text.RecapFormatter;
import io.github.junjun1z.whathitme.core.text.Txt;

/**
 * Everything the logical server does, independent of the loader: recording damage taken by players, death recaps,
 * totem / heavy-hit snapshots, the network protocol and the server-side /whm command. The loader calls the on* methods
 * from its events (players only: every event handler returns on its first line for other entities) on the server
 * thread. No packet is sent for normal hits.
 */
public final class ServerCore {

    /** Lethal / heavy hits whose outcome (death, totem) is known at the end of the tick */
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

    private static final List<Pending> PENDING = new ArrayList<>();
    /** The running server (null while none runs) */
    public static volatile ServerPort port;

    private ServerCore() {}

    public static DamageTracker tracker() {
        return DamageTracker.SERVER;
    }

    private static long mergeMs() {
        return (long) (WhmConfig.common.mergeWindowSeconds * 1000f);
    }

    // ================================================================== combat

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
    public static void onHurt(UUID id, long tick, Object source, float amount, float absorption, float health, boolean blocking, boolean freshHit, int maxInvul) {
        if (amount <= 0) return;
        PlayerHistory h = tracker().get(id);
        h.pendingRaw = amount;
        h.pendingAbsorption = absorption;
        h.pendingHealth = health;
        h.pendingSourceHash = System.identityHashCode(source);
        h.pendingTick = tick;
        h.pendingBlocking = blocking;
        // a fresh hit shortly after the previous one: i-frames were bypassed
        long since = tick - h.lastHurtTick;
        h.pendingIFrameBypass = freshHit && since >= 1 && since < maxInvul / 2;
        h.lastHurtTick = tick;
    }

    /**
     * Some mods cancel the hurt event and apply the damage their own way, so no damage event follows. Record those hits
     * with the amount they took over (Forge: LivingHurtEvent, lowest priority, receiveCanceled).
     */
    public static void onHurtCancelled(PlayerHandle p, long tick, Object source, float amount, Supplier<HitFacts> facts) {
        PlayerHistory h = tracker().peek(p.uuid());
        if (h == null || h.pendingTick != tick || h.pendingSourceHash != System.identityHashCode(source)) return;
        h.pendingTick = Long.MIN_VALUE;
        amount = Math.max(0f, amount);
        if (amount < WhmConfig.common.minDamageThreshold || amount <= 0f) return;
        DamageRecord r = facts.get().build(System.currentTimeMillis(), h.pendingRaw, amount, 0f, 0, p.health(), p.maxHealth());
        h.add(r, mergeMs());
        tracker().catalog.register(r);
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
        PlayerHistory h = tracker().get(p.uuid());
        boolean match = h.pendingTick == tick && h.pendingSourceHash == System.identityHashCode(source);
        h.pendingTick = Long.MIN_VALUE;
        actual = Math.max(0f, actual);
        float absorbed = match ? Math.max(0f, h.pendingAbsorption - absorptionAfter) : 0f;
        float taken = actual + absorbed;
        WhmConfig.Common c = WhmConfig.common;
        if (taken < c.minDamageThreshold || taken <= 0) return;
        float raw = match ? h.pendingRaw : -1f;
        int extra = 0;
        if (match && h.pendingBlocking) extra |= DamageRecord.THROUGH_SHIELD;
        if (match && h.pendingIFrameBypass) extra |= DamageRecord.IGNORES_IFRAMES;
        float max = p.maxHealth();
        DamageRecord r = facts.get().build(System.currentTimeMillis(), raw, actual, absorbed, extra, Math.max(0f, healthBefore - actual), max);
        DamageRecord stored = h.add(r, mergeMs());
        tracker().catalog.register(r);
        boolean lethal = healthBefore - actual <= 0f;
        boolean heavy = max > 0 && taken >= c.heavyHitThreshold * max;
        if (lethal || heavy) PENDING.add(new Pending(p.uuid(), stored, lethal, heavy, p.totems()));
    }

    /**
     * Like {@link #onDamage}, for loaders that only see health and absorption after the hit (Fabric AFTER_DAMAGE, and
     * ALLOW_DEATH for lethal hits): the health lost is computed from the health saved by {@link #onHurt}. Does nothing
     * when the hit does not match the pending one (already recorded, or no onHurt).
     */
    public static void onDamageAfter(PlayerHandle p, long tick, Object source, float healthNow, float absorptionNow, Supplier<HitFacts> facts) {
        PlayerHistory h = tracker().peek(p.uuid());
        if (h == null || h.pendingTick != tick || h.pendingSourceHash != System.identityHashCode(source)) return;
        float before = h.pendingHealth;
        onDamage(p, tick, source, Math.max(0f, before - Math.max(0f, healthNow)), absorptionNow, before, facts);
    }

    /**
     * The player died (Forge LivingDeathEvent at the lowest priority, Fabric AFTER_DEATH).
     *
     * @param facts reads the killing damage source (used when no hit was recorded just before)
     * @param deathMessageJson the vanilla death message as JSON ("" = none)
     */
    public static void onDeath(PlayerHandle p, Supplier<HitFacts> facts, String deathMessageJson) {
        UUID uid = p.uuid();
        PlayerHistory h = tracker().get(uid);
        long now = System.currentTimeMillis();
        DamageRecord last = h.last();
        DamageRecord killing;
        if (last != null && now - last.time <= 1500 && !last.has(DamageRecord.KILLING)) {
            killing = last.withFlags(DamageRecord.KILLING);
            h.replace(last, killing);
        } else {
            // killed without a recorded hit (setHealth, a cancelled damage event, ...)
            killing = facts.get().build(now, -1f, 0f, 0f, DamageRecord.KILLING, 0f, p.maxHealth());
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
        Snapshot s = new Snapshot(Snapshot.DEATH, now, recs, key, deathMessageJson == null ? "" : deathMessageJson);
        h.setLastDeath(s);
        h.summaryPending = true;
        h.lastHurtTick = Long.MIN_VALUE; // the respawned player starts without invulnerability frames
        for (int i = PENDING.size() - 1; i >= 0; i--) if (PENDING.get(i).id.equals(uid)) PENDING.remove(i);
        if (tracker().isModded(uid)) p.send(new Packets.Recap(Packets.Recap.DEATH, "", 0, 0, s));
    }

    /** The player respawned after a death (not after leaving the End) */
    public static void onRespawn(PlayerHandle p) {
        PlayerHistory h = tracker().peek(p.uuid());
        if (h == null || !h.summaryPending || h.lastDeath() == null) return;
        h.summaryPending = false;
        if (tracker().isModded(p.uuid())) return; // that client shows its own (translated) summary
        if ((WhmConfig.common.mask & Flag.RESPAWN_CHAT_SUMMARY.bit()) == 0 || p.chatOff()) return;
        RecapFormatter f = new RecapFormatter(Msg.ENGLISH, options(p, false));
        for (Txt line : f.respawnSummary(h.lastDeath())) p.chat(line);
    }

    /** End of every server tick: only an empty-list check unless a lethal / heavy hit happened this tick */
    public static void onTickEnd() {
        if (PENDING.isEmpty()) return;
        List<Pending> list = new ArrayList<>(PENDING);
        PENDING.clear();
        ServerPort srv = port;
        if (srv == null) return;
        long now = System.currentTimeMillis();
        WhmConfig.Common c = WhmConfig.common;
        for (Pending q : list) {
            PlayerHandle p = srv.player(q.id);
            if (p == null || !p.isAlive() || p.health() <= 0f) continue;
            int kind;
            int mark;
            if (q.lethal) {
                kind = p.totems() < q.totems ? Snapshot.TOTEM : Snapshot.PREVENTED;
                mark = DamageRecord.TOTEM;
            } else if (q.heavy && (c.mask & Flag.HEAVY_HIT_SNAPSHOT.bit()) != 0) {
                kind = Snapshot.HEAVY;
                mark = DamageRecord.HEAVY;
            } else {
                continue;
            }
            PlayerHistory h = tracker().get(q.id);
            DamageRecord marked = q.record.withFlags(mark);
            if (!h.replace(q.record, marked)) marked = h.last() == null ? marked : h.last();
            List<DamageRecord> recs = h.recent(now, c.recapSeconds * 1000L, 30);
            int key = recs.indexOf(marked);
            if (key < 0) key = recs.size() - 1;
            Snapshot s = new Snapshot(kind, now, recs, key, "");
            h.addSnapshot(s, c.maxSnapshots);
            if (tracker().isModded(q.id)) {
                p.send(new Packets.Recap(Packets.Recap.ALERT, "", 1, h.snapshots().size(), s));
            } else {
                Flag f = kind == Snapshot.HEAVY ? Flag.HEAVY_HIT_SNAPSHOT : Flag.TOTEM_CHAT_ALERT;
                if ((c.mask & f.bit()) != 0) p.chat(new RecapFormatter(Msg.ENGLISH, options(p, false)).alert(s, 1));
            }
        }
    }

    /** The player left: its buffer is dropped */
    public static void onLogout(UUID id) {
        tracker().remove(id);
        for (int i = PENDING.size() - 1; i >= 0; i--) if (PENDING.get(i).id.equals(id)) PENDING.remove(i);
    }

    public static void serverStarted(ServerPort p) {
        port = p;
    }

    /** Server stopped (also the integrated server of single player) */
    public static void serverStopped() {
        tracker().clear();
        PENDING.clear();
        port = null;
    }

    // ================================================================== network

    /** A packet from a client (call on the server thread) */
    public static void onPacket(PlayerHandle p, Packets.Packet pkt) {
        if (pkt instanceof Packets.Hello) {
            tracker().setModded(p.uuid());
            p.send(info());
        } else if (pkt instanceof Packets.Request) {
            handle(p, (Packets.Request) pkt);
        }
    }

    public static Packets.ServerInfo info() {
        WhmConfig.Common c = WhmConfig.common;
        Packets.ServerInfo i = new Packets.ServerInfo();
        i.mask = c.mask;
        i.summarySeconds = c.summarySeconds;
        i.ignored = new ArrayList<>(c.ignoredDamageIds);
        for (Map.Entry<String, String> e : c.renameMap.entrySet()) i.renames.add(e.getKey() + " = " + e.getValue());
        for (Map.Entry<String, String> e : c.hintMap.entrySet()) i.hints.add(e.getKey() + " = " + e.getValue());
        return i;
    }

    /** After a reload / config change: tell every client with the mod */
    public static void broadcastInfo() {
        ServerPort srv = port;
        if (srv == null) return;
        for (UUID id : tracker().modded()) {
            PlayerHandle p = srv.player(id);
            if (p != null) p.send(info());
        }
    }

    private static void handle(PlayerHandle p, Packets.Request r) {
        ServerPort srv = port;
        if (srv == null) return;
        PlayerHistory h = tracker().get(p.uuid());
        long now = System.currentTimeMillis();
        switch (r.kind) {
            case Packets.Request.HISTORY:
                p.send(new Packets.Recap(Packets.Recap.HISTORY, "", 0, h.snapshots().size(), new Snapshot(Snapshot.HISTORY, now, h.records(), -1, "")));
                break;
            case Packets.Request.LAST:
                p.send(new Packets.Recap(Packets.Recap.LAST, "", 0, 0, h.lastDeath()));
                break;
            case Packets.Request.SNAPSHOT: {
                int n = Completion.parseInt(r.arg, 1);
                List<Snapshot> list = h.snapshots();
                Snapshot s = n >= 1 && n <= list.size() ? list.get(n - 1) : null;
                p.send(new Packets.Recap(Packets.Recap.SNAPSHOT, "", n, list.size(), s));
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
                p.send(new Packets.Recap(Packets.Recap.INSPECT_DEATH, t.name(), 0, 0, th.lastDeath()));
                p.send(new Packets.Recap(Packets.Recap.INSPECT_HISTORY, t.name(), 0, 0, new Snapshot(Snapshot.HISTORY, now, th.records(), -1, "")));
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
                List<Entry> all = tracker().catalog.entries();
                p.send(new Packets.Entries(Packets.Entries.DUMP, "", all.size(), all.size() > 4000 ? new ArrayList<>(all.subList(0, 4000)) : all));
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
                WhmConfig.reloadCommon();
                broadcastInfo();
                p.send(new Packets.Message(false, "whathitme.reload.server"));
                break;
            default:
                break;
        }
    }

    /** Display options for chat the server itself sends (players without the mod, console) */
    public static DisplayOptions options(PlayerHandle p, boolean clientActions) {
        WhmConfig.Common c = WhmConfig.common;
        DisplayOptions o = new DisplayOptions(c.mask, clientActions, c.summarySeconds).ignore(c.ignoredDamageIds).renames(c.renameMap).hints(c.hintMap);
        if (p != null) o.ignore(p.personalIgnored());
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
        }
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
                if (!sender.isAdmin()) throw CommandText.err(m, "whathitme.error.noperm");
                if (args.length < 2) throw CommandText.usage(m, "/whm " + sub + " <damage_id> <text...>");
                boolean rn = sub.equals("rename");
                c.putMapEntry(rn ? "renameMap" : "hintMap", args[1], rest);
                broadcastInfo();
                if (rest.isEmpty()) sender.send(m.tr(Fmt.YELLOW, rn ? "whathitme.rename.cleared" : "whathitme.sethint.cleared", args[1]));
                else sender.send(m.tr(Fmt.YELLOW, rn ? "whathitme.rename.set" : "whathitme.sethint.set", args[1], rest.replace('&', '§')));
                break;
            }
            case "config": {
                if (!sender.isAdmin()) throw CommandText.err(m, "whathitme.error.noperm");
                CommandText.config(sender, m, c, args);
                broadcastInfo();
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
                if (!sender.isAdmin()) throw CommandText.err(m, "whathitme.error.noperm");
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
            case "dump": {
                if (!sender.isAdmin()) throw CommandText.err(m, "whathitme.error.noperm");
                if (srv == null) break;
                File file = new File(srv.gameDir(), "whathitme_dump.csv");
                try {
                    List<Entry> all = SearchEngine.all(tracker().catalog, srv.registries());
                    int n = DumpWriter.write(file, all, c);
                    sender.send(m.tr(Fmt.GREEN, "whathitme.dump.done", n, file.getAbsolutePath()));
                    File tpl = new File(Platform.whmConfigDir(), "hint_template.cfg");
                    int ids = DumpWriter.writeTemplate(tpl, all, srv.registries());
                    sender.send(m.tr(Fmt.GREEN, "whathitme.dump.template", ids, tpl.getAbsolutePath()));
                } catch (Exception e) {
                    throw CommandText.err(m, "whathitme.dump.failed", String.valueOf(e.getMessage()));
                }
                break;
            }
            case "reload": {
                if (!sender.isAdmin()) throw CommandText.err(m, "whathitme.error.noperm");
                WhmConfig.reloadCommon();
                broadcastInfo();
                sender.send(m.tr(Fmt.GREEN, "whathitme.reload.server"));
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
        if (modded(sender)) return Collections.emptyList(); // the client command completes itself
        if (args.length <= 1) return Completion.matching(args, CommandText.SERVER_SUBS);
        String sub = args[0].toLowerCase(Locale.ROOT);
        ServerPort srv = port;
        if (args.length == 2) {
            switch (sub) {
                case "ignore":
                case "rename":
                case "sethint": {
                    List<String> ids = new ArrayList<>(tracker().catalog.ids());
                    ids.addAll(WhmConfig.common.ignoredDamageIds);
                    return Completion.matching(args, ids);
                }
                case "config":
                    return Completion.matching(args, WhmConfig.common.keys());
                case "inspect":
                    return Completion.matching(args, srv == null ? Collections.<String>emptyList() : srv.onlinePlayerNames());
                case "snapshot":
                    return Completion.matching(args, "1", "2", "3", "4", "5");
                default:
                    break;
            }
        }
        if (sub.equals("search")) return Completion.matching(args, Completion.searchSuggestions(tracker().catalog.ids()));
        if (sub.equals("config") && args.length == 3) return Completion.matching(args, WhmConfig.common.values(args[1]));
        return Collections.emptyList();
    }
}
