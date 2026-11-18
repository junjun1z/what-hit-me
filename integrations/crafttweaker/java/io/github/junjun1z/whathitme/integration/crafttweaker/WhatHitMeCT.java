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

import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import net.minecraft.world.entity.player.Player;
import org.openzen.zencode.java.ZenCodeType;

/**
 * CraftTweaker 8+ (new ZenScript, Minecraft 1.17.1+): mods.whathitme.WhatHitMe. Loaded only by CraftTweaker
 * (@ZenRegister). The scripts run again on every /reload; everything they registered is removed first
 * (CTRun tells a new run).
 */
@ZenRegister
@ZenCodeType.Name("mods.whathitme.WhatHitMe")
public class WhatHitMeCT {

    public static final String OWNER = "crafttweaker";

    private static final List<RegisterHandler> HOOKS = new ArrayList<>();

    private WhatHitMeCT() {}

    /** > 0 while a script function called by What Hit Me? runs (its registrations do not start a new run) */
    private static int inHook;

    /** Runs a script function; what it registers belongs to the current run */
    private static <T> T callback(java.util.function.Supplier<T> f) {
        synchronized (HOOKS) {
            inHook++;
        }
        try {
            return f.get();
        } finally {
            synchronized (HOOKS) {
                inHook--;
            }
        }
    }

    /** The first registration of a new run (/reload) forgets what the previous run registered */
    private static void startRun() {
        synchronized (HOOKS) {
            if (inHook > 0 || !CTRun.newRun()) return;
            HOOKS.clear();
        }
        WhatHitMeApi.clear(OWNER);
    }

    @ZenCodeType.Method
    public static void condition(String name, PlayerCondition fn) {
        startRun();
        condition(name, "", fn);
    }

    @ZenCodeType.Method
    public static void condition(String name, String description, PlayerCondition fn) {
        startRun();
        WhatHitMeApi.registerCondition(OWNER, name, description, p -> callback(() -> fn.test(CTEvents.player(p))));
    }

    @ZenCodeType.Method
    public static void variable(String name, PlayerVariable fn) {
        startRun();
        variable(name, "", fn);
    }

    @ZenCodeType.Method
    public static void variable(String name, String description, PlayerVariable fn) {
        startRun();
        WhatHitMeApi.registerVariable(OWNER, name, description,
            (p, d) -> callback(() -> fn.value(CTEvents.player(p), CTDamage.of(d))));
    }

    @ZenCodeType.Method
    public static void addRename(String key, String name) {
        startRun();
        WhatHitMeApi.addRename(OWNER, key, name);
    }

    @ZenCodeType.Method
    public static void addHint(String key, String hint) {
        startRun();
        WhatHitMeApi.addHint(OWNER, key, hint);
    }

    @ZenCodeType.Method
    public static void addDeathMessage(String key, String message) {
        startRun();
        WhatHitMeApi.addDeathMessage(OWNER, key, message);
    }

    @ZenCodeType.Method
    public static void addTemplate(String output, String[] lines) {
        startRun();
        WhatHitMeApi.addTemplate(OWNER, output, Arrays.asList(lines));
    }

    /** A rule as JSON text (an entry of the pack "rules" list) */
    @ZenCodeType.Method
    public static void addRule(String json) {
        startRun();
        WhatHitMeApi.addRule(OWNER, json);
    }

    /** () => { ... } run on every rebuild; what it registers is removed before the next one */
    @ZenCodeType.Method
    public static void onRegister(RegisterHandler fn) {
        startRun();
        synchronized (HOOKS) {
            HOOKS.add(fn);
        }
        WhatHitMeApi.onRegister(OWNER, () -> {
            List<RegisterHandler> l;
            synchronized (HOOKS) {
                l = new ArrayList<>(HOOKS);
            }
            callback(() -> {
                for (RegisterHandler h : l) h.register();
                return null;
            });
        });
    }

    @ZenCodeType.Method
    public static void onDamageRecorded(DamageRecordedHandler fn) {
        startRun();
        WhatHitMeApi.listen(OWNER, DamageRecordedEvent.class, e -> callback(() -> {
            fn.handle(new CTDamageRecordedEvent(e));
            return null;
        }));
    }

    @ZenCodeType.Method
    public static void onDeath(DeathHandler fn) {
        startRun();
        WhatHitMeApi.listen(OWNER, DeathEvent.class, e -> callback(() -> {
            fn.handle(new CTDeathEvent(e));
            return null;
        }));
    }

    @ZenCodeType.Method
    public static void onRespawn(RespawnHandler fn) {
        startRun();
        WhatHitMeApi.listen(OWNER, RespawnEvent.class, e -> callback(() -> {
            fn.handle(new CTRespawnEvent(e));
            return null;
        }));
    }

    @ZenCodeType.Method
    public static void onAlert(AlertHandler fn) {
        startRun();
        WhatHitMeApi.listen(OWNER, AlertEvent.class, e -> callback(() -> {
            fn.handle(new CTAlertEvent(e));
            return null;
        }));
    }

    @ZenCodeType.Method
    @ZenCodeType.Nullable
    public static CTDeath lastDeath(Player player) {
        return CTDeath.of(WhatHitMeApi.lastDeath(player));
    }

    @ZenCodeType.Method
    public static CTDamage[] history(Player player, int n) {
        return CTDamage.all(WhatHitMeApi.history(player, n));
    }

    @ZenCodeType.Method
    public static CTResults.Stats stats(Player player) {
        WhmStats s = WhatHitMeApi.stats(player);
        return s == null ? null : new CTResults.Stats(s);
    }

    @ZenCodeType.Method
    public static CTResults.TestResult test(Player player, String damageId) {
        WhmTestResult r = WhatHitMeApi.test(player, damageId, null);
        return r == null ? null : new CTResults.TestResult(r);
    }

    @ZenCodeType.Method
    public static CTResults.TestResult test(Player player, String damageId, String entityId) {
        WhmTestResult r = WhatHitMeApi.test(player, damageId, entityId);
        return r == null ? null : new CTResults.TestResult(r);
    }

    /** Any rule condition for the player, e.g. "tag:boss_ready" or "!script:veteran" */
    @ZenCodeType.Method
    public static boolean check(Player player, String condition) {
        WhmPlayer p = WhatHitMeApi.player(player);
        return p != null && p.check(condition);
    }
}
