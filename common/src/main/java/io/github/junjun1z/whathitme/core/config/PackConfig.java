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
 * Modpack definitions in config/whathitme/pack/: every .json file (recursively, by its "order" number, then in alphabetical
 * path order; "enabled": false skips a file) is read and merged; later files override earlier entries of the same key. Keys: renameMap, hintMap (objects "key": "value", or
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
        /** if / any / unless (progression conditions of the dead player) */
        public final io.github.junjun1z.whathitme.core.rules.Conds conds;

        public DeathTrigger(String match, String command, boolean oncePerPlayer, int cooldownSeconds, String source) {
            this(match, command, oncePerPlayer, cooldownSeconds, source, io.github.junjun1z.whathitme.core.rules.Conds.NONE);
        }

        public DeathTrigger(String match, String command, boolean oncePerPlayer, int cooldownSeconds, String source,
                            io.github.junjun1z.whathitme.core.rules.Conds conds) {
            this.conds = conds == null ? io.github.junjun1z.whathitme.core.rules.Conds.NONE : conds;
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
    /** Files switched off with "enabled": false */
    public final List<String> disabled = new ArrayList<>();
    public final List<String> errors = new ArrayList<>();
    /** Templates of the outputs (later files override whole templates) */
    public final Map<String, io.github.junjun1z.whathitme.core.text.Template> templates = new LinkedHashMap<>();
    /** Custom death messages: composite match key -> message template */
    public final Map<String, String> deathMessages = new LinkedHashMap<>();
    /** Progression rules in file order (first match wins) */
    public final List<io.github.junjun1z.whathitme.core.rules.Rule> rules = new ArrayList<>();
    /** Unknown template variables and output names ("file: ...") */
    public final List<String> warnings = new ArrayList<>();
    /** Parsed files {name, root}, kept to merge the script entries again without reading the files */
    private final List<Object[]> roots = new ArrayList<>();

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
            // load order: "order" then path; "enabled": false switches a file off
            for (io.github.junjun1z.whathitme.core.pack.PackFiles.Cat cat : io.github.junjun1z.whathitme.core.pack.PackFiles.scan(dir)) {
                String name = cat.key();
                if (cat.root == null) {
                    c.errors.add(name + ": " + cat.error);
                    Platform.log.warn("What Hit Me? pack file " + name + " skipped: " + cat.error, null);
                    continue;
                }
                if (!cat.enabled) {
                    c.disabled.add(name);
                    continue;
                }
                try {
                    c.merge(cat.root, name);
                    c.roots.add(new Object[] {name, cat.root});
                    c.files.add(name);
                } catch (Exception e) {
                    c.errors.add(name + ": " + e.getMessage());
                    Platform.log.warn("What Hit Me? pack file " + name + " skipped: " + e.getMessage(), null);
                }
            }
        } catch (Throwable t) {
            Platform.log.warn("Could not read the What Hit Me? pack folder", t);
        }
        c.mergeRuntime();
        return c;
    }

    /** The pack files merged again with the current script entries (an extra source after the files) */
    public PackConfig withRuntime() {
        PackConfig c = new PackConfig();
        for (Object[] r : roots) {
            try {
                c.merge(r[1], (String) r[0]);
            } catch (Exception e) {
                c.errors.add(r[0] + ": " + e.getMessage());
            }
        }
        c.roots.addAll(roots);
        c.files.addAll(files);
        c.disabled.addAll(disabled);
        c.errors.addAll(errors);
        c.mergeRuntime();
        return c;
    }

    /** Entries scripts added through the API ("script:<owner>"); never part of {@link #files} */
    private void mergeRuntime() {
        try {
            for (Object[] r : io.github.junjun1z.whathitme.core.script.ScriptRegistry.packRoots()) {
                try {
                    merge(r[1], (String) r[0]);
                } catch (Exception e) {
                    errors.add(r[0] + ": " + e.getMessage());
                }
            }
        } catch (Throwable t) {
            Platform.log.warn("Could not merge the What Hit Me? script entries", t);
        }
    }

    private static void map(Object v, Map<String, String> into) {
        map(v, into, null, null, null);
    }

    private static void map(Object v, Map<String, String> into, String file, String section, List<String> warnings) {
        if (v instanceof Map) {
            for (Map.Entry<String, Object> e : Json.obj(v).entrySet()) {
                String k = e.getKey().trim();
                if (e.getValue() instanceof Map || e.getValue() instanceof List) {
                    if (warnings != null) warnings.add(file + ": " + section + " \"" + k + "\": the value must be a text");
                    continue;
                }
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

    /** Sections and the JSON type they need ("map": object or "key = value" list, "list", "object") */
    private static final String[][] SECTION_TYPES = {
        {"renameMap", "map"}, {"hintMap", "map"}, {"deathMessages", "map"}, {"defaultIgnoredDamageIds", "list"},
        {"lockedFeatures", "list"}, {"deathTriggers", "list"}, {"rules", "list"}, {"templates", "object"}};

    /** Wrong section types are skipped with a warning; a single text / object becomes a one-element list */
    private static Map<String, Object> checkTypes(Map<String, Object> in, String file, List<String> warnings) {
        Map<String, Object> o = new LinkedHashMap<>(in);
        for (String[] st : SECTION_TYPES) {
            Object v = o.get(st[0]);
            if (v == null) continue;
            boolean okType = "map".equals(st[1]) ? v instanceof Map || v instanceof List : "list".equals(st[1]) ? v instanceof List : v instanceof Map;
            if (okType) continue;
            if ("list".equals(st[1]) && (v instanceof String || v instanceof Map && (st[0].equals("rules") || st[0].equals("deathTriggers")))) {
                warnings.add(file + ": " + st[0] + " should be a list [ ... ] (read as one entry)");
                o.put(st[0], new ArrayList<Object>(java.util.Collections.singletonList(v)));
                continue;
            }
            warnings.add(file + ": " + st[0] + " is ignored: expected " + ("map".equals(st[1]) || "object".equals(st[1]) ? "an object { ... }" : "a list [ ... ]"));
            o.remove(st[0]);
        }
        return o;
    }

    void merge(Object root, String file) {
        Map<String, Object> o = checkTypes(Json.obj(root), file, warnings);
        map(o.get("renameMap"), renameMap, file, "renameMap", warnings);
        map(o.get("hintMap"), hintMap, file, "hintMap", warnings);
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
            triggers.add(new DeathTrigger(match, cmd, Json.bool(t.get("oncePerPlayer"), false), (int) Json.num(t.get("cooldownSeconds"), 0), file,
                io.github.junjun1z.whathitme.core.rules.Conds.read(t)));
        }
        int n = 0;
        for (Object v : Json.list(o.get("rules"))) {
            n++;
            if (!(v instanceof Map)) {
                warnings.add(file + ": rules #" + n + " is not an object");
                continue;
            }
            io.github.junjun1z.whathitme.core.rules.Rule r = io.github.junjun1z.whathitme.core.rules.Rule.read(Json.obj(v), file + " #" + n, warnings);
            rules.add(r);
            List<String> texts = new ArrayList<>();
            if (r.name != null) texts.add(r.name);
            if (r.hint != null) texts.add(r.hint);
            for (String u : io.github.junjun1z.whathitme.core.text.Template.unknown(texts)) warnings.add(file + " #" + n + ": rule \"" + r.match + "\": unknown variable {" + u + "}");
        }
        Templates.readSection(o.get("templates"), file, templates, warnings);
        Map<String, String> dm = new LinkedHashMap<>(deathMessages);
        map(o.get("deathMessages"), dm, file, "deathMessages", warnings);
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
            + "  // if / any / unless: progression conditions of the dead player, like in rules (e.g. \"unless\": [\"tag:veteran\"]).\n"
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
        {"rules/_example.json", "{\n"
            + "  // Progression rules: the server decides per player what a death shows. Evaluated top to bottom, the FIRST matching rule wins.\n"
            + "  // match: a key like renameMap (damage id, @entity, @item:, @dim:, wildcards). if: all of these, any: at least one, unless: none of these.\n"
            + "  // Conditions: advancement:<id>  tag:<scoreboard tag>  score:<objective>>=<n> (also > <= < = !=)  stage:<Game Stages stage>\n"
            + "  //             ftbquests:<quest or chapter id>  bq:<Better Questing quest id>  heracles:<quest id>\n"
            + "  //             script:<name> (registered by KubeJS / CraftTweaker scripts)   (\"!\" in front negates)\n"
            + "  // name / hint: replace the damage name / tip (template variables, & colors, lang:keys; \"hint\": \"\" = no tip).\n"
            + "  // hide: damage_id, attacker, mod, weapon, death_message, tip, properties, environment, owner, distance, position.\n"
            + "  // Hidden fields are never sent. hide implies \"enforce\": true (personal renames / tips / templates do not apply) unless \"enforce\": false.\n"
            + "  // commands: server commands run on a matching death (placeholders like deathTriggers; oncePerPlayer / cooldownSeconds work too).\n"
            + "  // Check with /whm test <damage_id> [entity] --as <player> --assume <condition>.\n"
            + "  \"rules\": [\n"
            + "    // { \"match\": \"*@twilightforest:lich\", \"unless\": [\"advancement:twilightforest:progress_lich\"],\n"
            + "    //   \"name\": \"&k???\", \"hint\": \"\", \"hide\": [\"damage_id\", \"attacker\", \"mod\", \"weapon\", \"death_message\"] },\n"
            + "    // { \"match\": \"minecraft:lava\", \"if\": [\"score:deaths>=3\"], \"hint\": \"Carry a water bucket!\" }\n"
            + "  ]\n"
            + "}\n"},
        {"custom/_example.json", "{\n"
            + "  // Any file may contain any of the keys renameMap, hintMap, defaultIgnoredDamageIds, lockedFeatures, deathTriggers, templates, deathMessages, rules.\n"
            + "  // Files are read in alphabetical path order; later files override earlier entries.\n"
            + "  // Optional: \"order\": 10 (load order, lower first, default 0), \"enabled\": false (skip this file), \"memo\": \"...\" (a note).\n"
            + "  // In game: /whm pack opens the pack editor (operators).\n"
            + "}\n"},
    };

    /** Samples added in 1.3 for pack folders created by older versions (only when their folder does not exist yet) */
    private static void addNewSamples(File dir) {
        for (String[] s : SAMPLES) {
            if (!s[0].startsWith("templates/") && !s[0].startsWith("deathmessages/") && !s[0].startsWith("rules/")) continue;
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
