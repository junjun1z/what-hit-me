package io.github.junjun1z.whathitme.core.rules.mods;

import java.lang.reflect.Method;
import java.util.UUID;

import io.github.junjun1z.whathitme.core.rules.ConditionProvider;
import io.github.junjun1z.whathitme.core.server.PlayerHandle;
import io.github.junjun1z.whathitme.core.server.ServerPort;

/**
 * ftbquests:&lt;quest or chapter id&gt; (the hex id FTB Quests shows, "#" optional) - completed by the player's team.
 * FTB Quests 1.16+ (dev.ftb.mods): ServerQuestFile.INSTANCE.getOrCreateTeamData(player).isCompleted(file.get(id)) (1.16.5: getData(player)).
 * FTB Quests 1.12.2 (com.feed_the_beast): ServerQuestFile.INSTANCE.get(file.getID(text)).isComplete(file.getData(uuid))
 * (1.15.2: file.getData(uuid).isComplete(quest)).
 */
public final class FtbQuestsProvider implements ConditionProvider {

    private final Class<?> modern = Reflect.first("dev.ftb.mods.ftbquests.quest.ServerQuestFile");
    private final Class<?> legacy = modern != null ? null : Reflect.first("com.feed_the_beast.ftbquests.quest.ServerQuestFile");

    @Override
    public String unavailable(ServerPort srv) {
        return modern == null && legacy == null ? "FTB Quests API (ServerQuestFile) not found" : null;
    }

    @Override
    public String checkArg(String arg) {
        String a = strip(arg);
        if (a.isEmpty()) return "empty quest id";
        try {
            Long.parseUnsignedLong(a, 16);
            return null;
        } catch (NumberFormatException e) {
            return "expected the hex quest / chapter id shown by FTB Quests (e.g. ftbquests:1A2B3C4D5E6F7081)";
        }
    }

    /** ServerQuestFile.INSTANCE, or getInstance() since 2111.x (2101.x: Optional) */
    private Object modernFile() throws Exception {
        try {
            return Reflect.staticField(modern, "INSTANCE");
        } catch (NoSuchFieldException e) {
            Method m = Reflect.staticMethod(modern, "getInstance");
            if (m == null) throw e;
            Object f = m.invoke(null);
            return f instanceof java.util.Optional ? ((java.util.Optional<?>) f).orElse(null) : f;
        }
    }

    private static String strip(String arg) {
        String a = arg == null ? "" : arg.trim();
        return a.startsWith("#") ? a.substring(1) : a;
    }

    /** Chapters and their quests: "hex id	chapter" / "hex id	chapter / quest" */
    @Override
    public java.util.List<String> suggestions() throws Exception {
        java.util.List<String> out = new java.util.ArrayList<>();
        Object file = modern != null ? modernFile() : legacy != null ? Reflect.staticField(legacy, "INSTANCE") : null;
        if (file == null) return out;
        java.util.Collection<?> chapters = Reflect.list(file, "getAllChapters", "chapters");
        for (Object ch : chapters) {
            String ct = title(ch);
            out.add(Reflect.entry(code(ch), ct));
            for (Object q : Reflect.list(ch, "getQuests", "quests")) {
                String qt = title(q);
                out.add(Reflect.entry(code(q), ct.isEmpty() ? qt : qt.isEmpty() ? ct : ct + " / " + qt));
            }
        }
        return out;
    }

    private static String code(Object o) {
        Object c = Reflect.read(o, "getCodeString", null);
        if (c != null) return String.valueOf(c);
        Object id = Reflect.read(o, "getId", "id");
        return id instanceof Long ? String.format("%016X", id) : String.format("%08X", id);
    }

    /** Raw title (1.12.2 / 1.15.2 getUnformattedTitle, 1.16.5 title field, 2101+ getRawTitle) */
    private static String title(Object o) {
        Object t = Reflect.read(o, "getUnformattedTitle", null);
        if (t == null) t = Reflect.read(o, "getRawTitle", "title");
        return t == null ? "" : String.valueOf(t).replaceAll("§.", "").trim();
    }

    @Override
    public boolean test(PlayerHandle p, String arg) throws Exception {
        String id = strip(arg);
        if (modern != null) {
            Object file = modernFile();
            Object player = p.nativePlayer();
            if (file == null || player == null) return false;
            Method data = Reflect.method(file.getClass(), "getOrCreateTeamData", player.getClass());
            // 1.16.5 (1605.x): getData(Entity)
            if (data == null) data = Reflect.method(file.getClass(), "getData", player.getClass());
            // 2111.x / 26.x: getTeamData(Player) -> Optional
            if (data == null) data = Reflect.method(file.getClass(), "getTeamData", player.getClass());
            Method get = Reflect.method(file.getClass(), "get", long.class);
            if (data == null || get == null) throw new NoSuchMethodException("ServerQuestFile.getOrCreateTeamData(player) / get(long)");
            Object obj = get.invoke(file, Long.parseUnsignedLong(id, 16));
            Object team = data.invoke(file, player);
            if (team instanceof java.util.Optional) team = ((java.util.Optional<?>) team).orElse(null);
            if (obj == null || team == null) return false;
            Method done = Reflect.method(team.getClass(), "isCompleted", obj.getClass());
            if (done == null) throw new NoSuchMethodException("TeamData.isCompleted(QuestObject)");
            return Reflect.truth(done.invoke(team, obj));
        }
        if (legacy != null) {
            Object file = Reflect.staticField(legacy, "INSTANCE");
            if (file == null) return false;
            Method getId = Reflect.method(file.getClass(), "getID", Object.class);
            Method get = Reflect.method(file.getClass(), "get", int.class);
            Method data = Reflect.method(file.getClass(), "getData", UUID.class);
            if (getId == null || get == null || data == null) throw new NoSuchMethodException("ServerQuestFile.getID / get(int) / getData(UUID)");
            Object obj = get.invoke(file, getId.invoke(file, "#" + id));
            Object team = data.invoke(file, p.uuid());
            if (obj == null || team == null) return false;
            Method done = Reflect.method(obj.getClass(), "isComplete", team.getClass());
            if (done != null) return Reflect.truth(done.invoke(obj, team));
            // 1.15.2 (1502.x): PlayerData.isComplete(QuestObject)
            done = Reflect.method(team.getClass(), "isComplete", obj.getClass());
            if (done == null) throw new NoSuchMethodException("QuestObject.isComplete(QuestData) / PlayerData.isComplete(QuestObject)");
            return Reflect.truth(done.invoke(team, obj));
        }
        return false;
    }
}
