package io.github.junjun1z.whathitme.core.text;

import java.util.Map;

import io.github.junjun1z.whathitme.core.data.DamageRecord;
import io.github.junjun1z.whathitme.core.data.MatchKey;
import io.github.junjun1z.whathitme.core.data.Snapshot;

/** Custom death messages of the pack (deathMessages): matching like renameMap, rendering like a one-line template. */
public final class DeathMessages {

    private DeathMessages() {}

    /** The template of the most specific matching key, null when none matches */
    public static String find(Map<String, String> map, DamageRecord killing) {
        if (map == null || map.isEmpty() || killing == null) return null;
        return MatchKey.best(map, MatchKey.parseAll(map.keySet()), MatchKey.Target.of(killing));
    }

    /**
     * The message text.
     *
     * @param vanillaJson the vanilla message ({death_message}), "" = unknown
     */
    public static Txt render(RecapFormatter f, String template, DamageRecord killing, String player, String vanillaJson) {
        java.util.List<DamageRecord> one = new java.util.ArrayList<>();
        one.add(killing);
        Snapshot s = new Snapshot(Snapshot.DEATH, killing.time, one, 0, vanillaJson == null ? "" : vanillaJson);
        TemplateVars v = new TemplateVars(f, killing, s, killing.time).player(player);
        Txt t = Template.renderLine(f.m, template, v);
        return t == null ? Txt.lit(player == null ? "" : player) : t;
    }
}
