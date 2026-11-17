package io.github.junjun1z.whathitme.core.script;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.github.junjun1z.whathitme.api.WhmDamage;
import io.github.junjun1z.whathitme.api.WhmDeath;
import io.github.junjun1z.whathitme.api.WhmPlayer;
import io.github.junjun1z.whathitme.api.WhmStats;
import io.github.junjun1z.whathitme.api.WhmTestResult;
import io.github.junjun1z.whathitme.core.Safe;
import io.github.junjun1z.whathitme.core.cmd.TestPreview;
import io.github.junjun1z.whathitme.core.config.PackConfig;
import io.github.junjun1z.whathitme.core.config.WhmConfig;
import io.github.junjun1z.whathitme.core.data.DamageRecord;
import io.github.junjun1z.whathitme.core.data.PlayerHistory;
import io.github.junjun1z.whathitme.core.data.Stats;
import io.github.junjun1z.whathitme.core.rules.Rule;
import io.github.junjun1z.whathitme.core.rules.RuleEval;
import io.github.junjun1z.whathitme.core.server.PlayerHandle;
import io.github.junjun1z.whathitme.core.server.ServerCore;
import io.github.junjun1z.whathitme.core.server.ServerPort;
import io.github.junjun1z.whathitme.core.text.DeathMessages;
import io.github.junjun1z.whathitme.core.text.Fmt;
import io.github.junjun1z.whathitme.core.text.Msg;
import io.github.junjun1z.whathitme.core.text.RecapFormatter;
import io.github.junjun1z.whathitme.core.text.Txt;

/** The read part of the API (server side). */
public final class ScriptReads {

    private ScriptReads() {}

    /** The online player for a native player object, a UUID, a name or a WhmPlayer; null when not found */
    public static PlayerHandle handle(Object o) {
        ServerPort srv = ServerCore.port;
        if (o == null || srv == null) return null;
        if (o instanceof ScriptViews.Player) return ((ScriptViews.Player) o).h;
        if (o instanceof WhmPlayer) return srv.player(((WhmPlayer) o).getUuid());
        if (o instanceof UUID) return srv.player((UUID) o);
        if (o instanceof CharSequence) {
            String s = o.toString().trim();
            try {
                if (s.length() == 36) return srv.player(UUID.fromString(s));
            } catch (IllegalArgumentException ignored) {
                // a name
            }
            return srv.playerByName(s);
        }
        // the version's player object: compare with the online players (no mapped names needed)
        for (String n : Safe.get("online players", srv::onlinePlayerNames, Collections.<String>emptyList())) {
            PlayerHandle h = srv.playerByName(n);
            if (h != null && Safe.get("native player", h::nativePlayer, null) == o) return h;
        }
        return null;
    }

    public static WhmPlayer player(Object o) {
        return ScriptViews.player(handle(o));
    }

    public static WhmDeath lastDeath(Object o) {
        PlayerHandle h = handle(o);
        PlayerHistory hist = h == null ? null : ServerCore.tracker().peek(h.uuid());
        return hist == null ? null : ScriptViews.death(hist.lastDeath());
    }

    public static List<WhmDamage> history(Object o, int n) {
        PlayerHandle h = handle(o);
        PlayerHistory hist = h == null ? null : ServerCore.tracker().peek(h.uuid());
        List<WhmDamage> out = new ArrayList<>();
        if (hist == null || n <= 0) return out;
        List<DamageRecord> all = hist.records();
        for (DamageRecord r : all.subList(Math.max(0, all.size() - n), all.size())) out.add(ScriptViews.damage(r));
        return out;
    }

    public static WhmStats stats(Object o) {
        PlayerHandle h = handle(o);
        if (h == null) return null;
        final Stats s = Stats.of(h.uuid());
        final Map<String, Integer> causes = new LinkedHashMap<>();
        for (Stats.Cause c : s.top(Stats.MAX_CAUSES)) causes.put(c.id, c.deaths);
        final int deaths = s.deaths;
        final double taken = s.damageTaken;
        return new WhmStats() {
            @Override
            public int getDeaths() {
                return deaths;
            }

            @Override
            public double getDamageTaken() {
                return taken;
            }

            @Override
            public Map<String, Integer> getCauses() {
                return Collections.unmodifiableMap(causes);
            }
        };
    }

    public static WhmTestResult test(Object o, String damageId, String entityId, Collection<String> assume) {
        if (damageId == null || damageId.trim().isEmpty()) return null;
        PlayerHandle h = handle(o);
        PackConfig pack = WhmConfig.pack;
        RuleEval e = new RuleEval(h, pack);
        if (assume != null) e.assume(assume);
        String[] args = entityId == null || entityId.trim().isEmpty() ? new String[] {"test", damageId.trim()} : new String[] {"test", damageId.trim(), entityId.trim()};
        float max = h == null ? 20f : Safe.get("max health", h::maxHealth, 20f);
        DamageRecord raw = TestPreview.record(args, h == null ? "" : Safe.get("dimension", h::dimension, ""), max);
        DamageRecord k = e.apply(raw);
        RecapFormatter f = new RecapFormatter(Msg.ENGLISH, ServerCore.options(h, false));
        String player = h == null ? "" : h.name();
        double[] pos = h == null ? null : Safe.get("position", h::position, null);
        final String name = plain(f.displayName(k));
        Txt tip = f.tip(k);
        final String hint = tip == null ? "" : plain(tip);
        Rule r = e.find(raw);
        final String rule = r == null ? "" : r.match + " (" + r.source + ")";
        final List<String> hidden = k.mark == null ? Collections.<String>emptyList() : k.mark.hiddenNames();
        String dm = DeathMessages.choose(pack.deathMessages, raw, k);
        final String death = dm == null ? "" : plain(DeathMessages.render(f, dm, k, player, ""));
        final List<String> cmds = new ArrayList<>();
        for (PackConfig.DeathTrigger t : TestPreview.matching(pack, raw, e)) cmds.add(TestPreview.fill(t.command, player, pos, raw));
        if (r != null) for (String c : r.commands) cmds.add(TestPreview.fill(c, player, pos, raw));
        return new WhmTestResult() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getHint() {
                return hint;
            }

            @Override
            public String getRule() {
                return rule;
            }

            @Override
            public List<String> getHidden() {
                return hidden;
            }

            @Override
            public String getDeathMessage() {
                return death;
            }

            @Override
            public List<String> getCommands() {
                return Collections.unmodifiableList(cmds);
            }

            @Override
            public String toString() {
                return name + (hint.isEmpty() ? "" : " / " + hint);
            }
        };
    }

    private static String plain(Txt t) {
        return Fmt.strip(t.toPlain(Msg.ENGLISH_LANG));
    }
}
