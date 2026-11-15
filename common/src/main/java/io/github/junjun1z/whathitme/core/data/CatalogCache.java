package io.github.junjun1z.whathitme.core.data;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.Safe;
import io.github.junjun1z.whathitme.core.config.WhmConfig;

/**
 * Smart catalog cache (config/whathitme/.cache/catalog_cache.json): damage types, entities and effects of the game,
 * scanned once and kept with a SHA-256 of the sorted "modid:version" list. Same mod list = loaded from the file, other
 * mod list = rescanned and rewritten. Search, dump and tab completion read this instead of the registries.
 */
public final class CatalogCache {

    private static final int FORMAT = 1;

    private static volatile List<Entry> damage;
    private static volatile List<Entry> mobs;
    private static volatile List<Entry> effects;
    private static volatile List<String> living;
    private static volatile List<String> vanilla;
    /** "created", "hit", "rescanned", "failed" or "none" */
    public static volatile String status = "none";
    public static volatile String hash = "";

    private CatalogCache() {}

    public static File file() {
        return new File(WhmConfig.cacheDir(), "catalog_cache.json");
    }

    /** Sorted "modid:version" list of the loaded mods */
    public static List<String> modList() {
        List<String> out = new ArrayList<>();
        try {
            for (String id : Platform.mods.modIds()) {
                String v = "";
                try {
                    v = Platform.mods.version(id);
                } catch (Throwable ignored) {
                    // no version
                }
                out.add(id + ":" + (v == null ? "" : v));
            }
        } catch (Throwable t) {
            Safe.warn("mod list", t);
        }
        Collections.sort(out);
        return out;
    }

    public static String hashOf(List<String> mods) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (String s : mods) md.update((s + "\n").getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest()) sb.append(String.format("%02x", b & 0xff));
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(mods.hashCode());
        }
    }

    public static boolean loaded() {
        return mobs != null;
    }

    /** Loads the cache or scans when missing / outdated; called after mod loading. Never throws. */
    public static void init(Registries reg) {
        init(reg, modList(), false);
    }

    /** /whm reload: always rescan and rewrite */
    public static void rescan(Registries reg) {
        init(reg, modList(), true);
    }

    /** Loads on first use (versions whose loader does not call {@link #init}) */
    public static void ensure(Registries reg) {
        if (mobs == null && reg != null) init(reg);
    }

    public static synchronized void init(Registries reg, List<String> mods, boolean force) {
        String h = hashOf(mods);
        File f = file();
        if (!force && f.isFile()) {
            try {
                Map<String, Object> root = Json.obj(Json.parse(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8)));
                if (h.equals(Json.str(root.get("hash"), "")) && Json.num(root.get("format"), 0) == FORMAT) {
                    damage = entries(root.get("damage"), Entry.DAMAGE);
                    mobs = entries(root.get("mobs"), Entry.MOB);
                    effects = entries(root.get("effects"), Entry.EFFECT);
                    living = strings(root.get("living"));
                    vanilla = strings(root.get("vanilla"));
                    hash = h;
                    status = "hit";
                    return;
                }
            } catch (Throwable t) {
                Safe.warn("catalog cache read", t);
            }
        }
        boolean existed = f.isFile();
        try {
            damage = Collections.unmodifiableList(new ArrayList<>(Safe.get("damage type scan", reg::damageTypes, new ArrayList<>())));
            mobs = Collections.unmodifiableList(new ArrayList<>(Safe.get("entity scan", reg::mobs, new ArrayList<>())));
            effects = Collections.unmodifiableList(new ArrayList<>(Safe.get("effect scan", reg::effects, new ArrayList<>())));
            living = Collections.unmodifiableList(new ArrayList<>(Safe.get("living entity scan", reg::livingEntityIds, new ArrayList<>())));
            vanilla = Collections.unmodifiableList(new ArrayList<>(Safe.get("vanilla damage ids", reg::vanillaDamageIds, new ArrayList<>())));
            hash = h;
            status = force || existed ? "rescanned" : "created";
            save(f, h, mods);
        } catch (Throwable t) {
            status = "failed";
            Safe.warn("catalog scan", t);
        }
    }

    private static void save(File f, String h, List<String> mods) {
        try {
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("format", FORMAT);
            root.put("hash", h);
            root.put("mods", mods);
            root.put("damage", rows(damage));
            root.put("mobs", rows(mobs));
            root.put("effects", rows(effects));
            root.put("living", living);
            root.put("vanilla", vanilla);
            Files.write(f.toPath(), Json.write(root).getBytes(StandardCharsets.UTF_8));
        } catch (Throwable t) {
            Safe.warn("catalog cache write", t);
        }
    }

    private static List<Object> rows(List<Entry> list) {
        List<Object> out = new ArrayList<>();
        for (Entry e : list) {
            List<Object> r = new ArrayList<>();
            r.add(e.id);
            r.add(e.name);
            r.add(e.nameKey);
            r.add(e.modId);
            r.add(e.flags);
            out.add(r);
        }
        return out;
    }

    private static List<Entry> entries(Object v, int type) {
        List<Entry> out = new ArrayList<>();
        for (Object o : Json.list(v)) {
            List<Object> r = Json.list(o);
            if (r.size() < 5) continue;
            String mod = Json.str(r.get(3), "");
            out.add(new Entry(type, Json.str(r.get(0), ""), Json.str(r.get(1), ""), Json.str(r.get(2), ""), mod, Platform.modName(mod),
                (int) Json.num(r.get(4), 0), 0, 0, 0, false, ""));
        }
        return Collections.unmodifiableList(out);
    }

    private static List<String> strings(Object v) {
        List<String> out = new ArrayList<>();
        for (Object o : Json.list(v)) out.add(Json.str(o, ""));
        return Collections.unmodifiableList(out);
    }

    public static List<Entry> damageTypes() {
        List<Entry> d = damage;
        return d == null ? Collections.<Entry>emptyList() : d;
    }

    /** The registries with mobs / effects / ids served from the cache (falls back to the live registries) */
    public static Registries view(final Registries reg) {
        ensure(reg);
        return new Registries() {
            @Override
            public List<Entry> mobs() {
                List<Entry> m = mobs;
                return m != null ? m : reg.mobs();
            }

            @Override
            public List<Entry> effects() {
                List<Entry> e = effects;
                return e != null ? e : reg.effects();
            }

            @Override
            public List<String> livingEntityIds() {
                List<String> l = living;
                return l != null ? l : reg.livingEntityIds();
            }

            @Override
            public List<String> vanillaDamageIds() {
                List<String> v = vanilla;
                return v != null ? v : reg.vanillaDamageIds();
            }

            @Override
            public List<Entry> damageTypes() {
                List<Entry> d = damage;
                return d != null ? d : reg.damageTypes();
            }

            @Override
            public String translate(String key) {
                return reg.translate(key);
            }
        };
    }

    /** Self-test: forget the loaded data */
    public static synchronized void forget() {
        damage = null;
        mobs = null;
        effects = null;
        living = null;
        vanilla = null;
        status = "none";
    }
}
