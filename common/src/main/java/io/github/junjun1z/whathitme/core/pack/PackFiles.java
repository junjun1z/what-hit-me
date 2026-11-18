package io.github.junjun1z.whathitme.core.pack;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.data.Json;

/**
 * The files of config/whathitme/pack/ as categories: every .json file (sub-folders are groups). Load order: the
 * optional top-level "order" number (missing = 0), then the lower-case path, so packs without "order" load in
 * alphabetical path order as before. "enabled": false switches a category off without deleting it, "memo" is a note
 * of the pack maker. Shared by the pack loader and the pack editor.
 */
public final class PackFiles {

    /** Top-level keys of a category file that are not pack sections */
    public static final String ORDER = "order";
    public static final String ENABLED = "enabled";
    public static final String MEMO = "memo";
    /** Memos of map / string list entries: {"renameMap": {"key": "memo"}, ...} */
    public static final String ENTRY_MEMOS = "entryMemos";

    /** One category file */
    public static final class Cat {
        /** Relative path with '/' (real case) */
        public final String path;
        public final File file;
        /** Raw text, null when unreadable */
        public final String text;
        /** Parsed root object, null on a parse error */
        public final Map<String, Object> root;
        public final String error;
        public final double order;
        public final boolean enabled;

        Cat(String path, File file, String text, Map<String, Object> root, String error) {
            this.path = path;
            this.file = file;
            this.text = text;
            this.root = root;
            this.error = error;
            this.order = root == null ? 0 : Json.num(root.get(ORDER), 0);
            this.enabled = root == null || Json.bool(root.get(ENABLED), true);
        }

        /** Lower-case path (sorting, names in messages) */
        public String key() {
            return path.toLowerCase(Locale.ROOT);
        }

        public String memo() {
            return root == null ? "" : Json.str(root.get(MEMO), "");
        }
    }

    private PackFiles() {}

    public static File dir() {
        return new File(Platform.whmConfigDir(), "pack");
    }

    /** Relative path with '/' (real case) */
    public static String rel(File dir, File f) {
        String d = dir.getAbsolutePath();
        String p = f.getAbsolutePath();
        return (p.startsWith(d) ? p.substring(Math.min(p.length(), d.length() + 1)) : p).replace('\\', '/');
    }

    /** Every category of the folder in load order (also the disabled ones and files with errors) */
    public static List<Cat> scan(File dir) {
        List<File> found = new ArrayList<>();
        collect(dir, found, 0);
        List<Cat> out = new ArrayList<>();
        for (File f : found) out.add(read(dir, f));
        sort(out);
        return out;
    }

    public static Cat read(File dir, File f) {
        String path = rel(dir, f);
        String text = null;
        try {
            text = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            Object root = Json.parse(text);
            if (!(root instanceof Map)) return new Cat(path, f, text, null, "not a JSON object");
            return new Cat(path, f, text, Json.obj(root), null);
        } catch (Exception e) {
            return new Cat(path, f, text, null, String.valueOf(e.getMessage()));
        }
    }

    public static void sort(List<Cat> cats) {
        Collections.sort(cats, (a, b) -> {
            int c = Double.compare(a.order, b.order);
            return c != 0 ? c : a.key().compareTo(b.key());
        });
    }

    /** Sub-folders (relative paths, sorted) */
    public static List<String> dirs(File dir) {
        List<String> out = new ArrayList<>();
        dirs(dir, dir, out, 0);
        Collections.sort(out, String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    private static void dirs(File root, File dir, List<String> out, int depth) {
        File[] list = dir.listFiles();
        if (list == null || depth > 8) return;
        for (File f : list) {
            if (f.isDirectory() && !f.getName().startsWith(".")) {
                out.add(rel(root, f));
                dirs(root, f, out, depth + 1);
            }
        }
    }

    private static void collect(File dir, List<File> out, int depth) {
        File[] list = dir.listFiles();
        if (list == null || depth > 8) return;
        for (File f : list) {
            if (f.isDirectory()) {
                if (!f.getName().startsWith(".")) collect(f, out, depth + 1);
            } else if (f.getName().toLowerCase(Locale.ROOT).endsWith(".json")) out.add(f);
        }
    }
}
