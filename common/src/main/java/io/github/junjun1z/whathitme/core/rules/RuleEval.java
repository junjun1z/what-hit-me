package io.github.junjun1z.whathitme.core.rules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.junjun1z.whathitme.core.config.PackConfig;
import io.github.junjun1z.whathitme.core.config.WhmConfig;
import io.github.junjun1z.whathitme.core.data.DamageRecord;
import io.github.junjun1z.whathitme.core.data.Gear;
import io.github.junjun1z.whathitme.core.data.MatchKey;
import io.github.junjun1z.whathitme.core.data.RuleMark;
import io.github.junjun1z.whathitme.core.data.Snapshot;
import io.github.junjun1z.whathitme.core.script.Scripts;
import io.github.junjun1z.whathitme.core.server.PlayerHandle;

/**
 * Evaluates the pack rules for one player during one output build (server side). Condition results are cached only in
 * this object, so a quest completed a moment ago already changes the next output, including old records.
 */
public final class RuleEval {

    private final PlayerHandle player;
    private final List<Rule> rules;
    /** Conditions treated as fulfilled (/whm test --assume; "!x" = treated as not fulfilled) */
    private final Set<String> assumed = new HashSet<>();
    private final Set<String> assumedNot = new HashSet<>();
    private final Map<String, Boolean> cache = new HashMap<>();

    public RuleEval(PlayerHandle player, PackConfig pack) {
        this.player = player;
        this.rules = pack == null ? new ArrayList<Rule>() : pack.rules;
    }

    /** Rules of the current pack folder for this player (null = console / nobody: every condition is false) */
    public static RuleEval of(PlayerHandle player) {
        return new RuleEval(player, WhmConfig.pack);
    }

    public RuleEval assume(Collection<String> conds) {
        for (String s : conds) {
            Condition c = Condition.parse(s);
            if (c.negate) assumedNot.add(c.positive());
            else assumed.add(c.positive());
        }
        return this;
    }

    public boolean hasRules() {
        return !rules.isEmpty();
    }

    /** Outputs need this evaluator: rules, script variables, or records that scripts changed */
    public boolean needed(List<DamageRecord> records) {
        if (!rules.isEmpty() || Scripts.active()) return true;
        for (DamageRecord r : records) if (r != null && r.mark != null) return true;
        return false;
    }

    public PlayerHandle player() {
        return player;
    }

    /** One condition with "!" applied */
    public boolean test(Condition c) {
        String k = c.positive();
        Boolean v;
        if (assumed.contains(k)) v = Boolean.TRUE;
        else if (assumedNot.contains(k)) v = Boolean.FALSE;
        else {
            v = cache.get(k);
            if (v == null) {
                v = Conditions.evaluate(c, player);
                cache.put(k, v);
            }
        }
        return c.negate != v;
    }

    /** The first rule whose key matches and whose conditions are met, null = none */
    public Rule find(MatchKey.Target t) {
        for (Rule r : rules) if (r.key.matches(t) && r.conds.met(this)) return r;
        return null;
    }

    public Rule find(DamageRecord r) {
        return r == null || rules.isEmpty() ? null : find(MatchKey.Target.of(r));
    }

    /**
     * The record as this player may see it: the rule result combined with what scripts changed, the script variable
     * values attached, hidden fields removed
     */
    public DamageRecord apply(DamageRecord r) {
        if (r == null) return null;
        Rule rule = rules.isEmpty() ? null : find(r);
        RuleMark m = RuleMark.combine(rule == null ? null : rule.mark(), r.mark);
        Map<String, String> vars = Scripts.variables(player, r);
        if (!vars.isEmpty()) m = (m == null ? new RuleMark(0, "", "") : m).withVars(vars);
        if (m == null) return r;
        return m.strip(r).withMark(m);
    }

    public List<DamageRecord> apply(List<DamageRecord> list) {
        if (!needed(list)) return list;
        List<DamageRecord> out = new ArrayList<>(list.size());
        for (DamageRecord r : list) out.add(apply(r));
        return out;
    }

    /**
     * A snapshot as this player may see it. When the key record hides the death message, the vanilla message is replaced
     * by {@code hiddenDeathJson} ("" = left out).
     */
    public Snapshot apply(Snapshot s, String hiddenDeathJson) {
        if (s == null || !needed(s.records)) return s;
        List<DamageRecord> list = apply(s.records);
        DamageRecord k = s.keyIndex >= 0 && s.keyIndex < list.size() ? list.get(s.keyIndex) : null;
        String dm = s.deathMessage;
        Gear g = s.gear;
        if (k != null && k.hidden(RuleMark.H_DEATH_MESSAGE) && !dm.isEmpty()) dm = hiddenDeathJson == null ? "" : hiddenDeathJson;
        if (k != null && k.hidden(RuleMark.H_POSITION) && g != null) g = g.withoutPosition();
        return new Snapshot(s.kind, s.time, list, s.keyIndex, dm, g);
    }
}
