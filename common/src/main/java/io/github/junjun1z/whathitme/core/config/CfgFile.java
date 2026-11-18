package io.github.junjun1z.whathitme.core.config;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * A configuration file in the Forge 1.12 ".cfg" format (categories in braces, "B:key=value", lists in angle brackets,
 * comments above each entry). Used on every Minecraft version without Forge's old Configuration class, so the files,
 * keys and the hint_template.cfg blocks are the same everywhere.
 */
public final class CfgFile implements ConfigBackend {

    private final File file;
    /** category -> key -> String or String[] */
    private final Map<String, Map<String, Object>> values = new TreeMap<>();
    private final Map<String, Setting> settings = new TreeMap<>();
    private final Map<String, String> catComments = new TreeMap<>();
    private boolean changed;
    /** The file exists but could not be read: do not rewrite it with defaults */
    private boolean readFailed;

    public CfgFile(File file) {
        this.file = file;
    }

    public File file() {
        return file;
    }

    @Override
    public synchronized void load() {
        values.clear();
        changed = false;
        readFailed = false;
        if (!file.isFile()) {
            changed = true;
            return;
        }
        List<String> lines;
        try {
            lines = java.util.Arrays.asList(decode(Files.readAllBytes(file.toPath())).split("\r?\n", -1));
        } catch (IOException e) {
            // unreadable now (locked by an editor?): keep the defaults in memory, never overwrite the file
            io.github.junjun1z.whathitme.core.Platform.log.warn("Could not read " + file, e);
            readFailed = true;
            return;
        }
        String cat = null;
        String listKey = null;
        List<String> list = null;
        for (String raw : lines) {
            String line = raw.trim();
            if (list != null) {
                if (line.equals(">")) {
                    cat(cat).put(listKey, list.toArray(new String[0]));
                    list = null;
                } else {
                    list.add(line);
                }
                continue;
            }
            if (line.isEmpty() || line.startsWith("#")) continue;
            if (line.endsWith("{")) {
                cat = unquote(line.substring(0, line.length() - 1).trim());
                cat(cat);
                continue;
            }
            if (line.equals("}")) {
                cat = null;
                continue;
            }
            if (cat == null) continue;
            int colon = line.indexOf(':');
            String rest = colon == 1 ? line.substring(2) : line;
            if (rest.endsWith("<")) {
                listKey = unquote(rest.substring(0, rest.length() - 1).trim());
                list = new ArrayList<>();
                continue;
            }
            int eq = rest.indexOf('=');
            if (eq <= 0) continue;
            cat(cat).put(unquote(rest.substring(0, eq).trim()), rest.substring(eq + 1));
        }
    }

    /** UTF-8; a file saved in another encoding (e.g. by Notepad as ANSI) is read with the system encoding instead of being reset */
    public static String decode(byte[] b) {
        String t;
        try {
            t = StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(b)).toString();
        } catch (java.nio.charset.CharacterCodingException e) {
            java.nio.charset.Charset cs = StandardCharsets.ISO_8859_1;
            try {
                String n = System.getProperty("native.encoding", System.getProperty("sun.jnu.encoding"));
                if (n != null && !n.toUpperCase(Locale.ROOT).contains("UTF")) cs = java.nio.charset.Charset.forName(n);
            } catch (RuntimeException ignored) {
                // keep ISO-8859-1
            }
            io.github.junjun1z.whathitme.core.Platform.log.warn("What Hit Me? a config file is not UTF-8, read as " + cs.name() + "", null);
            t = new String(b, cs);
        }
        return !t.isEmpty() && t.charAt(0) == 0xFEFF ? t.substring(1) : t;
    }

    private static String unquote(String s) {
        return s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"") ? s.substring(1, s.length() - 1) : s;
    }

    private Map<String, Object> cat(String c) {
        return values.computeIfAbsent(c, k -> new TreeMap<>());
    }

    @Override
    public synchronized boolean hasChanged() {
        return changed && !readFailed;
    }

    @Override
    public synchronized String get(Setting s) {
        settings.put(s.category + "." + s.key, s);
        Map<String, Object> c = cat(s.category);
        Object v = c.get(s.key);
        String str = v instanceof String ? (String) v : null;
        String fixed = valid(s, str);
        if (fixed == null) {
            c.put(s.key, s.def);
            changed = true;
            return s.def;
        }
        return clamp(s, fixed);
    }

    /** The value when it can be read, null when it must be reset to the default */
    private static String valid(Setting s, String v) {
        if (v == null) return null;
        v = v.trim();
        try {
            switch (s.type) {
                case BOOLEAN:
                    return v.equalsIgnoreCase("true") || v.equalsIgnoreCase("false") ? v.toLowerCase(Locale.ROOT) : null;
                case INT:
                    Integer.parseInt(v);
                    return v;
                case FLOAT:
                    double d = Double.parseDouble(v);
                    return Double.isNaN(d) ? null : v;
                case STRING:
                    if (s.valid != null && s.valid.length > 0) {
                        for (String x : s.valid) if (x.equalsIgnoreCase(v)) return x;
                        return null;
                    }
                    return v;
                default:
                    return v;
            }
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String clamp(Setting s, String v) {
        if (s.type == Setting.Type.INT) {
            int i = Integer.parseInt(v);
            return String.valueOf(Math.max((int) s.min, Math.min((int) s.max, i)));
        }
        if (s.type == Setting.Type.FLOAT) {
            double d = Double.parseDouble(v);
            if (d < s.min) return Float.toString((float) s.min);
            if (d > s.max) return Float.toString((float) s.max);
        }
        return v;
    }

    @Override
    public synchronized String[] getList(Setting s) {
        settings.put(s.category + "." + s.key, s);
        Map<String, Object> c = cat(s.category);
        Object v = c.get(s.key);
        if (v instanceof String[]) return ((String[]) v).clone();
        String[] empty = new String[0];
        c.put(s.key, empty);
        changed = true;
        return empty;
    }

    @Override
    public synchronized void set(Setting s, String value) {
        cat(s.category).put(s.key, value);
        changed = true;
    }

    @Override
    public synchronized void setList(Setting s, String[] list) {
        cat(s.category).put(s.key, list.clone());
        changed = true;
    }

    @Override
    public synchronized String raw(String category, String key) {
        Map<String, Object> c = values.get(category);
        Object v = c == null ? null : c.get(key);
        return v instanceof String ? (String) v : null;
    }

    @Override
    public synchronized void remove(String category, String key) {
        Map<String, Object> c = values.get(category);
        if (c != null && c.remove(key) != null) changed = true;
    }

    @Override
    public synchronized void categoryComment(String category, String comment) {
        catComments.put(category, comment);
    }

    @Override
    public synchronized void save() {
        StringBuilder sb = new StringBuilder("# Configuration file\n\n");
        String bar = "##########################################################################################################";
        for (Map.Entry<String, Map<String, Object>> ce : values.entrySet()) {
            String cat = ce.getKey();
            String cc = catComments.get(cat);
            sb.append(bar).append('\n').append("# ").append(cat).append('\n');
            sb.append("#--------------------------------------------------------------------------------------------------------#\n");
            if (cc != null) sb.append("# ").append(cc).append('\n');
            sb.append(bar).append("\n\n");
            sb.append(cat).append(" {\n");
            boolean first = true;
            for (Map.Entry<String, Object> e : ce.getValue().entrySet()) {
                if (!first) sb.append('\n');
                first = false;
                Setting s = settings.get(cat + "." + e.getKey());
                if (s != null) sb.append("    # ").append(s.comment).append(' ').append(s.rangeText()).append('\n');
                String type = s == null ? "S" : s.type == Setting.Type.BOOLEAN ? "B" : s.type == Setting.Type.INT ? "I" : "S";
                if (e.getValue() instanceof String[]) {
                    sb.append("    ").append(type).append(':').append(e.getKey()).append(" <\n");
                    for (String item : (String[]) e.getValue()) sb.append("        ").append(item).append('\n');
                    sb.append("     >\n");
                } else {
                    sb.append("    ").append(type).append(':').append(e.getKey()).append('=').append(e.getValue()).append('\n');
                }
            }
            sb.append("}\n\n\n");
        }
        try {
            File dir = file.getParentFile();
            if (dir != null && !dir.isDirectory()) dir.mkdirs();
            Files.write(file.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
            changed = false;
        } catch (IOException e) {
            io.github.junjun1z.whathitme.core.Platform.log.warn("Could not save " + file, e);
        }
    }
}
