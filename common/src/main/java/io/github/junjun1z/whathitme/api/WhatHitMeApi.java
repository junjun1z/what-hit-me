package io.github.junjun1z.whathitme.api;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.github.junjun1z.whathitme.core.script.ScriptRegistry;
import io.github.junjun1z.whathitme.core.script.ScriptReads;

/**
 * Entry point of the What Hit Me? API (server side). Thread: call from the server thread or while scripts load.
 *
 * <p>Players can be given as the version's player object, a {@link java.util.UUID}, a name or a {@link WhmPlayer}.
 */
public final class WhatHitMeApi {

    /** Raised when the API changes in a compatible way; 1 = What Hit Me? 1.3.0 */
    public static final int API_VERSION = 1;

    /** Event names for {@link #listen(String, String, WhmListener)} */
    public static final String DAMAGE_RECORDED = "damageRecorded";
    public static final String DEATH = "death";
    public static final String RESPAWN = "respawn";
    public static final String ALERT = "alert";

    private WhatHitMeApi() {}

    // registration

    /** A condition for rules: {@code script:<name>} ("!" negates). Names are case-insensitive. */
    public static void registerCondition(String owner, String name, String description, ScriptCondition condition) {
        ScriptRegistry.condition(owner, name, description, condition);
    }

    /** A template variable {@code {script:<name>}}. Names are case-insensitive (letters, digits, _ . -). */
    public static void registerVariable(String owner, String name, String description, ScriptVariable variable) {
        ScriptRegistry.variable(owner, name, description, variable);
    }

    /** Listens to {@link DamageRecordedEvent}, {@link DeathEvent}, {@link RespawnEvent} or {@link AlertEvent} */
    public static <E extends WhmEvent> void listen(String owner, Class<E> type, WhmListener<? super E> listener) {
        ScriptRegistry.listen(owner, ScriptRegistry.eventName(type), listener);
    }

    /** Like {@link #listen(String, Class, WhmListener)} with the event name ("damageRecorded", "death", "respawn", "alert") */
    public static void listen(String owner, String event, WhmListener<? extends WhmEvent> listener) {
        ScriptRegistry.listen(owner, event, listener);
    }

    /** A renameMap entry, merged after the pack files */
    public static void addRename(String owner, String key, String name) {
        ScriptRegistry.entry(owner, "rename", key, name);
    }

    /** A hintMap entry, merged after the pack files */
    public static void addHint(String owner, String key, String hint) {
        ScriptRegistry.entry(owner, "hint", key, hint);
    }

    /** A deathMessages entry, merged after the pack files */
    public static void addDeathMessage(String owner, String key, String message) {
        ScriptRegistry.entry(owner, "deathMessage", key, message);
    }

    /** A template of an output (hover, line, summary, alert, copy), merged after the pack files */
    public static void addTemplate(String owner, String output, List<String> lines) {
        ScriptRegistry.entry(owner, "template", output, lines == null ? "" : String.join("\n", lines));
    }

    /** A rule (the object of the pack "rules" list), added after the rules of the pack files */
    public static void addRule(String owner, Map<String, Object> rule) {
        ScriptRegistry.rule(owner, rule);
    }

    /** A rule as JSON text */
    public static void addRule(String owner, String json) {
        ScriptRegistry.rule(owner, json);
    }

    /**
     * A hook run on every rebuild (server start, /whm reload, the script mod's reload); what it registers is removed
     * before the next rebuild. A hook of the same owner and name replaces the old one.
     */
    public static void onRegister(String owner, Runnable hook) {
        ScriptRegistry.hook(owner, hook);
    }

    /** Removes everything the owner registered (hooks stay; see {@link #removeHooks}) */
    public static void clear(String owner) {
        ScriptRegistry.clear(owner);
    }

    public static void removeHooks(String owner) {
        ScriptRegistry.removeHooks(owner);
    }

    /** Removes the registrations made by hooks and runs every hook again */
    public static void rebuild() {
        ScriptRegistry.rebuild();
    }

    /**
     * {@link #rebuild()} on the next server tick, for script mods that only announce a reload before their scripts run.
     * Nothing happens while no server runs (the server start rebuilds anyway).
     */
    public static void rebuildLater() {
        ScriptRegistry.rebuildLater();
    }

    // reading

    /** The player, null when not online or no server runs */
    public static WhmPlayer player(Object player) {
        return ScriptReads.player(player);
    }

    /** The last death of an online player, null = none */
    public static WhmDeath lastDeath(Object player) {
        return ScriptReads.lastDeath(player);
    }

    /** The newest n records of an online player, oldest first */
    public static List<WhmDamage> history(Object player, int n) {
        return ScriptReads.history(player, n);
    }

    /** Statistics of an online player, null when unknown */
    public static WhmStats stats(Object player) {
        return ScriptReads.stats(player);
    }

    /** What a death from this damage id (and attacker entity id, may be null) would show for the player (may be null) */
    public static WhmTestResult test(Object player, String damageId, String entityId) {
        return ScriptReads.test(player, damageId, entityId, Collections.<String>emptyList());
    }

    /** Like {@link #test(Object, String, String)}, treating these conditions as fulfilled ("!x" = not fulfilled) */
    public static WhmTestResult test(Object player, String damageId, String entityId, Collection<String> assume) {
        return ScriptReads.test(player, damageId, entityId, assume);
    }

    /** Every registration (conditions, variables, listeners, runtime pack entries, hooks) */
    public static List<WhmRegistration> registrations() {
        return ScriptRegistry.registrations();
    }
}
