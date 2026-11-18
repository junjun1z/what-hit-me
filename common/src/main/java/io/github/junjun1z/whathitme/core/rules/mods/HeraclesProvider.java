package io.github.junjun1z.whathitme.core.rules.mods;

import java.lang.reflect.Method;
import java.util.UUID;

import io.github.junjun1z.whathitme.core.rules.ConditionProvider;
import io.github.junjun1z.whathitme.core.server.PlayerHandle;
import io.github.junjun1z.whathitme.core.server.ServerPort;

/**
 * heracles:&lt;quest id&gt; - Heracles (Team Resourceful, now "Odyssey Quests", mod id heracles):
 * QuestProgressHandler.getProgress(server, player uuid).isComplete(id). The server comes from the player object. Reflection only.
 */
public final class HeraclesProvider implements ConditionProvider {

    private final Class<?> handler = Reflect.first("earth.terrarium.heracles.common.handlers.progress.QuestProgressHandler");
    private final Class<?> quests = Reflect.first("earth.terrarium.heracles.common.handlers.quests.QuestHandler");

    @Override
    public String unavailable(ServerPort srv) {
        return handler == null ? "Heracles API (QuestProgressHandler) not found" : null;
    }

    /** Quest ids with titles: QuestHandler.quests() (id -> quest), title from display().title() */
    @Override
    public java.util.List<String> suggestions() throws Exception {
        java.util.List<String> out = new java.util.ArrayList<>();
        Method m = quests == null ? null : Reflect.staticMethod(quests, "quests");
        Object v = m == null ? null : m.invoke(null);
        if (!(v instanceof java.util.Map)) return out;
        for (java.util.Map.Entry<?, ?> e : ((java.util.Map<?, ?>) v).entrySet()) {
            Object d = e.getValue() == null ? null : Reflect.read(e.getValue(), "display", null);
            Object t = d == null ? null : Reflect.read(d, "title", null);
            // Component implements brigadier's Message.getString(), unmapped on every loader
            Object s = t == null ? null : Reflect.read(t, "getString", null);
            out.add(Reflect.entry(String.valueOf(e.getKey()), s));
        }
        java.util.Collections.sort(out);
        return out;
    }

    @Override
    public boolean test(PlayerHandle p, String arg) throws Exception {
        Object player = p.nativePlayer();
        if (handler == null || player == null) return false;
        Method get = null;
        for (Method m : handler.getMethods()) {
            if (m.getName().equals("getProgress") && m.getParameterCount() == 2 && m.getParameterTypes()[1] == UUID.class
                && java.lang.reflect.Modifier.isStatic(m.getModifiers())) get = m;
        }
        if (get == null) throw new NoSuchMethodException("QuestProgressHandler.getProgress(MinecraftServer, UUID)");
        Object server = Reflect.getterOfType(player, get.getParameterTypes()[0]);
        if (server == null) return false;
        Object progress = get.invoke(null, server, p.uuid());
        if (progress == null) return false;
        Method done = Reflect.method(progress.getClass(), "isComplete", String.class);
        if (done == null) throw new NoSuchMethodException("QuestsProgress.isComplete(String)");
        return Reflect.truth(done.invoke(progress, arg.trim()));
    }
}
