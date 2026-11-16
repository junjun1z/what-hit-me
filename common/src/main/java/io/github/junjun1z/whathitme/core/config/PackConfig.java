package io.github.junjun1z.whathitme.core.config;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.data.Json;
import io.github.junjun1z.whathitme.core.data.MatchKey;

/**
 * Modpack definitions in config/whathitme/pack/: every .json file (recursively, in alphabetical path order) is read and
 * merged; later files override earlier entries of the same key. Keys: renameMap, hintMap (objects "key": "value", or
 * lists of "key = value"), defaultIgnoredDamageIds, lockedFeatures (lists) and deathTriggers (list of objects).
 * Priority: the .cfg files &gt; pack/*.json &gt; built-in automatic hints.
 */
public final class PackConfig {

    /** A command run on the server when a matching death happens */
    public static final class DeathTrigger {
        public final String match;
        public final MatchKey key;
        public final String command;
        public final boolean oncePerPlayer;
        public final int cooldownSeconds;
        /** File the trigger comes from (for messages) */
        public final String source;

        public DeathTrigger(String match, String command, boolean oncePerPlayer, int cooldownSeconds, String source) {
            this.match = match;
            this.key = MatchKey.parse(match);
            this.command = command;
            this.oncePerPlayer = oncePerPlayer;
            this.cooldownSeconds = Math.max(0, cooldownSeconds);
            this.source = source;
        }

        /** Identity used for oncePerPlayer / cooldowns */
        public String id() {
            return match + " -> " + command;
        }
    }

    public static final PackConfig EMPTY = new PackConfig();

    public final Map<String, String> renameMap = new LinkedHashMap<>();
    public final Map<String, String> hintMap = new LinkedHashMap<>();
    public final List<String> ignored = new ArrayList<>();
    /** Lower-case config keys */
    public final Set<String> locked = new TreeSet<>();
    public final List<DeathTrigger> triggers = new ArrayList<>();
    public final List<String> files = new ArrayList<>();
    public final List<String> errors = new ArrayList<>();
    /** Templates of the outputs (later files override whole templates) */
    public final Map<String, io.github.junjun1z.whathitme.core.text.Template> templates = new LinkedHashMap<>();
    /** Custom death messages: composite match key -> message template */
    public final Map<String, String> deathMessages = new LinkedHashMap<>();
    /** Unknown template variables and output names ("file: ...") */
    public final List<String> warnings = new ArrayList<>();

    public boolean isLocked(String key) {
        return key != null && locked.contains(key.toLowerCase(Locale.ROOT));
    }

    /** Loads config/whathitme/pack/ (creating it with inert examples when missing). Never throws. */
    public static PackConfig load(File whmDir) {
        PackConfig c = new PackConfig();
        try {
            File dir = new File(whmDir, "pack");
            if (!dir.isDirectory()) createSamples(dir);
            else addNewSamples(dir);
            List<File> found = new ArrayList<>();
            collect(dir, found);
            Collections.sort(found, (a, b) -> rel(dir, a).compareTo(rel(dir, b)));
            for (File f : found) {
                String name = rel(dir, f);
                try {
                    c.merge(Json.parse(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8)), name);
                    c.files.add(name);
                } catch (Exception e) {
                    c.errors.add(name + ": " + e.getMessage());
                    Platform.log.warn("What Hit Me? pack file " + name + " skipped: " + e.getMessage(), null);
                }
            }
        } catch (Throwable t) {
            Platform.log.warn("Could not read the What Hit Me? pack folder", t);
        }
        return c;
    }

    private static String rel(File dir, File f) {
        String d = dir.getAbsolutePath();
        String p = f.getAbsolutePath();
        return (p.startsWith(d) ? p.substring(d.length() + 1) : p).replace('\\', '/').toLowerCase(Locale.ROOT);
    }

    private static void collect(File dir, List<File> out) {
        File[] list = dir.listFiles();
        if (list == null) return;
        for (File f : list) {
            if (f.isDirectory()) collect(f, out);
            else if (f.getName().toLowerCase(Locale.ROOT).endsWith(".json")) out.add(f);
        }
    }

    private static void map(Object v, Map<String, String> into) {
        if (v instanceof Map) {
            for (Map.Entry<String, Object> e : Json.obj(v).entrySet()) {
                String k = e.getKey().trim();
                String val = Json.str(e.getValue(), "").trim();
                if (k.isEmpty()) continue;
                if (val.isEmpty()) into.remove(k);
                else into.put(k, val);
            }
        } else {
            for (Object o : Json.list(v)) {
                String s = Json.str(o, "");
                int eq = s.indexOf('=');
                if (eq <= 0) continue;
                String k = s.substring(0, eq).trim();
                String val = s.substring(eq + 1).trim();
                if (val.isEmpty()) into.remove(k);
                else into.put(k, val);
            }
        }
    }

    void merge(Object root, String file) {
        Map<String, Object> o = Json.obj(root);
        map(o.get("renameMap"), renameMap);
        map(o.get("hintMap"), hintMap);
        for (Object v : Json.list(o.get("defaultIgnoredDamageIds"))) {
            String s = Json.str(v, "").trim();
            if (!s.isEmpty() && !ignored.contains(s)) ignored.add(s);
        }
        for (Object v : Json.list(o.get("lockedFeatures"))) {
            String s = Json.str(v, "").trim();
            if (!s.isEmpty()) locked.add(s.toLowerCase(Locale.ROOT));
        }
        for (Object v : Json.list(o.get("deathTriggers"))) {
            Map<String, Object> t = Json.obj(v);
            String match = Json.str(t.get("match"), "").trim();
            String cmd = Json.str(t.get("command"), "").trim();
            if (match.isEmpty() || cmd.isEmpty()) continue;
            if (cmd.startsWith("/")) cmd = cmd.substring(1);
            triggers.add(new DeathTrigger(match, cmd, Json.bool(t.get("oncePerPlayer"), false), (int) Json.num(t.get("cooldownSeconds"), 0), file));
        }
        Templates.readSection(o.get("templates"), file, templates, warnings);
        Map<String, String> dm = new LinkedHashMap<>(deathMessages);
        map(o.get("deathMessages"), dm);
        for (Map.Entry<String, String> e : dm.entrySet()) {
            if (e.getValue().equals(deathMessages.get(e.getKey()))) continue;
            for (String u : io.github.junjun1z.whathitme.core.text.Template.unknown(java.util.Collections.singletonList(e.getValue()))) {
                warnings.add(file + ": deathMessages \"" + e.getKey() + "\": unknown variable {" + u + "}");
            }
        }
        deathMessages.clear();
        deathMessages.putAll(dm);
    }

    private static final String[][] SAMPLES = {
        {"hints/_example.json", "{\n"
            + "  // Custom tips. Keys: a damage id, optionally combined with @entity_id, @item:modid:item_id, @dim:dimension_id (any order, * wildcards).\n"
            + "  // The most specific matching key wins. Values: text (& color codes) or lang:your.translation.key\n"
            + "  // Remove the // in front of a line to use it.\n"
            + "  \"hintMap\": {\n"
            + "    // \"mob:minecraft:creeper\": \"Creepers hiss before exploding - sprint away!\",\n"
            + "    // \"minecraft:lava @dim:minecraft:the_nether\": \"Carry Fire Resistance potions in the Nether.\"\n"
            + "  }\n"
            + "}\n"},
        {"renames/_example.json", "{\n"
            + "  // Custom display names, same keys as hintMap.\n"
            + "  \"renameMap\": {\n"
            + "    // \"minecraft:outOfWorld\": \"&5The Abyss\",\n"
            + "    // \"mob:minecraft:zombie @item:minecraft:iron_sword\": \"Armed Zombie\"\n"
            + "  }\n"
            + "}\n"},
        {"ignored/_example.json", "{\n"
            + "  // Damage ids hidden by default for everyone (wildcards allowed).\n"
            + "  \"defaultIgnoredDamageIds\": [\n"
            + "    // \"minecraft:cactus\"\n"
            + "  ],\n"
            + "  // Config keys players cannot change (config screen and /whm config), e.g. \"showInternalDamageId\".\n"
            + "  \"lockedFeatures\": [\n"
            + "  ]\n"
            + "}\n"},
        {"triggers/_example.json", "{\n"
            + "  // Server commands run (with console permission) when a death matches. Placeholders: {player} {x} {y} {z} {damage} (health points of the killing blow).\n"
            + "  // oncePerPlayer: only the first matching death of each player. cooldownSeconds: per player.\n"
            + "  \"deathTriggers\": [\n"
            + "    // { \"match\": \"minecraft:lava\", \"command\": \"say {player} fell into lava at {x} {y} {z}\", \"oncePerPlayer\": true, \"cooldownSeconds\": 0 }\n"
            + "  ]\n"
            + "}\n"},
        {"templates/_example.json", Templates.packSample()},
        {"deathmessages/_example.json", "{\n"
            + "  // Custom death messages (nothing changes while this is empty). Keys like renameMap (damage id, @entity, @item:, @dim:, wildcards);\n"
            + "  // values are message templates: the template variables, & color codes, or lang:your.translation.key.\n"
            + "  // Fabric servers replace the vanilla message for everyone. Forge / NeoForge servers send it to players who have What Hit Me?\n"
            + "  // (players without the mod keep the vanilla message).\n"
            + "  \"deathMessages\": {\n"
            + "    // \"mekanism:acid\": \"{player} dissolved in acid\",\n"
            + "    // \"*@twilightforest:lich\": \"{player} was outsmarted by the Lich\"\n"
            + "  }\n"
            + "}\n"},
        {"custom/_example.json", "{\n"
            + "  // Any file may contain any of the keys renameMap, hintMap, defaultIgnoredDamageIds, lockedFeatures, deathTriggers, templates, deathMessages.\n"
            + "  // Files are read in alphabetical path order; later files override earlier entries.\n"
            + "}\n"},
    };

    /** Samples added in 1.3 for pack folders created by older versions (only when their folder does not exist yet) */
    private static void addNewSamples(File dir) {
        for (String[] s : SAMPLES) {
            if (!s[0].startsWith("templates/") && !s[0].startsWith("deathmessages/")) continue;
            File f = new File(dir, s[0]);
            File p = f.getParentFile();
            if (p == null || p.exists()) continue;
            try {
                p.mkdirs();
                Files.write(f.toPath(), s[1].getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                Platform.log.warn("Could not write " + f, e);
            }
        }
    }

    private static void createSamples(File dir) throws IOException {

        for (String[] s : SAMPLES) {
            File f = new File(dir, s[0]);
            File p = f.getParentFile();
            if (p != null && !p.isDirectory()) p.mkdirs();
            if (!f.exists()) Files.write(f.toPath(), s[1].getBytes(StandardCharsets.UTF_8));
        }
    }
}
