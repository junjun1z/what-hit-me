package io.github.junjun1z.whathitme.core.data;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import io.github.junjun1z.whathitme.core.Safe;
import io.github.junjun1z.whathitme.core.config.WhmConfig;

/**
 * Death statistics of one player (/whm stats): deaths per cause, total deaths, total damage taken, and which
 * oncePerPlayer death triggers already ran. At most 100 causes in memory. Saved to config/whathitme/.cache/stats_&lt;uuid&gt;.json
 * only on death, logout and world save.
 */
public final class Stats {

    public static final int MAX_CAUSES = 100;

    /** One cause of death */
    public static final class Cause {
        public final String id;
        public String name;
        public String nameKey;
        public int deaths;

        Cause(String id, String name, String nameKey) {
            this.id = id;
            this.name = name;
            this.nameKey = nameKey;
        }
    }

    private static final Map<UUID, Stats> LOADED = new HashMap<>();

    public final UUID id;
    public final Map<String, Cause> causes = new LinkedHashMap<>();
    public int deaths;
    public double damageTaken;
    public final Set<String> triggersRun = new LinkedHashSet<>();
    private boolean dirty;

    private Stats(UUID id) {
        this.id = id;
    }

    public static File file(UUID id) {
        return new File(WhmConfig.cacheDir(), "stats_" + id + ".json");
    }

    /** The stats of a player, loaded from its file on first use */
    public static synchronized Stats of(UUID id) {
        Stats s = LOADED.get(id);
        if (s == null) {
            s = load(id);
            LOADED.put(id, s);
        }
        return s;
    }

    public static synchronized Stats peek(UUID id) {
        return LOADED.get(id);
    }

    /** Saves and forgets (logout) */
    public static synchronized void unload(UUID id) {
        Stats s = LOADED.remove(id);
        if (s != null) s.save();
    }

    /** World save: writes every changed player */
    public static synchronized void saveAll() {
        for (Stats s : LOADED.values()) s.save();
    }

    /** Server stopped */
    public static synchronized void unloadAll() {
        saveAll();
        LOADED.clear();
    }

    static Stats load(UUID id) {
        Stats s = new Stats(id);
        File f = file(id);
        if (!f.isFile()) return s;
        try {
            Map<String, Object> root = Json.obj(Json.parse(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8)));
            s.deaths = (int) Json.num(root.get("deaths"), 0);
            s.damageTaken = Json.num(root.get("damageTaken"), 0);
            for (Object o : Json.list(root.get("causes"))) {
                Map<String, Object> c = Json.obj(o);
                String cid = Json.str(c.get("id"), "");
                if (cid.isEmpty() || s.causes.size() >= MAX_CAUSES) continue;
                Cause cause = new Cause(cid, Json.str(c.get("name"), ""), Json.str(c.get("nameKey"), ""));
                cause.deaths = (int) Json.num(c.get("deaths"), 0);
                s.causes.put(cid, cause);
            }
            for (Object o : Json.list(root.get("triggersRun"))) s.triggersRun.add(Json.str(o, ""));
        } catch (Throwable t) {
            Safe.warn("stats read", t);
        }
        return s;
    }

    public synchronized void save() {
        if (!dirty) return;
        try {
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("deaths", deaths);
            root.put("damageTaken", Math.round(damageTaken * 10) / 10.0);
            List<Object> list = new ArrayList<>();
            for (Cause c : causes.values()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", c.id);
                m.put("name", c.name);
                m.put("nameKey", c.nameKey);
                m.put("deaths", c.deaths);
                list.add(m);
            }
            root.put("causes", list);
            root.put("triggersRun", new ArrayList<Object>(triggersRun));
            Files.write(file(id).toPath(), Json.write(root).getBytes(StandardCharsets.UTF_8));
            dirty = false;
        } catch (Throwable t) {
            Safe.warn("stats write", t);
        }
    }

    public synchronized void addDamage(float hp) {
        if (hp > 0f && !Float.isInfinite(hp) && !Float.isNaN(hp)) {
            damageTaken += hp;
            dirty = true;
        }
    }

    /** Counts a death by the killing record (name = attacker or damage type) */
    public synchronized void addDeath(DamageRecord k) {
        deaths++;
        dirty = true;
        if (k == null) return;
        Cause c = causes.get(k.damageId);
        boolean mob = k.damageId.startsWith("mob:") && !k.attackerName.isEmpty();
        if (c == null) {
            if (causes.size() >= MAX_CAUSES) {
                String least = null;
                for (Cause x : causes.values()) if (least == null || x.deaths < causes.get(least).deaths) least = x.id;
                causes.remove(least);
            }
            c = new Cause(k.damageId, "", "");
            causes.put(k.damageId, c);
        }
        c.name = mob ? k.attackerName : k.damageType;
        c.nameKey = mob ? k.attackerKey : "";
        c.deaths++;
    }

    public synchronized void markDirty() {
        dirty = true;
    }

    /** Causes with the most deaths first */
    public synchronized List<Cause> top(int n) {
        List<Cause> l = new ArrayList<>(causes.values());
        l.sort((a, b) -> b.deaths - a.deaths);
        return l.size() > n ? new ArrayList<>(l.subList(0, n)) : l;
    }

    /** Self-test: drop the loaded stats without saving */
    public static synchronized void forget(UUID id) {
        LOADED.remove(id);
    }
}
