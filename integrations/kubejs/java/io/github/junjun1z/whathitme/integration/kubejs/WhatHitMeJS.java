package io.github.junjun1z.whathitme.integration.kubejs;

import java.util.List;

import io.github.junjun1z.whathitme.api.ScriptValues;
import io.github.junjun1z.whathitme.api.WhatHitMeApi;
import io.github.junjun1z.whathitme.api.WhmDamage;
import io.github.junjun1z.whathitme.api.WhmDeath;
import io.github.junjun1z.whathitme.api.WhmPlayer;
import io.github.junjun1z.whathitme.api.WhmRegistration;
import io.github.junjun1z.whathitme.api.WhmStats;
import io.github.junjun1z.whathitme.api.WhmTestResult;

/** The global "WhatHitMe" of server scripts, and the methods of the register event. Thin wrapper of WhatHitMeApi. */
public class WhatHitMeJS {

    public static final String OWNER = "kubejs";

    static Object nativePlayer(WhmPlayer p) {
        Object n = p == null ? null : p.getNativePlayer();
        return n != null ? n : p;
    }

    public void condition(String name, PlayerPredicate fn) {
        condition(name, "", fn);
    }

    public void condition(String name, String description, PlayerPredicate fn) {
        WhatHitMeApi.registerCondition(OWNER, name, description, p -> fn.test(nativePlayer(p)));
    }

    public void variable(String name, PlayerValue fn) {
        variable(name, "", fn);
    }

    public void variable(String name, String description, PlayerValue fn) {
        WhatHitMeApi.registerVariable(OWNER, name, description, (p, d) -> ScriptValues.text(fn.apply(nativePlayer(p), d)));
    }

    public void addRename(String key, String name) {
        WhatHitMeApi.addRename(OWNER, key, name);
    }

    public void addHint(String key, String hint) {
        WhatHitMeApi.addHint(OWNER, key, hint);
    }

    public void addDeathMessage(String key, String message) {
        WhatHitMeApi.addDeathMessage(OWNER, key, message);
    }

    /** lines: an array of lines or one text */
    public void addTemplate(String output, Object lines) {
        WhatHitMeApi.addTemplate(OWNER, output, ScriptValues.lines(lines));
    }

    /** rule: an object like an entry of the pack "rules" list, or its JSON text */
    @SuppressWarnings("unchecked")
    public void addRule(Object rule) {
        Object p = ScriptValues.plain(rule);
        if (p instanceof java.util.Map) WhatHitMeApi.addRule(OWNER, (java.util.Map<String, Object>) p);
        else if (p != null) WhatHitMeApi.addRule(OWNER, p.toString());
    }

    public WhmDeath lastDeath(Object player) {
        return WhatHitMeApi.lastDeath(player);
    }

    public List<WhmDamage> history(Object player, int n) {
        return WhatHitMeApi.history(player, n);
    }

    public WhmStats stats(Object player) {
        return WhatHitMeApi.stats(player);
    }

    public WhmTestResult test(Object player, String damageId) {
        return WhatHitMeApi.test(player, damageId, null);
    }

    public WhmTestResult test(Object player, String damageId, String entityId) {
        return WhatHitMeApi.test(player, damageId, entityId);
    }

    /** Any rule condition for the player, e.g. "tag:boss_ready" or "!script:veteran" */
    public boolean check(Object player, String condition) {
        WhmPlayer p = WhatHitMeApi.player(player);
        return p != null && p.check(condition);
    }

    public List<WhmRegistration> registrations() {
        return WhatHitMeApi.registrations();
    }

    public int getApiVersion() {
        return WhatHitMeApi.API_VERSION;
    }
}
