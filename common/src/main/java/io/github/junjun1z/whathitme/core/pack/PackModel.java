package io.github.junjun1z.whathitme.core.pack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.github.junjun1z.whathitme.core.data.Json;

/**
 * What the pack editor shows, built from a state message of the server (or of the personal backend): categories (pack
 * files, plus read-only script sources), their entries per section, the folder tree, server settings and the helper
 * lists for the match-key builder and the condition picker.
 */
public final class PackModel {

    /** One category (pack file or script owner) */
    public static final class Cat {
        public final String path;
        public final double order;
        public final boolean enabled;
        public final String memo;
        /** null when the file has a syntax error */
        public final Map<String, Object> content;
        public final String error;
        /** Registered by a script (read-only) */
        public final boolean script;
        /** Position in the load order (1 = first), 0 = not loaded (disabled / error / script) */
        public int loadPos;

        Cat(String path, double order, boolean enabled, String memo, Map<String, Object> content, String error, boolean script) {
            this.path = path;
            this.order = order;
            this.enabled = enabled;
            this.memo = memo == null ? "" : memo;
            this.content = content;
            this.error = error;
            this.script = script;
        }

        public boolean readOnly() {
            return script || content == null;
        }

        /** Folder of the file ("" = pack root) */
        public String folder() {
            int i = path.lastIndexOf('/');
            return i < 0 ? "" : path.substring(0, i);
        }

        /** File name without .json */
        public String name() {
            String n = path.substring(path.lastIndexOf('/') + 1);
            return n.toLowerCase(Locale.ROOT).endsWith(".json") ? n.substring(0, n.length() - 5) : n;
        }
    }

    /** One entry of a section in a category */
    public static final class Ent {
        public final Cat cat;
        public final String section;
        /** Map key (maps) or the string itself (string lists), null for object lists */
        public final String key;
        /** Index in its list, -1 for maps */
        public final int index;
        /** String, list of lines (templates) or object (rules / triggers) */
        public final Object value;
        public final String memo;

        Ent(Cat cat, String section, String key, int index, Object value, String memo) {
            this.cat = cat;
            this.section = section;
            this.key = key;
            this.index = index;
            this.value = value;
            this.memo = memo == null ? "" : memo;
        }

        /** Element as stored in its list (to find it again on the server) */
        public Object expect() {
            return index < 0 ? null : value;
        }
    }

    /** A row of the category tree: a folder or a category */
    public static final class Node {
        public final String folder;
        public final Cat cat;
        public final int depth;

        Node(String folder, Cat cat, int depth) {
            this.folder = folder;
            this.cat = cat;
            this.depth = depth;
        }

        public String id() {
            return cat != null ? "cat:" + cat.path : "dir:" + folder;
        }
    }

    public final List<Cat> cats = new ArrayList<>();
    public final List<String> dirs = new ArrayList<>();
    public final List<Map<String, Object>> settings = new ArrayList<>();
    public final List<String> warnings = new ArrayList<>();
    public final List<String> errors = new ArrayList<>();
    public final List<String> editors = new ArrayList<>();
    public boolean canUndo;
    public long rev = -1;

    // helper lists
    public final List<String[]> damage = new ArrayList<>();
    public final List<String[]> mobs = new ArrayList<>();
    public final Map<String, List<String>> conds = new LinkedHashMap<>();
    public final Map<String, String> condProblems = new LinkedHashMap<>();
    public final List<String> players = new ArrayList<>();
    public final List<String> hide = new ArrayList<>();
    public boolean hasLists;

    public static String str(Object v) {
        return Json.str(v, "");
    }

    /** Applies a "state" message */
    public void state(Map<String, Object> m) {
        cats.clear();
        for (Object o : Json.list(m.get("files"))) {
            Map<String, Object> f = Json.obj(o);
            Object c = f.get("content");
            cats.add(new Cat(str(f.get("path")), Json.num(f.get("order"), 0), Json.bool(f.get("enabled"), true), str(f.get("memo")),
                c instanceof Map ? Json.obj(c) : null, f.containsKey("error") ? str(f.get("error")) : null, false));
        }
        List<String> loaded = new ArrayList<>();
        for (Object o : Json.list(m.get("loaded"))) loaded.add(str(o));
        for (Cat c : cats) c.loadPos = loaded.indexOf(c.path.toLowerCase(Locale.ROOT)) + 1;
        for (Object o : Json.list(m.get("scripts"))) {
            Map<String, Object> s = Json.obj(o);
            cats.add(new Cat(str(s.get("owner")), 0, true, "", Json.obj(s.get("content")), null, true));
        }
        dirs.clear();
        for (Object o : Json.list(m.get("dirs"))) dirs.add(str(o));
        settings.clear();
        for (Object o : Json.list(m.get("settings"))) settings.add(Json.obj(o));
        warnings.clear();
        for (Object o : Json.list(m.get("warnings"))) warnings.add(str(o));
        errors.clear();
        for (Object o : Json.list(m.get("errors"))) errors.add(str(o));
        editors.clear();
        for (Object o : Json.list(m.get("editors"))) editors.add(str(o));
        canUndo = Json.bool(m.get("canUndo"), false);
        rev = (long) Json.num(m.get("rev"), 0);
    }

    /** Applies a "lists" message */
    public void lists(Map<String, Object> m) {
        damage.clear();
        for (Object o : Json.list(m.get("damage"))) damage.add(row(o));
        mobs.clear();
        for (Object o : Json.list(m.get("mobs"))) mobs.add(row(o));
        conds.clear();
        for (Map.Entry<String, Object> e : Json.obj(m.get("conds")).entrySet()) {
            List<String> l = new ArrayList<>();
            for (Object o : Json.list(e.getValue())) l.add(str(o));
            conds.put(e.getKey(), l);
        }
        condProblems.clear();
        for (Map.Entry<String, Object> e : Json.obj(m.get("condProblems")).entrySet()) condProblems.put(e.getKey(), str(e.getValue()));
        players.clear();
        for (Object o : Json.list(m.get("players"))) players.add(str(o));
        hide.clear();
        for (Object o : Json.list(m.get("hide"))) hide.add(str(o));
        hasLists = true;
    }

    private static String[] row(Object o) {
        List<Object> l = Json.list(o);
        String[] r = new String[3];
        for (int i = 0; i < 3; i++) r[i] = i < l.size() ? str(l.get(i)) : "";
        return r;
    }

    public Cat cat(String path) {
        for (Cat c : cats) if (c.path.equals(path)) return c;
        return null;
    }

    /** Pack files (no script sources) in load order */
    public List<Cat> files() {
        List<Cat> out = new ArrayList<>();
        for (Cat c : cats) if (!c.script) out.add(c);
        return out;
    }

    public static boolean isMap(String section) {
        return PackStore.MAPS.contains(section);
    }

    public static boolean isObjectList(String section) {
        return "rules".equals(section) || "deathTriggers".equals(section);
    }

    /** The entries of one section of a category, in file order */
    public static List<Ent> entries(Cat c, String section) {
        List<Ent> out = new ArrayList<>();
        if (c == null || c.content == null) return out;
        Object v = c.content.get(section);
        if (isMap(section)) {
            Map<String, Object> m = new LinkedHashMap<>();
            if (v instanceof Map) m.putAll(Json.obj(v));
            else if (v instanceof List) {
                for (Object o : Json.list(v)) {
                    String s = str(o);
                    int eq = s.indexOf('=');
                    if (eq > 0) m.put(s.substring(0, eq).trim(), s.substring(eq + 1).trim());
                }
            }
            for (Map.Entry<String, Object> e : m.entrySet()) {
                Object val = e.getValue();
                if ("templates".equals(section) && val instanceof String) val = Collections.singletonList(val);
                out.add(new Ent(c, section, e.getKey(), -1, val, PackStore.entryMemo(c.content, section, e.getKey())));
            }
            return out;
        }
        List<Object> l = v instanceof List ? Json.list(v) : v == null ? new ArrayList<>() : Collections.singletonList(v);
        for (int i = 0; i < l.size(); i++) {
            Object val = l.get(i);
            if (isObjectList(section)) out.add(new Ent(c, section, null, i, val, str(Json.obj(val).get("memo"))));
            else out.add(new Ent(c, section, str(val), i, val, PackStore.entryMemo(c.content, section, str(val))));
        }
        return out;
    }

    /**
     * The category tree: folders (from the folder list and the file paths) and files. Siblings are sorted by load
     * order (a folder by its first file); empty folders come after, by name. Script sources follow at the end.
     */
    public List<Node> tree() {
        List<Node> out = new ArrayList<>();
        addLevel("", 0, out);
        return out;
    }

    private double rank(Cat c) {
        int i = files().indexOf(c);
        return i < 0 ? Double.MAX_VALUE : i;
    }

    private double folderRank(String folder) {
        double best = Double.MAX_VALUE;
        for (Cat c : files()) if (c.path.startsWith(folder + "/")) best = Math.min(best, rank(c));
        return best;
    }

    private List<String> childFolders(String folder) {
        java.util.Set<String> set = new java.util.LinkedHashSet<>();
        List<String> all = new ArrayList<>(dirs);
        for (Cat c : files()) if (!c.folder().isEmpty()) all.add(c.folder());
        for (String d : all) {
            if (folder.isEmpty() ? true : d.startsWith(folder + "/")) {
                String rest = folder.isEmpty() ? d : d.substring(folder.length() + 1);
                if (rest.isEmpty()) continue;
                int slash = rest.indexOf('/');
                set.add((folder.isEmpty() ? "" : folder + "/") + (slash < 0 ? rest : rest.substring(0, slash)));
            }
        }
        return new ArrayList<>(set);
    }

    /** Children of a folder in display order: folder names (String) and categories (Cat) */
    public List<Object> children(String folder) {
        List<Object> kids = new ArrayList<>();
        for (Cat c : files()) if (c.folder().equals(folder)) kids.add(c);
        kids.addAll(childFolders(folder));
        Collections.sort(kids, (a, b) -> {
            double ra = a instanceof Cat ? rank((Cat) a) : folderRank((String) a);
            double rb = b instanceof Cat ? rank((Cat) b) : folderRank((String) b);
            int c = Double.compare(ra, rb);
            if (c != 0) return c;
            return name(a).compareToIgnoreCase(name(b));
        });
        return kids;
    }

    private static String name(Object o) {
        return o instanceof Cat ? ((Cat) o).path : (String) o;
    }

    private void addLevel(String folder, int depth, List<Node> out) {
        for (Object k : children(folder)) {
            if (k instanceof Cat) out.add(new Node(null, (Cat) k, depth));
            else {
                out.add(new Node((String) k, null, depth));
                addLevel((String) k, depth + 1, out);
            }
        }
    }

    /** Pack files in tree order (what the load order becomes after a move) */
    public List<String> treeOrder(String folder, List<Object> override, String overrideFolder) {
        List<String> out = new ArrayList<>();
        List<Object> kids = folder.equals(overrideFolder) && override != null ? override : children(folder);
        for (Object k : kids) {
            if (k instanceof Cat) out.add(((Cat) k).path);
            else out.addAll(treeOrder((String) k, override, overrideFolder));
        }
        return out;
    }

    /** Number of entries of a section in a category */
    public static int count(Cat c, String section) {
        return entries(c, section).size();
    }
}
