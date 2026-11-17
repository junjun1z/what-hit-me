package io.github.junjun1z.whathitme.core.script;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.github.junjun1z.whathitme.api.AlertEvent;
import io.github.junjun1z.whathitme.api.DamageRecordedEvent;
import io.github.junjun1z.whathitme.api.DeathEvent;
import io.github.junjun1z.whathitme.api.RespawnEvent;
import io.github.junjun1z.whathitme.api.ScriptCondition;
import io.github.junjun1z.whathitme.api.ScriptVariable;
import io.github.junjun1z.whathitme.api.WhatHitMeApi;
import io.github.junjun1z.whathitme.api.WhmEvent;
import io.github.junjun1z.whathitme.api.WhmListener;
import io.github.junjun1z.whathitme.api.WhmRegistration;
import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.Safe;
import io.github.junjun1z.whathitme.core.config.WhmConfig;
import io.github.junjun1z.whathitme.core.data.Json;
import io.github.junjun1z.whathitme.core.server.ServerCore;
import io.github.junjun1z.whathitme.core.server.ServerPort;

/**
 * Everything scripts registered through the API: conditions, variables, listeners, runtime pack entries and register
 * hooks. Registrations made while a hook runs are marked and removed on the next rebuild; others stay until their owner
 * is cleared. A function that throws counts as false / empty and is logged once.
 */
public final class ScriptRegistry {

    /** A registered function */
    public static final class Fn<T> {
        public final String owner;
        public final String name;
        public final String description;
        public final T fn;
        final boolean hook;
        volatile boolean failed;

        Fn(String owner, String name, String description, T fn, boolean hook) {
            this.owner = owner;
            this.name = name;
            this.description = description == null ? "" : description;
            this.fn = fn;
            this.hook = hook;
        }

        /** Logs the first failure of this function */
        void fail(String kind, Throwable t) {
            if (failed) return;
            failed = true;
            Platform.log.warn("What Hit Me? script " + kind + " \"" + name + "\" (" + owner + ") failed and counts as "
                + ("variable".equals(kind) ? "empty" : "condition".equals(kind) ? "false" : "not run") + " (logged once)", t);
        }
    }

    /** A runtime pack entry */
    static final class Entry {
        final String owner;
        final String kind;
        final String key;
        final Object value;
        final boolean hook;

        Entry(String owner, String kind, String key, Object value, boolean hook) {
            this.owner = owner;
            this.kind = kind;
            this.key = key;
            this.value = value;
            this.hook = hook;
        }
    }

    private static final Object LOCK = new Object();
    private static final Map<String, Fn<ScriptCondition>> CONDITIONS = new LinkedHashMap<>();
    private static final Map<String, Fn<ScriptVariable>> VARIABLES = new LinkedHashMap<>();
    private static final List<Fn<WhmListener<?>>> LISTENERS = new ArrayList<>();
    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static final Map<String, Runnable> HOOKS = new LinkedHashMap<>();
    private static final Map<String, Boolean> FAILED_HOOKS = new LinkedHashMap<>();
    /** > 0 while hooks run: registrations are marked as made by a hook */
    private static int inHook;
    private static volatile boolean applyScheduled;
    private static volatile boolean entriesChanged;

    private ScriptRegistry() {}

    static String owner(String owner) {
        return owner == null || owner.trim().isEmpty() ? "unknown" : owner.trim();
    }

    /** Lower-case name, or null when it has characters other than letters, digits, _ . - */
    public static String name(String name) {
        if (name == null) return null;
        String n = name.trim().toLowerCase(Locale.ROOT);
        if (n.isEmpty()) return null;
        for (int i = 0; i < n.length(); i++) {
            char c = n.charAt(i);
            if (!(c >= 'a' && c <= 'z') && !(c >= '0' && c <= '9') && c != '_' && c != '.' && c != '-') return null;
        }
        return n;
    }

    private static String checkedName(String kind, String owner, String name) {
        String n = name(name);
        if (n == null) Platform.log.warn("What Hit Me? script " + kind + " name \"" + name + "\" (" + owner(owner) + ") ignored: use letters, digits, _ . -", null);
        return n;
    }

    public static void condition(String owner, String name, String description, ScriptCondition c) {
        String n = checkedName("condition", owner, name);
        if (n == null || c == null) return;
        synchronized (LOCK) {
            CONDITIONS.put(n, new Fn<>(owner(owner), n, description, c, inHook > 0));
        }
        infoChanged();
    }

    public static void variable(String owner, String name, String description, ScriptVariable v) {
        String n = checkedName("variable", owner, name);
        if (n == null || v == null) return;
        synchronized (LOCK) {
            VARIABLES.put(n, new Fn<>(owner(owner), n, description, v, inHook > 0));
        }
        infoChanged();
    }

    /** Event name of an API event class */
    public static String eventName(Class<?> type) {
        if (type == DamageRecordedEvent.class) return WhatHitMeApi.DAMAGE_RECORDED;
        if (type == DeathEvent.class) return WhatHitMeApi.DEATH;
        if (type == RespawnEvent.class) return WhatHitMeApi.RESPAWN;
        if (type == AlertEvent.class) return WhatHitMeApi.ALERT;
        return type == null ? "" : type.getSimpleName();
    }

    private static String eventKey(String event) {
        if (event == null) return null;
        for (String e : new String[] {WhatHitMeApi.DAMAGE_RECORDED, WhatHitMeApi.DEATH, WhatHitMeApi.RESPAWN, WhatHitMeApi.ALERT}) {
            if (e.equalsIgnoreCase(event.trim())) return e;
        }
        return null;
    }

    public static void listen(String owner, String event, WhmListener<?> l) {
        String e = eventKey(event);
        if (e == null || l == null) {
            Platform.log.warn("What Hit Me? script listener for unknown event \"" + event + "\" (" + owner(owner) + ") ignored (damageRecorded, death, respawn, alert)", null);
            return;
        }
        synchronized (LOCK) {
            for (Fn<WhmListener<?>> f : LISTENERS) if (f.fn == l && f.name.equals(e)) return;
            LISTENERS.add(new Fn<WhmListener<?>>(owner(owner), e, "", l, inHook > 0));
        }
    }

    /** rename / hint / deathMessage / template */
    public static void entry(String owner, String kind, String key, String value) {
        if (key == null || key.trim().isEmpty()) return;
        String k = "template".equals(kind) ? key.trim().toLowerCase(Locale.ROOT) : key.trim();
        synchronized (LOCK) {
            boolean hook = inHook > 0;
            String o = owner(owner);
            // the same key of the same owner replaces the old entry
            for (int i = ENTRIES.size() - 1; i >= 0; i--) {
                Entry x = ENTRIES.get(i);
                if (x.owner.equals(o) && x.kind.equals(kind) && x.key.equals(k)) ENTRIES.remove(i);
            }
            ENTRIES.add(new Entry(o, kind, k, value == null ? "" : value, hook));
        }
        entriesChanged();
    }

    public static void rule(String owner, Map<String, Object> rule) {
        if (rule == null) return;
        synchronized (LOCK) {
            ENTRIES.add(new Entry(owner(owner), "rule", Json.str(rule.get("match"), "*"), new LinkedHashMap<>(rule), inHook > 0));
        }
        entriesChanged();
    }

    public static void rule(String owner, String json) {
        try {
            rule(owner, Json.obj(Json.parse(json)));
        } catch (Exception e) {
            Platform.log.warn("What Hit Me? script rule (" + owner(owner) + ") is not valid JSON: " + e.getMessage(), null);
        }
    }

    /** One hook per owner; a new one replaces it. Runs at once when a server is running. */
    public static void hook(String owner, Runnable hook) {
        if (hook == null) return;
        String o = owner(owner);
        synchronized (LOCK) {
            HOOKS.put(o, hook);
        }
        if (ServerCore.port != null) rebuild();
    }

    public static void removeHooks(String owner) {
        synchronized (LOCK) {
            HOOKS.remove(owner(owner));
        }
    }

    public static void clear(String owner) {
        String o = owner(owner);
        synchronized (LOCK) {
            CONDITIONS.values().removeIf(f -> f.owner.equals(o));
            VARIABLES.values().removeIf(f -> f.owner.equals(o));
            LISTENERS.removeIf(f -> f.owner.equals(o));
            ENTRIES.removeIf(x -> x.owner.equals(o));
        }
        entriesChanged();
        infoChanged();
    }

    /** Everything (server stopped: scripts register again with the next server) */
    public static void clearAll() {
        synchronized (LOCK) {
            CONDITIONS.clear();
            VARIABLES.clear();
            LISTENERS.clear();
            ENTRIES.clear();
            FAILED_HOOKS.clear();
        }
    }

    /** Removes what hooks registered and runs every hook again; the runtime pack entries are merged at the end */
    public static void rebuild() {
        List<Map.Entry<String, Runnable>> hooks;
        synchronized (LOCK) {
            CONDITIONS.values().removeIf(f -> f.hook);
            VARIABLES.values().removeIf(f -> f.hook);
            LISTENERS.removeIf(f -> f.hook);
            ENTRIES.removeIf(x -> x.hook);
            FAILED_HOOKS.clear();
            hooks = new ArrayList<>(HOOKS.entrySet());
            inHook++;
        }
        try {
            for (Map.Entry<String, Runnable> h : hooks) {
                try {
                    h.getValue().run();
                } catch (Throwable t) {
                    synchronized (LOCK) {
                        FAILED_HOOKS.put(h.getKey(), Boolean.TRUE);
                    }
                    Platform.log.warn("What Hit Me? script register hook of " + h.getKey() + " failed", t);
                }
            }
        } finally {
            synchronized (LOCK) {
                inHook--;
            }
        }
        entriesChanged = false;
        applyNow();
    }

    // runtime pack entries

    private static void entriesChanged() {
        entriesChanged = true;
        synchronized (LOCK) {
            if (inHook > 0) return; // merged once when the hooks are done
        }
        ServerPort srv = ServerCore.port;
        if (srv == null) {
            applyNow();
            return;
        }
        if (applyScheduled) return;
        applyScheduled = true;
        boolean ok = Safe.get("schedule", () -> srv.schedule(() -> {
            applyScheduled = false;
            applyNow();
        }), false);
        if (!ok) {
            applyScheduled = false;
            applyNow();
        }
    }

    private static void infoChanged() {
        synchronized (LOCK) {
            if (inHook > 0) return;
        }
        if (ServerCore.port != null && !applyScheduled) entriesChanged();
    }

    /** Merges the runtime entries into the pack (and tells the clients) */
    static void applyNow() {
        entriesChanged = false;
        Safe.run("script pack entries", () -> {
            WhmConfig.pack = WhmConfig.pack.withRuntime();
            if (ServerCore.port != null) ServerCore.broadcastInfo();
        });
    }

    /** The runtime entries as pack JSON roots, one per owner: {name, root} */
    public static List<Object[]> packRoots() {
        Map<String, Map<String, Object>> roots = new LinkedHashMap<>();
        synchronized (LOCK) {
            for (Entry x : ENTRIES) {
                Map<String, Object> root = roots.computeIfAbsent(x.owner, k -> new LinkedHashMap<>());
                switch (x.kind) {
                    case "rename":
                        section(root, "renameMap").put(x.key, x.value);
                        break;
                    case "hint":
                        section(root, "hintMap").put(x.key, x.value);
                        break;
                    case "deathMessage":
                        section(root, "deathMessages").put(x.key, x.value);
                        break;
                    case "template":
                        section(root, "templates").put(x.key, x.value);
                        break;
                    case "rule":
                        list(root, "rules").add(x.value);
                        break;
                    default:
                        break;
                }
            }
        }
        List<Object[]> out = new ArrayList<>();
        for (Map.Entry<String, Map<String, Object>> e : roots.entrySet()) out.add(new Object[] {"script:" + e.getKey(), e.getValue()});
        return out;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> section(Map<String, Object> root, String name) {
        return (Map<String, Object>) root.computeIfAbsent(name, k -> new LinkedHashMap<String, Object>());
    }

    @SuppressWarnings("unchecked")
    private static List<Object> list(Map<String, Object> root, String name) {
        return (List<Object>) root.computeIfAbsent(name, k -> new ArrayList<Object>());
    }

    // lookups

    public static Fn<ScriptCondition> conditionFn(String name) {
        String n = name(name);
        synchronized (LOCK) {
            return n == null ? null : CONDITIONS.get(n);
        }
    }

    public static List<Fn<ScriptCondition>> conditions() {
        synchronized (LOCK) {
            return new ArrayList<>(CONDITIONS.values());
        }
    }

    public static List<Fn<ScriptVariable>> variables() {
        synchronized (LOCK) {
            return new ArrayList<>(VARIABLES.values());
        }
    }

    public static boolean hasVariables() {
        synchronized (LOCK) {
            return !VARIABLES.isEmpty();
        }
    }

    public static boolean hasListeners(String event) {
        synchronized (LOCK) {
            for (Fn<WhmListener<?>> f : LISTENERS) if (f.name.equals(event)) return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    static void fire(String event, WhmEvent e) {
        List<Fn<WhmListener<?>>> list = new ArrayList<>();
        synchronized (LOCK) {
            for (Fn<WhmListener<?>> f : LISTENERS) if (f.name.equals(event)) list.add(f);
        }
        for (Fn<WhmListener<?>> f : list) {
            try {
                ((WhmListener<WhmEvent>) f.fn).handle(e);
            } catch (Throwable t) {
                f.fail("listener", t);
            }
        }
    }

    /** Runs a condition (false when unknown or failing) */
    public static boolean test(String name, io.github.junjun1z.whathitme.api.WhmPlayer p) {
        Fn<ScriptCondition> f = conditionFn(name);
        if (f == null || p == null) return false;
        try {
            return f.fn.test(p);
        } catch (Throwable t) {
            f.fail("condition", t);
            return false;
        }
    }

    /** Runs a variable ("" when failing) */
    static String value(Fn<ScriptVariable> f, io.github.junjun1z.whathitme.api.WhmPlayer p, io.github.junjun1z.whathitme.api.WhmDamage d) {
        try {
            String v = f.fn.value(p, d);
            return v == null ? "" : v;
        } catch (Throwable t) {
            f.fail("variable", t);
            return "";
        }
    }

    public static List<WhmRegistration> registrations() {
        List<WhmRegistration> out = new ArrayList<>();
        synchronized (LOCK) {
            for (Fn<ScriptCondition> f : CONDITIONS.values()) out.add(new WhmRegistration("condition", f.owner, f.name, f.description, f.hook, f.failed));
            for (Fn<ScriptVariable> f : VARIABLES.values()) out.add(new WhmRegistration("variable", f.owner, f.name, f.description, f.hook, f.failed));
            for (Fn<WhmListener<?>> f : LISTENERS) out.add(new WhmRegistration("listener", f.owner, f.name, "", f.hook, f.failed));
            for (Entry x : ENTRIES) {
                String v = x.value instanceof String ? (String) x.value : Json.write(x.value);
                out.add(new WhmRegistration(x.kind, x.owner, x.key, v, x.hook, false));
            }
            for (String o : HOOKS.keySet()) out.add(new WhmRegistration("hook", o, o, "", false, FAILED_HOOKS.containsKey(o)));
        }
        return out;
    }

    /** /whm reload: one summary line ("" = nothing registered) and the failures */
    public static List<String> summary() {
        List<WhmRegistration> all = registrations();
        Map<String, List<String>> by = new LinkedHashMap<>();
        Map<String, Integer> listeners = new LinkedHashMap<>();
        int entries = 0;
        for (WhmRegistration r : all) {
            switch (r.kind) {
                case "condition":
                case "variable":
                    by.computeIfAbsent(r.kind, k -> new ArrayList<>()).add(r.name);
                    break;
                case "listener":
                    listeners.merge(r.name, 1, Integer::sum);
                    break;
                case "hook":
                    break;
                default:
                    entries++;
            }
        }
        List<String> out = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        List<String> c = by.getOrDefault("condition", Collections.<String>emptyList());
        List<String> v = by.getOrDefault("variable", Collections.<String>emptyList());
        if (!c.isEmpty()) sb.append(c.size()).append(c.size() == 1 ? " condition (" : " conditions (").append(String.join(", ", c)).append(")");
        if (!v.isEmpty()) sb.append(sb.length() > 0 ? ", " : "").append(v.size()).append(v.size() == 1 ? " variable (" : " variables (").append(String.join(", ", v)).append(")");
        if (!listeners.isEmpty()) {
            List<String> l = new ArrayList<>();
            for (Map.Entry<String, Integer> e : listeners.entrySet()) l.add(e.getKey() + (e.getValue() > 1 ? " x" + e.getValue() : ""));
            sb.append(sb.length() > 0 ? ", " : "").append("listeners: ").append(String.join(", ", l));
        }
        if (entries > 0) sb.append(sb.length() > 0 ? ", " : "").append(entries).append(entries == 1 ? " pack entry" : " pack entries");
        if (sb.length() > 0) out.add(sb.toString());
        return out;
    }

    /** Failed functions and hooks (one line each) */
    public static List<String> failures() {
        List<String> out = new ArrayList<>();
        for (WhmRegistration r : registrations()) {
            if (r.failed) out.add("script " + r.kind + " \"" + r.name + "\" (" + r.owner + ") failed, see the log");
        }
        return out;
    }
}
