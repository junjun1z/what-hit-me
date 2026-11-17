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

    private static String strip(String arg) {
        String a = arg == null ? "" : arg.trim();
        return a.startsWith("#") ? a.substring(1) : a;
    }

    @Override
    public boolean test(PlayerHandle p, String arg) throws Exception {
        String id = strip(arg);
        if (modern != null) {
            Object file = Reflect.staticField(modern, "INSTANCE");
            Object player = p.nativePlayer();
            if (file == null || player == null) return false;
            Method data = Reflect.method(file.getClass(), "getOrCreateTeamData", player.getClass());
            // 1.16.5 (1605.x): getData(Entity)
            if (data == null) data = Reflect.method(file.getClass(), "getData", player.getClass());
            Method get = Reflect.method(file.getClass(), "get", long.class);
            if (data == null || get == null) throw new NoSuchMethodException("ServerQuestFile.getOrCreateTeamData(player) / get(long)");
            Object obj = get.invoke(file, Long.parseUnsignedLong(id, 16));
            Object team = data.invoke(file, player);
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
