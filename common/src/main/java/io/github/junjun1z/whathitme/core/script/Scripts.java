package io.github.junjun1z.whathitme.core.script;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.junjun1z.whathitme.api.AlertEvent;
import io.github.junjun1z.whathitme.api.DamageRecordedEvent;
import io.github.junjun1z.whathitme.api.DeathEvent;
import io.github.junjun1z.whathitme.api.RecordChanges;
import io.github.junjun1z.whathitme.api.RespawnEvent;
import io.github.junjun1z.whathitme.api.ScriptVariable;
import io.github.junjun1z.whathitme.api.WhatHitMeApi;
import io.github.junjun1z.whathitme.api.WhmDamage;
import io.github.junjun1z.whathitme.api.WhmPlayer;
import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.data.DamageRecord;
import io.github.junjun1z.whathitme.core.data.RuleMark;
import io.github.junjun1z.whathitme.core.data.Snapshot;
import io.github.junjun1z.whathitme.core.rules.ConditionProvider;
import io.github.junjun1z.whathitme.core.server.PlayerHandle;
import io.github.junjun1z.whathitme.core.server.ServerPort;

/** Where the core calls into scripts: events, variables, the "script:" condition. Never throws. */
public final class Scripts {

    private static final java.util.Set<String> BAD_HIDE = new java.util.HashSet<>();

    private Scripts() {}

    /** The changes of a listener as a mark over the record's current one (null = nothing changed) */
    static RuleMark mark(RecordChanges e, RuleMark base, List<String> lines) {
        if (!e.changed()) return base;
        int bits = base == null ? 0 : base.bits & ~RuleMark.EXTRA;
        String name = base == null ? "" : base.name;
        String hint = base == null ? "" : base.hint;
        if (e.getNewName() != null) {
            bits |= RuleMark.NAME;
            name = e.getNewName();
        }
        if (e.getNewHint() != null) {
            bits |= RuleMark.HINT;
            hint = e.getNewHint();
        }
        for (String h : e.getHiddenFields()) {
            int b = RuleMark.hideBit(h);
            if (b == 0) {
                synchronized (BAD_HIDE) {
                    if (BAD_HIDE.add(h)) Platform.log.warn("What Hit Me? script hide field \"" + h + "\" is unknown (known: " + String.join(", ", RuleMark.HIDE_NAMES) + ")", null);
                }
                continue;
            }
            // hiding enforces, like rules
            bits |= b | RuleMark.ENFORCE;
        }
        List<String> badges = new ArrayList<>(base == null ? Collections.<String>emptyList() : base.badges);
        badges.addAll(e.getNewBadges());
        List<String> ln = new ArrayList<>(base == null ? Collections.<String>emptyList() : base.lines);
        if (lines != null) ln.addAll(lines);
        return new RuleMark(bits, name, hint, base == null ? null : base.vars, badges, ln);
    }

    /** damageRecorded: the record to store (null = cancelled) */
    public static DamageRecord recorded(PlayerHandle p, DamageRecord r) {
        if (r == null || !ScriptRegistry.hasListeners(WhatHitMeApi.DAMAGE_RECORDED)) return r;
        DamageRecordedEvent e = new DamageRecordedEvent(ScriptViews.player(p), ScriptViews.damage(r));
        ScriptRegistry.fire(WhatHitMeApi.DAMAGE_RECORDED, e);
        if (e.isCancelled()) return null;
        return e.changed() ? r.withMark(mark(e, r.mark, null)) : r;
    }

    /** death: the killing blow as the listeners changed it (the same object when nothing changed) */
    public static DamageRecord death(PlayerHandle p, Snapshot s) {
        DamageRecord k = s == null ? null : s.key();
        if (!ScriptRegistry.hasListeners(WhatHitMeApi.DEATH)) return k;
        DeathEvent e = new DeathEvent(ScriptViews.player(p), ScriptViews.death(s));
        ScriptRegistry.fire(WhatHitMeApi.DEATH, e);
        if (k == null || !e.changed()) return k;
        return k.withMark(mark(e, k.mark, e.getSummaryLines()));
    }

    public static void respawn(PlayerHandle p, Snapshot s) {
        if (!ScriptRegistry.hasListeners(WhatHitMeApi.RESPAWN)) return;
        ScriptRegistry.fire(WhatHitMeApi.RESPAWN, new RespawnEvent(ScriptViews.player(p), ScriptViews.death(s)));
    }

    public static void alert(PlayerHandle p, Snapshot s) {
        if (!ScriptRegistry.hasListeners(WhatHitMeApi.ALERT)) return;
        ScriptRegistry.fire(WhatHitMeApi.ALERT, new AlertEvent(ScriptViews.player(p), ScriptViews.death(s)));
    }

    /** Values of every script variable for a record of this player (empty values left out) */
    public static Map<String, String> variables(PlayerHandle p, DamageRecord r) {
        if (p == null || r == null || !ScriptRegistry.hasVariables()) return Collections.emptyMap();
        Map<String, String> out = new LinkedHashMap<>();
        WhmPlayer wp = ScriptViews.player(p);
        WhmDamage d = ScriptViews.damage(r);
        for (ScriptRegistry.Fn<ScriptVariable> f : ScriptRegistry.variables()) {
            String v = ScriptRegistry.value(f, wp, d);
            if (!v.isEmpty()) out.put(f.name, v);
        }
        return out;
    }

    /** The record with the script variable values in its mark (death trigger commands) */
    public static DamageRecord withVars(PlayerHandle p, DamageRecord r) {
        Map<String, String> v = variables(p, r);
        if (v.isEmpty()) return r;
        RuleMark m = r.mark == null ? new RuleMark(0, "", "") : r.mark;
        return r.withMark(m.withVars(v));
    }

    /** Is there anything scripts add to outputs (variables)? */
    public static boolean active() {
        return ScriptRegistry.hasVariables();
    }

    /** The "script:" condition provider */
    public static final ConditionProvider CONDITION = new ConditionProvider() {
        @Override
        public String unavailable(ServerPort srv) {
            return null;
        }

        @Override
        public boolean test(PlayerHandle p, String arg) {
            return ScriptRegistry.test(arg, ScriptViews.player(p));
        }

        @Override
        public String checkArg(String arg) {
            if (ScriptRegistry.name(arg) == null) return "script condition names use letters, digits, _ . -";
            if (ScriptRegistry.conditionFn(arg) != null) return null;
            List<String> names = new ArrayList<>();
            for (ScriptRegistry.Fn<?> f : ScriptRegistry.conditions()) names.add(f.name);
            if (names.isEmpty()) return "unknown script condition \"" + arg + "\" (no script registered a condition: is the script mod installed?)";
            return "unknown script condition \"" + arg + "\" (registered: " + String.join(", ", names) + ")";
        }
    };
}
