package io.github.junjun1z.whathitme.devtest;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Supplier;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.client.ClientCore;
import io.github.junjun1z.whathitme.core.client.TemplateEditor;
import io.github.junjun1z.whathitme.core.config.Templates;
import io.github.junjun1z.whathitme.core.config.WhmConfig;
import io.github.junjun1z.whathitme.core.data.DamageRecord;
import io.github.junjun1z.whathitme.core.data.RuleMark;
import io.github.junjun1z.whathitme.core.server.ServerCore;
import io.github.junjun1z.whathitme.core.text.Fmt;
import io.github.junjun1z.whathitme.core.text.Msg;
import io.github.junjun1z.whathitme.core.text.Template;
import io.github.junjun1z.whathitme.core.text.Txt;
import io.github.junjun1z.whathitme.devtest.DevPort.Act;
import io.github.junjun1z.whmdriver.VanillaCommands;

/**
 * Single player checks of the progression rules (DevPort.rules13()): advancement / tag / score conditions, first match,
 * hide + enforce, --assume, the hidden death message, deathTrigger conditions, rule commands, search, editor preview.
 */
final class V13RulesPlan {

    static final String ADV = "minecraft:story/mine_stone";

    private final SelfTest t;
    private final DevPort p;
    private final boolean modern;

    V13RulesPlan(SelfTest t) {
        this.t = t;
        this.p = t.port;
        this.modern = VanillaCommands.of(p.mcVersion()).atLeast(1, 13, 0);
    }

    private static void write(File f, String text) {
        try {
            f.getParentFile().mkdirs();
            Files.write(f.toPath(), text.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String tag(boolean add, String tag) {
        return modern ? "/tag @p " + (add ? "add " : "remove ") + tag : "/scoreboard players tag @p " + (add ? "add " : "remove ") + tag;
    }

    private void vanilla(String cmd) {
        t.act(() -> p.say(cmd));
        t.sleep(10);
    }

    static String rules() {
        String zombie = "mob:minecraft:zombie";
        String voidId = SelfTest.id("outOfWorld");
        String fall = SelfTest.id("fall");
        return "{\n \"rules\": [\n"
            + "  {\"match\": \"" + zombie + "\", \"unless\": [\"advancement:" + ADV + "\"], \"name\": \"&5WHMT-HIDDEN\", \"hint\": \"\",\n"
            + "   \"hide\": [\"damage_id\", \"attacker\", \"mod\", \"weapon\", \"death_message\"], \"commands\": [\"say WHMT-RULECMD {player}\"]},\n"
            + "  {\"match\": \"" + zombie + "\", \"name\": \"WHMT-KNOWN {attacker_name}\", \"hint\": \"WHMT-ZTIP\"},\n"
            + "  {\"match\": \"" + voidId + "\", \"if\": [\"tag:whmt_tag\"], \"name\": \"WHMT-VOID-TAG\"},\n"
            + "  {\"match\": \"" + voidId + "\", \"if\": [\"score:whmt_s>=5\"], \"name\": \"WHMT-VOID-SCORE\"},\n"
            + "  {\"match\": \"" + voidId + "\", \"any\": [\"tag:whmt_dm\", \"tag:whmt_other\"], \"unless\": [\"tag:whmt_never\"], \"hide\": [\"death_message\"]},\n"
            + "  {\"match\": \"" + fall + "\", \"if\": [\"!tag:whmt_pro\"], \"name\": \"WHMT-FALL\", \"hide\": [\"tip\"], \"enforce\": false},\n"
            + "  {\"match\": \"minecraft:cactus\", \"if\": [\"nosuch:thing\"], \"name\": \"never\"}\n"
            + " ],\n"
            + " \"deathTriggers\": [\n"
            + "  {\"match\": \"" + voidId + "\", \"command\": \"say WHMT-TRIG-IF {player}\", \"if\": [\"tag:whmt_tag\"]},\n"
            + "  {\"match\": \"" + voidId + "\", \"command\": \"say WHMT-TRIG-UNLESS {player}\", \"unless\": [\"tag:whmt_tag\"]}\n"
            + " ]\n}\n";
    }

    private static List<String> hoverOf(DamageRecord r) {
        List<String> out = new java.util.ArrayList<>();
        for (Txt l : ClientCore.formatter().hoverLines(r)) out.add(Fmt.strip(l.toPlain(Msg.LOCAL.lang())));
        return out;
    }

    /** Chat contains the text, color codes ignored */
    private boolean has(String text) {
        return Fmt.strip(t.chatText()).contains(text);
    }

    private String me() {
        return p.playerName();
    }

    private static String voidId() {
        return SelfTest.id("outOfWorld");
    }

    private static String fall() {
        return SelfTest.id("fall");
    }

    /** Like SelfTest.cmd with the command made when the step runs */
    private void cmd(Supplier<String> cmd, String expect, int timeout) {
        t.clearChat();
        t.act(() -> p.command(cmd.get()));
        t.waitFor("command => \"" + expect + "\"", () -> has(expect), timeout);
        t.act(() -> t.info(cmd.get() + " chat: " + t.chatText().replace('\n', '|')));
    }

    void plan() {
        File rulesFile = new File(Platform.whmConfigDir(), "pack/test13r/rules.json");
        t.act(() -> t.info("v13 rules section"));
        t.check("rules sample folder exists and is inert", () -> new File(Platform.whmConfigDir(), "pack/rules/_example.json").isFile() && WhmConfig.pack.rules.isEmpty(),
            (Supplier<?>) () -> WhmConfig.pack.rules);
        // clean progress
        vanilla("/advancement revoke @p only " + ADV);
        vanilla(tag(false, "whmt_tag"));
        vanilla(tag(false, "whmt_dm"));
        vanilla(tag(false, "whmt_pro"));
        vanilla("/scoreboard objectives remove whmt_s");
        vanilla("/scoreboard objectives add whmt_s dummy");
        t.act(() -> write(rulesFile, rules()));
        t.clearChat();
        t.act(() -> p.command("/whm reload"));
        t.waitFor("/whm reload warns about the unknown condition prefix", () -> has("unknown condition prefix \"nosuch\""), 80);
        t.check("death message warning only where the loader cannot replace it (names version and loader)", () -> ServerCore.port.replacesDeathMessages()
            ? !has("still see the vanilla death message") : has("players without What Hit Me? still see the vanilla death message") && has("On " + ServerCore.port.platformName()),
            (Supplier<?>) t::chatText);
        t.check("rules loaded", () -> WhmConfig.pack.rules.size() == 7, (Supplier<?>) () -> WhmConfig.pack.rules);
        // ---- hide + enforce (no advancement)
        t.act(() -> {
            WhmConfig.client.putMapEntry("renameMap", "mob:minecraft:zombie", "WHMT-PERSONAL");
            WhmConfig.client.putMapEntry("hintMap", "mob:minecraft:zombie", "WHMT-PTIP");
        });
        t.srv(Act.HEAL);
        t.hit(Act.ZOMBIE, 2f);
        t.cmd("/whm", "WHMT-HIDDEN", 60);
        t.check("hidden: no attacker / weapon / id / buttons, personal rename ignored (enforced)", () -> !has("Zombie") && !has("WHMT-PERSONAL")
            && !has("mob:minecraft:zombie"), (Supplier<?>) t::chatText);
        t.cmd("/whm test mob:minecraft:zombie", "Chat preview", 60);
        t.check("/whm test shows the rule, the hidden fields and the hidden death message", () -> has("Rule: mob:minecraft:zombie")
            && has("Hidden: damage_id, attacker, mod, weapon, death_message") && has("Name: WHMT-HIDDEN") && has("was killed by WHMT-HIDDEN")
            && has("WHMT-RULECMD"), (Supplier<?>) t::chatText);
        t.cmd("/whm test mob:minecraft:zombie --assume advancement:" + ADV, "Chat preview", 60);
        t.check("--assume: the next rule matches", () -> has("Assumed: advancement:" + ADV) && has("Name: WHMT-KNOWN Zombie") && has("WHMT-ZTIP"),
            (Supplier<?>) t::chatText);
        t.cmd("/whm search zombie", "Search", 60);
        t.check("search: entries whose rule hides the id are left out", () -> !has("mob:minecraft:zombie ") && !has("minecraft:zombie (") && has("zombie_villager"), (Supplier<?>) t::chatText);
        // ---- advancement reveals old entries too
        vanilla("/advancement grant @p only " + ADV);
        t.cmd("/whm", "WHMT-PERSONAL", 60);
        t.check("advancement done: next rule, not enforced -> personal rename wins, old record revealed", () -> !has("WHMT-HIDDEN"), (Supplier<?>) t::chatText);
        t.act(() -> {
            WhmConfig.client.putMapEntry("renameMap", "mob:minecraft:zombie", "");
            WhmConfig.client.putMapEntry("hintMap", "mob:minecraft:zombie", "");
        });
        t.cmd("/whm", "WHMT-KNOWN Zombie", 60);
        // ---- tag / score / first match / negation (through /whm test)
        cmd(() -> "/whm test " + voidId(), "Chat preview", 60);
        t.check("no tag / score: no void rule, unless-trigger listed", () -> has("Rule: none matches") && has("WHMT-TRIG-UNLESS") && !has("WHMT-TRIG-IF"),
            (Supplier<?>) t::chatText);
        vanilla(tag(true, "whmt_tag"));
        cmd(() -> "/whm test " + voidId(), "Chat preview", 60);
        t.check("tag condition", () -> has("Name: WHMT-VOID-TAG") && has("WHMT-TRIG-IF") && !has("WHMT-TRIG-UNLESS"), (Supplier<?>) t::chatText);
        vanilla("/scoreboard players set @p whmt_s 5");
        cmd(() -> "/whm test " + voidId(), "Chat preview", 60);
        t.check("first match wins (tag rule before score rule)", () -> has("Name: WHMT-VOID-TAG"), (Supplier<?>) t::chatText);
        vanilla(tag(false, "whmt_tag"));
        cmd(() -> "/whm test " + voidId(), "Chat preview", 60);
        t.check("score condition (>= 5)", () -> has("Name: WHMT-VOID-SCORE"), (Supplier<?>) t::chatText);
        vanilla("/scoreboard players set @p whmt_s 4");
        cmd(() -> "/whm test " + voidId(), "Chat preview", 60);
        t.check("score below: no rule", () -> has("Rule: none matches"), (Supplier<?>) t::chatText);
        cmd(() -> "/whm test " + fall(), "Chat preview", 60);
        t.check("negated condition without the tag", () -> has("Name: WHMT-FALL") && has("Hidden: tip") && !has("Enforced"), (Supplier<?>) t::chatText);
        vanilla(tag(true, "whmt_pro"));
        cmd(() -> "/whm test " + fall(), "Chat preview", 60);
        t.check("negated condition with the tag: no rule", () -> has("Rule: none matches"), (Supplier<?>) t::chatText);
        // ---- hidden death message without an attacker + deathTrigger conditions in a real death
        vanilla(tag(true, "whmt_dm"));
        t.srv(Act.HEAL);
        t.sleep(25);
        t.clearChat();
        t.srv(Act.VOID, 1000f);
        t.waitFor("death screen", p::deathScreen, 100);
        t.waitFor("hidden death message without attacker: \"<player> died\"", () -> has(me() + " died"), 100);
        t.check("vanilla void message not shown", () -> !has("fell out of the world"), (Supplier<?>) t::chatText);
        t.waitFor("deathTrigger with unless ran (no whmt_tag)", () -> has("WHMT-TRIG-UNLESS " + me()), 60);
        t.check("deathTrigger with if did not run", () -> !has("WHMT-TRIG-IF"), (Supplier<?>) t::chatText);
        t.sleep(20);
        t.act(p::respawn);
        t.waitFor("respawned", () -> p.health() > 0f && p.noScreen(), 200);
        vanilla(tag(false, "whmt_dm"));
        // ---- killed by the hidden zombie
        vanilla("/advancement revoke @p only " + ADV);
        t.srv(Act.HEAL);
        t.sleep(80); // respawn invulnerability (60 ticks) blocks mob damage
        t.clearChat();
        t.srv(Act.ZOMBIE, 1000f);
        t.waitFor("death screen", p::deathScreen, 100);
        t.waitFor("hidden death message: \"<player> was killed by <rule name>\"", () -> has(me() + " was killed by WHMT-HIDDEN"), 100);
        t.check("vanilla zombie message not shown", () -> !has("slain by Zombie"), (Supplier<?>) t::chatText);
        t.waitFor("rule commands ran", () -> has("WHMT-RULECMD " + me()), 60);
        t.check("death record of the client carries the rule result", () -> {
            DamageRecord k = ClientCore.currentDeath == null ? null : ClientCore.currentDeath.key();
            return k != null && k.mark != null && k.mark.hides(RuleMark.H_ATTACKER) && k.attackerName.isEmpty() && k.weaponName.isEmpty()
                && !ClientCore.currentDeath.deathMessage.contains("Zombie");
        }, (Supplier<?>) () -> ClientCore.currentDeath == null ? "none" : ClientCore.currentDeath.key().damageId + " " + ClientCore.currentDeath.deathMessage);
        t.check("enforced record: the personal hover template is not used", () -> {
            try {
                Templates.savePersonal(Template.HOVER, java.util.Collections.singletonList("WHMT-PT {attacker_name}"));
            } catch (Exception e) {
                return false;
            }
            WhmConfig.reloadClient();
            List<String> h = hoverOf(ClientCore.currentDeath.key());
            return !h.isEmpty() && !h.get(0).startsWith("WHMT-PT") && h.get(0).contains("WHMT-HIDDEN");
        }, (Supplier<?>) () -> hoverOf(ClientCore.currentDeath.key()));
        t.sleep(10);
        t.screenshot("whm_selftest_rules_deathscreen");
        t.act(p::respawn);
        t.waitFor("respawned", () -> p.health() > 0f && p.noScreen(), 200);
        t.cmd("/whm last", "WHMT-HIDDEN", 60);
        t.check("/whm last: hidden death message quoted, no attacker", () -> has("was killed by WHMT-HIDDEN") && !has("Zombie"), (Supplier<?>) t::chatText);
        // ---- editor preview shows the rule result
        t.act(() -> p.command("/whm template"));
        t.waitFor("editor open", () -> p.templateEditorOpen() && TemplateEditor.current != null, 60);
        t.act(() -> TemplateEditor.current.select(Template.HOVER));
        t.sleep(3);
        t.check("editor preview: real death with the rule result", () -> !TemplateEditor.current.usesSample() && TemplateEditor.current.ruleNote().contains("attacker")
            && String.join("|", TemplateEditor.current.previewText()).contains("WHMT-HIDDEN"), (Supplier<?>) () -> TemplateEditor.current.ruleNote() + " "
            + TemplateEditor.current.previewText());
        t.screenshot("whm_selftest_rules_editor");
        t.act(() -> TemplateEditor.current.key(TemplateEditor.K_ESCAPE, false, false));
        t.waitFor("editor closed", () -> !p.templateEditorOpen(), 40);
        // ---- clean up
        t.act(() -> {
            rulesFile.delete();
            new File(Platform.whmConfigDir(), "pack/test13r").delete();
            try {
                Templates.resetPersonal();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        vanilla("/advancement revoke @p only " + ADV);
        vanilla(tag(false, "whmt_pro"));
        vanilla("/scoreboard objectives remove whmt_s");
        t.cmd("/whm reload", "reloaded", 60);
        t.waitFor("rules gone after clean up", () -> WhmConfig.pack.rules.isEmpty(), 60);
    }
}
