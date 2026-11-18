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
 * Base64 form). Reflection only.
 */
public final class BetterQuestingProvider implements ConditionProvider {

    private final Class<?> api = Reflect.first("betterquesting.api.api.QuestingAPI");
    private final Class<?> ref = Reflect.first("betterquesting.api.api.ApiReference");

    @Override
    public String unavailable(ServerPort srv) {
        return api == null || ref == null ? "Better Questing API (betterquesting.api.api.QuestingAPI) not found" : null;
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
        if (api == null || ref == null) return out;
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
        } else {
            for (Object e : Reflect.list(db, "getEntries", null)) {
                out.add(Reflect.entry(String.valueOf(Reflect.read(e, "getID", null)), name(Reflect.read(e, "getValue", null), name)));
            }
        }
        java.util.Collections.sort(out, (a, b) -> {
            String x = a.split("	")[0], y = b.split("	")[0];
            try {
                return Integer.compare(Integer.parseInt(x), Integer.parseInt(y));
            } catch (NumberFormatException ex) {
                return a.compareTo(b);
            }
        });
        return out;
    }

    private static Object name(Object quest, Object prop) {
        if (quest == null || prop == null) return null;
        for (Method m : quest.getClass().getMethods()) {
            if (m.getName().equals("getProperty") && m.getParameterCount() == 1) {
                try {
                    Reflect.open(m);
                    return m.invoke(quest, prop);
                } catch (Throwable ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    @Override
    public boolean test(PlayerHandle p, String arg) throws Exception {
        Object id = parse(arg);
        if (api == null || ref == null || id == null) return false;
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
}
