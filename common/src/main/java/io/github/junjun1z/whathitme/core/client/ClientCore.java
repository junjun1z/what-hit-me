package io.github.junjun1z.whathitme.core.client;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.Safe;
import io.github.junjun1z.whathitme.core.cmd.TestPreview;
import io.github.junjun1z.whathitme.core.config.PackConfig;
import io.github.junjun1z.whathitme.core.data.CatalogCache;
import io.github.junjun1z.whathitme.core.data.Stats;
import io.github.junjun1z.whathitme.core.cmd.CommandText;
import io.github.junjun1z.whathitme.core.cmd.Completion;
import io.github.junjun1z.whathitme.core.cmd.Sink;
import io.github.junjun1z.whathitme.core.config.WhmConfig;
import io.github.junjun1z.whathitme.core.config.WhmConfig.Flag;
import io.github.junjun1z.whathitme.core.data.DamageCatalog;
import io.github.junjun1z.whathitme.core.data.DamageRecord;
import io.github.junjun1z.whathitme.core.data.DumpWriter;
import io.github.junjun1z.whathitme.core.data.Entry;
import io.github.junjun1z.whathitme.core.data.PlayerHistory;
import io.github.junjun1z.whathitme.core.data.SearchEngine;
import io.github.junjun1z.whathitme.core.data.Snapshot;
import io.github.junjun1z.whathitme.core.net.Packets;
import io.github.junjun1z.whathitme.core.text.DisplayOptions;
import io.github.junjun1z.whathitme.core.text.Fmt;
import io.github.junjun1z.whathitme.core.text.Msg;
import io.github.junjun1z.whathitme.core.text.RecapFormatter;
import io.github.junjun1z.whathitme.core.text.Txt;
import io.github.junjun1z.whathitme.core.text.Template;
import io.github.junjun1z.whathitme.core.text.DeathMessages;
import io.github.junjun1z.whathitme.core.config.Templates;

/**
 * Client side state and logic, independent of the loader: server info, death recaps, the respawn summary, the client
 * fallback history and the client /whm command. Only touched on the client thread.
 */
public final class ClientCore {

    /** Set by the loader on client start */
    public static ClientPort port;
    /**
     * Damage id of a vanilla damage type name (msgId), for client estimates: "minecraft:" + msgId before 1.19.4, the
     * DamageType registry key from 1.19.4 on (set by those versions).
     */
    public static Function<String, String> msgIdToTypeId = t -> "minecraft:" + t;

    /** The server answered our hello: accurate data comes from it */
    public static boolean serverHasMod;
    public static Packets.ServerInfo serverInfo;
    static boolean helloSent;

    /** Latest death recap (server or client estimate) */
    public static Snapshot currentDeath;
    /** Death waiting for the respawn chat summary */
    static Snapshot pendingSummary;
    static boolean sawDead;
    static long pendingSince;

    /** Client fallback history (server without the mod) */
    public static final PlayerHistory FALLBACK = new PlayerHistory(50);
    public static final DamageCatalog CATALOG = new DamageCatalog();

    private static final Sink CHAT = new Sink() {
        @Override
        public void send(Txt line) {
            print(line);
        }
    };

    private ClientCore() {}

    static String typeIdOfMsgId(String type) {
        try {
            String id = msgIdToTypeId.apply(type);
            return id == null ? "minecraft:" + type : id;
        } catch (Throwable t) {
            return "minecraft:" + type;
        }
    }

    /** Disconnected: forget everything about that server */
    public static void reset() {
        serverHasMod = false;
        serverInfo = null;
        helloSent = false;
        currentDeath = null;
        pendingSummary = null;
        serverTemplates = new java.util.LinkedHashMap<>();
        serverDeathMessages = new java.util.LinkedHashMap<>();
        DEATH_LINES.clear();
        FALLBACK.clear();
        FallbackCore.INSTANCE.reset();
    }

    /** Stats of the own player when the server does not have the mod */
    public static final java.util.UUID LOCAL_ID = new java.util.UUID(0L, 0L);

    /** Config keys locked by the modpack: the server's when it has the mod, otherwise the local pack folder */
    public static boolean locked(String key) {
        Packets.ServerInfo i = serverHasMod ? serverInfo : null;
        if (i != null && i.extended) {
            for (String k : i.locked) if (k.equalsIgnoreCase(key)) return true;
            return false;
        }
        return WhmConfig.isLocked(key);
    }

    private static Map<String, String> map(List<String> lines) {
        return WhmConfig.parseMap(lines.toArray(new String[0]));
    }

    public static DisplayOptions options() {
        WhmConfig.Common c = WhmConfig.common;
        WhmConfig.Client cl = WhmConfig.client;
        Packets.ServerInfo i = serverHasMod ? serverInfo : null;
        int common = i != null ? i.mask : c.mask;
        if (i != null && i.protocol < 2) common |= ~WhmConfig.LEGACY_FLAGS;
        int personal = cl.mask;
        for (Flag f : Flag.values()) if (locked(f.key)) personal |= f.bit();
        DisplayOptions o = new DisplayOptions(common & personal, true, i != null ? i.summarySeconds : c.summarySeconds);
        if (i != null) {
            o.ignore(i.ignored).renames(map(i.packRenames)).renames(map(i.renames)).hints(map(i.packHints)).hints(map(i.hints));
        } else {
            PackConfig pack = WhmConfig.pack;
            o.ignore(c.ignoredDamageIds).ignore(pack.ignored).renames(pack.renameMap).renames(c.renameMap).hints(pack.hintMap).hints(c.hintMap);
        }
        Map<String, Template> packTemplates = i != null ? serverTemplates : WhmConfig.pack.templates;
        o.templates(Templates.effective(cl.templates, packTemplates, ClientCore::locked)).packTemplates(packTemplates)
            .deathMessages(i != null ? serverDeathMessages : WhmConfig.pack.deathMessages).player(Safe.get("player name", () -> port == null ? "" : port.playerName(), ""));
        return o.ignore(cl.ignoredDamageIds).personalRenames(cl.renameMap).personalHints(cl.hintMap);
    }

    /** Pack templates / death messages of the server (protocol 3), or empty */
    public static Map<String, Template> serverTemplates = new java.util.LinkedHashMap<>();
    public static Map<String, String> serverDeathMessages = new java.util.LinkedHashMap<>();
    /** Custom death messages waiting for their vanilla chat line: {plain vanilla text, replacement Txt, expiry ms, used in chat} */
    private static final List<Object[]> DEATH_LINES = new ArrayList<>();

    /** The pack templates in use (the server's when it has the mod) */
    public static Map<String, Template> packTemplates() {
        return serverHasMod && serverInfo != null ? serverTemplates : WhmConfig.pack.templates;
    }

    /** Script variables of the server: name -&gt; description (template editor) */
    public static Map<String, String> scriptVariables() {
        Map<String, String> out = new java.util.LinkedHashMap<>();
        Packets.ServerInfo i = serverHasMod ? serverInfo : null;
        if (i == null) return out;
        for (String e : i.scriptVars) {
            int nl = e.indexOf('\n');
            out.put(nl < 0 ? e : e.substring(0, nl), nl < 0 ? "" : e.substring(nl + 1));
        }
        return out;
    }

    public static Map<String, String> packDeathMessages() {
        return serverHasMod && serverInfo != null ? serverDeathMessages : WhmConfig.pack.deathMessages;
    }

    /**
     * A chat line arrived (Forge / NeoForge client chat event): the custom death message that replaces it, or null to keep
     * it. Only the vanilla death message the server announced right before is replaced.
     */
    public static Txt chatReplacement(String plain) {
        return Safe.get("death message", () -> {
            if (DEATH_LINES.isEmpty() || plain == null) return null;
            long now = System.currentTimeMillis();
            String p = Fmt.strip(plain).trim();
            for (int i = 0; i < DEATH_LINES.size(); i++) {
                Object[] e = DEATH_LINES.get(i);
                if ((Long) e[2] < now) {
                    DEATH_LINES.remove(i--);
                    continue;
                }
                if (e[0].equals(p) && e[3] == null) {
                    e[3] = Boolean.TRUE;
                    return (Txt) e[1];
                }
            }
            return null;
        }, null);
    }

    /** The death screen shows a vanilla death message: its custom replacement, or null */
    public static Txt deathScreenReplacement(String plain) {
        return Safe.get("death message", () -> {
            if (plain == null) return null;
            String p = Fmt.strip(plain).trim();
            long now = System.currentTimeMillis();
            for (Object[] e : DEATH_LINES) if ((Long) e[2] >= now && e[0].equals(p)) return (Txt) e[1];
            return null;
        }, null);
    }

    static void onDeathMessage(Packets.Recap msg) {
        Snapshot s = msg.snapshot;
        if (s == null || s.key() == null || s.deathMessage.isEmpty()) return;
        // "name\nchoice": the server decided (deathMessages template or a hidden death message); older form: name only
        int nl = msg.who.indexOf('\n');
        String who = nl < 0 ? msg.who : msg.who.substring(0, nl);
        String tpl = nl < 0 ? DeathMessages.find(packDeathMessages(), s.key()) : msg.who.substring(nl + 1);
        if (tpl == null || tpl.isEmpty()) return;
        Txt t = DeathMessages.render(formatter(), tpl, s.key(), who, s.deathMessage);
        String plain = Fmt.strip(Msg.LOCAL.lang().jsonText(s.deathMessage)).trim();
        if (plain.isEmpty()) return;
        if (DEATH_LINES.size() > 20) DEATH_LINES.remove(0);
        DEATH_LINES.add(new Object[] {plain, t, System.currentTimeMillis() + 10000L, null});
    }

    public static boolean on(Flag f) {
        return options().on(f);
    }

    public static RecapFormatter formatter() {
        return new RecapFormatter(Msg.LOCAL, options());
    }

    public static void print(Txt c) {
        if (port != null) port.print(c);
    }

    public static void print(List<Txt> lines) {
        for (Txt c : lines) print(c);
    }

    static void register(Snapshot s) {
        if (s == null) return;
        // ids hidden by a progression rule stay out of the local catalog (completion, local search)
        for (DamageRecord r : s.records) if (RecapFormatter.idShown(r)) CATALOG.register(r);
    }

    // ------------------------------------------------------------ ticks / events from the loader

    /**
     * End of a client tick while the own player and world exist.
     *
     * @param env the own player for the client fallback (only read when the server does not have the mod)
     */
    public static void tick(FallbackEnv env) {
        Safe.run("client tick", () -> tickImpl(env));
    }

    private static void tickImpl(FallbackEnv env) {
        if (!helloSent && port.connected()) {
            helloSent = true;
            try {
                port.send(new Packets.Hello());
            } catch (Throwable ignored) {
                // a server that does not know the channel simply ignores it
            }
        }
        if (!serverHasMod && !port.isSingleplayer() && env != null) FallbackCore.INSTANCE.tick(env);
        if (pendingSummary != null) checkRespawn();
    }

    private static void checkRespawn() {
        if (port.playerDead()) {
            sawDead = true;
            return;
        }
        if (!sawDead) {
            // the recap arrived but no death followed (another mod saved the player)
            if (System.currentTimeMillis() - pendingSince > 10000) pendingSummary = null;
            return;
        }
        RecapFormatter f = formatter();
        if (f.o.on(Flag.RESPAWN_CHAT_SUMMARY)) print(f.respawnSummary(pendingSummary));
        pendingSummary = null;
    }

    /** The death screen opened (deathMessageJson: its message as JSON, "" when unknown) */
    public static void onDeathScreen(String deathMessageJson) {
        Safe.run("death screen", () -> {
            DeathPanel.INSTANCE.onOpen();
            FallbackCore.INSTANCE.onDeathScreen(deathMessageJson);
        });
    }

    // ------------------------------------------------------------ packets (call on the client thread)

    public static void onPacket(Packets.Packet p) {
        Safe.run("client packet", () -> packet(p));
    }

    private static void packet(Packets.Packet p) {
        if (p instanceof Packets.ServerInfo) onServerInfo((Packets.ServerInfo) p);
        else if (p instanceof Packets.Recap) onRecap((Packets.Recap) p);
        else if (p instanceof Packets.Entries) onEntries((Packets.Entries) p);
        else if (p instanceof Packets.Message) onMessage((Packets.Message) p);
    }

    static void onServerInfo(Packets.ServerInfo msg) {
        boolean first = !serverHasMod;
        Map<String, Template> t = new java.util.LinkedHashMap<>();
        for (String e : msg.templates) {
            int nl = e.indexOf('\n');
            String name = nl < 0 ? e : e.substring(0, nl);
            t.put(name, Template.of(nl < 0 ? new ArrayList<String>() : java.util.Arrays.asList(e.substring(nl + 1).split("\n", -1))));
        }
        serverTemplates = t;
        serverDeathMessages = WhmConfig.parseMap(msg.deathMessages.toArray(new String[0]));
        serverHasMod = true;
        serverInfo = msg;
        if (first) {
            FALLBACK.clear();
            FallbackCore.INSTANCE.reset();
        }
    }

    static void onDeath(Snapshot s) {
        register(s);
        currentDeath = s;
        pendingSummary = s;
        sawDead = false;
        pendingSince = System.currentTimeMillis();
        DeathPanel.INSTANCE.onNewData();
    }

    static void onRecap(Packets.Recap msg) {
        long now = System.currentTimeMillis();
        Snapshot s = msg.snapshot;
        register(s);
        RecapFormatter f = formatter();
        switch (msg.kind) {
            case Packets.Recap.DEATH:
                if (s != null) onDeath(s);
                break;
            case Packets.Recap.DEATH_MESSAGE:
                onDeathMessage(msg);
                break;
            case Packets.Recap.HISTORY:
                print(f.history(s == null ? new ArrayList<>() : s.records, now, WhmConfig.client.chatHistoryLines, msg.count, false, null));
                break;
            case Packets.Recap.LAST:
                if (s == null) print(Msg.LOCAL.tr(Fmt.GRAY, "whathitme.last.none"));
                else {
                    currentDeath = s;
                    print(f.fullRecap(s, now, null));
                }
                break;
            case Packets.Recap.ALERT:
                if (s == null) break;
                Flag fl = s.kind == Snapshot.HEAVY ? Flag.HEAVY_HIT_SNAPSHOT : Flag.TOTEM_CHAT_ALERT;
                if (f.o.on(fl)) print(f.alert(s, 1));
                break;
            case Packets.Recap.SNAPSHOT:
                if (s == null) print(Msg.LOCAL.tr(Fmt.GRAY, "whathitme.snapshot.none"));
                else {
                    print(f.fullRecap(s, now, null));
                    print(Msg.LOCAL.tr(Fmt.GRAY, "whathitme.snapshot.position", msg.index, msg.count));
                }
                break;
            case Packets.Recap.INSPECT_DEATH:
                if (s == null) print(Msg.LOCAL.tr(Fmt.GRAY, "whathitme.inspect.nodeath", msg.who));
                else print(new RecapFormatter(Msg.LOCAL, f.o.without(Flag.ACTION_BUTTONS)).fullRecap(s, now, msg.who));
                break;
            case Packets.Recap.INSPECT_HISTORY:
                // inspect shows another player's records: no [Copy ID] / [Mute] buttons there
                print(new RecapFormatter(Msg.LOCAL, f.o.without(Flag.ACTION_BUTTONS)).history(s == null ? new ArrayList<>() : s.records, now,
                    WhmConfig.client.chatHistoryLines, 0, false, msg.who));
                break;
            default:
                break;
        }
    }

    static void onEntries(Packets.Entries msg) {
        RecapFormatter f = formatter();
        switch (msg.kind) {
            case Packets.Entries.SEARCH:
                print(f.search(msg.query, msg.entries.size() > 30 ? msg.entries.subList(0, 30) : msg.entries, msg.total));
                break;
            case Packets.Entries.EFFECTS:
                print(f.effects(msg.entries, false));
                break;
            case Packets.Entries.DUMP:
                writeDump(msg.entries);
                break;
            default:
                break;
        }
    }

    static void onMessage(Packets.Message msg) {
        if (io.github.junjun1z.whathitme.core.pack.PackProto.KEY.equals(msg.key)) {
            io.github.junjun1z.whathitme.core.pack.PackClient.onChunk(msg);
            return;
        }
        print(Msg.LOCAL.tr(msg.error ? Fmt.RED : Fmt.GREEN, msg.key, (Object[]) msg.args));
    }

    /** Damage entries (from the server or the local catalog) + local mobs / effects + settings */
    static void writeDump(List<Entry> damage) {
        List<Entry> all = new ArrayList<>(damage);
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (Entry e : damage) seen.add(e.type + "|" + e.id);
        for (Entry e : SearchEngine.all(new DamageCatalog(), port.registries())) if (seen.add(e.type + "|" + e.id)) all.add(e);
        File file = new File(WhmConfig.cacheDir(), "whathitme_dump.csv");
        try {
            int n = DumpWriter.write(file, all, WhmConfig.common, WhmConfig.client);
            print(Msg.LOCAL.tr(Fmt.GREEN, "whathitme.dump.done", n, file.getAbsolutePath()));
            File tpl = new File(Platform.whmConfigDir(), "hint_template.cfg");
            int ids = DumpWriter.writeTemplate(tpl, all, CatalogCache.view(port.registries()));
            print(Msg.LOCAL.tr(Fmt.GREEN, "whathitme.dump.template", ids, tpl.getAbsolutePath()));
        } catch (Exception e) {
            print(Msg.LOCAL.tr(Fmt.RED, "whathitme.dump.failed", String.valueOf(e.getMessage())));
        }
    }

    /** Copies the current death recap as plain text; false when there is none */
    public static boolean copyRecap() {
        Snapshot s = currentDeath;
        if (s == null) return false;
        Snapshot withGear = s;
        if (s.gear == null && !serverHasMod) {
            io.github.junjun1z.whathitme.core.data.Gear g = Safe.get("local equipment", port::localGear, null);
            if (g != null) withGear = s.withGear(g);
        }
        port.setClipboard(new RecapFormatter(Msg.LOCAL, options()).copyText(withGear, System.currentTimeMillis(), Msg.LOCAL.lang()));
        return true;
    }

    // ------------------------------------------------------------ /whm on the client

    private static void request(int kind, String arg) {
        port.send(new Packets.Request(kind, arg));
    }

    /**
     * /whm on a client with the mod. Data that only the server knows is requested with a packet when the server has the
     * mod; otherwise the client fallback ([Client Est.]) is used. Personal settings go to whathitme-client.cfg.
     */
    public static void execute(String[] args) {
        try {
            run(args);
        } catch (CommandText.Failure f) {
            print(f.text);
        } catch (Throwable t) {
            Safe.warn("client command", t);
            print(Msg.LOCAL.tr(Fmt.RED, "whathitme.error.internal"));
        }
    }

    /** Operator (level 2) on the server, or single player with cheats */
    static boolean op() {
        return Safe.get("permission", port::hasOpPermission, false);
    }

    private static void requireOp(Msg m) {
        if (!op()) throw CommandText.err(m, "whathitme.error.noperm");
    }

    /** /whm config [key [value]]: personal keys for everyone, server / modpack keys for operators */
    private static void config(Msg m, String[] args) {
        WhmConfig.Client cfg = WhmConfig.client;
        if (args.length == 1) {
            if (Safe.get("config screen", port::openConfigScreen, false)) return;
            CommandText.config(CHAT, m, cfg, args);
            return;
        }
        boolean personal = cfg.byKey().containsKey(args[1]);
        if (personal) {
            if (args.length > 2 && locked(args[1])) throw CommandText.err(m, "whathitme.config.locked", args[1]);
            CommandText.config(CHAT, m, cfg, args);
            return;
        }
        if (!WhmConfig.common.byKey().containsKey(args[1])) throw CommandText.err(m, "whathitme.config.unknown", args[1]);
        if (args.length == 2) {
            CommandText.config(CHAT, m, WhmConfig.common, args);
            return;
        }
        requireOp(m);
        if (locked(args[1])) throw CommandText.err(m, "whathitme.config.locked", args[1]);
        if (serverHasMod) port.sendServerCommand("whm " + Completion.join(args, 0));
        else CommandText.config(CHAT, m, WhmConfig.common, args);
    }

    private static void run(String[] args) {
        Msg m = Msg.LOCAL;
        RecapFormatter f = formatter();
        String sub = args.length == 0 ? "history" : args[0].toLowerCase(Locale.ROOT);
        String rest = args.length > 2 ? Completion.join(args, 2) : "";
        long now = System.currentTimeMillis();
        WhmConfig.Client cfg = WhmConfig.client;
        boolean server = serverHasMod;
        switch (sub) {
            case "history": {
                if (server) request(Packets.Request.HISTORY, "");
                else print(f.history(FALLBACK.records(), now, cfg.chatHistoryLines, FALLBACK.snapshots().size(), true, null));
                break;
            }
            case "last": {
                if (server) {
                    request(Packets.Request.LAST, "");
                } else {
                    Snapshot s = FALLBACK.lastDeath();
                    if (s == null) print(m.tr(Fmt.GRAY, "whathitme.last.none"));
                    else print(f.fullRecap(s, now, null));
                }
                break;
            }
            case "snapshot": {
                int n = args.length > 1 ? Completion.parseInt(args[1], 1) : 1;
                if (server) {
                    request(Packets.Request.SNAPSHOT, String.valueOf(n));
                } else {
                    List<Snapshot> list = FALLBACK.snapshots();
                    if (n < 1 || n > list.size()) print(m.tr(Fmt.GRAY, "whathitme.snapshot.none"));
                    else {
                        print(f.fullRecap(list.get(n - 1), now, null));
                        print(m.tr(Fmt.GRAY, "whathitme.snapshot.position", n, list.size()));
                    }
                }
                break;
            }
            case "effects": {
                if (server) request(Packets.Request.EFFECTS, "");
                else print(f.effects(port.localEffects(), true));
                break;
            }
            case "togglechat": {
                cfg.set(Flag.RESPAWN_CHAT_SUMMARY.key, "toggle");
                boolean on = (cfg.mask & Flag.RESPAWN_CHAT_SUMMARY.bit()) != 0;
                print(m.tr(Fmt.YELLOW, on ? "whathitme.togglechat.on" : "whathitme.togglechat.off"));
                break;
            }
            case "ignore": {
                if (args.length < 2) throw CommandText.usage(m, "/whm ignore <damage_id>");
                boolean ign = cfg.toggleIgnore(args[1]);
                print(m.tr(Fmt.YELLOW, ign ? "whathitme.ignore.added" : "whathitme.ignore.removed", args[1]));
                break;
            }
            case "rename":
            case "sethint": {
                requireOp(m);
                if (args.length < 2) throw CommandText.usage(m, "/whm " + sub + " <damage_id> <text...>");
                boolean rn = sub.equals("rename");
                cfg.putMapEntry(rn ? "renameMap" : "hintMap", args[1], rest);
                if (rest.isEmpty()) print(m.tr(Fmt.YELLOW, rn ? "whathitme.rename.cleared" : "whathitme.sethint.cleared", args[1]));
                else print(m.tr(Fmt.YELLOW, rn ? "whathitme.rename.set" : "whathitme.sethint.set", args[1], rest.replace('&', '§')));
                break;
            }
            case "config":
                config(m, args);
                break;
            case "stats": {
                if (server) port.sendServerCommand("whm stats");
                else print(CommandText.stats(m, f, Stats.of(LOCAL_ID), m.text("whathitme.stats.you")));
                break;
            }
            case "test": {
                requireOp(m);
                if (args.length < 2) throw CommandText.usage(m, TestPreview.USAGE);
                if (server) port.sendServerCommand("whm " + Completion.join(args, 0));
                else print(TestPreview.lines(m, f, args, null, "", null, "", 20f));
                break;
            }
            case "search": {
                if (args.length < 2) throw CommandText.usage(m, "/whm search <query | #tag | @modid>");
                String q = Completion.join(args, 1);
                if (server) {
                    request(Packets.Request.SEARCH, q);
                } else {
                    List<Entry> all = SearchEngine.search(q, CATALOG, port.registries());
                    print(f.search(q, all.size() > 30 ? all.subList(0, 30) : all, all.size()));
                }
                break;
            }
            case "inspect": {
                if (args.length < 2) throw CommandText.usage(m, "/whm inspect <player>");
                if (!server) throw CommandText.err(m, "whathitme.error.needserver");
                request(Packets.Request.INSPECT, args[1]);
                break;
            }
            case "dump": {
                requireOp(m);
                if (server) request(Packets.Request.DUMP, "");
                else writeDump(CATALOG.entries());
                break;
            }
            case "reload": {
                requireOp(m);
                WhmConfig.reloadCommon();
                WhmConfig.reloadClient();
                if (!server) CatalogCache.rescan(port.registries());
                print(m.tr(Fmt.GREEN, "whathitme.reload.done"));
                for (String w : WhmConfig.client.templateWarnings) print(m.tr(Fmt.GOLD, "whathitme.reload.warning", w));
                if (!server) for (String w : WhmConfig.pack.warnings) print(m.tr(Fmt.GOLD, "whathitme.reload.warning", w));
                if (server && (port.isSingleplayer() || port.hasOpPermission())) request(Packets.Request.RELOAD, "");
                break;
            }
            case "copy": {
                if (args.length < 2) throw CommandText.usage(m, "/whm copy <text>");
                String text = Completion.join(args, 1);
                port.setClipboard(text);
                print(m.tr(Fmt.GREEN, "whathitme.copy.done", text));
                break;
            }
            case "copyrecap": {
                if (!copyRecap()) {
                    print(m.tr(Fmt.GRAY, "whathitme.last.none"));
                    break;
                }
                print(m.tr(Fmt.GREEN, "whathitme.copyrecap.done"));
                break;
            }
            case "server": {
                if (!server) throw CommandText.err(m, "whathitme.error.needserver");
                port.sendServerCommand("whm " + (args.length > 1 ? Completion.join(args, 1) : "help"));
                break;
            }
            case "template":
                template(m, f, args, now);
                break;
            case "pack":
                pack(m, args);
                break;
            case "help":
                CommandText.help(CHAT, m, true);
                break;
            default:
                print(m.tr(Fmt.RED, "whathitme.error.unknown", sub));
                CommandText.help(CHAT, m, true);
        }
    }

    /** The latest death recap this client knows (server or estimate), null = none */
    public static Snapshot latestDeath() {
        if (currentDeath != null) return currentDeath;
        return serverHasMod ? null : FALLBACK.lastDeath();
    }

    /** /whm pack [personal]: the pack editor (operators, server with the mod) or the personal lists editor */
    private static void pack(Msg m, String[] args) {
        boolean personal = args.length > 1 && args[1].equalsIgnoreCase("personal");
        if (!personal) {
            if (!serverHasMod) throw CommandText.err(m, "whathitme.pack.needserver");
            if (!op()) throw CommandText.err(m, "whathitme.pack.noperm");
        }
        if (!Safe.get("pack editor", () -> port.openPackEditor(personal), false)) throw CommandText.err(m, "whathitme.pack.unavailable");
    }

    /** /whm template [preview &lt;output&gt; | reset [output]] */
    private static void template(Msg m, RecapFormatter f, String[] args, long now) {
        String usage = "/whm template [preview <" + String.join("|", Template.OUTPUTS) + "> | reset [output]]";
        if (args.length == 1) {
            if (Safe.get("template editor", port::openTemplateEditor, false)) return;
            throw CommandText.usage(m, usage);
        }
        String a = args[1].toLowerCase(Locale.ROOT);
        if (a.equals("preview") && args.length >= 3) {
            print(io.github.junjun1z.whathitme.core.cmd.TemplatePreview.lines(f, args[2], latestDeath(), now));
            return;
        }
        if (a.equals("reset")) {
            try {
                if (args.length >= 3) {
                    String out = args[2].toLowerCase(Locale.ROOT);
                    if (!Template.isOutput(out)) throw CommandText.err(m, "whathitme.template.unknown", args[2], String.join(", ", Template.OUTPUTS));
                    if (Templates.locked(out, ClientCore::locked)) throw CommandText.err(m, "whathitme.config.locked", "templates." + out);
                    Templates.savePersonal(out, null);
                } else {
                    Templates.resetPersonal();
                }
            } catch (java.io.IOException e) {
                throw CommandText.err(m, "whathitme.template.savefailed", String.valueOf(e.getMessage()));
            }
            WhmConfig.reloadClient();
            print(m.tr(Fmt.GREEN, "whathitme.template.resetdone"));
            return;
        }
        throw CommandText.usage(m, usage);
    }

    /** Tab completion of the client command */
    public static List<String> complete(String[] args) {
        try {
            return completeImpl(args);
        } catch (Throwable t) {
            Safe.warn("client tab completion", t);
            return Collections.emptyList();
        }
    }

    private static List<String> completeImpl(String[] args) {
        if (args.length <= 1) return Completion.matching(args, CommandText.CLIENT_SUBS);
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            switch (sub) {
                case "ignore":
                case "rename":
                case "sethint":
                case "test":
                case "copy": {
                    List<String> ids = new ArrayList<>(CATALOG.ids());
                    ids.addAll(WhmConfig.client.ignoredDamageIds);
                    ids.addAll(WhmConfig.client.renameMap.keySet());
                    if (sub.equals("test")) for (Entry e : CatalogCache.view(port.registries()).damageTypeEntries()) ids.add(e.id);
                    return Completion.matching(args, ids);
                }
                case "config": {
                    java.util.Set<String> keys = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                    keys.addAll(WhmConfig.client.keys());
                    if (op()) keys.addAll(WhmConfig.common.keys());
                    return Completion.matching(args, keys);
                }
                case "inspect":
                    return Completion.matching(args, port.onlinePlayerNames());
                case "snapshot":
                    return Completion.matching(args, "1", "2", "3", "4", "5");
                case "server":
                    return Completion.matching(args, CommandText.SERVER_FORWARD);
                case "template":
                    return Completion.matching(args, "preview", "reset");
                case "pack":
                    return Completion.matching(args, "personal");
                default:
                    break;
            }
        }
        if (sub.equals("test")) {
            List<String> extra = TestPreview.completeOptions(args, java.util.Collections.<String>emptyList());
            if (extra != null) return extra;
            return Completion.testArgs(args, CatalogCache.view(port.registries()));
        }
        if (sub.equals("search")) return Completion.matching(args, Completion.searchSuggestions(CATALOG.ids()));
        if (sub.equals("template") && args.length == 3) return Completion.matching(args, Template.OUTPUTS);

        if (sub.equals("config") && args.length == 3) {
            WhmConfig.Base b = WhmConfig.client.byKey().containsKey(args[1]) ? WhmConfig.client : WhmConfig.common;
            return Completion.matching(args, b.values(args[1]));
        }
        return Collections.emptyList();
    }
}
