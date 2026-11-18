package io.github.junjun1z.whathitme.core.pack;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.Safe;
import io.github.junjun1z.whathitme.core.config.Setting;
import io.github.junjun1z.whathitme.core.config.WhmConfig;
import io.github.junjun1z.whathitme.core.data.Entry;
import io.github.junjun1z.whathitme.core.data.Json;
import io.github.junjun1z.whathitme.core.data.RuleMark;
import io.github.junjun1z.whathitme.core.data.SearchEngine;
import io.github.junjun1z.whathitme.core.net.Packets;
import io.github.junjun1z.whathitme.core.rules.Conditions;
import io.github.junjun1z.whathitme.core.script.ScriptRegistry;
import io.github.junjun1z.whathitme.core.server.CommandSource;
import io.github.junjun1z.whathitme.core.server.PlayerHandle;
import io.github.junjun1z.whathitme.core.server.ServerCore;
import io.github.junjun1z.whathitme.core.server.ServerPort;
import io.github.junjun1z.whathitme.core.text.Template;
import io.github.junjun1z.whathitme.core.text.Txt;
import io.github.junjun1z.whathitme.core.text.TxtCodec;

/**
 * The server side of the pack editor (/whm pack): validates every edit of an operator, writes the pack JSON files
 * (backup first, undo per editor), reloads the pack at once and sends the new state to every open editor. Nothing
 * from the client is trusted: permission, paths (inside the pack folder only), sections, value types and sizes are
 * checked here.
 */
public final class PackStore {

    /** Sections holding "key": value maps (templates: output -> lines) */
    public static final List<String> MAPS = Arrays.asList("renameMap", "hintMap", "deathMessages", "templates");
    /** Sections holding lists (rules / deathTriggers: objects, the others: strings) */
    public static final List<String> LISTS = Arrays.asList("rules", "deathTriggers", "defaultIgnoredDamageIds", "lockedFeatures");

    static final int MAX_FILE_CHARS = 512 * 1024;
    static final int MAX_STRING = 8000;
    static final int MAX_KEY = 600;
    static final int MAX_FILES = 500;
    static final int MAX_ENTRIES = 5000;
    static final int MAX_UNDO = 30;
    static final int MAX_BACKUPS = 60;

    /** A refused edit: translation key + arguments for the editor's status line */
    static final class Fail extends RuntimeException {
        final String key;
        final String[] args;

        Fail(String key, String... args) {
            super(key, null, false, false);
            this.key = key;
            this.args = args;
        }
    }

    /** What one edit changed, for undo */
    static final class Batch {
        final Map<String, byte[]> prev = new LinkedHashMap<>();
        final List<String> dirsCreated = new ArrayList<>();
        final List<String> dirsRemoved = new ArrayList<>();
        String settingKey;
        String settingPrev;
        /** Files changed (for the notice of other editors) */
        final Set<String> touched = new java.util.LinkedHashSet<>();
        final String backupId = new SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.ROOT).format(new Date());
        boolean overwrote;
    }

    private static final Map<UUID, PackProto.Assembler> ASM = new HashMap<>();
    /** Open editors: uuid -> player name */
    private static final Map<UUID, String> EDITORS = new LinkedHashMap<>();
    private static final Map<UUID, Deque<Batch>> UNDO = new HashMap<>();
    private static long rev;

    private PackStore() {}

    /** Server stopped */
    public static synchronized void reset() {
        ASM.clear();
        EDITORS.clear();
        UNDO.clear();
    }

    public static synchronized void onLogout(UUID id) {
        ASM.remove(id);
        EDITORS.remove(id);
    }

    /** After /whm reload (hand edits on disk): every open editor gets the new state */
    public static synchronized void refreshEditors() {
        if (EDITORS.isEmpty()) return;
        rev++;
        ServerPort srv = ServerCore.port;
        for (UUID id : new ArrayList<>(EDITORS.keySet())) {
            PlayerHandle to = srv == null ? null : srv.player(id);
            if (to == null) {
                EDITORS.remove(id);
                continue;
            }
            final Map<String, Object> st = state(to, null);
            Safe.run("pack editor state", () -> send(to, st));
        }
    }

    /** Number of open editors (self-test) */
    public static synchronized int editors() {
        return EDITORS.size();
    }

    // ------------------------------------------------------------ messages

    /** One Request PACK chunk from a client (server thread) */
    public static void onRequest(PlayerHandle p, String arg) {
        Map<String, Object> msg;
        synchronized (PackStore.class) {
            PackProto.Assembler a = ASM.computeIfAbsent(p.uuid(), k -> new PackProto.Assembler(PackProto.MAX_UP));
            try {
                msg = a.addUp(arg);
            } catch (RuntimeException e) {
                ASM.remove(p.uuid());
                send(p, result(false, "whathitme.pack.err.bad", String.valueOf(e.getMessage())));
                return;
            }
        }
        if (msg != null) handle(p, msg);
    }

    static void send(PlayerHandle p, Map<String, Object> msg) {
        for (Packets.Packet pk : PackProto.down(msg)) p.send(pk);
    }

    static Map<String, Object> result(boolean ok, String key, String... args) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "result");
        m.put("ok", ok);
        m.put("key", key);
        m.put("args", new ArrayList<Object>(Arrays.asList((Object[]) args)));
        return m;
    }

    static synchronized void handle(PlayerHandle p, Map<String, Object> op) {
        String o = Json.str(op.get("op"), "");
        String req = Json.str(op.get("req"), "");
        if (!Safe.get("permission", p::isAdmin, false)) {
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("type", "denied");
            send(p, d);
            Platform.log.info("[What Hit Me?] pack editor: " + p.name() + " has no permission (" + o + ")");
            return;
        }
        ServerPort srv = ServerCore.port;
        try {
            switch (o) {
                case "open":
                    EDITORS.put(p.uuid(), p.name());
                    send(p, lists(srv));
                    send(p, state(p, null));
                    return;
                case "close":
                    EDITORS.remove(p.uuid());
                    return;
                case "state":
                    send(p, state(p, null));
                    return;
                case "test":
                    send(p, test(p, Json.str(op.get("args"), "")));
                    return;
                case "undo": {
                    Deque<Batch> st = UNDO.get(p.uuid());
                    if (st == null || st.isEmpty()) throw new Fail("whathitme.pack.err.noundo");
                    Batch b = st.pop();
                    undo(b);
                    changed(p, b, req, "whathitme.pack.undone");
                    return;
                }
                default: {
                    Batch b = new Batch();
                    String done = apply(p, o, op, b);
                    Deque<Batch> st = UNDO.computeIfAbsent(p.uuid(), k -> new ArrayDeque<>());
                    st.push(b);
                    while (st.size() > MAX_UNDO) st.removeLast();
                    changed(p, b, req, b.overwrote ? "whathitme.pack.overwrote" : done);
                }
            }
        } catch (Fail f) {
            Map<String, Object> r = result(false, f.key, f.args);
            r.put("req", req);
            send(p, r);
        } catch (Exception e) {
            Safe.warn("pack editor", e);
            Map<String, Object> r = result(false, "whathitme.pack.err.io", String.valueOf(e.getMessage()));
            r.put("req", req);
            send(p, r);
        }
    }

    /** After a change: reload the pack, tell all clients, send the new state to every open editor */
    private static void changed(PlayerHandle p, Batch b, String req, String doneKey) {
        WhmConfig.reloadCommon();
        ServerCore.broadcastInfo();
        rev++;
        Platform.log.info("[What Hit Me?] pack editor: " + p.name() + " changed " + (b.touched.isEmpty() ? "-" : String.join(", ", b.touched)));
        Map<String, Object> r = result(true, doneKey, String.join(", ", b.touched));
        r.put("req", req);
        send(p, r);
        ServerPort srv = ServerCore.port;
        for (Map.Entry<UUID, String> e : new ArrayList<>(EDITORS.entrySet())) {
            PlayerHandle to = e.getKey().equals(p.uuid()) ? p : srv == null ? null : srv.player(e.getKey());
            if (to == null) {
                EDITORS.remove(e.getKey());
                continue;
            }
            Map<String, Object> notice = null;
            if (!e.getKey().equals(p.uuid())) {
                notice = new LinkedHashMap<>();
                notice.put("who", p.name());
                notice.put("files", new ArrayList<Object>(b.touched));
            }
            final Map<String, Object> st = state(to, notice);
            Safe.run("pack editor state", () -> send(to, st));
        }
    }

    // ------------------------------------------------------------ state

    static Map<String, Object> state(PlayerHandle p, Map<String, Object> notice) {
        File dir = PackFiles.dir();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "state");
        m.put("rev", rev);
        List<Object> files = new ArrayList<>();
        for (PackFiles.Cat c : PackFiles.scan(dir)) {
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("path", c.path);
            f.put("order", c.order);
            f.put("enabled", c.enabled);
            f.put("memo", c.memo());
            if (c.root != null) f.put("content", c.root);
            if (c.error != null) f.put("error", c.error);
            files.add(f);
        }
        m.put("files", files);
        m.put("dirs", new ArrayList<Object>(PackFiles.dirs(dir)));
        List<Object> settings = new ArrayList<>();
        WhmConfig.Common c = WhmConfig.common;
        if (c != null) {
            for (Setting s : c.settings()) {
                Map<String, Object> e = new LinkedHashMap<>();
                e.put("key", s.key);
                e.put("cat", s.category);
                e.put("type", s.type.name());
                e.put("value", String.valueOf(c.get(s.key)));
                e.put("def", s.def);
                e.put("min", s.min);
                e.put("max", s.max);
                if (s.valid != null) e.put("valid", new ArrayList<Object>(Arrays.asList((Object[]) s.valid)));
                e.put("comment", s.comment);
                settings.add(e);
            }
        }
        m.put("settings", settings);
        List<Object> scripts = new ArrayList<>();
        for (Object[] r : Safe.get("script entries", ScriptRegistry::packRoots, new ArrayList<Object[]>())) {
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("owner", r[0]);
            e.put("content", r[1]);
            scripts.add(e);
        }
        m.put("scripts", scripts);
        List<Object> warn = new ArrayList<Object>(WhmConfig.pack.warnings);
        warn.addAll(Safe.get("rule warnings", () -> ServerCore.ruleWarnings(ServerCore.port), new ArrayList<String>()));
        m.put("warnings", warn);
        m.put("errors", new ArrayList<Object>(WhmConfig.pack.errors));
        m.put("loaded", new ArrayList<Object>(WhmConfig.pack.files));
        Deque<Batch> st = p == null ? null : UNDO.get(p.uuid());
        m.put("canUndo", st != null && !st.isEmpty());
        List<Object> others = new ArrayList<>();
        for (Map.Entry<UUID, String> e : EDITORS.entrySet()) if (p == null || !e.getKey().equals(p.uuid())) others.add(e.getValue());
        m.put("editors", others);
        if (notice != null) m.put("notice", notice);
        return m;
    }

    /** Helper lists for the editor: damage ids, mobs, condition values, players */
    static Map<String, Object> lists(ServerPort srv) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "lists");
        List<Object> damage = new ArrayList<>();
        List<Object> mobs = new ArrayList<>();
        if (srv != null) {
            List<Entry> all = Safe.get("catalog", () -> SearchEngine.all(ServerCore.tracker().catalog, srv.registries()), new ArrayList<Entry>());
            for (Entry e : all) {
                List<Object> row = new ArrayList<Object>(Arrays.asList(e.id, e.name == null ? "" : e.name, e.modName == null ? "" : e.modName));
                if (e.type == Entry.DAMAGE && damage.size() < MAX_ENTRIES) damage.add(row);
                else if (e.type == Entry.MOB && mobs.size() < MAX_ENTRIES) mobs.add(row);
            }
        }
        m.put("damage", damage);
        m.put("mobs", mobs);
        Map<String, Object> conds = new LinkedHashMap<>();
        Map<String, Object> avail = new LinkedHashMap<>();
        for (String pre : Conditions.prefixes()) {
            List<Object> vals = new ArrayList<>();
            for (String v : Conditions.suggestions(pre, srv)) if (vals.size() < MAX_ENTRIES) vals.add(v);
            conds.put(pre, vals);
            String problem = Conditions.problem(pre, srv);
            avail.put(pre, problem == null ? "" : problem);
        }
        m.put("conds", conds);
        m.put("condProblems", avail);
        m.put("players", new ArrayList<Object>(srv == null ? new ArrayList<String>() : Safe.get("players", srv::onlinePlayerNames, new ArrayList<String>())));
        m.put("hide", new ArrayList<Object>(Arrays.asList((Object[]) RuleMark.HIDE_NAMES)));
        return m;
    }

    /** /whm test with the given arguments, run as this operator; the lines as Txt JSON */
    static Map<String, Object> test(PlayerHandle p, String args) {
        List<Object> lines = new ArrayList<>();
        final PlayerHandle who = p;
        String[] a = io.github.junjun1z.whathitme.core.cmd.Completion.args(args.length() > 2000 ? args.substring(0, 2000) : args);
        String[] full = new String[a.length + 1];
        full[0] = "test";
        System.arraycopy(a, 0, full, 1, a.length);
        ServerCore.execute(new CommandSource() {
            @Override
            public PlayerHandle player() {
                return who;
            }

            @Override
            public boolean isAdmin() {
                return true;
            }

            @Override
            public void send(Txt line) {
                if (lines.size() < 200) lines.add(TxtCodec.write(line));
            }
        }, full);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "test");
        m.put("args", args);
        m.put("lines", lines);
        return m;
    }

    // ------------------------------------------------------------ validation

    /** A relative path inside the pack folder (file: ends with .json) */
    static String cleanPath(Object v, boolean file) {
        String s = Json.str(v, "").trim().replace('\\', '/');
        if (s.isEmpty() || s.length() > 160 || s.startsWith("/") || s.indexOf(':') >= 0 || s.contains("//") || s.endsWith("/")) {
            throw new Fail("whathitme.pack.err.path", s);
        }
        String[] segs = s.split("/");
        if (segs.length > 6) throw new Fail("whathitme.pack.err.path", s);
        for (String seg : segs) {
            if (seg.isEmpty() || seg.startsWith(".") || seg.endsWith(".") && !seg.endsWith(".json") || !seg.matches("[A-Za-z0-9 _\\-.()]+") || seg.trim().length() != seg.length()) {
                throw new Fail("whathitme.pack.err.path", s);
            }
        }
        boolean json = s.toLowerCase(Locale.ROOT).endsWith(".json");
        if (file != json) throw new Fail("whathitme.pack.err.path", s);
        try {
            File base = PackFiles.dir().getCanonicalFile();
            File f = new File(base, s).getCanonicalFile();
            if (!f.getPath().startsWith(base.getPath() + File.separator)) throw new Fail("whathitme.pack.err.path", s);
        } catch (IOException e) {
            throw new Fail("whathitme.pack.err.path", s);
        }
        return s;
    }

    private static String text(Object v, int max, boolean allowEmpty) {
        if (v != null && !(v instanceof String) && !(v instanceof Number) && !(v instanceof Boolean)) throw new Fail("whathitme.pack.err.type");
        String s = Json.str(v, "");
        if (s.length() > max) throw new Fail("whathitme.pack.err.toolong", String.valueOf(max));
        if (!allowEmpty && s.trim().isEmpty()) throw new Fail("whathitme.pack.err.empty");
        return s;
    }

    /** A rule / death trigger object: string, number, boolean or string list values only */
    private static Map<String, Object> entryObject(Object v, String section) {
        if (!(v instanceof Map)) throw new Fail("whathitme.pack.err.type");
        Map<String, Object> in = Json.obj(v);
        if (in.size() > 40) throw new Fail("whathitme.pack.err.type");
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : in.entrySet()) {
            String k = text(e.getKey(), 60, false);
            Object x = e.getValue();
            if (x instanceof List) {
                List<Object> l = new ArrayList<>();
                for (Object y : Json.list(x)) {
                    if (l.size() >= 200) throw new Fail("whathitme.pack.err.toolong", "200");
                    l.add(text(y, MAX_STRING, true));
                }
                out.put(k, l);
            } else if (x instanceof Boolean || x instanceof Number) {
                out.put(k, x);
            } else if (x == null) {
                continue;
            } else {
                out.put(k, text(x, MAX_STRING, true));
            }
        }
        String match = Json.str(out.get("match"), "").trim();
        if (section.equals("deathTriggers")) {
            if (match.isEmpty()) throw new Fail("whathitme.pack.err.nomatch");
            if (Json.str(out.get("command"), "").trim().isEmpty()) throw new Fail("whathitme.pack.err.nocommand");
        }
        return out;
    }

    private static Object entryValue(String section, Object v) {
        switch (section) {
            case "rules":
            case "deathTriggers":
                return entryObject(v, section);
            case "templates": {
                List<Object> lines = new ArrayList<>();
                List<Object> src = v instanceof String ? java.util.Collections.<Object>singletonList(v) : Json.list(v);
                if (src.size() > 100) throw new Fail("whathitme.pack.err.toolong", "100");
                for (Object o : src) lines.add(text(o, MAX_STRING, true));
                return lines;
            }
            case "defaultIgnoredDamageIds":
            case "lockedFeatures":
                return text(v, MAX_KEY, false).trim();
            default:
                return text(v, MAX_STRING, true);
        }
    }

    private static String section(Object v) {
        String s = Json.str(v, "");
        if (!MAPS.contains(s) && !LISTS.contains(s)) throw new Fail("whathitme.pack.err.section", s);
        return s;
    }

    // ------------------------------------------------------------ file access

    private static File file(String path) {
        return new File(PackFiles.dir(), path);
    }

    private static byte[] bytes(File f) throws IOException {
        return f.isFile() ? Files.readAllBytes(f.toPath()) : null;
    }

    private static File backupDir() {
        return new File(Platform.whmConfigDir(), ".backup");
    }

    /** Copies the current file into .backup/&lt;time&gt;/ once per batch and remembers it for undo */
    private static void remember(Batch b, String path) throws IOException {
        if (b.prev.containsKey(path)) return;
        byte[] old = bytes(file(path));
        b.prev.put(path, old);
        b.touched.add(path);
        if (old != null) {
            File bk = new File(new File(backupDir(), b.backupId), "pack/" + path);
            bk.getParentFile().mkdirs();
            Files.write(bk.toPath(), old);
            pruneBackups();
        }
    }

    private static void pruneBackups() {
        File[] list = backupDir().listFiles(File::isDirectory);
        if (list == null || list.length <= MAX_BACKUPS) return;
        Arrays.sort(list, (a, c) -> a.getName().compareTo(c.getName()));
        for (int i = 0; i < list.length - MAX_BACKUPS; i++) deleteTree(list[i]);
    }

    private static void deleteTree(File f) {
        File[] kids = f.listFiles();
        if (kids != null) for (File k : kids) deleteTree(k);
        f.delete();
    }

    private static void write(Batch b, String path, String text) throws IOException {
        if (text.length() > MAX_FILE_CHARS) throw new Fail("whathitme.pack.err.filesize", path);
        remember(b, path);
        File f = file(path);
        File parent = f.getParentFile();
        if (parent != null && !parent.isDirectory()) {
            mkdirs(b, parent);
        }
        Files.write(f.toPath(), text.getBytes(StandardCharsets.UTF_8));
    }

    private static void mkdirs(Batch b, File dir) {
        List<File> created = new ArrayList<>();
        File d = dir;
        File base = PackFiles.dir();
        while (d != null && !d.isDirectory() && !d.equals(base)) {
            created.add(0, d);
            d = d.getParentFile();
        }
        for (File c : created) {
            if (c.mkdir()) b.dirsCreated.add(PackFiles.rel(base, c));
        }
    }

    private static void delete(Batch b, String path) throws IOException {
        remember(b, path);
        Files.deleteIfExists(file(path).toPath());
    }

    /** Parsed root of an existing category (refused when the file has a syntax error) */
    private static Map<String, Object> load(String path) {
        File f = file(path);
        if (!f.isFile()) throw new Fail("whathitme.pack.err.nofile", path);
        PackFiles.Cat c = PackFiles.read(PackFiles.dir(), f);
        if (c.root == null) throw new Fail("whathitme.pack.err.parse", path, String.valueOf(c.error));
        return c.root;
    }

    private static void save(Batch b, String path, Map<String, Object> root) throws IOException {
        write(b, path, Json.write(root));
    }

    // ------------------------------------------------------------ sections

    /** The map of a section, converted from the "key = value" list form when needed */
    @SuppressWarnings("unchecked")
    static Map<String, Object> mapSection(Map<String, Object> root, String section) {
        Object v = root.get(section);
        if (v instanceof Map) return (Map<String, Object>) v;
        Map<String, Object> m = new LinkedHashMap<>();
        if (v instanceof List) {
            for (Object o : (List<Object>) v) {
                String s = Json.str(o, "");
                int eq = s.indexOf('=');
                if (eq > 0) m.put(s.substring(0, eq).trim(), s.substring(eq + 1).trim());
            }
        }
        root.put(section, m);
        return m;
    }

    @SuppressWarnings("unchecked")
    static List<Object> listSection(Map<String, Object> root, String section) {
        Object v = root.get(section);
        if (v instanceof List) return (List<Object>) v;
        List<Object> l = new ArrayList<>();
        if (v != null) l.add(v);
        root.put(section, l);
        return l;
    }

    private static boolean objectList(String section) {
        return section.equals("rules") || section.equals("deathTriggers");
    }

    /** Memo of a map / string-list entry */
    @SuppressWarnings("unchecked")
    static void setEntryMemo(Map<String, Object> root, String section, String key, String memo) {
        Object all = root.get(PackFiles.ENTRY_MEMOS);
        Map<String, Object> m = all instanceof Map ? (Map<String, Object>) all : new LinkedHashMap<>();
        Object sec = m.get(section);
        Map<String, Object> s = sec instanceof Map ? (Map<String, Object>) sec : new LinkedHashMap<>();
        if (memo == null || memo.trim().isEmpty()) s.remove(key);
        else s.put(key, memo);
        if (s.isEmpty()) m.remove(section);
        else m.put(section, s);
        if (m.isEmpty()) root.remove(PackFiles.ENTRY_MEMOS);
        else root.put(PackFiles.ENTRY_MEMOS, m);
    }

    static String entryMemo(Map<String, Object> root, String section, String key) {
        return Json.str(Json.obj(Json.obj(root.get(PackFiles.ENTRY_MEMOS)).get(section)).get(key), "");
    }

    /** Index of a list element: the one equal to expect (preferring index), else index itself ("last save wins") */
    private static int locate(List<Object> list, int index, Object expect, Batch b) {
        if (expect != null) {
            String e = Json.write(expect);
            if (index >= 0 && index < list.size() && Json.write(list.get(index)).equals(e)) return index;
            for (int i = 0; i < list.size(); i++) if (Json.write(list.get(i)).equals(e)) return i;
        }
        if (index >= 0 && index < list.size()) {
            if (expect != null) b.overwrote = true;
            return index;
        }
        throw new Fail("whathitme.pack.err.gone");
    }

    private static int num(Object v, int def) {
        return (int) Json.num(v, def);
    }

    /** Puts an entry into root (map: new key, possibly renamed from oldKey; list: at index or appended) */
    static void putEntry(Map<String, Object> root, String section, String oldKey, String key, int index, Object expect, Object value, String memo, Batch b) {
        if (MAPS.contains(section)) {
            key = text(key, MAX_KEY, false).trim();
            if (section.equals("templates")) {
                key = key.toLowerCase(Locale.ROOT);
                if (!Template.isOutput(key) && !Template.DEATH.equals(key)) throw new Fail("whathitme.pack.err.output", key);
            }
            Map<String, Object> m = mapSection(root, section);
            if ((oldKey == null || !oldKey.equals(key)) && m.containsKey(key)) throw new Fail("whathitme.pack.err.exists", key);
            if (oldKey != null && !m.containsKey(oldKey)) b.overwrote = true;
            if (m.size() >= MAX_ENTRIES && !m.containsKey(oldKey)) throw new Fail("whathitme.pack.err.toolong", String.valueOf(MAX_ENTRIES));
            Map<String, Object> rebuilt = new LinkedHashMap<>();
            boolean placed = false;
            for (Map.Entry<String, Object> e : m.entrySet()) {
                if (oldKey != null && e.getKey().equals(oldKey)) {
                    rebuilt.put(key, value);
                    placed = true;
                } else rebuilt.put(e.getKey(), e.getValue());
            }
            if (!placed) rebuilt.put(key, value);
            m.clear();
            m.putAll(rebuilt);
            if (oldKey != null) setEntryMemo(root, section, oldKey, null);
            setEntryMemo(root, section, key, memo);
            return;
        }
        List<Object> l = listSection(root, section);
        if (!objectList(section)) {
            String v = (String) value;
            for (int i = 0; i < l.size(); i++) {
                if (Json.str(l.get(i), "").equals(v) && (index < 0 || i != index)) throw new Fail("whathitme.pack.err.exists", v);
            }
        }
        if (index < 0) {
            if (l.size() >= MAX_ENTRIES) throw new Fail("whathitme.pack.err.toolong", String.valueOf(MAX_ENTRIES));
            l.add(value);
        } else {
            int i = locate(l, index, expect, b);
            if (!objectList(section)) setEntryMemo(root, section, Json.str(l.get(i), ""), null);
            l.set(i, value);
        }
        if (!objectList(section)) setEntryMemo(root, section, (String) value, memo);
    }

    /** Removes an entry; returns {value, memo} */
    static Object[] removeEntry(Map<String, Object> root, String section, String key, int index, Object expect, Batch b) {
        if (MAPS.contains(section)) {
            Map<String, Object> m = mapSection(root, section);
            if (key == null || !m.containsKey(key)) throw new Fail("whathitme.pack.err.gone");
            Object v = m.remove(key);
            String memo = entryMemo(root, section, key);
            setEntryMemo(root, section, key, null);
            if (m.isEmpty()) root.remove(section);
            return new Object[] {v, memo};
        }
        List<Object> l = listSection(root, section);
        int i = locate(l, index, expect, b);
        Object v = l.remove(i);
        String memo = "";
        if (!objectList(section)) {
            memo = entryMemo(root, section, Json.str(v, ""));
            setEntryMemo(root, section, Json.str(v, ""), null);
        }
        if (l.isEmpty()) root.remove(section);
        return new Object[] {v, memo};
    }

    // ------------------------------------------------------------ edits

    private static double nextOrder(List<PackFiles.Cat> cats) {
        double max = 0;
        boolean any = false;
        for (PackFiles.Cat c : cats) {
            if (c.order != 0) any = true;
            max = Math.max(max, c.order);
        }
        return any ? Math.floor(max / 10) * 10 + 10 : 0;
    }

    /** Applies one edit; returns the translation key of the confirmation */
    static String apply(PlayerHandle p, String o, Map<String, Object> op, Batch b) throws IOException {
        File dir = PackFiles.dir();
        switch (o) {
            case "entry.put": {
                String path = cleanPath(op.get("file"), true);
                String section = section(op.get("section"));
                Map<String, Object> root = load(path);
                Object value = entryValue(section, op.get("value"));
                String oldKey = op.get("oldKey") == null ? null : Json.str(op.get("oldKey"), "");
                putEntry(root, section, oldKey, Json.str(op.get("key"), ""), num(op.get("index"), -1), op.get("expect"), value,
                    text(op.get("memo"), MAX_STRING, true), b);
                save(b, path, root);
                return "whathitme.pack.saved";
            }
            case "entry.delete": {
                String path = cleanPath(op.get("file"), true);
                String section = section(op.get("section"));
                Map<String, Object> root = load(path);
                removeEntry(root, section, op.get("key") == null ? null : Json.str(op.get("key"), ""), num(op.get("index"), -1), op.get("expect"), b);
                save(b, path, root);
                return "whathitme.pack.deleted";
            }
            case "entry.move": {
                String path = cleanPath(op.get("file"), true);
                String section = section(op.get("section"));
                if (!LISTS.contains(section)) {
                    // map order: rebuild with the key moved by delta
                    Map<String, Object> root = load(path);
                    Map<String, Object> m = mapSection(root, section);
                    String key = Json.str(op.get("key"), "");
                    List<String> keys = new ArrayList<>(m.keySet());
                    int i = keys.indexOf(key);
                    if (i < 0) throw new Fail("whathitme.pack.err.gone");
                    int to = Math.max(0, Math.min(keys.size() - 1, num(op.get("to"), i)));
                    keys.remove(i);
                    keys.add(to, key);
                    Map<String, Object> copy = new LinkedHashMap<>(m);
                    m.clear();
                    for (String k : keys) m.put(k, copy.get(k));
                    save(b, path, root);
                    return "whathitme.pack.moved";
                }
                Map<String, Object> root = load(path);
                List<Object> l = listSection(root, section);
                int i = locate(l, num(op.get("index"), -1), op.get("expect"), b);
                int to = Math.max(0, Math.min(l.size() - 1, num(op.get("to"), i)));
                Object v = l.remove(i);
                l.add(to, v);
                save(b, path, root);
                return "whathitme.pack.moved";
            }
            case "entry.transfer": {
                String from = cleanPath(op.get("file"), true);
                String to = cleanPath(op.get("to"), true);
                String section = section(op.get("section"));
                if (from.equalsIgnoreCase(to)) throw new Fail("whathitme.pack.err.same");
                Map<String, Object> src = load(from);
                Map<String, Object> dst = load(to);
                String key = op.get("key") == null ? null : Json.str(op.get("key"), "");
                Object[] got = removeEntry(src, section, key, num(op.get("index"), -1), op.get("expect"), b);
                putEntry(dst, section, null, key == null ? "" : key, -1, null, got[0], (String) got[1], b);
                save(b, from, src);
                save(b, to, dst);
                return "whathitme.pack.transferred";
            }
            case "cat.create": {
                String path = cleanPath(op.get("path"), true);
                List<PackFiles.Cat> cats = PackFiles.scan(dir);
                if (cats.size() >= MAX_FILES) throw new Fail("whathitme.pack.err.toolong", String.valueOf(MAX_FILES));
                for (PackFiles.Cat c : cats) if (c.path.equalsIgnoreCase(path)) throw new Fail("whathitme.pack.err.exists", path);
                if (file(path).exists()) throw new Fail("whathitme.pack.err.exists", path);
                Map<String, Object> root = new LinkedHashMap<>();
                double order = nextOrder(cats);
                if (order != 0) root.put(PackFiles.ORDER, order);
                String memo = text(op.get("memo"), MAX_STRING, true);
                if (!memo.trim().isEmpty()) root.put(PackFiles.MEMO, memo);
                save(b, path, root);
                return "whathitme.pack.catcreated";
            }
            case "cat.rename": {
                String from = cleanPath(op.get("from"), true);
                String to = cleanPath(op.get("to"), true);
                File ff = file(from);
                if (!ff.isFile()) throw new Fail("whathitme.pack.err.nofile", from);
                if (from.equals(to)) throw new Fail("whathitme.pack.err.nochange");
                if (file(to).exists() && !from.equalsIgnoreCase(to)) throw new Fail("whathitme.pack.err.exists", to);
                byte[] data = bytes(ff);
                delete(b, from);
                remember(b, to);
                File tf = file(to);
                if (tf.getParentFile() != null && !tf.getParentFile().isDirectory()) mkdirs(b, tf.getParentFile());
                Files.write(tf.toPath(), data);
                return "whathitme.pack.catrenamed";
            }
            case "cat.delete": {
                String path = cleanPath(op.get("path"), true);
                if (!file(path).isFile()) throw new Fail("whathitme.pack.err.nofile", path);
                delete(b, path);
                return "whathitme.pack.catdeleted";
            }
            case "cat.order": {
                List<PackFiles.Cat> cats = PackFiles.scan(dir);
                Map<String, PackFiles.Cat> byPath = new HashMap<>();
                for (PackFiles.Cat c : cats) byPath.put(c.path, c);
                List<Object> want = Json.list(op.get("paths"));
                if (want.size() > MAX_FILES) throw new Fail("whathitme.pack.err.toolong", String.valueOf(MAX_FILES));
                List<PackFiles.Cat> ordered = new ArrayList<>();
                Set<String> seen = new HashSet<>();
                for (Object w : want) {
                    String path = cleanPath(w, true);
                    PackFiles.Cat c = byPath.get(path);
                    if (c != null && seen.add(path)) ordered.add(c);
                }
                // files the client did not list keep their place at the end
                for (PackFiles.Cat c : cats) if (!seen.contains(c.path)) ordered.add(c);
                int n = 0;
                for (PackFiles.Cat c : ordered) {
                    n += 10;
                    if (c.root == null || c.text == null) continue;
                    if (c.order == n && c.root.containsKey(PackFiles.ORDER)) continue;
                    write(b, c.path, JsonPatch.set(c.text, PackFiles.ORDER, (double) n));
                }
                if (b.touched.isEmpty()) throw new Fail("whathitme.pack.err.nochange");
                return "whathitme.pack.ordered";
            }
            case "cat.enabled": {
                String path = cleanPath(op.get("path"), true);
                PackFiles.Cat c = PackFiles.read(dir, file(path));
                if (!file(path).isFile()) throw new Fail("whathitme.pack.err.nofile", path);
                if (c.root == null || c.text == null) throw new Fail("whathitme.pack.err.parse", path, String.valueOf(c.error));
                boolean on = Json.bool(op.get("enabled"), true);
                write(b, path, JsonPatch.set(c.text, PackFiles.ENABLED, on ? null : Boolean.FALSE));
                return on ? "whathitme.pack.catenabled" : "whathitme.pack.catdisabled";
            }
            case "cat.memo": {
                String path = cleanPath(op.get("path"), true);
                if (!file(path).isFile()) throw new Fail("whathitme.pack.err.nofile", path);
                PackFiles.Cat c = PackFiles.read(dir, file(path));
                if (c.root == null || c.text == null) throw new Fail("whathitme.pack.err.parse", path, String.valueOf(c.error));
                String memo = text(op.get("memo"), MAX_STRING, true);
                write(b, path, JsonPatch.set(c.text, PackFiles.MEMO, memo.trim().isEmpty() ? null : memo));
                return "whathitme.pack.memosaved";
            }
            case "dir.create": {
                String path = cleanPath(op.get("path"), false);
                File d = file(path);
                if (d.exists()) throw new Fail("whathitme.pack.err.exists", path);
                mkdirs(b, d);
                b.touched.add(path + "/");
                return "whathitme.pack.dircreated";
            }
            case "dir.rename": {
                String from = cleanPath(op.get("from"), false);
                String to = cleanPath(op.get("to"), false);
                File fd = file(from);
                if (!fd.isDirectory()) throw new Fail("whathitme.pack.err.nofile", from);
                if (file(to).exists()) throw new Fail("whathitme.pack.err.exists", to);
                if ((to + "/").toLowerCase(Locale.ROOT).startsWith((from + "/").toLowerCase(Locale.ROOT))) throw new Fail("whathitme.pack.err.path", to);
                List<File> files = new ArrayList<>();
                collectAll(fd, files);
                for (File f : files) {
                    String rel = PackFiles.rel(dir, f);
                    String target = to + rel.substring(from.length());
                    byte[] data = bytes(f);
                    delete(b, rel);
                    remember(b, target);
                    File tf = file(target);
                    if (!tf.getParentFile().isDirectory()) mkdirs(b, tf.getParentFile());
                    Files.write(tf.toPath(), data);
                }
                if (!file(to).isDirectory()) mkdirs(b, file(to));
                removeEmpty(b, fd);
                b.touched.add(from + "/");
                b.touched.add(to + "/");
                return "whathitme.pack.dirrenamed";
            }
            case "dir.delete": {
                String path = cleanPath(op.get("path"), false);
                File d = file(path);
                if (!d.isDirectory()) throw new Fail("whathitme.pack.err.nofile", path);
                List<File> files = new ArrayList<>();
                collectAll(d, files);
                if (!files.isEmpty()) throw new Fail("whathitme.pack.err.dirnotempty", path);
                removeEmpty(b, d);
                b.touched.add(path + "/");
                return "whathitme.pack.dirdeleted";
            }
            case "setting": {
                WhmConfig.Common c = WhmConfig.common;
                String key = text(op.get("key"), 100, false);
                Setting s = c.byKey().get(key);
                if (s == null) throw new Fail("whathitme.config.unknown", key);
                String value = text(op.get("value"), MAX_STRING, true);
                String prev = c.get(s.key);
                File cfg = new File(Platform.whmConfigDir(), "whathitme-common.cfg");
                if (cfg.isFile()) {
                    File bk = new File(new File(backupDir(), b.backupId), cfg.getName());
                    bk.getParentFile().mkdirs();
                    Files.copy(cfg.toPath(), bk.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                String r = c.set(s.key, value, false);
                if (r != null) throw new Fail("whathitme.pack.err.setting", s.key, value);
                b.settingKey = s.key;
                b.settingPrev = prev;
                b.touched.add("whathitme-common.cfg: " + s.key);
                return "whathitme.pack.settingsaved";
            }
            default:
                throw new Fail("whathitme.pack.err.bad", o);
        }
    }

    private static void collectAll(File d, List<File> out) {
        File[] kids = d.listFiles();
        if (kids == null) return;
        for (File k : kids) {
            if (k.isDirectory()) collectAll(k, out);
            else out.add(k);
        }
    }

    /** Removes a folder tree without files (deepest first), remembering it for undo */
    private static void removeEmpty(Batch b, File d) {
        File[] kids = d.listFiles();
        if (kids != null) for (File k : kids) if (k.isDirectory()) removeEmpty(b, k);
        String rel = PackFiles.rel(PackFiles.dir(), d);
        if (d.delete()) b.dirsRemoved.add(rel);
    }

    private static void undo(Batch b) throws IOException {
        Batch redo = new Batch();
        // deletions first: on a case-insensitive file system "Boss.json" and "boss.json" are the same file
        for (Map.Entry<String, byte[]> e : b.prev.entrySet()) {
            redo.touched.add(e.getKey());
            if (e.getValue() == null) Files.deleteIfExists(file(e.getKey()).toPath());
        }
        for (Map.Entry<String, byte[]> e : b.prev.entrySet()) {
            if (e.getValue() == null) continue;
            File f = file(e.getKey());
            if (f.getParentFile() != null) f.getParentFile().mkdirs();
            Files.write(f.toPath(), e.getValue());
        }
        List<String> created = new ArrayList<>(b.dirsCreated);
        java.util.Collections.reverse(created);
        for (String d : created) file(d).delete();
        List<String> removed = new ArrayList<>(b.dirsRemoved);
        java.util.Collections.reverse(removed);
        for (String d : removed) file(d).mkdirs();
        if (b.settingKey != null) WhmConfig.common.set(b.settingKey, b.settingPrev == null ? "" : b.settingPrev, false);
        b.touched.clear();
        b.touched.addAll(redo.touched);
        if (b.settingKey != null) b.touched.add("whathitme-common.cfg: " + b.settingKey);
        b.touched.addAll(b.dirsCreated);
        b.touched.addAll(b.dirsRemoved);
    }
}
