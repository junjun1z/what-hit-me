package io.github.junjun1z.whathitme.integration.minetweaker;

import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

/**
 * The registration methods of WhatHitMe as an object, for use inside script functions (ZenScript 1 functions cannot call
 * static methods): the argument of onRegister and event.api.
 */
@ZenClass("mods.whathitme.Api")
public class ZenApi {

    static final ZenApi INSTANCE = new ZenApi();

    @ZenMethod
    public void condition(String name, IPlayerCondition fn) {
        WhatHitMeZS.condition(name, fn);
    }

    @ZenMethod
    public void condition(String name, String description, IPlayerCondition fn) {
        WhatHitMeZS.condition(name, description, fn);
    }

    @ZenMethod
    public void variable(String name, IPlayerVariable fn) {
        WhatHitMeZS.variable(name, fn);
    }

    @ZenMethod
    public void variable(String name, String description, IPlayerVariable fn) {
        WhatHitMeZS.variable(name, description, fn);
    }

    @ZenMethod
    public void addRename(String key, String name) {
        WhatHitMeZS.addRename(key, name);
    }

    @ZenMethod
    public void addHint(String key, String hint) {
        WhatHitMeZS.addHint(key, hint);
    }

    @ZenMethod
    public void addDeathMessage(String key, String message) {
        WhatHitMeZS.addDeathMessage(key, message);
    }

    @ZenMethod
    public void addTemplate(String output, String[] lines) {
        WhatHitMeZS.addTemplate(output, lines);
    }

    @ZenMethod
    public void addRule(String json) {
        WhatHitMeZS.addRule(json);
    }
}
