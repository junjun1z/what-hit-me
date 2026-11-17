package io.github.junjun1z.whathitme.integration.crafttweaker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.github.junjun1z.whathitme.api.AlertEvent;
import io.github.junjun1z.whathitme.api.DamageRecordedEvent;
import io.github.junjun1z.whathitme.api.DeathEvent;
import io.github.junjun1z.whathitme.api.RespawnEvent;
import io.github.junjun1z.whathitme.api.WhatHitMeApi;
import io.github.junjun1z.whathitme.api.WhmPlayer;
import io.github.junjun1z.whathitme.api.WhmStats;
import io.github.junjun1z.whathitme.api.WhmTestResult;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.player.IPlayer;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

/**
 * CraftTweaker 1.12.2 (ZenScript): mods.whathitme.WhatHitMe. Loaded only by CraftTweaker (@ZenRegister). Scripts load once
 * at game start; registrations made inside onRegister are made again on every rebuild (server start, /whm reload).
 */
@ZenRegister
@ZenClass("mods.whathitme.WhatHitMe")
public class WhatHitMeZS {

    public static final String OWNER = "crafttweaker";

    private static final List<IRegisterHandler> HOOKS = new ArrayList<>();

    private WhatHitMeZS() {}

    @ZenMethod
    public static void condition(String name, IPlayerCondition fn) {
        condition(name, "", fn);
    }

    @ZenMethod
    public static void condition(String name, String description, IPlayerCondition fn) {
        WhatHitMeApi.registerCondition(OWNER, name, description, p -> fn.test(ZenEvents.player(p)));
    }

    @ZenMethod
    public static void variable(String name, IPlayerVariable fn) {
        variable(name, "", fn);
    }

    @ZenMethod
    public static void variable(String name, String description, IPlayerVariable fn) {
        WhatHitMeApi.registerVariable(OWNER, name, description, (p, d) -> fn.value(ZenEvents.player(p), ZenDamage.of(d)));
    }

    @ZenMethod
    public static void addRename(String key, String name) {
        WhatHitMeApi.addRename(OWNER, key, name);
    }

    @ZenMethod
    public static void addHint(String key, String hint) {
        WhatHitMeApi.addHint(OWNER, key, hint);
    }

    @ZenMethod
    public static void addDeathMessage(String key, String message) {
        WhatHitMeApi.addDeathMessage(OWNER, key, message);
    }

    @ZenMethod
    public static void addTemplate(String output, String[] lines) {
        WhatHitMeApi.addTemplate(OWNER, output, Arrays.asList(lines));
    }

    /** A rule as JSON text */
    @ZenMethod
    public static void addRule(String json) {
        WhatHitMeApi.addRule(OWNER, json);
    }

    /** function() { ... } run on every rebuild; what it registers is removed before the next one */
    @ZenMethod
    public static void onRegister(IRegisterHandler fn) {
        synchronized (HOOKS) {
            HOOKS.add(fn);
        }
        WhatHitMeApi.onRegister(OWNER, () -> {
            List<IRegisterHandler> l;
            synchronized (HOOKS) {
                l = new ArrayList<>(HOOKS);
            }
            for (IRegisterHandler h : l) h.register();
        });
    }

    @ZenMethod
    public static void onDamageRecorded(IDamageRecordedHandler fn) {
        WhatHitMeApi.listen(OWNER, DamageRecordedEvent.class, e -> fn.handle(new ZenDamageRecordedEvent(e)));
    }

    @ZenMethod
    public static void onDeath(IDeathHandler fn) {
        WhatHitMeApi.listen(OWNER, DeathEvent.class, e -> fn.handle(new ZenDeathEvent(e)));
    }

    @ZenMethod
    public static void onRespawn(IRespawnHandler fn) {
        WhatHitMeApi.listen(OWNER, RespawnEvent.class, e -> fn.handle(new ZenRespawnEvent(e)));
    }

    @ZenMethod
    public static void onAlert(IAlertHandler fn) {
        WhatHitMeApi.listen(OWNER, AlertEvent.class, e -> fn.handle(new ZenAlertEvent(e)));
    }

    @ZenMethod
    public static ZenDeath lastDeath(IPlayer player) {
        return ZenDeath.of(WhatHitMeApi.lastDeath(ZenEvents.nativePlayer(player)));
    }

    @ZenMethod
    public static ZenDamage[] history(IPlayer player, int n) {
        return ZenDamage.all(WhatHitMeApi.history(ZenEvents.nativePlayer(player), n));
    }

    @ZenMethod
    public static ZenResults.Stats stats(IPlayer player) {
        WhmStats s = WhatHitMeApi.stats(ZenEvents.nativePlayer(player));
        return s == null ? null : new ZenResults.Stats(s);
    }

    @ZenMethod
    public static ZenResults.TestResult test(IPlayer player, String damageId) {
        WhmTestResult r = WhatHitMeApi.test(ZenEvents.nativePlayer(player), damageId, null);
        return r == null ? null : new ZenResults.TestResult(r);
    }

    @ZenMethod
    public static ZenResults.TestResult test(IPlayer player, String damageId, String entityId) {
        WhmTestResult r = WhatHitMeApi.test(ZenEvents.nativePlayer(player), damageId, entityId);
        return r == null ? null : new ZenResults.TestResult(r);
    }

    /** Any rule condition for the player, e.g. "tag:boss_ready" or "!script:veteran" */
    @ZenMethod
    public static boolean check(IPlayer player, String condition) {
        WhmPlayer p = WhatHitMeApi.player(ZenEvents.nativePlayer(player));
        return p != null && p.check(condition);
    }
}
