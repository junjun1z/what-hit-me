package io.github.junjun1z.whathitme.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

/**
 * Two configuration files in config/whathitme/: whathitme-common.cfg (modpack / server limits) and whathitme-client.cfg
 * (personal). A display element is shown only when it is enabled in both.
 */
public final class WhmConfig {

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
        HINTS_AND_TIPS("showHintsAndTips", "Automatic counter-measure tips and custom tips (hintMap)."),
        ACTION_BUTTONS("showActionButtons", "Clickable [Copy ID], [Mute] and [Copy Recap] buttons."),
        TOGGLE_CHAT_NOTICE("showToggleChatNotice", "The '/whm togglechat' notice at the end of the summary.");

        public final String key;
        public final String comment;

        Flag(String key, String comment) {
            this.key = key;
            this.comment = comment;
        }

        public int bit() {
            return 1 << ordinal();
        }

        public static Flag byKey(String key) {
            for (Flag f : values()) if (f.key.equalsIgnoreCase(key)) return f;
            return null;
        }
    }

    public static final int ALL_FLAGS = (1 << Flag.values().length) - 1;

    public static final String CAT_DISPLAY = "display";
    public static final String CAT_FILTERS = "filters";
    public static final String CAT_TRACKING = "tracking";
    public static final String CAT_LAYOUT = "layout";

    public static final String[] ANCHORS = {"TOP_LEFT", "TOP_RIGHT", "BOTTOM_LEFT", "BOTTOM_RIGHT", "CENTER_LEFT", "CENTER_RIGHT"};

    /** Settings that exist in both files */
    public abstract static class Base {
        public final Configuration cfg;
        public int mask = ALL_FLAGS;
        public float minDamageThreshold;
        public float heavyHitThreshold;
        public List<String> ignoredDamageIds = Collections.emptyList();
        private List<Pattern> ignorePatterns = Collections.emptyList();
        public Map<String, String> renameMap = Collections.emptyMap();
        public Map<String, String> hintMap = Collections.emptyMap();

        Base(File file) {
            cfg = new Configuration(file);
        }

        public final synchronized void load() {
            cfg.load();
            read();
            if (cfg.hasChanged()) cfg.save();
        }

        void read() {
            int m = 0;
            for (Flag f : Flag.values()) {
                if (cfg.getBoolean(f.key, CAT_DISPLAY, true, f.comment)) m |= f.bit();
            }
            mask = m;
            cfg.setCategoryComment(CAT_DISPLAY, "Display switches. An element is shown only when it is true in BOTH whathitme-common.cfg and whathitme-client.cfg.");
            minDamageThreshold = cfg.getFloat("minDamageThreshold", CAT_FILTERS, 0.05f, 0f, 1000f, "Damage below this (health + absorption) is not recorded.");
            heavyHitThreshold = cfg.getFloat("heavyHitThreshold", CAT_FILTERS, 0.8f, 0.05f, 100f, "A single hit taking at least this part of your max health is a heavy hit (0.8 = 80%).");
            String[] ign = cfg.getStringList("ignoredDamageIds", CAT_FILTERS, new String[0], "Damage ids to hide (wildcard * allowed), e.g. minecraft:cactus or mob:minecraft:*");
            ignoredDamageIds = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(ign)));
            List<Pattern> pats = new ArrayList<>();
            for (String s : ign) {
                String t = s.trim();
                if (!t.isEmpty()) pats.add(glob(t));
            }
            ignorePatterns = pats;
            renameMap = parseMap(cfg.getStringList("renameMap", CAT_FILTERS, new String[0], "Custom display names, one per line: \"modid:damage_id = Custom Name\". & or section sign color codes work. Use \"modid:damage_id = lang:your.translation.key\" to translate it with a resource pack. Wildcards (*) are allowed in the id. /whm dump writes config/whathitme/hint_template.cfg with every known id."));
            cfg.setCategoryComment(CAT_FILTERS, "What is recorded and how it is named. In whathitme-common.cfg these apply to everyone (a server with the mod sends them to clients); in whathitme-client.cfg they add personal entries.");
            hintMap = parseMap(cfg.getStringList("hintMap", CAT_FILTERS, new String[0], "Custom tips, one per line: \"modid:damage_id = Tip text\" or \"modid:damage_id = lang:your.translation.key\". Ids without an entry get an automatic tip from the damage properties. Example: \"mob:minecraft:creeper = Creepers hiss before exploding - sprint away!\""));
        }

        public boolean isIgnored(String id) {
            List<Pattern> pats = ignorePatterns;
            for (Pattern p : pats) if (p.matcher(id).matches()) return true;
            return false;
        }

        public synchronized void save() {
            cfg.save();
        }

        /** Adds or removes an id in ignoredDamageIds. @return true when it is ignored now */
        public synchronized boolean toggleIgnore(String id) {
            Property p = cfg.get(CAT_FILTERS, "ignoredDamageIds", new String[0]);
            List<String> list = new ArrayList<>(Arrays.asList(p.getStringList()));
            boolean now;
            if (list.remove(id)) {
                now = false;
            } else {
                list.add(id);
                now = true;
            }
            p.set(list.toArray(new String[0]));
            cfg.save();
            read();
            return now;
        }

        /** Sets (or with an empty value removes) an entry of renameMap / hintMap */
        public synchronized void putMapEntry(String listKey, String id, String value) {
            Property p = cfg.get(CAT_FILTERS, listKey, new String[0]);
            List<String> list = new ArrayList<>();
            for (String s : p.getStringList()) {
                int eq = s.indexOf('=');
                if (eq >= 0 && s.substring(0, eq).trim().equals(id)) continue;
                list.add(s);
            }
            if (value != null && !value.trim().isEmpty()) list.add(id + " = " + value.trim());
            p.set(list.toArray(new String[0]));
            cfg.save();
            read();
        }

        /** Every setting by name (for /whm config) */
        public synchronized Map<String, Property> properties() {
            Map<String, Property> out = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            for (String cat : cfg.getCategoryNames()) {
                ConfigCategory c = cfg.getCategory(cat);
                for (Map.Entry<String, Property> e : c.getValues().entrySet()) out.put(e.getKey(), e.getValue());
            }
            return out;
        }

        /**
         * Changes one setting.
         *
         * @return null when done, otherwise the reason it failed
         */
        public synchronized String set(String key, String value) {
            Property p = properties().get(key);
            if (p == null) return "unknown";
            if (p.isList()) {
                p.set(value.isEmpty() ? new String[0] : value.split("\\s*,\\s*"));
            } else {
                switch (p.getType()) {
                    case BOOLEAN:
                        String v = value.toLowerCase(Locale.ROOT);
                        if (v.equals("on") || v.equals("yes")) v = "true";
                        if (v.equals("off") || v.equals("no")) v = "false";
                        if (v.equals("toggle")) v = String.valueOf(!p.getBoolean());
                        if (!v.equals("true") && !v.equals("false")) return "invalid";
                        p.set(Boolean.parseBoolean(v));
                        break;
                    case INTEGER:
                        try {
                            int i = Integer.parseInt(value);
                            if (p.getMinValue() != null && i < Integer.parseInt(p.getMinValue())) return "invalid";
                            if (p.getMaxValue() != null && i > Integer.parseInt(p.getMaxValue())) return "invalid";
                            p.set(i);
                        } catch (NumberFormatException e) {
                            return "invalid";
                        }
                        break;
                    case DOUBLE:
                        try {
                            double d = Double.parseDouble(value);
                            if (p.getMinValue() != null && d < Double.parseDouble(p.getMinValue())) return "invalid";
                            if (p.getMaxValue() != null && d > Double.parseDouble(p.getMaxValue())) return "invalid";
                            p.set(d);
                        } catch (NumberFormatException e) {
                            return "invalid";
                        }
                        break;
                    default:
                        if (p.getMinValue() != null && p.getMaxValue() != null && !String.valueOf(Integer.MIN_VALUE).equals(p.getMinValue())) {
                            // getFloat() stores numbers as strings with a range
                            try {
                                double d = Double.parseDouble(value);
                                if (d < Double.parseDouble(p.getMinValue()) || d > Double.parseDouble(p.getMaxValue())) return "invalid";
                            } catch (NumberFormatException e) {
                                return "invalid";
                            }
                        }
                        String[] valid = p.getValidValues();
                        if (valid != null && valid.length > 0) {
                            String match = null;
                            for (String s : valid) if (s.equalsIgnoreCase(value)) match = s;
                            if (match == null) return "invalid";
                            value = match;
                        }
                        p.set(value);
                }
            }
            cfg.save();
            read();
            return null;
        }

        public synchronized String get(String key) {
            Property p = properties().get(key);
            if (p == null) return null;
            return p.isList() ? String.join(", ", p.getStringList()) : p.getString();
        }
    }

    public static final class Common extends Base {
        public int historySize;
        public float mergeWindowSeconds;
        public int summarySeconds;
        public int recapSeconds;
        public int maxSnapshots;

        Common(File f) {
            super(f);
        }

        @Override
        void read() {
            super.read();
            historySize = cfg.getInt("historySize", CAT_TRACKING, 50, 5, 500, "Damage records kept per player (fixed-size ring buffer).");
            mergeWindowSeconds = cfg.getFloat("mergeWindowSeconds", CAT_TRACKING, 3.0f, 0f, 60f, "Repeated damage with the same id and source within this many seconds is merged into one line (0 = off).");
            summarySeconds = cfg.getInt("summarySeconds", CAT_TRACKING, 10, 1, 300, "Time window of the 'Last Xs Total' line.");
            recapSeconds = cfg.getInt("recapSeconds", CAT_TRACKING, 30, 1, 600, "Damage from this many seconds before death is kept in the death recap.");
            maxSnapshots = cfg.getInt("maxSnapshots", CAT_TRACKING, 5, 1, 50, "Totem / heavy-hit snapshots kept per player.");
            cfg.setCategoryComment(CAT_TRACKING, "Recording (server side, or the client when the server does not have the mod).");
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

        Client(File f) {
            super(f);
        }

        @Override
        void read() {
            super.read();
            anchorPoint = cfg.getString("anchorPoint", CAT_LAYOUT, "CENTER_LEFT", "Death screen panel anchor.", ANCHORS);
            offsetX = cfg.getInt("offsetX", CAT_LAYOUT, 8, -4000, 4000, "Horizontal offset from the anchor (GUI pixels). Saved when you drag the panel.");
            offsetY = cfg.getInt("offsetY", CAT_LAYOUT, 0, -4000, 4000, "Vertical offset from the anchor (GUI pixels). Saved when you drag the panel.");
            scale = cfg.getFloat("scale", CAT_LAYOUT, 0.85f, 0.25f, 3f, "Panel scale.");
            backgroundOpacity = cfg.getFloat("backgroundOpacity", CAT_LAYOUT, 0.75f, 0f, 1f, "Panel background opacity (0.0 - 1.0).");
            maxVisibleLines = cfg.getInt("maxVisibleLines", CAT_LAYOUT, 8, 1, 40, "Damage lines visible at once (scroll with the mouse wheel for more).");
            minimized = cfg.getBoolean("minimized", CAT_LAYOUT, false, "Panel minimized with the [-] button.");
            chatHistoryLines = cfg.getInt("chatHistoryLines", CAT_LAYOUT, 15, 1, 100, "Lines shown by /whm history.");
            cfg.setCategoryComment(CAT_LAYOUT, "Death screen panel layout. Drag the panel header on the death screen to move it.");
        }

        public synchronized void saveLayout(int x, int y, boolean min) {
            cfg.get(CAT_LAYOUT, "offsetX", 8).set(x);
            cfg.get(CAT_LAYOUT, "offsetY", 0).set(y);
            cfg.get(CAT_LAYOUT, "minimized", false).set(min);
            cfg.save();
            offsetX = x;
            offsetY = y;
            minimized = min;
        }
    }

    public static Common common;
    /** null on a dedicated server */
    public static Client client;

    private WhmConfig() {}

    public static void init(File configDir, boolean clientSide) {
        File dir = new File(configDir, "whathitme");
        if (!dir.isDirectory()) dir.mkdirs();
        common = new Common(new File(dir, "whathitme-common.cfg"));
        common.load();
        if (clientSide) {
            client = new Client(new File(dir, "whathitme-client.cfg"));
            client.load();
        }
    }

    public static void reloadCommon() {
        common.load();
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
