package io.github.junjun1z.whathitme.core.cmd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import io.github.junjun1z.whathitme.core.config.PackConfig;
import io.github.junjun1z.whathitme.core.data.DamageRecord;
import io.github.junjun1z.whathitme.core.data.MatchKey;
import io.github.junjun1z.whathitme.core.data.RuleMark;
import io.github.junjun1z.whathitme.core.data.Snapshot;
import io.github.junjun1z.whathitme.core.rules.Rule;
import io.github.junjun1z.whathitme.core.rules.RuleEval;
import io.github.junjun1z.whathitme.core.text.DeathMessages;
import io.github.junjun1z.whathitme.core.text.Fmt;
import io.github.junjun1z.whathitme.core.text.Msg;
import io.github.junjun1z.whathitme.core.text.RecapFormatter;
import io.github.junjun1z.whathitme.core.text.Txt;

/**
 * /whm test &lt;damage_id&gt; [entity_id] [item_id] [dim_id]: a dummy death record, its resulting name and tip, the death
 * triggers that would run (not run) and the chat preview. Also used for the death triggers themselves.
 */
public final class TestPreview {

    public static final String USAGE = "/whm test <damage_id> [entity_id] [item_id] [dim_id] [--as <player>] [--assume <condition>]...";

    private TestPreview() {}

    /** The arguments without the options, plus --as / --assume */
    public static final class Args {
        public final String[] args;
        /** Player whose progress is used, null = the sender */
        public final String as;
        public final List<String> assume;

        private Args(String[] args, String as, List<String> assume) {
            this.args = args;
            this.as = as;
            this.assume = assume;
        }

        public static Args parse(String[] all) {
            List<String> plain = new ArrayList<>();
            List<String> assume = new ArrayList<>();
            String as = null;
            for (int i = 0; i < all.length; i++) {
                String a = all[i];
                if (a.equalsIgnoreCase("--as") && i + 1 < all.length) as = all[++i];
                else if (a.equalsIgnoreCase("--assume") && i + 1 < all.length) assume.add(all[++i]);
                else plain.add(a);
            }
            return new Args(plain.toArray(new String[0]), as, assume);
        }
    }

    /** Completion of --as / --assume (null = not an option position) */
    public static List<String> completeOptions(String[] args, List<String> players) {
        if (args.length < 3) return null;
        String prev = args[args.length - 2];
        if (prev.equalsIgnoreCase("--as")) return Completion.matching(args, players);
        if (prev.equalsIgnoreCase("--assume")) {
            List<String> c = new ArrayList<>();
            for (String p : io.github.junjun1z.whathitme.core.rules.Conditions.prefixes()) c.add(p + ":");
            return Completion.matching(args, c);
        }
        String last = args[args.length - 1];
        if (last.startsWith("-")) return Completion.matching(args, "--as", "--assume");
        return null;
    }

    /** The dummy killing blow */
    public static DamageRecord record(String[] args, String defaultDim, float maxHealth) {
        String id = args.length > 1 ? args[1] : "minecraft:generic";
        String entity = args.length > 2 && !"-".equals(args[2]) ? args[2] : id.startsWith("mob:") ? id.substring(4) : "";
        if (entity.startsWith("@")) entity = entity.substring(1);
        if (entity.startsWith("mob:")) entity = entity.substring(4);
        String item = args.length > 3 && !"-".equals(args[3]) ? args[3] : "";
        if (item.startsWith("@item:")) item = item.substring(6);
        String dim = args.length > 4 ? args[4] : defaultDim;
        if (dim != null && dim.startsWith("@dim:")) dim = dim.substring(5);
        String type;
        if (id.startsWith("mob:")) type = "minecraft:player".equals(entity) ? "player" : "mob";
        else type = id.indexOf(':') >= 0 ? id.substring(id.indexOf(':') + 1) : id;
        String atkName = entity.isEmpty() ? "" : RecapFormatter.pretty(entity);
        String modId = id.startsWith("mob:") && entity.indexOf(':') > 0 ? entity.substring(0, entity.indexOf(':'))
            : id.indexOf(':') > 0 ? id.substring(0, id.indexOf(':')) : "unknown";
        String weaponName = item.isEmpty() ? "" : RecapFormatter.pretty(item);
        float dmg = Math.max(1f, maxHealth * 0.3f);
        return new DamageRecord(System.currentTimeMillis(), id, type, atkName, "", entity, "", modId,
            io.github.junjun1z.whathitme.core.Platform.modName(modId), dmg, dmg, 0f, DamageRecord.KILLING, -1f, weaponName, "", "", "", 1, 0f,
            maxHealth, "test", item, dim, "");
    }

    /** Death triggers matching a killing blow, in file order (conditions not checked) */
    public static List<PackConfig.DeathTrigger> matching(PackConfig pack, DamageRecord k) {
        return matching(pack, k, null);
    }

    /** Death triggers matching a killing blow whose if / any / unless hold for the player of e (null = not checked) */
    public static List<PackConfig.DeathTrigger> matching(PackConfig pack, DamageRecord k, RuleEval e) {
        if (pack == null || k == null) return Collections.emptyList();
        List<PackConfig.DeathTrigger> out = new ArrayList<>();
        MatchKey.Target t = MatchKey.Target.of(k);
        for (PackConfig.DeathTrigger d : pack.triggers) if (d.key.matches(t) && (e == null || d.conds.met(e))) out.add(d);
        return out;
    }

    /** A trigger command with {player} {x} {y} {z} {damage} filled in */
    public static String fill(String command, String player, double[] pos, DamageRecord k) {
        String c = command.replace("{player}", player == null ? "" : player);
        if (pos != null && pos.length >= 3) {
            c = c.replace("{x}", String.valueOf((long) Math.floor(pos[0]))).replace("{y}", String.valueOf((long) Math.floor(pos[1])))
                .replace("{z}", String.valueOf((long) Math.floor(pos[2])));
        } else {
            c = c.replace("{x}", "0").replace("{y}", "0").replace("{z}", "0");
        }
        c = c.replace("{damage}", String.format(Locale.ROOT, "%.1f", k == null ? 0f : k.taken()));
        if (k == null || c.indexOf('{') < 0) return c;
        // every other template variable (unknown braces stay as they are)
        io.github.junjun1z.whathitme.core.config.WhmConfig.Common cc = io.github.junjun1z.whathitme.core.config.WhmConfig.common;
        RecapFormatter f = new RecapFormatter(Msg.ENGLISH, new io.github.junjun1z.whathitme.core.text.DisplayOptions(
            io.github.junjun1z.whathitme.core.config.WhmConfig.ALL_FLAGS, false, cc == null ? 10 : cc.summarySeconds));
        io.github.junjun1z.whathitme.core.text.TemplateVars v = new io.github.junjun1z.whathitme.core.text.TemplateVars(f, k, null, System.currentTimeMillis())
            .player(player).pos(pos).unmasked();
        return io.github.junjun1z.whathitme.core.text.Template.fillPlain(c, Msg.ENGLISH, v);
    }

    /** The lines shown by /whm test (pack = null: no death triggers on this side) */
    public static List<Txt> lines(Msg m, RecapFormatter f, String[] args, PackConfig pack, String player, double[] pos, String dim, float maxHealth) {
        return lines(m, f, Args.parse(args), pack, player, pos, dim, maxHealth, null);
    }

    /**
     * @param e the rules with the progress of the tested player (null = no rules: a client without a server that has the mod)
     */
    public static List<Txt> lines(Msg m, RecapFormatter f, Args a, PackConfig pack, String player, double[] pos, String dim, float maxHealth, RuleEval e) {
        List<Txt> out = new ArrayList<>();
        String[] args = a.args;
        DamageRecord raw = record(args, dim, maxHealth);
        DamageRecord k = e == null ? raw : e.apply(raw);
        out.add(f.header(m.tr("whathitme.test.header", Completion.join(args, 1))));
        if (e != null && e.hasRules()) {
            if (a.as != null) out.add(m.tr(Fmt.GRAY, "whathitme.test.as", player));
            for (String c : a.assume) out.add(m.tr(Fmt.GRAY, "whathitme.test.assume", c));
            Rule r = e.find(raw);
            if (r == null) out.add(m.tr(Fmt.GRAY, "whathitme.test.rule.none"));
            else {
                out.add(m.tr(Fmt.LIGHT_PURPLE, "whathitme.test.rule", r.match + " (" + r.source + ")"));
                RuleMark mk = k.mark;
                if (mk != null && !mk.hiddenNames().isEmpty()) out.add(m.tr(Fmt.LIGHT_PURPLE, "whathitme.test.rule.hidden", String.join(", ", mk.hiddenNames())));
                if (mk != null && mk.enforced()) out.add(m.tr(Fmt.LIGHT_PURPLE, "whathitme.test.rule.enforced"));
                for (String c : r.commands) out.add(Msg.str(Fmt.YELLOW, " /" + fill(c, player, pos, raw)).append(Msg.str(Fmt.DARK_GRAY, "  (rule)")));
            }
        }
        out.add(m.tr(Fmt.WHITE, "whathitme.test.name", f.displayName(k).restyle(Fmt.WHITE)));
        Txt tip = f.tip(k);
        out.add(m.tr(Fmt.GREEN, "whathitme.test.hint", tip == null ? m.tr("whathitme.test.hint.none") : tip));
        if (pack != null) {
            List<PackConfig.DeathTrigger> trig = matching(pack, raw, e);
            if (trig.isEmpty()) out.add(m.tr(Fmt.GRAY, "whathitme.test.notriggers"));
            else out.add(m.tr(Fmt.GOLD, "whathitme.test.triggers", trig.size()));
            for (PackConfig.DeathTrigger d : trig) out.add(Msg.str(Fmt.YELLOW, " /" + fill(d.command, player, pos, raw)).append(Msg.str(Fmt.DARK_GRAY, "  (" + d.match + ")")));
            String dm = DeathMessages.choose(pack.deathMessages, raw, k);
            if (dm == null) out.add(m.tr(Fmt.GRAY, "whathitme.test.deathmessage.none"));
            else out.add(m.tr(Fmt.GOLD, "whathitme.test.deathmessage", DeathMessages.render(f, dm, k, player, "")));
        }

        out.add(m.tr(Fmt.GRAY, "whathitme.test.preview"));
        List<DamageRecord> recs = new ArrayList<>();
        recs.add(k);
        out.addAll(f.respawnSummary(new Snapshot(Snapshot.DEATH, k.time, recs, 0, "")));
        return out;
    }
}
