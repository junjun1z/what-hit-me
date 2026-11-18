package io.github.junjun1z.whathitme.core.rules.mods;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;

import io.github.junjun1z.whathitme.core.rules.ConditionProvider;
import io.github.junjun1z.whathitme.core.server.PlayerHandle;
import io.github.junjun1z.whathitme.core.server.ServerPort;

/**
 * bq:&lt;quest id&gt; - Better Questing: QuestingAPI.getAPI(ApiReference.QUEST_DB).getValue(id).isComplete(questing uuid).
 * Better Questing 3.x for 1.12.2 uses numeric quest ids; the GTNH fork (1.7.10) uses UUIDs (written with dashes or as its
 * Base64 form). Older and newer generations without QuestingAPI: 1.x (1.8.9 / 1.9.4, static QuestDatabase) and 4.x
 * (1.20.1, funwayguy.betterquesting, ProgressManager). Reflection only.
 */
public final class BetterQuestingProvider implements ConditionProvider {

    private final Class<?> api = Reflect.first("betterquesting.api.api.QuestingAPI");
    private final Class<?> ref = Reflect.first("betterquesting.api.api.ApiReference");
    /** Better Questing 1.x (1.8.9 - 1.9.4): static quest map, QuestInstance.isComplete(UUID) */
    private final Class<?> v1 = api != null ? null : Reflect.first("betterquesting.quests.QuestDatabase");
    /** Better Questing 4.x (1.20.1): QuestDatabase.INSTANCE, progress in ProgressManager */
    private final Class<?> v4 = api != null ? null : Reflect.first("funwayguy.betterquesting.questing.quests.QuestDatabase");

    @Override
    public String unavailable(ServerPort srv) {
        return (api == null || ref == null) && v1 == null && v4 == null ? "Better Questing API (betterquesting.api.api.QuestingAPI) not found" : null;
    }

    @Override
    public String checkArg(String arg) {
        return parse(arg) == null ? "expected a quest id (a number, or a UUID for the GTNH version)" : null;
    }

    /** Integer or UUID, null = neither */
    static Object parse(String arg) {
        String a = arg == null ? "" : arg.trim();
        if (a.isEmpty()) return null;
        try {
            return Integer.valueOf(a);
        } catch (NumberFormatException ignored) {
            // not a number
        }
        try {
            if (a.indexOf('-') > 0 && a.length() >= 32) return UUID.fromString(a);
        } catch (IllegalArgumentException ignored) {
            // not a UUID
        }
        try {
            byte[] b = Base64.getUrlDecoder().decode(a.replace('+', '-').replace('/', '_').replace("=", ""));
            if (b.length == 16) {
                ByteBuffer bb = ByteBuffer.wrap(b);
                return new UUID(bb.getLong(), bb.getLong());
            }
        } catch (IllegalArgumentException ignored) {
            // not Base64
        }
        return null;
    }

    /** Quest ids with their names: numeric ids (getEntries) or the GTNH fork's UUIDs (a BiMap) */
    @Override
    public java.util.List<String> suggestions() throws Exception {
        java.util.List<String> out = new java.util.ArrayList<>();
        if (v1 != null) {
            Object m = Reflect.staticField(v1, "questDB");
            if (m instanceof java.util.Map) {
                for (java.util.Map.Entry<?, ?> e : ((java.util.Map<?, ?>) m).entrySet()) out.add(Reflect.entry(String.valueOf(e.getKey()), Reflect.read(e.getValue(), null, "name")));
            }
        } else if (v4 != null) {
            Object db = Reflect.staticField(v4, "INSTANCE");
            Class<?> props = Reflect.first("funwayguy.betterquesting.properties.CommonProps");
            Object name = props == null ? null : Reflect.staticField(props, "NAME");
            for (Object e : Reflect.list(db, "getEntries", null)) {
                Object q = Reflect.read(e, "getValue", null);
                Object info = q == null ? null : Reflect.read(q, "getInfo", null);
                out.add(Reflect.entry(String.valueOf(Reflect.read(e, "getID", null)), info == null || name == null ? null : call(info, "getProp", name)));
            }
        }
        if (api == null || ref == null) {
            sort(out);
            return out;
        }
        Object key = Reflect.staticField(ref, "QUEST_DB");
        Method getApi = Reflect.staticMethod(api, "getAPI", key.getClass());
        Object db = getApi == null ? null : getApi.invoke(null, key);
        if (db == null) return out;
        Class<?> props = Reflect.first("betterquesting.api.properties.NativeProps");
        Object name = props == null ? null : Reflect.staticField(props, "NAME");
        if (db instanceof java.util.Map) {
            for (Object o : ((java.util.Map<?, ?>) db).entrySet()) {
                java.util.Map.Entry<?, ?> e = (java.util.Map.Entry<?, ?>) o;
                out.add(Reflect.entry(String.valueOf(e.getKey()), name(e.getValue(), name)));
            }
        } else if (Reflect.method(db.getClass(), "getEntries") != null) {
            // 1.10.2 (3.3.x): an array
            Object entries = Reflect.read(db, "getEntries", null);
            Iterable<?> it = entries instanceof Object[] ? java.util.Arrays.asList((Object[]) entries) : Reflect.list(db, "getEntries", null);
            for (Object e : it) {
                out.add(Reflect.entry(String.valueOf(Reflect.read(e, "getID", null)), name(Reflect.read(e, "getValue", null), name)));
            }
        } else {
            // 2.x (1.11.2 / 1.12.1): keys and values
            Method get = Reflect.method(db.getClass(), "getValue", Integer.class);
            for (Object k : Reflect.list(db, "getAllKeys", null)) {
                out.add(Reflect.entry(String.valueOf(k), get == null ? null : name(get.invoke(db, k), name)));
            }
        }
        sort(out);
        return out;
    }

    private static void sort(java.util.List<String> out) {
        java.util.Collections.sort(out, (a, b) -> {
            String x = a.split("	")[0], y = b.split("	")[0];
            try {
                return Integer.compare(Integer.parseInt(x), Integer.parseInt(y));
            } catch (NumberFormatException ex) {
                return a.compareTo(b);
            }
        });
    }

    /** A public one-argument method by name (generic parameter types), null on any problem */
    private static Object call(Object o, String method, Object arg) {
        for (Method m : o.getClass().getMethods()) {
            if (m.getName().equals(method) && m.getParameterCount() == 1) {
                try {
                    Reflect.open(m);
                    return m.invoke(o, arg);
                } catch (Throwable ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private static Object name(Object quest, Object prop) {
        if (quest == null || prop == null) return null;
        Object n = call(quest, "getProperty", prop);
        if (n != null) return n;
        // 2.x: getProperties().getProperty(NAME)
        Object c = Reflect.read(quest, "getProperties", null);
        return c == null ? null : call(c, "getProperty", prop);
    }

    @Override
    public boolean test(PlayerHandle p, String arg) throws Exception {
        Object id = parse(arg);
        if (id == null) return false;
        if (v1 != null || v4 != null) {
            if (!(id instanceof Integer)) return false;
            return v1 != null ? testV1((Integer) id, p.uuid()) : testV4((Integer) id, p.uuid());
        }
        if (api == null || ref == null) return false;
        Object key = Reflect.staticField(ref, "QUEST_DB");
        Method getApi = Reflect.staticMethod(api, "getAPI", key.getClass());
        if (getApi == null) throw new NoSuchMethodException("QuestingAPI.getAPI(ApiKey)");
        Object db = getApi.invoke(null, key);
        if (db == null) return false;
        Method get = Reflect.method(db.getClass(), "getValue", id instanceof Integer ? int.class : UUID.class);
        // GTNH fork: the quest database is a BiMap<UUID, IQuest>
        if (get == null && id instanceof UUID) get = Reflect.method(db.getClass(), "get", UUID.class);
        if (get == null) throw new NoSuchMethodException("quest database getValue(" + (id instanceof Integer ? "int" : "UUID") + ")");
        Object quest = get.invoke(db, id);
        if (quest == null) return false;
        UUID who = p.uuid();
        Object player = p.nativePlayer();
        Method qid = player == null ? null : Reflect.staticMethod(api, "getQuestingUUID", player.getClass());
        if (qid != null) {
            Object u = qid.invoke(null, player);
            if (u instanceof UUID) who = (UUID) u;
        }
        Method done = Reflect.method(quest.getClass(), "isComplete", UUID.class);
        if (done == null) throw new NoSuchMethodException("IQuest.isComplete(UUID)");
        return Reflect.truth(done.invoke(quest, who));
    }

    private boolean testV1(int id, UUID who) throws Exception {
        Method get = Reflect.staticMethod(v1, "getQuestByID", int.class);
        if (get == null) throw new NoSuchMethodException("QuestDatabase.getQuestByID(int)");
        Object quest = get.invoke(null, id);
        if (quest == null) return false;
        Method done = Reflect.method(quest.getClass(), "isComplete", UUID.class);
        if (done == null) throw new NoSuchMethodException("QuestInstance.isComplete(UUID)");
        return Reflect.truth(done.invoke(quest, who));
    }

    private boolean testV4(int id, UUID who) throws Exception {
        Object db = Reflect.staticField(v4, "INSTANCE");
        Method entry = Reflect.method(db.getClass(), "getEntry", int.class);
        if (entry == null) throw new NoSuchMethodException("QuestDatabase.getEntry(int)");
        Object e = entry.invoke(db, id);
        if (e instanceof java.util.Optional) e = ((java.util.Optional<?>) e).orElse(null);
        if (e == null) return false;
        Class<?> pm = Reflect.first("funwayguy.betterquesting.questing.player.progress.ProgressManager");
        Method get = pm == null ? null : Reflect.staticMethod(pm, "get");
        Object mgr = get == null ? null : get.invoke(null);
        if (mgr == null) throw new NoSuchMethodException("ProgressManager.get()");
        Method prog = Reflect.method(mgr.getClass(), "getProgress", UUID.class);
        Object progress = prog == null ? null : prog.invoke(mgr, who);
        if (progress == null) return false;
        Method done = Reflect.method(progress.getClass(), "isQuestDone", int.class);
        if (done == null) throw new NoSuchMethodException("PlayerProgress.isQuestDone(int)");
        Object t = done.invoke(progress, id);
        // a Tuple<Boolean, Long> record
        return Reflect.truth(t instanceof Boolean ? t : t == null ? null : Reflect.read(t, "first", null));
    }
}
