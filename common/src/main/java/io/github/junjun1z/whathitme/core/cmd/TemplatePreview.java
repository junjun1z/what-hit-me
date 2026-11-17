package io.github.junjun1z.whathitme.core.cmd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.junjun1z.whathitme.core.data.DamageRecord;
import io.github.junjun1z.whathitme.core.data.Gear;
import io.github.junjun1z.whathitme.core.data.Snapshot;
import io.github.junjun1z.whathitme.core.text.Fmt;
import io.github.junjun1z.whathitme.core.text.Msg;
import io.github.junjun1z.whathitme.core.text.RecapFormatter;
import io.github.junjun1z.whathitme.core.text.Template;
import io.github.junjun1z.whathitme.core.text.Txt;

/** /whm template preview &lt;output&gt; and the sample death of the template editor. */
public final class TemplatePreview {

    private TemplatePreview() {}

    /** A sample death: zombie with an enchanted iron sword, a skeleton's arrow, then a creeper explosion (killing blow) */
    public static Snapshot sample(long now) {
        List<DamageRecord> l = new ArrayList<>();
        l.add(new DamageRecord(now - 6200, "mob:minecraft:zombie", "mob", "Zombie", "", "minecraft:zombie", "", "minecraft", "Minecraft", 7f, 4.5f, 0f, 0,
            2.4f, "Iron Sword", "", "enchantment.minecraft.sharpness|3|Sharpness III", "", 2, 15.5f, 20f, "s1", "minecraft:iron_sword",
            "minecraft:overworld", ""));
        l.add(new DamageRecord(now - 3100, "minecraft:arrow", "arrow", "Skeleton", "", "minecraft:skeleton", "Arrow", "minecraft", "Minecraft", 4f, 3f, 0f,
            DamageRecord.PROJECTILE, 11.8f, "Bow", "", "", "", 1, 12.5f, 20f, "s2", "minecraft:bow", "minecraft:overworld", ""));
        l.add(new DamageRecord(now - 400, "minecraft:explosion.player", "explosion.player", "Creeper", "", "minecraft:creeper", "", "minecraft", "Minecraft",
            22f, 12.5f, 0f, DamageRecord.EXPLOSION | DamageRecord.KILLING, 1.6f, "", "", "", "", 1, 0f, 20f, "s3", "", "minecraft:overworld", ""));
        // script variables of the server show their name
        java.util.Map<String, String> vars = new java.util.LinkedHashMap<>();
        for (String n : io.github.junjun1z.whathitme.core.client.ClientCore.scriptVariables().keySet()) vars.put(n, "[" + n + "]");
        if (!vars.isEmpty()) {
            io.github.junjun1z.whathitme.core.data.RuleMark mk = new io.github.junjun1z.whathitme.core.data.RuleMark(0, "", "", vars, null, null);
            for (int i = 0; i < l.size(); i++) l.set(i, l.get(i).withMark(mk));
        }
        Gear g = new Gear("minecraft:overworld", 120.5, 64, -38.2, 20f, new Gear.Item[] {null, new Gear.Item("minecraft:iron_chestplate", "", "Iron Chestplate",
            "minecraft", "", false), null, null}, Collections.emptyList());
        return new Snapshot(Snapshot.DEATH, now, l, 2, "", g);
    }

    /** The output rendered for the caller: the real latest death (null: the sample) */
    public static List<Txt> lines(RecapFormatter f, String output, Snapshot latest, long now) {
        Msg m = f.m;
        String out = output.toLowerCase(java.util.Locale.ROOT);
        if (!Template.isOutput(out)) throw CommandText.err(m, "whathitme.template.unknown", output, String.join(", ", Template.OUTPUTS));
        List<Txt> lines = new ArrayList<>();
        boolean custom = f.o.template(out) != null;
        lines.add(f.header(m.tr("whathitme.template.header", out, m.tr(custom ? "whathitme.template.custom" : "whathitme.template.builtin"))));
        Snapshot s = latest;
        if (s == null || s.key() == null) {
            s = sample(now);
            lines.add(m.tr(Fmt.GRAY, "whathitme.template.sample"));
        }
        lines.addAll(render(f, out, s));
        return lines;
    }

    /** The output for a snapshot: hover lines, damage lines, summary, alert or the copy text */
    public static List<Txt> render(RecapFormatter f, String output, Snapshot s) {
        List<Txt> lines = new ArrayList<>();
        DamageRecord k = s.key();
        switch (output) {
            case Template.HOVER:
                if (k != null) lines.addAll(f.hoverLines(k));
                break;
            case Template.LINE:
                for (DamageRecord r : f.visible(s.records, 5)) lines.add(f.line(r, s.time));
                break;
            case Template.SUMMARY:
                lines.addAll(f.respawnSummary(s));
                break;
            case Template.ALERT:
                lines.add(f.alert(new Snapshot(Snapshot.TOTEM, s.time, s.records, s.keyIndex, "", s.gear), 1));
                break;
            default:
                for (String l : f.copyText(s, s.time, f.m.lang()).split("\n")) lines.add(Msg.str(l));
        }
        return lines;
    }
}
