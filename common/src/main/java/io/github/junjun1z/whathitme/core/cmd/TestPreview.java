package io.github.junjun1z.whathitme.core.cmd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import io.github.junjun1z.whathitme.core.config.PackConfig;
import io.github.junjun1z.whathitme.core.data.DamageRecord;
import io.github.junjun1z.whathitme.core.data.MatchKey;
import io.github.junjun1z.whathitme.core.data.Snapshot;
import io.github.junjun1z.whathitme.core.text.Fmt;
import io.github.junjun1z.whathitme.core.text.Msg;
import io.github.junjun1z.whathitme.core.text.RecapFormatter;
import io.github.junjun1z.whathitme.core.text.Txt;

/**
 * /whm test &lt;damage_id&gt; [entity_id] [item_id] [dim_id]: a dummy death record, its resulting name and tip, the death
 * triggers that would run (not run) and the chat preview. Also used for the death triggers themselves.
 */
public final class TestPreview {

    private TestPreview() {}

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

    /** Death triggers matching a killing blow, in file order */
    public static List<PackConfig.DeathTrigger> matching(PackConfig pack, DamageRecord k) {
        if (pack == null || k == null) return Collections.emptyList();
        List<PackConfig.DeathTrigger> out = new ArrayList<>();
        MatchKey.Target t = MatchKey.Target.of(k);
        for (PackConfig.DeathTrigger d : pack.triggers) if (d.key.matches(t)) out.add(d);
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
        List<Txt> out = new ArrayList<>();
        DamageRecord k = record(args, dim, maxHealth);
        out.add(f.header(m.tr("whathitme.test.header", Completion.join(args, 1))));
        out.add(m.tr(Fmt.WHITE, "whathitme.test.name", f.displayName(k).restyle(Fmt.WHITE)));
        Txt tip = f.tip(k);
        out.add(m.tr(Fmt.GREEN, "whathitme.test.hint", tip == null ? m.tr("whathitme.test.hint.none") : tip));
        if (pack != null) {
            List<PackConfig.DeathTrigger> trig = matching(pack, k);
            if (trig.isEmpty()) out.add(m.tr(Fmt.GRAY, "whathitme.test.notriggers"));
            else out.add(m.tr(Fmt.GOLD, "whathitme.test.triggers", trig.size()));
            for (PackConfig.DeathTrigger d : trig) out.add(Msg.str(Fmt.YELLOW, " /" + fill(d.command, player, pos, k)).append(Msg.str(Fmt.DARK_GRAY, "  (" + d.match + ")")));
            String dm = io.github.junjun1z.whathitme.core.text.DeathMessages.find(pack.deathMessages, k);
            if (dm == null) out.add(m.tr(Fmt.GRAY, "whathitme.test.deathmessage.none"));
            else out.add(m.tr(Fmt.GOLD, "whathitme.test.deathmessage", io.github.junjun1z.whathitme.core.text.DeathMessages.render(f, dm, k, player, "")));
        }

        out.add(m.tr(Fmt.GRAY, "whathitme.test.preview"));
        List<DamageRecord> recs = new ArrayList<>();
        recs.add(k);
        out.addAll(f.respawnSummary(new Snapshot(Snapshot.DEATH, k.time, recs, 0, "")));
        return out;
    }
}
