package io.github.junjun1z.whathitme.core.client;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import io.github.junjun1z.whathitme.core.Platform;
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
        FALLBACK.clear();
        FallbackCore.INSTANCE.reset();
    }

    public static DisplayOptions options() {
        WhmConfig.Common c = WhmConfig.common;
        WhmConfig.Client cl = WhmConfig.client;
        Packets.ServerInfo i = serverHasMod ? serverInfo : null;
        int mask = (i != null ? i.mask : c.mask) & cl.mask;
        DisplayOptions o = new DisplayOptions(mask, true, i != null ? i.summarySeconds : c.summarySeconds);
        if (i != null) {
            o.ignore(i.ignored).renames(WhmConfig.parseMap(i.renames.toArray(new String[0]))).hints(WhmConfig.parseMap(i.hints.toArray(new String[0])));
        } else {
            o.ignore(c.ignoredDamageIds).renames(c.renameMap).hints(c.hintMap);
        }
        return o.ignore(cl.ignoredDamageIds).renames(cl.renameMap).hints(cl.hintMap);
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
        for (DamageRecord r : s.records) CATALOG.register(r);
    }

    // ------------------------------------------------------------ ticks / events from the loader

    /**
     * End of a client tick while the own player and world exist.
     *
     * @param env the own player for the client fallback (only read when the server does not have the mod)
     */
    public static void tick(FallbackEnv env) {
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
        DeathPanel.INSTANCE.onOpen();
        FallbackCore.INSTANCE.onDeathScreen(deathMessageJson);
    }

    // ------------------------------------------------------------ packets (call on the client thread)

    public static void onPacket(Packets.Packet p) {
        if (p instanceof Packets.ServerInfo) onServerInfo((Packets.ServerInfo) p);
        else if (p instanceof Packets.Recap) onRecap((Packets.Recap) p);
        else if (p instanceof Packets.Entries) onEntries((Packets.Entries) p);
        else if (p instanceof Packets.Message) onMessage((Packets.Message) p);
    }

    static void onServerInfo(Packets.ServerInfo msg) {
        boolean first = !serverHasMod;
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
        print(Msg.LOCAL.tr(msg.error ? Fmt.RED : Fmt.GREEN, msg.key, (Object[]) msg.args));
    }

    /** Damage entries (from the server or the local catalog) + local mobs / effects + settings */
    static void writeDump(List<Entry> damage) {
        List<Entry> all = new ArrayList<>(damage);
        all.addAll(SearchEngine.all(new DamageCatalog(), port.registries()));
        File file = new File(port.gameDir(), "whathitme_dump.csv");
        try {
            int n = DumpWriter.write(file, all, WhmConfig.common, WhmConfig.client);
            print(Msg.LOCAL.tr(Fmt.GREEN, "whathitme.dump.done", n, file.getAbsolutePath()));
            File tpl = new File(Platform.whmConfigDir(), "hint_template.cfg");
            int ids = DumpWriter.writeTemplate(tpl, all, port.registries());
            print(Msg.LOCAL.tr(Fmt.GREEN, "whathitme.dump.template", ids, tpl.getAbsolutePath()));
        } catch (Exception e) {
            print(Msg.LOCAL.tr(Fmt.RED, "whathitme.dump.failed", String.valueOf(e.getMessage())));
        }
    }

    /** Copies the current death recap as plain text; false when there is none */
    public static boolean copyRecap() {
        Snapshot s = currentDeath;
        if (s == null) return false;
        port.setClipboard(RecapFormatter.plain(new RecapFormatter(Msg.LOCAL, options()).fullRecap(s, System.currentTimeMillis(), null), Msg.LOCAL.lang()));
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
        }
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
                if (args.length < 2) throw CommandText.usage(m, "/whm " + sub + " <damage_id> <text...>");
                boolean rn = sub.equals("rename");
                cfg.putMapEntry(rn ? "renameMap" : "hintMap", args[1], rest);
                if (rest.isEmpty()) print(m.tr(Fmt.YELLOW, rn ? "whathitme.rename.cleared" : "whathitme.sethint.cleared", args[1]));
                else print(m.tr(Fmt.YELLOW, rn ? "whathitme.rename.set" : "whathitme.sethint.set", args[1], rest.replace('&', '§')));
                break;
            }
            case "config":
                CommandText.config(CHAT, m, cfg, args);
                break;
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
                if (server) request(Packets.Request.DUMP, "");
                else writeDump(CATALOG.entries());
                break;
            }
            case "reload": {
                WhmConfig.reloadCommon();
                WhmConfig.reloadClient();
                print(m.tr(Fmt.GREEN, "whathitme.reload.done"));
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
            case "help":
                CommandText.help(CHAT, m, true);
                break;
            default:
                print(m.tr(Fmt.RED, "whathitme.error.unknown", sub));
                CommandText.help(CHAT, m, true);
        }
    }

    /** Tab completion of the client command */
    public static List<String> complete(String[] args) {
        if (args.length <= 1) return Completion.matching(args, CommandText.CLIENT_SUBS);
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            switch (sub) {
                case "ignore":
                case "rename":
                case "sethint":
                case "copy": {
                    List<String> ids = new ArrayList<>(CATALOG.ids());
                    ids.addAll(WhmConfig.client.ignoredDamageIds);
                    ids.addAll(WhmConfig.client.renameMap.keySet());
                    return Completion.matching(args, ids);
                }
                case "config":
                    return Completion.matching(args, WhmConfig.client.keys());
                case "inspect":
                    return Completion.matching(args, port.onlinePlayerNames());
                case "snapshot":
                    return Completion.matching(args, "1", "2", "3", "4", "5");
                case "server":
                    return Completion.matching(args, CommandText.SERVER_FORWARD);
                default:
                    break;
            }
        }
        if (sub.equals("search")) return Completion.matching(args, Completion.searchSuggestions(CATALOG.ids()));
        if (sub.equals("config") && args.length == 3) return Completion.matching(args, WhmConfig.client.values(args[1]));
        return Collections.emptyList();
    }
}
