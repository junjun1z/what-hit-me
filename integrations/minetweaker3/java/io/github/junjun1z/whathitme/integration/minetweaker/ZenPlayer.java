package io.github.junjun1z.whathitme.integration.minetweaker;

import io.github.junjun1z.whathitme.api.WhatHitMeApi;
import io.github.junjun1z.whathitme.api.WhmPlayer;
import io.github.junjun1z.whathitme.api.WhmStats;
import io.github.junjun1z.whathitme.api.WhmTestResult;

import minetweaker.api.player.IPlayer;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenMethod;

/**
 * The player in script functions and events. ZenScript 1 functions cannot reach static classes, so the read methods are
 * here too: player.check("score:boss>=1"), player.lastDeath, ...
 */
@ZenClass("mods.whathitme.Player")
public class ZenPlayer {

    final WhmPlayer p;

    ZenPlayer(WhmPlayer p) {
        this.p = p;
    }

    static ZenPlayer of(WhmPlayer p) {
        return p == null ? null : new ZenPlayer(p);
    }

    @ZenGetter("name")
    public String name() {
        return p.getName();
    }

    @ZenGetter("player")
    public IPlayer player() {
        return ZenEvents.player(p);
    }

    /** Any rule condition, e.g. "score:boss>=1" or "!script:veteran" */
    @ZenMethod
    public boolean check(String condition) {
        return p.check(condition);
    }

    @ZenGetter("lastDeath")
    public ZenDeath lastDeath() {
        return ZenDeath.of(WhatHitMeApi.lastDeath(p.getNativePlayer()));
    }

    @ZenMethod
    public ZenDamage[] history(int n) {
        return ZenDamage.all(WhatHitMeApi.history(p.getNativePlayer(), n));
    }

    @ZenGetter("stats")
    public ZenResults.Stats stats() {
        WhmStats s = WhatHitMeApi.stats(p.getNativePlayer());
        return s == null ? null : new ZenResults.Stats(s);
    }

    @ZenMethod
    public ZenResults.TestResult test(String damageId) {
        WhmTestResult r = WhatHitMeApi.test(p.getNativePlayer(), damageId, null);
        return r == null ? null : new ZenResults.TestResult(r);
    }
}
