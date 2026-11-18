package io.github.junjun1z.whathitme.integration.minetweaker;

import io.github.junjun1z.whathitme.api.WhatHitMeApi;

import minetweaker.MineTweakerAPI;
import minetweaker.MineTweakerImplementationAPI;

/**
 * Entry of the MineTweaker binding (MineTweaker has no class discovery): called by name through core.script.ScriptBindings
 * when the mod MineTweaker3 is loaded.
 */
public final class WhatHitMeMT {

    private WhatHitMeMT() {}

    public static void init() {
        Class<?>[] classes = {WhatHitMeZS.class, ZenDamage.class, ZenDeath.class, ZenResults.Stats.class, ZenResults.TestResult.class,
            ZenDamageRecordedEvent.class, ZenDeathEvent.class, ZenRespawnEvent.class, ZenAlertEvent.class, IPlayerCondition.class,
            IPlayerVariable.class, IRegisterHandler.class, IDamageRecordedHandler.class, IDeathHandler.class, IRespawnHandler.class,
            IAlertHandler.class, ZenPlayer.class, ZenApi.class};
        for (Class<?> c : classes) MineTweakerAPI.registerClass(c);
        MineTweakerImplementationAPI.onReloadEvent(e -> WhatHitMeZS.reloading());
        MineTweakerImplementationAPI.onPostReload(e -> WhatHitMeApi.rebuild());
    }
}
