package io.github.junjun1z.whathitme.core.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every damage id seen so far with its properties. Filled as damage happens (no scanning). Bounded so that a mod that
 * creates endless unique damage types cannot grow it forever.
 */
public final class DamageCatalog {

    private static final int LIMIT = 4096;

    private static final class Info {
        String name;
        String nameKey;
        String modId;
        String modName;
        int flags;
        int count;
    }

    private final Map<String, Info> map = new LinkedHashMap<>();

    public synchronized void register(DamageRecord r) {
        Info i = map.get(r.damageId);
        if (i == null) {
            if (map.size() >= LIMIT) return;
            i = new Info();
            map.put(r.damageId, i);
        }
        boolean mob = r.damageId.startsWith("mob:");
        i.name = mob ? r.attackerName : r.damageType;
        i.nameKey = mob ? r.attackerKey : "";
        i.modId = r.modId;
        i.modName = r.modName;
        i.flags |= r.flags & DamageRecord.PROPERTY_MASK;
        i.count += Math.max(1, r.hits);
    }

    public synchronized boolean contains(String id) {
        return map.containsKey(id);
    }

    public synchronized List<String> ids() {
        return new ArrayList<>(map.keySet());
    }

    public synchronized List<Entry> entries() {
        List<Entry> out = new ArrayList<>(map.size());
        for (Map.Entry<String, Info> e : map.entrySet()) {
            Info i = e.getValue();
            out.add(new Entry(Entry.DAMAGE, e.getKey(), i.name, i.nameKey, i.modId, i.modName, i.flags, i.count, 0, 0, false, ""));
        }
        return out;
    }

    public synchronized void clear() {
        map.clear();
    }
}
