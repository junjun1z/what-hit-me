package io.github.junjun1z.whathitme.core.text;

import java.util.Map;

import io.github.junjun1z.whathitme.core.data.DamageRecord;
import io.github.junjun1z.whathitme.core.data.MatchKey;
import io.github.junjun1z.whathitme.core.data.Snapshot;

/** Custom death messages of the pack (deathMessages): matching like renameMap, rendering like a one-line template. */
public final class DeathMessages {

    /** Built-in message when a rule hides the death message and the killer was an entity: "{player} was killed by {name}" */
    public static final String HIDDEN_KILLED = "#killed";
    /** Built-in message when a rule hides the death message without an attacker: "{player} died" */
    public static final String HIDDEN_DIED = "#died";

    private DeathMessages() {}

    /** The template of the most specific matching key, null when none matches */
    public static String find(Map<String, String> map, DamageRecord killing) {
        if (map == null || map.isEmpty() || killing == null) return null;
        return MatchKey.best(map, MatchKey.parseAll(map.keySet()), MatchKey.Target.of(killing));
    }

    /**
     * The message to use for a killing blow: a deathMessages template, {@link #HIDDEN_KILLED} / {@link #HIDDEN_DIED} when a
     * rule hides the death message, null = the vanilla message.
     *
     * @param raw the killing blow as recorded (matched against the keys)
     * @param ruled the same record after the rules (may hide the death message)
     */
    public static String choose(Map<String, String> map, DamageRecord raw, DamageRecord ruled) {
        String tpl = find(map, raw);
        if (tpl != null) return tpl;
        if (ruled != null && ruled.hidden(io.github.junjun1z.whathitme.core.data.RuleMark.H_DEATH_MESSAGE)) {
            boolean attacker = raw != null && (!raw.attackerName.isEmpty() || raw.damageId.startsWith("mob:"));
            return attacker ? HIDDEN_KILLED : HIDDEN_DIED;
        }
        return null;
    }

    public static boolean isHidden(String choice) {
        return HIDDEN_KILLED.equals(choice) || HIDDEN_DIED.equals(choice);
    }

    /**
     * The message text.
     *
     * @param template a deathMessages template or one of the HIDDEN_* choices
     * @param vanillaJson the vanilla message ({death_message}), "" = unknown
     */
    public static Txt render(RecapFormatter f, String template, DamageRecord killing, String player, String vanillaJson) {
        return render(f, template, killing, player, vanillaJson, false);
    }

    /**
     * @param fallback build the hidden messages as translations with an English fallback (server text for every client on
     *                 versions with {@code ServerPort.translationFallback()})
     */
    public static Txt render(RecapFormatter f, String template, DamageRecord killing, String player, String vanillaJson, boolean fallback) {
        String who = player == null ? "" : player;
        if (isHidden(template)) {
            boolean killed = HIDDEN_KILLED.equals(template);
            String key = killed ? "whathitme.deathmsg.hidden.killed" : "whathitme.deathmsg.hidden.died";
            Txt name = killing == null ? Msg.str("?") : f.displayName(killing);
            Object[] args = killed ? new Object[] {who, name} : new Object[] {who};
            // the English pattern is the fallback format of clients without the key
            if (fallback && !f.m.localized) return Txt.trFallback(key, Msg.englishRaw(key), args);
            return f.m.tr(key, args);
        }
        java.util.List<DamageRecord> one = new java.util.ArrayList<>();
        one.add(killing);
        Snapshot s = new Snapshot(Snapshot.DEATH, killing.time, one, 0, vanillaJson == null ? "" : vanillaJson);
        TemplateVars v = new TemplateVars(f, killing, s, killing.time).player(who);
        Txt t = Template.renderLine(f.m, template, v);
        return t == null ? Txt.lit(who) : t;
    }

    /** A literal JSON text component ({"text": ...}) of a plain message */
    public static String literalJson(String plain) {
        StringBuilder sb = new StringBuilder("{\"text\":\"");
        for (int i = 0; i < plain.length(); i++) {
            char c = plain.charAt(i);
            if (c == '"' || c == '\\') sb.append('\\').append(c);
            else if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
            else sb.append(c);
        }
        return sb.append("\"}").toString();
    }
}
