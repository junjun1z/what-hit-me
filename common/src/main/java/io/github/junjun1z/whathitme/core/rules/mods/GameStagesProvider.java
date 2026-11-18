package io.github.junjun1z.whathitme.core.rules.mods;

import java.lang.reflect.Method;

import io.github.junjun1z.whathitme.core.rules.ConditionProvider;
import io.github.junjun1z.whathitme.core.server.PlayerHandle;
import io.github.junjun1z.whathitme.core.server.ServerPort;

/** stage:&lt;name&gt; - Game Stages (Darkhax): GameStageHelper.hasStage(player, name), 1.12.2 and newer. Reflection only. */
public final class GameStagesProvider implements ConditionProvider {

    private final Class<?> helper = Reflect.first("net.darkhax.gamestages.GameStageHelper");

    @Override
    public String unavailable(ServerPort srv) {
        return helper == null ? "Game Stages API (net.darkhax.gamestages.GameStageHelper) not found" : null;
    }

    @Override
    public boolean test(PlayerHandle p, String arg) throws Exception {
        Object player = p.nativePlayer();
        if (helper == null || player == null) return false;
        Method m = Reflect.staticMethod(helper, "hasStage", player.getClass(), String.class);
        if (m == null) throw new NoSuchMethodException("GameStageHelper.hasStage(player, String)");
        return Reflect.truth(m.invoke(null, player, arg));
    }

    /** The stages Game Stages knows (its known stages list) */
    @Override
    public java.util.List<String> suggestions() throws Exception {
        java.util.List<String> out = new java.util.ArrayList<>();
        Method m = helper == null ? null : Reflect.staticMethod(helper, "getKnownStages");
        Object v = m == null ? null : m.invoke(null);
        if (v instanceof java.util.Collection) {
            for (Object o : (java.util.Collection<?>) v) out.add(String.valueOf(o));
        }
        java.util.Collections.sort(out);
        return out;
    }
}
