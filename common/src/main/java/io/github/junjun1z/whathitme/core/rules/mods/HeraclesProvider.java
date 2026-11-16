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

    @Override
    public String unavailable(ServerPort srv) {
        return handler == null ? "Heracles API (QuestProgressHandler) not found" : null;
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
