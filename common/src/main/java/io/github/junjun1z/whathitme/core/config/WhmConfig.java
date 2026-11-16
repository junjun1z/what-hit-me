package io.github.junjun1z.whathitme.core.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Pattern;

import io.github.junjun1z.whathitme.core.Platform;

/**
 * Two configuration files in config/whathitme/: whathitme-common.cfg (modpack / server limits) and whathitme-client.cfg
 * (personal). A display element is shown only when it is enabled in both. Every key, default, range and comment is
 * declared here; the storage is a {@link ConfigBackend}.
 */
public final class WhmConfig {

    /** Which file(s) a display switch lives in */
    public enum Scope { BOTH, COMMON, CLIENT }

    /** Display switches. The ordinal is the bit sent to clients: never reorder, only append. */
    public enum Flag {
        DEATH_SCREEN_PANEL("enableDeathScreenPanel", "Show the damage recap panel on the death screen."),
        RESPAWN_CHAT_SUMMARY("enableRespawnChatSummary", "Show the death summary in chat after respawning (/whm togglechat)."),
        TOTEM_CHAT_ALERT("enableTotemChatAlert", "Show a chat alert with a details link when a Totem of Undying saves you."),
        HEAVY_HIT_SNAPSHOT("enableHeavyHitSnapshot", "Save a snapshot and alert when one hit takes heavyHitThreshold of your max health."),
        TIMESTAMP("showTimestamp", "How long ago the damage happened."),
        DAMAGE_AMOUNT("showDamageAmount", "Final damage amount."),
        MITIGATION_DETAILS("showMitigationDetails", "Damage before armor, mitigation % and absorption (yellow hearts) used."),
        HIT_COUNT("showSmartMergeHitCount", "Hit counter of merged repeated damage (x12 hits)."),
        SOURCE_MOD_NAME("showSourceModName", "Name of the mod that added the damage or the attacker."),
        DAMAGE_PROPERTIES("showDamageProperties", "Properties such as Fire, Magic, Bypasses Armor."),
        INTERNAL_DAMAGE_ID("showInternalDamageId", "Internal damage id in the hover text."),
        ATTACKER_NAME("showAttackerName", "Attacker name."),
        ATTACKER_WEAPON("showAttackerWeapon", "Attacker weapon and its enchantments."),
        ATTACKER_DISTANCE("showAttackerDistance", "Distance to the attacker."),
        ATTACKER_INVISIBILITY("showAttackerInvisibility", "Badge when the attacker was invisible."),
        ENTITY_OWNER("showEntityOwner", "Owner of turrets, pets, summons and projectiles."),
        /** showHintsAndTips (both files) up to 1.1; in the common file its bit is enableAutoHints || enableCustomHints */
        HINTS_AND_TIPS("showHints", "Show counter-measure tips (automatic tips and the modpack's hintMap).", Scope.CLIENT),
        ACTION_BUTTONS("showActionButtons", "Clickable [Copy ID], [Mute] and [Copy Recap] buttons."),
        TOGGLE_CHAT_NOTICE("showToggleChatNotice", "The '/whm togglechat' notice at the end of the summary."),
        AUTO_HINTS("enableAutoHints", "Automatic tips from the damage type and properties.", Scope.COMMON),
        CUSTOM_HINTS("enableCustomHints", "Custom tips from hintMap (config files and the pack folder).", Scope.COMMON),
        MAX_HP_PERCENT("showMaxHpPercentage", "Damage as a percentage of max health, e.g. (30% Max HP)."),
        ENVIRONMENT("showEnvironmentContext", "For damage without an attacker: the block / fluid at your feet and body and the items in your hands."),
        REFLECTED_BADGE("showReflectedBadge", "[Reflected / Thorns] badge for thorns and damage reflected by an entity you just hit."),
        ARMOR_BREAK("showArmorBreakEvents", "Timeline line when an armor piece breaks."),
        EQUIPMENT_IN_COPY("includeEquipmentInCopy", "[Copy Recap] adds dimension, max health, armor and effects."),
        COORDS_IN_COPY("includeCoordsInCopy", "[Copy Recap] adds the death coordinates (needs includeEquipmentInCopy).");

        public final String key;
        public final String comment;
        public final Setting setting;
        public final Scope scope;

        Flag(String key, String comment) {
            this(key, comment, Scope.BOTH);
        }

        Flag(String key, String comment, Scope scope) {
            this.key = key;
            this.comment = comment;
            this.scope = scope;
            this.setting = Setting.bool(CAT_DISPLAY, key, true, comment);
        }

        public int bit() {
            return 1 << ordinal();
        }

        public static Flag byKey(String key) {
            for (Flag f : values()) if (f.key.equalsIgnoreCase(key)) return f;
            return null;
        }
    }

    /** Bits known by What Hit Me? 1.0 / 1.1 servers; clients treat newer bits of such servers as on */
    public static final int LEGACY_FLAGS = (1 << 19) - 1;

    public static final int ALL_FLAGS = (1 << Flag.values().length) - 1;

    public static final String CAT_DISPLAY = "display";
    public static final String CAT_FILTERS = "filters";
    public static final String CAT_TRACKING = "tracking";
    public static final String CAT_LAYOUT = "layout";

    public static final String[] ANCHORS = {"TOP_LEFT", "TOP_RIGHT", "BOTTOM_LEFT", "BOTTOM_RIGHT", "CENTER_LEFT", "CENTER_RIGHT"};

    public static final String COMMENT_DISPLAY = "Display switches. An element is shown only when it is true in BOTH whathitme-common.cfg and whathitme-client.cfg.";
    public static final String COMMENT_FILTERS = "What is recorded and how it is named. In whathitme-common.cfg these apply to everyone (a server with the mod sends them to clients); in whathitme-client.cfg they add personal entries.";
    public static final String COMMENT_TRACKING = "Recording (server side, or the client when the server does not have the mod).";
    public static final String COMMENT_LAYOUT = "Death screen panel layout. Drag the panel header on the death screen to move it.";

    // ---- filters (both files)
    public static final Setting MIN_DAMAGE = Setting.decimal(CAT_FILTERS, "minDamageThreshold", 0.05f, 0f, 1000f, "Damage below this (health + absorption) is not recorded.");
    public static final Setting HEAVY_HIT = Setting.decimal(CAT_FILTERS, "heavyHitThreshold", 0.8f, 0.05f, 100f,
        "A single hit taking at least this part of your max health is a heavy hit (0.8 = 80%).");
    public static final Setting IGNORED = Setting.list(CAT_FILTERS, "ignoredDamageIds", "Damage ids to hide (wildcard * allowed), e.g. minecraft:cactus or mob:minecraft:*");
    public static final Setting RENAME = Setting.list(CAT_FILTERS, "renameMap",
        "Custom display names, one per line: \"modid:damage_id = Custom Name\". & or section sign color codes work. Use \"modid:damage_id = lang:your.translation.key\" to translate it with a resource pack. Wildcards (*) are allowed in the id. /whm dump writes config/whathitme/hint_template.cfg with every known id.");
    public static final Setting HINT = Setting.list(CAT_FILTERS, "hintMap",
        "Custom tips, one per line: \"modid:damage_id = Tip text\" or \"modid:damage_id = lang:your.translation.key\". Ids without an entry get an automatic tip from the damage properties. Example: \"mob:minecraft:creeper = Creepers hiss before exploding - sprint away!\"");
    // ---- tracking (common only)
    public static final Setting HISTORY_SIZE = Setting.integer(CAT_TRACKING, "historySize", 50, 5, 500, "Damage records kept per player (fixed-size ring buffer).");
    public static final Setting MERGE_WINDOW = Setting.decimal(CAT_TRACKING, "mergeWindowSeconds", 3.0f, 0f, 60f,
        "Repeated damage with the same id and source within this many seconds is merged into one line (0 = off).");
    public static final Setting SUMMARY_SECONDS = Setting.integer(CAT_TRACKING, "summarySeconds", 10, 1, 300, "Time window of the 'Last Xs Total' line.");
    public static final Setting RECAP_SECONDS = Setting.integer(CAT_TRACKING, "recapSeconds", 30, 1, 600, "Damage from this many seconds before death is kept in the death recap.");
    public static final Setting MAX_SNAPSHOTS = Setting.integer(CAT_TRACKING, "maxSnapshots", 5, 1, 50, "Totem / heavy-hit snapshots kept per player.");
    // ---- layout (client only)
    public static final Setting ANCHOR = Setting.string(CAT_LAYOUT, "anchorPoint", "CENTER_LEFT", ANCHORS, "Death screen panel anchor.");
    public static final Setting OFFSET_X = Setting.integer(CAT_LAYOUT, "offsetX", 4, -4000, 4000, "Horizontal offset from the anchor (GUI pixels). Saved when you drag the panel.");
    public static final Setting OFFSET_Y = Setting.integer(CAT_LAYOUT, "offsetY", 0, -4000, 4000, "Vertical offset from the anchor (GUI pixels). Saved when you drag the panel.");
    public static final Setting SCALE = Setting.decimal(CAT_LAYOUT, "scale", 0.85f, 0.25f, 3f, "Panel scale.");
    public static final Setting OPACITY = Setting.decimal(CAT_LAYOUT, "backgroundOpacity", 0.75f, 0f, 1f, "Panel background opacity (0.0 - 1.0).");
    public static final Setting MAX_LINES = Setting.integer(CAT_LAYOUT, "maxVisibleLines", 8, 1, 40, "Damage lines visible at once (scroll with the mouse wheel for more).");
    public static final Setting MINIMIZED = Setting.bool(CAT_LAYOUT, "minimized", false, "Panel minimized with the [-] button.");
    public static final Setting CHAT_LINES = Setting.integer(CAT_LAYOUT, "chatHistoryLines", 15, 1, 100, "Lines shown by /whm history.");

    /** Settings that exist in both files */
    public abstract static class Base {
        public final ConfigBackend backend;
        public int mask = ALL_FLAGS;
        public float minDamageThreshold;
        public float heavyHitThreshold;
        public List<String> ignoredDamageIds = Collections.emptyList();
        private List<Pattern> ignorePatterns = Collections.emptyList();
        public Map<String, String> renameMap = Collections.emptyMap();
        public Map<String, String> hintMap = Collections.emptyMap();

        Base(ConfigBackend backend) {
            this.backend = backend;
        }

        /** Does this file have the display switch? */
        public abstract boolean has(Flag f);

        /** Every setting of this file */
        public List<Setting> settings() {
            List<Setting> out = new ArrayList<>();
            for (Flag f : Flag.values()) if (has(f)) out.add(f.setting);
            out.addAll(Arrays.asList(MIN_DAMAGE, HEAVY_HIT, IGNORED, RENAME, HINT));
            return out;
        }

        public final synchronized void load() {
            backend.load();
            read();
            if (backend.hasChanged()) backend.save();
        }

        protected static boolean bool(String v) {
            return Boolean.parseBoolean(v.trim());
        }

        protected static int integer(String v, Setting s) {
            try {
                return Integer.parseInt(v.trim());
            } catch (NumberFormatException e) {
                return Integer.parseInt(s.def);
            }
        }

        protected static float decimal(String v, Setting s) {
            try {
                return Float.parseFloat(v.trim());
            } catch (NumberFormatException e) {
                return Float.parseFloat(s.def);
            }
        }

        /** showHintsAndTips (1.0 / 1.1) became showHints / enableAutoHints / enableCustomHints */
        private void migrate() {
            String old = backend.raw(CAT_DISPLAY, "showHintsAndTips");
            if (old == null) return;
            if (!bool(old)) {
                for (Flag f : new Flag[] {Flag.HINTS_AND_TIPS, Flag.AUTO_HINTS, Flag.CUSTOM_HINTS}) if (has(f)) backend.set(f.setting, "false");
            }
            backend.remove(CAT_DISPLAY, "showHintsAndTips");
        }

        void read() {
            migrate();
            int m = 0;
            for (Flag f : Flag.values()) if (!has(f) || bool(backend.get(f.setting))) m |= f.bit();
            mask = m;
            backend.categoryLangKey(CAT_DISPLAY, "whathitme.config.cat.display");
            backend.categoryLangKey(CAT_FILTERS, "whathitme.config.cat.filters");
            backend.categoryComment(CAT_DISPLAY, COMMENT_DISPLAY);
            minDamageThreshold = decimal(backend.get(MIN_DAMAGE), MIN_DAMAGE);
            heavyHitThreshold = decimal(backend.get(HEAVY_HIT), HEAVY_HIT);
            String[] ign = backend.getList(IGNORED);
            ignoredDamageIds = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(ign)));
            List<Pattern> pats = new ArrayList<>();
            for (String s : ign) {
                String t = s.trim();
                if (!t.isEmpty()) pats.add(glob(t));
            }
            ignorePatterns = pats;
            renameMap = parseMap(backend.getList(RENAME));
            backend.categoryComment(CAT_FILTERS, COMMENT_FILTERS);
            hintMap = parseMap(backend.getList(HINT));
        }

        public boolean isIgnored(String id) {
            List<Pattern> pats = ignorePatterns;
            for (Pattern p : pats) if (p.matcher(id).matches()) return true;
            return false;
        }

        public synchronized void save() {
            backend.save();
        }

        /** After an in-game config screen changed values: write the file and apply the values */
        public final synchronized void saveAndReload() {
            if (backend.hasChanged()) backend.save();
            read();
        }

        /** Adds or removes an id in ignoredDamageIds. @return true when it is ignored now */
        public synchronized boolean toggleIgnore(String id) {
            List<String> list = new ArrayList<>(Arrays.asList(backend.getList(IGNORED)));
            boolean now;
            if (list.remove(id)) {
                now = false;
            } else {
                list.add(id);
                now = true;
            }
            backend.setList(IGNORED, list.toArray(new String[0]));
            backend.save();
            read();
            return now;
        }

        /** Sets (or with an empty value removes) an entry of renameMap / hintMap */
        public synchronized void putMapEntry(String listKey, String id, String value) {
            Setting s = "renameMap".equals(listKey) ? RENAME : HINT;
            List<String> list = new ArrayList<>();
            for (String e : backend.getList(s)) {
                int eq = e.indexOf('=');
                if (eq >= 0 && e.substring(0, eq).trim().equals(id)) continue;
                list.add(e);
            }
            if (value != null && !value.trim().isEmpty()) list.add(id + " = " + value.trim());
            backend.setList(s, list.toArray(new String[0]));
            backend.save();
            read();
        }

        /** Every setting by name, case-insensitive (for /whm config) */
        public Map<String, Setting> byKey() {
            Map<String, Setting> out = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            for (Setting s : settings()) out.put(s.key, s);
            return out;
        }

        public List<String> keys() {
            return new ArrayList<>(byKey().keySet());
        }

        /**
         * Changes one setting.
         *
         * @return null when done, "unknown" or "invalid" otherwise
         */
        public synchronized String set(String key, String value) {
            Setting s = byKey().get(key);
            if (s == null) return "unknown";
            if (isLocked(s.key)) return "locked";
            switch (s.type) {
                case LIST:
                    backend.setList(s, value.isEmpty() ? new String[0] : value.split("\\s*,\\s*"));
                    break;
                case BOOLEAN: {
                    String v = value.toLowerCase(Locale.ROOT);
                    if (v.equals("on") || v.equals("yes")) v = "true";
                    if (v.equals("off") || v.equals("no")) v = "false";
                    if (v.equals("toggle")) v = String.valueOf(!bool(backend.get(s)));
                    if (!v.equals("true") && !v.equals("false")) return "invalid";
                    backend.set(s, v);
                    break;
                }
                case INT:
                    try {
                        int i = Integer.parseInt(value.trim());
                        if (i < s.min || i > s.max) return "invalid";
                        backend.set(s, String.valueOf(i));
                    } catch (NumberFormatException e) {
                        return "invalid";
                    }
                    break;
                case FLOAT:
                    try {
                        double d = Double.parseDouble(value.trim());
                        if (d < s.min || d > s.max) return "invalid";
                        backend.set(s, value.trim());
                    } catch (NumberFormatException e) {
                        return "invalid";
                    }
                    break;
                default: {
                    String v = value;
                    if (s.valid != null && s.valid.length > 0) {
                        String match = null;
                        for (String x : s.valid) if (x.equalsIgnoreCase(value)) match = x;
                        if (match == null) return "invalid";
                        v = match;
                    }
                    backend.set(s, v);
                }
            }
            backend.save();
            read();
            return null;
        }

        /** Current value as text (lists joined with ", "), null for an unknown key */
        public synchronized String get(String key) {
            Setting s = byKey().get(key);
            if (s == null) return null;
            return s.isList() ? String.join(", ", backend.getList(s)) : backend.get(s);
        }

        /** Tab completion values for /whm config &lt;key&gt; */
        public List<String> values(String key) {
            Setting s = byKey().get(key);
            if (s == null) return Collections.emptyList();
            if (s.type == Setting.Type.BOOLEAN) return Arrays.asList("true", "false");
            if (s.valid != null && s.valid.length > 0) return Arrays.asList(s.valid);
            String v = s.isList() ? String.join(",", backend.getList(s)) : backend.get(s);
            return Collections.singletonList(v);
        }
    }

    public static final class Common extends Base {
        public int historySize;
        public float mergeWindowSeconds;
        public int summarySeconds;
        public int recapSeconds;
        public int maxSnapshots;

        public Common(ConfigBackend b) {
            super(b);
        }

        @Override
        public boolean has(Flag f) {
            return f.scope != Scope.CLIENT;
        }

        @Override
        public List<Setting> settings() {
            List<Setting> out = super.settings();
            out.addAll(Arrays.asList(HISTORY_SIZE, MERGE_WINDOW, SUMMARY_SECONDS, RECAP_SECONDS, MAX_SNAPSHOTS));
            return out;
        }

        @Override
        void read() {
            super.read();
            historySize = integer(backend.get(HISTORY_SIZE), HISTORY_SIZE);
            mergeWindowSeconds = decimal(backend.get(MERGE_WINDOW), MERGE_WINDOW);
            summarySeconds = integer(backend.get(SUMMARY_SECONDS), SUMMARY_SECONDS);
            recapSeconds = integer(backend.get(RECAP_SECONDS), RECAP_SECONDS);
            maxSnapshots = integer(backend.get(MAX_SNAPSHOTS), MAX_SNAPSHOTS);
            if ((mask & (Flag.AUTO_HINTS.bit() | Flag.CUSTOM_HINTS.bit())) == 0) mask &= ~Flag.HINTS_AND_TIPS.bit();
            backend.categoryLangKey(CAT_TRACKING, "whathitme.config.cat.tracking");
            backend.categoryComment(CAT_TRACKING, COMMENT_TRACKING);
        }
    }

    public static final class Client extends Base {
        public String anchorPoint;
        public int offsetX;
        public int offsetY;
        public float scale;
        public float backgroundOpacity;
        public int maxVisibleLines;
        public boolean minimized;
        public int chatHistoryLines;
        /** Personal templates (whathitme-client-templates.json) */
        public Map<String, io.github.junjun1z.whathitme.core.text.Template> templates = Collections.emptyMap();
        public List<String> templateWarnings = Collections.emptyList();

        public Client(ConfigBackend b) {
            super(b);
        }

        @Override
        public boolean has(Flag f) {
            return f.scope != Scope.COMMON;
        }

        @Override
        public List<Setting> settings() {
            List<Setting> out = super.settings();
            out.addAll(Arrays.asList(ANCHOR, OFFSET_X, OFFSET_Y, SCALE, OPACITY, MAX_LINES, MINIMIZED, CHAT_LINES));
            return out;
        }

        @Override
        void read() {
            super.read();
            anchorPoint = backend.get(ANCHOR);
            offsetX = integer(backend.get(OFFSET_X), OFFSET_X);
            offsetY = integer(backend.get(OFFSET_Y), OFFSET_Y);
            scale = decimal(backend.get(SCALE), SCALE);
            backgroundOpacity = decimal(backend.get(OPACITY), OPACITY);
            maxVisibleLines = integer(backend.get(MAX_LINES), MAX_LINES);
            minimized = bool(backend.get(MINIMIZED));
            chatHistoryLines = integer(backend.get(CHAT_LINES), CHAT_LINES);
            List<String> warn = new ArrayList<>();
            templates = Collections.unmodifiableMap(Templates.loadPersonal(warn));
            templateWarnings = Collections.unmodifiableList(warn);

            backend.categoryLangKey(CAT_LAYOUT, "whathitme.config.cat.layout");
            backend.categoryComment(CAT_LAYOUT, COMMENT_LAYOUT);
        }

        public synchronized void saveLayout(int x, int y, boolean min) {
            backend.set(OFFSET_X, String.valueOf(x));
            backend.set(OFFSET_Y, String.valueOf(y));
            backend.set(MINIMIZED, String.valueOf(min));
            backend.save();
            offsetX = x;
            offsetY = y;
            minimized = min;
        }
    }

    public static volatile Common common;
    /** config/whathitme/pack/ (merged), never null */
    public static volatile PackConfig pack = PackConfig.EMPTY;
    /** null on a dedicated server */
    public static volatile Client client;

    private WhmConfig() {}

    /**
     * Loads config/whathitme/whathitme-common.cfg (and whathitme-client.cfg on a client).
     *
     * @param backends creates the storage of one file ({@code CfgFile::new} except on 1.12.2)
     */
    public static void init(File configDir, boolean clientSide, Function<File, ConfigBackend> backends) {
        File dir = new File(configDir, "whathitme");
        if (!dir.isDirectory()) dir.mkdirs();
        pack = PackConfig.load(dir);
        Common c = new Common(backends.apply(new File(dir, "whathitme-common.cfg")));
        c.load();
        common = c;
        if (clientSide) {
            Client cl = new Client(backends.apply(new File(dir, "whathitme-client.cfg")));
            cl.load();
            client = cl;
        }
    }

    /** Re-reads whathitme-common.cfg and the pack folder */
    public static void reloadCommon() {
        pack = PackConfig.load(Platform.whmConfigDir());
        common.load();
    }

    /** A key locked by the modpack (lockedFeatures) */
    public static boolean isLocked(String key) {
        return pack.isLocked(key);
    }

    /** config/whathitme/.cache/ */
    public static File cacheDir() {
        File d = new File(Platform.whmConfigDir(), ".cache");
        if (!d.isDirectory()) d.mkdirs();
        return d;
    }

    public static void reloadClient() {
        if (client != null) client.load();
    }

    public static Map<String, String> parseMap(String[] lines) {
        Map<String, String> out = new LinkedHashMap<>();
        for (String s : lines) {
            int eq = s.indexOf('=');
            if (eq <= 0) continue;
            String k = s.substring(0, eq).trim();
            String v = s.substring(eq + 1).trim();
            if (!k.isEmpty() && !v.isEmpty()) out.put(k, v);
        }
        return Collections.unmodifiableMap(out);
    }

    public static Pattern glob(String g) {
        StringBuilder sb = new StringBuilder();
        for (char c : g.toCharArray()) {
            if (c == '*') sb.append(".*");
            else if (c == '?') sb.append('.');
            else sb.append(Pattern.quote(String.valueOf(c)));
        }
        return Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE);
    }
}
