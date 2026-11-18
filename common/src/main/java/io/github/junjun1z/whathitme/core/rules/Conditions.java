package io.github.junjun1z.whathitme.core.rules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.config.PackConfig;
import io.github.junjun1z.whathitme.core.server.PlayerHandle;
import io.github.junjun1z.whathitme.core.server.ServerPort;

/**
 * The condition providers by prefix. Built-in ones (advancement, tag, score) ask the version adapter through
 * {@link PlayerHandle}; quest mod providers use reflection in their own classes (rules.mods), created only when the mod
 * is loaded, so no mod is ever a dependency. A failing provider counts as false and is logged once.
 */
public final class Conditions {

    /** A provider created on first use */
    private static final class ModEntry {
        final String[] modIds;
        final String className;
        final String label;
        ConditionProvider instance;
        String failure;

        ModEntry(String label, String className, String... modIds) {
            this.label = label;
            this.className = className;
            this.modIds = modIds;
        }

        String loadedModId() {
            Collection<String> ids = Platform.mods.modIds();
            if (ids == null) return null;
            for (String m : modIds) if (ids.contains(m)) return m;
            return null;
        }

        synchronized ConditionProvider get() {
            if (instance != null || failure != null) return instance;
            try {
                instance = (ConditionProvider) Class.forName(className).getDeclaredConstructor().newInstance();
            } catch (Throwable t) {
                failure = String.valueOf(t);
                Platform.log.warn("What Hit Me? could not start the " + label + " condition provider", t);
            }
            return instance;
        }
    }

    private static final Map<String, ConditionProvider> BUILTIN = new LinkedHashMap<>();
    private static final Map<String, ModEntry> MODS = new LinkedHashMap<>();
    /** Messages already logged ("prefix|reason") */
    private static final Set<String> LOGGED = new HashSet<>();

    static {
        BUILTIN.put("advancement", new Builtin("advancement") {
            @Override
            public boolean test(PlayerHandle p, String arg) {
                Boolean b = p.hasAdvancement(arg);
                return b != null && b;
            }

            @Override
            public String checkArg(String arg) {
                return arg.isEmpty() ? "empty advancement id" : null;
            }
        });
        BUILTIN.put("tag", new Builtin("tag") {
            @Override
            public boolean test(PlayerHandle p, String arg) {
                Boolean b = p.hasTag(arg);
                return b != null && b;
            }
        });
        BUILTIN.put("score", new Builtin("score") {
            @Override
            public boolean test(PlayerHandle p, String arg) {
                Score s = Score.parse(arg);
                if (s == null) return false;
                Long v = p.score(s.objective);
                return v != null && v != PlayerHandle.NO_SCORE && s.test(v);
            }

            @Override
            public String checkArg(String arg) {
                return Score.parse(arg) == null ? "expected <objective><op><number> with op one of >= > <= < = (e.g. score:kills>=5)" : null;
            }
        });
        // conditions registered by scripts through the API
        BUILTIN.put("script", io.github.junjun1z.whathitme.core.script.Scripts.CONDITION);
        String pkg = Conditions.class.getPackage().getName() + ".mods.";
        MODS.put("stage", new ModEntry("Game Stages", pkg + "GameStagesProvider", "gamestages"));
        MODS.put("ftbquests", new ModEntry("FTB Quests", pkg + "FtbQuestsProvider", "ftbquests"));
        MODS.put("bq", new ModEntry("Better Questing", pkg + "BetterQuestingProvider", "betterquesting", "bq_standard"));
        MODS.put("heracles", new ModEntry("Heracles", pkg + "HeraclesProvider", "heracles"));
    }

    private Conditions() {}

    /** A built-in condition: available when the version adapter supports it */
    private abstract static class Builtin implements ConditionProvider {
        final String prefix;

        Builtin(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public String unavailable(ServerPort srv) {
            if (srv != null && !srv.builtinConditions().contains(prefix)) {
                String name = srv.platformName();
                return "not supported " + (name.isEmpty() ? "on this version" : "on " + name);
            }
            return null;
        }
    }

    /** "objective>=5" */
    static final class Score {
        final String objective;
        final String op;
        final long value;

        private Score(String objective, String op, long value) {
            this.objective = objective;
            this.op = op;
            this.value = value;
        }

        static Score parse(String arg) {
            if (arg == null) return null;
            String[] ops = {">=", "<=", "!=", ">", "<", "="};
            for (int i = 0; i < arg.length(); i++) {
                for (String op : ops) {
                    if (arg.startsWith(op, i) && i > 0) {
                        try {
                            return new Score(arg.substring(0, i).trim(), op, Long.parseLong(arg.substring(i + op.length()).trim()));
                        } catch (NumberFormatException e) {
                            return null;
                        }
                    }
                }
            }
            return null;
        }

        boolean test(long v) {
            switch (op) {
                case ">=":
                    return v >= value;
                case "<=":
                    return v <= value;
                case ">":
                    return v > value;
                case "<":
                    return v < value;
                case "!=":
                    return v != value;
                default:
                    return v == value;
            }
        }
    }

    /** Pack editor values of a prefix ("id" or "id	title"), empty when unknown / failing */
    public static List<String> suggestions(String prefix, ServerPort srv) {
        try {
            if (BUILTIN.containsKey(prefix) && !"script".equals(prefix)) return srv == null ? java.util.Collections.<String>emptyList() : srv.conditionValues(prefix);
            if ("script".equals(prefix)) {
                List<String> out = new java.util.ArrayList<>();
                for (io.github.junjun1z.whathitme.core.script.ScriptRegistry.Fn<?> f : io.github.junjun1z.whathitme.core.script.ScriptRegistry.conditions()) {
                    out.add(f.description == null || f.description.isEmpty() ? f.name : f.name + "	" + f.description);
                }
                return out;
            }
            ConditionProvider p = provider(prefix);
            if (p == null || p.unavailable(srv) != null) return java.util.Collections.emptyList();
            return p.suggestions();
        } catch (Throwable t) {
            logOnce(prefix + "|suggestions", "What Hit Me? could not list the " + prefix + " values for the pack editor: " + t);
            return java.util.Collections.emptyList();
        }
    }

    public static Set<String> prefixes() {
        Set<String> out = new LinkedHashSet<>(BUILTIN.keySet());
        out.addAll(MODS.keySet());
        return out;
    }

    public static boolean known(String prefix) {
        return BUILTIN.containsKey(prefix) || MODS.containsKey(prefix);
    }

    /** The provider of a prefix, null when unknown or its mod is missing */
    static ConditionProvider provider(String prefix) {
        ConditionProvider b = BUILTIN.get(prefix);
        if (b != null) return b;
        ModEntry m = MODS.get(prefix);
        if (m == null || m.loadedModId() == null) return null;
        return m.get();
    }

    /** Why a prefix cannot be evaluated here (unknown, mod missing, unsupported), null = fine */
    public static String problem(String prefix, ServerPort srv) {
        ConditionProvider b = BUILTIN.get(prefix);
        if (b != null) return b.unavailable(srv);
        ModEntry m = MODS.get(prefix);
        if (m == null) return "unknown condition prefix \"" + prefix + "\" (known: " + String.join(", ", prefixes()) + ")";
        if (m.loadedModId() == null) return m.label + " is not installed";
        ConditionProvider p = m.get();
        if (p == null) return m.label + " provider failed: " + m.failure;
        return p.unavailable(srv);
    }

    /** Evaluates one condition for a player (null player: false), without "!" handling */
    static boolean evaluate(Condition c, PlayerHandle p) {
        if (p == null) return false;
        ConditionProvider pr = provider(c.prefix);
        if (pr == null) {
            logOnce(c.prefix + "|missing", "What Hit Me? rule condition " + c.raw + " is false: " + problem(c.prefix, null));
            return false;
        }
        try {
            return pr.test(p, c.arg);
        } catch (Throwable t) {
            logOnce(c.prefix + "|error|" + t.getClass().getName(), "What Hit Me? rule condition " + c.raw + " failed (counted as false): " + t);
            return false;
        }
    }

    private static void logOnce(String key, String msg) {
        synchronized (LOGGED) {
            if (!LOGGED.add(key)) return;
        }
        Platform.log.warn(msg, null);
    }

    /** /whm reload: unknown prefixes, providers whose mod is missing, unsupported built-ins, bad arguments (each once) */
    public static List<String> warnings(PackConfig pack, ServerPort srv) {
        List<String> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Rule r : pack.rules) check(r.conds, "rule " + r, srv, out, seen);
        for (PackConfig.DeathTrigger t : pack.triggers) check(t.conds, "deathTrigger \"" + t.match + "\" (" + t.source + ")", srv, out, seen);
        synchronized (LOGGED) {
            LOGGED.clear();
        }
        return out;
    }

    private static void check(Conds cs, String where, ServerPort srv, List<String> out, Set<String> seen) {
        for (Condition c : cs.every()) {
            if (c.prefix.isEmpty()) {
                if (seen.add("noprefix|" + c.raw)) out.add(where + ": condition \"" + c.raw + "\" has no prefix (e.g. advancement:, tag:, score:)");
                continue;
            }
            String p = problem(c.prefix, srv);
            if (p != null) {
                if (seen.add("prefix|" + c.prefix)) out.add(where + ": " + c.prefix + ": " + p);
                continue;
            }
            ConditionProvider pr = provider(c.prefix);
            String bad = pr == null ? null : pr.checkArg(c.arg);
            if (bad != null && seen.add("arg|" + c.raw)) out.add(where + ": " + c.raw + ": " + bad);
        }
    }
}
