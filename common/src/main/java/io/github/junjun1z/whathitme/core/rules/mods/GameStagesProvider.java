package io.github.junjun1z.whathitme.core.rules.mods;

import java.lang.reflect.Method;

import io.github.junjun1z.whathitme.core.rules.ConditionProvider;
import io.github.junjun1z.whathitme.core.server.PlayerHandle;
import io.github.junjun1z.whathitme.core.server.ServerPort;

/**
 * stage:&lt;name&gt; - Game Stages (Darkhax): GameStageHelper.hasStage(player, name) (1.12.2 2.x and newer), or
 * PlayerDataHandler.getStageData(player).hasUnlockedStage(name) (1.0.x for 1.11.2 - 1.12.1). Reflection only.
 */
public final class GameStagesProvider implements ConditionProvider {

    private final Class<?> helper = Reflect.first("net.darkhax.gamestages.GameStageHelper");
    private final Class<?> old = helper != null ? null : Reflect.first("net.darkhax.gamestages.capabilities.PlayerDataHandler");

    @Override
    public String unavailable(ServerPort srv) {
        return helper == null && old == null ? "Game Stages API (net.darkhax.gamestages.GameStageHelper) not found" : null;
    }

    @Override
    public boolean test(PlayerHandle p, String arg) throws Exception {
        Object player = p.nativePlayer();
        if (player == null) return false;
        if (helper == null) {
            if (old == null) return false;
            Method get = Reflect.staticMethod(old, "getStageData", player.getClass());
            if (get == null) throw new NoSuchMethodException("PlayerDataHandler.getStageData(player)");
            Object data = get.invoke(null, player);
            if (data == null) return false;
            Method has = Reflect.method(data.getClass(), "hasUnlockedStage", String.class);
            if (has == null) throw new NoSuchMethodException("IStageData.hasUnlockedStage(String)");
            Reflect.open(has);
            return Reflect.truth(has.invoke(data, arg));
        }
        Method m = Reflect.staticMethod(helper, "hasStage", player.getClass(), String.class);
        if (m == null) throw new NoSuchMethodException("GameStageHelper.hasStage(player, String)");
        return Reflect.truth(m.invoke(null, player, arg));
    }

    /** The stages Game Stages knows (its known stages list; 1.0.x has none) */
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
