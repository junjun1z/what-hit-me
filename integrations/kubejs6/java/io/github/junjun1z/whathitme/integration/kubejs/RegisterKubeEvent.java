package io.github.junjun1z.whathitme.integration.kubejs;

import dev.latvian.mods.kubejs.event.EventJS;

/**
 * WhatHitMeEvents.register: everything registered here is removed and registered again on every rebuild (server start,
 * /whm reload, /reload). The same methods as the global WhatHitMe.
 */
public class RegisterKubeEvent extends EventJS {

    private final WhatHitMeJS w = new WhatHitMeJS();

    public void condition(String name, PlayerPredicate fn) {
        w.condition(name, fn);
    }

    public void condition(String name, String description, PlayerPredicate fn) {
        w.condition(name, description, fn);
    }

    public void variable(String name, PlayerValue fn) {
        w.variable(name, fn);
    }

    public void variable(String name, String description, PlayerValue fn) {
        w.variable(name, description, fn);
    }

    public void addRename(String key, String name) {
        w.addRename(key, name);
    }

    public void addHint(String key, String hint) {
        w.addHint(key, hint);
    }

    public void addDeathMessage(String key, String message) {
        w.addDeathMessage(key, message);
    }

    public void addTemplate(String output, Object lines) {
        w.addTemplate(output, lines);
    }

    public void addRule(Object rule) {
        w.addRule(rule);
    }
}
