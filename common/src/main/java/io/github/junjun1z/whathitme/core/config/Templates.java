package io.github.junjun1z.whathitme.core.config;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.data.Json;
import io.github.junjun1z.whathitme.core.text.Template;

/**
 * Where templates come from: the personal file config/whathitme/whathitme-client-templates.json (client) &gt; the pack
 * folder's "templates" sections &gt; the built-in output. lockedFeatures "templates" or "templates.&lt;output&gt;" ignore
 * the personal template.
 */
public final class Templates {

    public static final String PERSONAL_FILE = "whathitme-client-templates.json";

    private Templates() {}

    /** Example templates in English, the starting point of the editor and of the sample files */
    public static final Map<String, List<String>> EXAMPLES = new LinkedHashMap<>();

    static {
        EXAMPLES.put(Template.HOVER, Arrays.asList(
            "&l{name}",
            "{badges}",
            "&8ID: {damage_id}",
            "&cDamage: {damage} ❤ ({damage_hp} HP)",
            "&7Before armor: {raw_damage} ❤, {mitigation}% mitigated",
            "&eAbsorbed: {absorbed} ❤",
            "&7Health: {health_after} / {max_health} ❤",
            "&eMerged hits: x{hits}",
            "&fAttacker: {attacker_name}",
            "&7Via: {direct}",
            "&bWeapon: {weapon} &d{weapon_enchants}",
            "&7Distance: {attacker_distance}m",
            "&7Owner: {attacker_owner}",
            "&3Properties: {properties}",
            "&9Mod: {damage_mod}",
            "&7{environment}",
            "&aTip: {tip}"));
        EXAMPLES.put(Template.LINE, Arrays.asList(
            "&4{killing} &8{time_ago} &f{name} &c-{damage} ❤ &e{badges}"));
        EXAMPLES.put(Template.SUMMARY, Arrays.asList(
            "&6&l===== What Hit Me? =====",
            "&cKilling blow: &f{killing_blow} &c({damage} ❤)",
            "&3Properties: {properties}",
            "{badges}",
            "&7{environment}",
            "&6Last {total_seconds}s: {total} ❤ in {total_hits} hits",
            "&aTip: {tip}",
            "&b{recap_link}",
            "{toggle_notice}"));
        EXAMPLES.put(Template.ALERT, Arrays.asList(
            "&6{alert_kind}: &f{name} &c({damage} ❤) {details_link}"));
        EXAMPLES.put(Template.COPY, Arrays.asList(
            "**Death recap** ({time})",
            "Killing blow: {killing_blow} ({damage} hearts)",
            "Death message: {death_message}",
            "{timeline}",
            "Last {total_seconds}s: {total} hearts in {total_hits} hits",
            "{equipment}"));
        EXAMPLES.put(Template.DEATH, Arrays.asList(
            "mekanism:acid = {player} dissolved in acid"));
    }

    /** Reads a "templates" object: output -> string or list of lines. Unknown outputs and variables become warnings. */
    public static void readSection(Object section, String file, Map<String, Template> into, List<String> warnings) {
        if (!(section instanceof Map)) return;
        for (Map.Entry<String, Object> e : Json.obj(section).entrySet()) {
            String out = e.getKey().trim().toLowerCase(Locale.ROOT);
            if (!Template.isOutput(out)) {
                warnings.add(file + ": unknown template output \"" + e.getKey() + "\" (" + String.join(", ", Template.OUTPUTS) + ")");
                continue;
            }
            List<String> lines = lines(e.getValue());
            if (lines.isEmpty()) {
                into.remove(out);
                continue;
            }
            for (String u : Template.unknown(lines)) warnings.add(file + ": template " + out + ": unknown variable {" + u + "}");
            into.put(out, Template.of(lines));
        }
    }

    static List<String> lines(Object v) {
        List<String> out = new ArrayList<>();
        if (v instanceof String) {
            for (String l : ((String) v).split("\n", -1)) out.add(l);
        } else {
            for (Object o : Json.list(v)) out.add(Json.str(o, ""));
        }
        while (!out.isEmpty() && out.get(out.size() - 1).isEmpty()) out.remove(out.size() - 1);
        return out;
    }

    /** Is the personal template of this output locked by the modpack? */
    public static boolean locked(String output, Predicate<String> isLocked) {
        return isLocked.test("templates") || isLocked.test("templates." + output);
    }

    /** The templates in use: personal (unless locked) over the pack's */
    public static Map<String, Template> effective(Map<String, Template> personal, Map<String, Template> pack, Predicate<String> isLocked) {
        Map<String, Template> out = new LinkedHashMap<>();
        if (pack != null) out.putAll(pack);
        if (personal != null) {
            for (Map.Entry<String, Template> e : personal.entrySet()) if (!locked(e.getKey(), isLocked)) out.put(e.getKey(), e.getValue());
        }
        return out;
    }

    // ---------------------------------------------------------------- files

    public static File personalFile() {
        return new File(Platform.whmConfigDir(), PERSONAL_FILE);
    }

    private static String commented(String indent, List<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (String l : lines) sb.append(indent).append("//   ").append(quote(l)).append(",\n");
        return sb.toString();
    }

    private static String quote(String s) {
        return Json.write(s).trim();
    }

    private static String examples(String indent) {
        StringBuilder sb = new StringBuilder();
        for (String o : Template.OUTPUTS) {
            sb.append(indent).append("// \"").append(o).append("\": [\n");
            sb.append(commented(indent, EXAMPLES.get(o)));
            sb.append(indent).append("// ],\n");
        }
        return sb.toString();
    }

    private static final String HELP = "  // Templates: one list of lines per output. {variable} placeholders, {variable|fallback}, {damage:2} (decimals), & color codes,\n"
        + "  // a line \"lang:your.translation.key\" is translated. A line whose placeholders are all empty is left out.\n"
        + "  // Outputs: hover (death screen and /whm hover details), line (death screen panel line and /whm history line), summary\n"
        + "  // (respawn chat summary), alert (totem / heavy hit alert), copy ([Copy Recap] text). In game: /whm template opens the editor.\n";

    /** Sample of the pack folder (every example commented out) */
    public static String packSample() {
        return "{\n" + HELP
            + "  // Players can override these in " + PERSONAL_FILE + " unless lockedFeatures contains \"templates\" or \"templates.<output>\".\n"
            + "  // Remove the // in front of the lines of a template to use it.\n"
            + "  \"templates\": {\n" + examples("    ") + "  }\n}\n";
    }

    private static String personalSample() {
        return "{\n" + HELP
            + "  // Personal templates (they beat the modpack's, unless it locked them). Remove the // to use an example.\n"
            + "  \"templates\": {\n" + examples("    ") + "  }\n}\n";
    }

    /** Reads the personal file (created with commented examples when missing) */
    public static Map<String, Template> loadPersonal(List<String> warnings) {
        Map<String, Template> out = new LinkedHashMap<>();
        File f = personalFile();
        try {
            if (!f.isFile()) {
                write(f, personalSample());
                return out;
            }
            Object root = Json.parse(CfgFile.decode(Files.readAllBytes(f.toPath())));
            readSection(Json.obj(root).get("templates"), PERSONAL_FILE, out, warnings);
        } catch (Exception e) {
            warnings.add(PERSONAL_FILE + ": " + e.getMessage());
        }
        return out;
    }

    /** Stored lines of every personal template (to rewrite the file) */
    private static Map<String, Object> personalRaw() {
        Map<String, Object> m = new LinkedHashMap<>();
        File f = personalFile();
        try {
            if (f.isFile()) {
                Object t = Json.obj(Json.parse(CfgFile.decode(Files.readAllBytes(f.toPath())))).get("templates");
                if (t instanceof Map) for (Map.Entry<String, Object> e : Json.obj(t).entrySet()) m.put(e.getKey().toLowerCase(Locale.ROOT), lines(e.getValue()));
            }
        } catch (Exception ignored) {
            // rewritten from scratch
        }
        return m;
    }

    /** Saves (lines empty: removes) one personal template */
    public static void savePersonal(String output, List<String> lines) throws IOException {
        Map<String, Object> m = personalRaw();
        if (lines == null || lines.isEmpty()) m.remove(output);
        else m.put(output, new ArrayList<>(lines));
        if (m.isEmpty()) {
            write(personalFile(), personalSample());
            return;
        }
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("templates", m);
        write(personalFile(), "// Personal What Hit Me? templates (written by the template editor). See the pack folder's templates/_example.json.\n"
            + Json.write(root));
    }

    /** Removes every personal template */
    public static void resetPersonal() throws IOException {
        write(personalFile(), personalSample());
    }

    /** The pack file the editor writes for an output */
    public static File packFile(String output) {
        return new File(Platform.whmConfigDir(), "pack/templates/" + output + ".json");
    }

    /** Writes config/whathitme/pack/templates/&lt;output&gt;.json (deathmessages: "match = template" lines) */
    public static void savePack(String output, List<String> lines) throws IOException {
        Map<String, Object> root = new LinkedHashMap<>();
        if (Template.DEATH.equals(output)) {
            Map<String, Object> dm = new LinkedHashMap<>();
            for (String l : lines) {
                int eq = l.indexOf('=');
                if (eq > 0) dm.put(l.substring(0, eq).trim(), l.substring(eq + 1).trim());
            }
            root.put("deathMessages", dm);
        } else {
            Map<String, Object> t = new LinkedHashMap<>();
            t.put(output, new ArrayList<>(lines));
            root.put("templates", t);
        }
        write(packFile(output), "// Written by the What Hit Me? template editor.\n" + Json.write(root));
    }

    private static void write(File f, String text) throws IOException {
        File p = f.getParentFile();
        if (p != null && !p.isDirectory()) p.mkdirs();
        Files.write(f.toPath(), text.getBytes(StandardCharsets.UTF_8));
    }
}
