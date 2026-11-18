package io.github.junjun1z.whathitme.core.pack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import io.github.junjun1z.whathitme.core.Safe;
import io.github.junjun1z.whathitme.core.client.ClientCore;
import io.github.junjun1z.whathitme.core.cmd.Completion;
import io.github.junjun1z.whathitme.core.cmd.TestPreview;
import io.github.junjun1z.whathitme.core.config.Setting;
import io.github.junjun1z.whathitme.core.config.WhmConfig;
import io.github.junjun1z.whathitme.core.data.Entry;
import io.github.junjun1z.whathitme.core.data.Json;
import io.github.junjun1z.whathitme.core.data.SearchEngine;
import io.github.junjun1z.whathitme.core.text.Msg;
import io.github.junjun1z.whathitme.core.text.Txt;
import io.github.junjun1z.whathitme.core.text.TxtCodec;

/**
 * Personal mode: the player's own renameMap, hintMap and ignoredDamageIds (whathitme-client.cfg) shown as one category,
 * edited with the same operations as the pack. No permission needed, nothing is sent to the server.
 */
public final class PackPersonal implements PackBackend {

    /** Name of the single category */
    public static final String FILE = "whathitme-client.cfg";

    private Consumer<Map<String, Object>> listener;
    private final Deque<String[][]> undo = new ArrayDeque<>();

    @Override
    public boolean personal() {
        return true;
    }

    @Override
    public void listen(Consumer<Map<String, Object>> l) {
        listener = l;
    }

    private void reply(Map<String, Object> m) {
        if (listener != null) listener.accept(m);
    }

    private static final Setting[] LISTS = {WhmConfig.RENAME, WhmConfig.HINT, WhmConfig.IGNORED};
    private static final String[] SECTIONS = {"renameMap", "hintMap", "defaultIgnoredDamageIds"};

    private static String[][] snapshot() {
        String[][] s = new String[3][];
        for (int i = 0; i < 3; i++) s[i] = WhmConfig.client.list(LISTS[i]);
        return s;
    }

    /** The client lists as one pack-style object */
    static Map<String, Object> content() {
        Map<String, Object> root = new LinkedHashMap<>();
        String[][] s = snapshot();
        for (int i = 0; i < 2; i++) {
            Map<String, Object> m = new LinkedHashMap<>();
            for (String line : s[i]) {
                int eq = line.indexOf('=');
                if (eq > 0) m.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
            }
            if (!m.isEmpty()) root.put(SECTIONS[i], m);
        }
        List<Object> ign = new ArrayList<>();
        for (String x : s[2]) if (!x.trim().isEmpty()) ign.add(x.trim());
        if (!ign.isEmpty()) root.put(SECTIONS[2], ign);
        return root;
    }

    private static void store(Map<String, Object> root) {
        for (int i = 0; i < 2; i++) {
            List<String> lines = new ArrayList<>();
            for (Map.Entry<String, Object> e : Json.obj(root.get(SECTIONS[i])).entrySet()) lines.add(e.getKey() + " = " + Json.str(e.getValue(), ""));
            WhmConfig.client.setList(LISTS[i], lines);
        }
        List<String> ign = new ArrayList<>();
        for (Object o : Json.list(root.get(SECTIONS[2]))) ign.add(Json.str(o, ""));
        WhmConfig.client.setList(LISTS[2], ign);
    }

    private static void restore(String[][] s) {
        for (int i = 0; i < 3; i++) WhmConfig.client.setList(LISTS[i], Arrays.asList(s[i]));
    }

    static Map<String, Object> state(boolean canUndo) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "state");
        List<Object> files = new ArrayList<>();
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("path", FILE);
        f.put("order", 0);
        f.put("enabled", true);
        f.put("memo", "");
        f.put("content", content());
        files.add(f);
        m.put("files", files);
        m.put("loaded", new ArrayList<Object>(java.util.Collections.singletonList(FILE)));
        m.put("canUndo", canUndo);
        return m;
    }

    static Map<String, Object> lists() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "lists");
        List<Object> damage = new ArrayList<>();
        List<Object> mobs = new ArrayList<>();
        List<Entry> all = Safe.get("catalog", () -> SearchEngine.all(ClientCore.CATALOG, ClientCore.port.registries()), new ArrayList<Entry>());
        for (Entry e : all) {
            List<Object> row = new ArrayList<Object>(Arrays.asList(e.id, e.name == null ? "" : e.name, e.modName == null ? "" : e.modName));
            if (e.type == Entry.DAMAGE) damage.add(row);
            else if (e.type == Entry.MOB) mobs.add(row);
        }
        m.put("damage", damage);
        m.put("mobs", mobs);
        return m;
    }

    @Override
    public void send(Map<String, Object> op) {
        String o = Json.str(op.get("op"), "");
        String req = Json.str(op.get("req"), "");
        try {
            switch (o) {
                case "open":
                    reply(lists());
                    reply(state(!undo.isEmpty()));
                    return;
                case "state":
                    reply(state(!undo.isEmpty()));
                    return;
                case "close":
                    return;
                case "test": {
                    String[] a = Completion.args(Json.str(op.get("args"), ""));
                    String[] full = new String[a.length + 1];
                    full[0] = "test";
                    System.arraycopy(a, 0, full, 1, a.length);
                    List<Object> lines = new ArrayList<>();
                    String name = Safe.get("player name", () -> ClientCore.port.playerName(), "");
                    for (Txt t : TestPreview.lines(Msg.LOCAL, ClientCore.formatter(), TestPreview.Args.parse(full), null, name.isEmpty() ? "Player" : name,
                        null, "", 20f, null)) lines.add(TxtCodec.write(t));
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("type", "test");
                    m.put("lines", lines);
                    reply(m);
                    return;
                }
                case "undo": {
                    if (undo.isEmpty()) throw new PackStore.Fail("whathitme.pack.err.noundo");
                    restore(undo.pop());
                    done(req, "whathitme.pack.undone");
                    return;
                }
                default:
                    break;
            }
            String section = Json.str(op.get("section"), "");
            if (!Arrays.asList(SECTIONS).contains(section)) throw new PackStore.Fail("whathitme.pack.err.section", section);
            Map<String, Object> root = content();
            PackStore.Batch b = new PackStore.Batch();
            Object idx = op.get("index");
            int index = idx == null ? -1 : (int) Json.num(idx, -1);
            String key = op.get("key") == null ? null : Json.str(op.get("key"), "");
            switch (o) {
                case "entry.put": {
                    String value = Json.str(op.get("value"), "");
                    if (section.equals(SECTIONS[2])) value = value.trim();
                    if (section.equals(SECTIONS[2]) && value.isEmpty()) throw new PackStore.Fail("whathitme.pack.err.empty");
                    if (value.length() > PackStore.MAX_STRING) throw new PackStore.Fail("whathitme.pack.err.toolong", String.valueOf(PackStore.MAX_STRING));
                    String oldKey = op.get("oldKey") == null ? null : Json.str(op.get("oldKey"), "");
                    PackStore.putEntry(root, section, oldKey, key == null ? "" : key, index, op.get("expect"), value, "", b);
                    break;
                }
                case "entry.delete":
                    PackStore.removeEntry(root, section, key, index, op.get("expect"), b);
                    break;
                case "entry.move": {
                    if (section.equals(SECTIONS[2])) {
                        List<Object> l = PackStore.listSection(root, section);
                        int i = l.indexOf(op.get("expect"));
                        if (i < 0) throw new PackStore.Fail("whathitme.pack.err.gone");
                        int to = Math.max(0, Math.min(l.size() - 1, (int) Json.num(op.get("to"), i)));
                        l.add(to, l.remove(i));
                    } else {
                        Map<String, Object> m = PackStore.mapSection(root, section);
                        List<String> keys = new ArrayList<>(m.keySet());
                        int i = keys.indexOf(key);
                        if (i < 0) throw new PackStore.Fail("whathitme.pack.err.gone");
                        int to = Math.max(0, Math.min(keys.size() - 1, (int) Json.num(op.get("to"), i)));
                        keys.add(to, keys.remove(i));
                        Map<String, Object> copy = new LinkedHashMap<>(m);
                        m.clear();
                        for (String k : keys) m.put(k, copy.get(k));
                    }
                    break;
                }
                default:
                    throw new PackStore.Fail("whathitme.pack.err.personal");
            }
            String[][] before = snapshot();
            store(root);
            undo.push(before);
            while (undo.size() > PackStore.MAX_UNDO) undo.removeLast();
            done(req, o.equals("entry.delete") ? "whathitme.pack.deleted" : o.equals("entry.move") ? "whathitme.pack.moved" : "whathitme.pack.saved");
        } catch (PackStore.Fail f) {
            Map<String, Object> r = PackStore.result(false, f.key, f.args);
            r.put("req", req);
            reply(r);
        } catch (RuntimeException e) {
            Safe.warn("pack editor (personal)", e);
            Map<String, Object> r = PackStore.result(false, "whathitme.pack.err.io", String.valueOf(e.getMessage()));
            r.put("req", req);
            reply(r);
        }
    }

    private void done(String req, String key) {
        Map<String, Object> r = PackStore.result(true, key, FILE);
        r.put("req", req);
        reply(r);
        reply(state(!undo.isEmpty()));
    }
}
