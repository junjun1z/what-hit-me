package io.github.junjun1z.whathitme.devtest;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.client.ClientCore;
import io.github.junjun1z.whathitme.core.client.TemplateEditor;
import io.github.junjun1z.whathitme.core.cmd.TemplatePreview;
import io.github.junjun1z.whathitme.core.config.Templates;
import io.github.junjun1z.whathitme.core.config.WhmConfig;
import io.github.junjun1z.whathitme.core.data.DamageRecord;
import io.github.junjun1z.whathitme.core.data.Snapshot;
import io.github.junjun1z.whathitme.core.server.ServerCore;
import io.github.junjun1z.whathitme.core.text.DeathMessages;
import io.github.junjun1z.whathitme.core.text.Fmt;
import io.github.junjun1z.whathitme.core.text.Msg;
import io.github.junjun1z.whathitme.core.text.RecapFormatter;
import io.github.junjun1z.whathitme.core.text.Template;
import io.github.junjun1z.whathitme.core.text.Txt;
import io.github.junjun1z.whathitme.devtest.DevPort.Act;

/** Single player checks of the 1.3 features: templates, custom death messages, the template editor (DevPort.v13()). */
final class V13Plan {

    private final SelfTest t;
    private final DevPort p;

    V13Plan(SelfTest t) {
        this.t = t;
        this.p = t.port;
    }

    private static File whm() {
        return Platform.whmConfigDir();
    }

    private static void write(File f, String text) {
        try {
            f.getParentFile().mkdirs();
            Files.write(f.toPath(), text.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String read(File f) {
        try {
            return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    private static List<String> plain(List<Txt> lines) {
        List<String> out = new ArrayList<>();
        for (Txt l : lines) out.add(Fmt.strip(l.toPlain(Msg.LOCAL.lang())));
        return out;
    }

    private static boolean any(List<String> lines, String s) {
        for (String l : lines) if (l.contains(s)) return true;
        return false;
    }

    private static boolean starts(List<String> lines, String s) {
        for (String l : lines) if (l.startsWith(s)) return true;
        return false;
    }

    private static Snapshot sample() {
        return TemplatePreview.sample(System.currentTimeMillis());
    }

    private static List<String> hover(DamageRecord r) {
        return plain(ClientCore.formatter().hoverLines(r));
    }

    private static final String GENERIC = "minecraft:generic";

    private static String pack(boolean withDeathMessages) {
        return "{\n // 1.3 self-test\n \"templates\": {\n"
            + "  \"hover\": [\"&cWHMT-HOVER {name}\", \"Weapon: {weapon}\", \"Owner: {attacker_owner|nobody}\", \"Dmg2: {damage:2}\", \"Mob: {mob_name}\",\n"
            + "            \"lang:whathitme.summary.header\", \"Bogus: {bogusvar}\"],\n"
            + "  \"line\": \"&fWHMT-LINE {name} &c-{damage} {badges}\",\n"
            + "  \"summary\": [\"&6WHMT-SUM Killing Blow: {killing_blow}\", \"{recap_link}\", \"Total {total}/{total_seconds}s\"],\n"
            + "  \"alert\": [\"WHMT-ALERT {alert_kind}: {name} {damage}\"],\n"
            + "  \"copy\": [\"WHMT-COPY {player}\", \"{timeline}\", \"{equipment}\"],\n"
            + "  \"wrongoutput\": [\"x\"]\n"
            + " }" + (withDeathMessages ? ",\n \"deathMessages\": {\"*\": \"WHMT-DM-ANY {player}\", \"" + SelfTest.id("outOfWorld")
                + "\": \"&cWHMT-DM-VOID {player} ({damage_name}, {damage} hearts)\"}\n" : "\n")
            + "}\n";
    }

    void plan() {
        File packFile = new File(whm(), "pack/test13/templates.json");
        File lockFile = new File(whm(), "pack/test13/lock.json");
        File packLine = Templates.packFile(Template.LINE);
        t.act(() -> t.info("v13 section"));
        t.check("server info protocol 3", () -> ClientCore.serverInfo != null && ClientCore.serverInfo.protocol == 3 && ClientCore.serverInfo.v3,
            (Supplier<?>) () -> ClientCore.serverInfo == null ? "null" : "protocol " + ClientCore.serverInfo.protocol);
        t.check("pack samples for templates and death messages exist and are inert", () -> new File(whm(), "pack/templates/_example.json").isFile()
            && new File(whm(), "pack/deathmessages/_example.json").isFile() && WhmConfig.pack.templates.isEmpty() && WhmConfig.pack.deathMessages.isEmpty()
            && WhmConfig.pack.warnings.isEmpty(), (Supplier<?>) () -> WhmConfig.pack.templates.keySet() + " " + WhmConfig.pack.warnings);
        t.check("personal template file created with inert examples", () -> Templates.personalFile().isFile() && read(Templates.personalFile()).contains("\"hover\"")
            && WhmConfig.client.templates.isEmpty(), null);
        t.check("no template configured: every output is the built-in one", () -> {
            for (String o : Template.OUTPUTS) if (ClientCore.formatter().o.template(o) != null) return false;
            Snapshot s = sample();
            return any(plain(ClientCore.formatter().respawnSummary(s)), "Killing Blow") && !hover(s.records.get(0)).get(0).contains("WHMT") && hover(s.records.get(0)).contains("Attacker: Zombie");
        }, (Supplier<?>) () -> plain(ClientCore.formatter().respawnSummary(sample())) + " " + hover(sample().records.get(0)));
        // ---- pack templates
        t.act(() -> write(packFile, pack(true)));
        t.clearChat();
        t.act(() -> p.command("/whm reload"));
        t.waitFor("/whm reload warns about the unknown variable with the file name", () -> t.chatHas("test13/templates.json") && t.chatHas("unknown variable {bogusvar}"), 80);
        t.check("/whm reload warns about the unknown output", () -> t.chatHas("wrongoutput"), (Supplier<?>) t::chatText);
        t.waitFor("pack templates reach the client (server info)", () -> ClientCore.serverTemplates.containsKey(Template.HOVER)
            && ClientCore.formatter().o.template(Template.HOVER) != null, 60);
        t.check("hover template: colors, fallback, decimals, alias, lang: line, dropped line", () -> {
            List<String> h = hover(sample().records.get(0));
            t.info("hover: " + h);
            return h.get(0).equals("WHMT-HOVER Zombie") && h.contains("Weapon: Iron Sword") && h.contains("Owner: nobody") && h.contains("Dmg2: 2.25")
                && h.contains("Mob: Zombie") && any(h, "What Hit Me?") && !starts(h, "Bogus");
        }, (Supplier<?>) () -> hover(sample().records.get(0)));
        t.check("hover template: a line whose variables are all empty is left out", () -> !starts(hover(sample().key()), "Weapon:"),
            (Supplier<?>) () -> hover(sample().key()));
        t.cmd("/whm", "WHMT-LINE", 60);
        t.check("history line template keeps the [Copy ID] / [Mute] buttons", () -> t.chatHas("[Copy ID]"), (Supplier<?>) t::chatText);
        t.cmd("/whm template preview summary", "WHMT-SUM", 60);
        t.check("template preview names the source", () -> t.chatHas("Template preview: summary (custom)"), (Supplier<?>) t::chatText);
        t.cmd("/whm template preview copy", "WHMT-COPY", 60);
        t.cmd("/whm template preview alert", "WHMT-ALERT", 60);
        t.cmd("/whm template preview hover", "WHMT-HOVER", 60);
        t.cmd("/whm template preview line", "WHMT-LINE", 60);
        t.cmd("/whm template preview nothing", "Unknown output", 60);
        t.check("tab completion of /whm template", () -> ClientCore.complete(new String[] {"template", ""}).containsAll(Arrays.asList("preview", "reset"))
            && ClientCore.complete(new String[] {"template", "preview", ""}).containsAll(Arrays.asList(Template.OUTPUTS)), null);
        t.check("server side (players without the mod) uses the pack templates", () -> any(plain(new RecapFormatter(Msg.ENGLISH, ServerCore.options(null, false))
            .respawnSummary(sample())), "WHMT-SUM"), null);
        // heavy hit alert
        t.srv(Act.HEAL);
        t.sleep(2);
        t.clearChat();
        t.hit(Act.VOID, 17f);
        t.waitFor("alert template used for the heavy hit alert", () -> t.chatHas("WHMT-ALERT"), 60);
        t.check("alert template gets the [Details] link", () -> t.chatHas("[Details]"), (Supplier<?>) t::chatText);
        // personal beats pack, lock beats personal
        t.check("personal template beats the pack", () -> {
            write(Templates.personalFile(), "{\"templates\": {\"hover\": [\"WHMT-PERSONAL {name}\"]}}");
            WhmConfig.reloadClient();
            return hover(sample().records.get(0)).get(0).equals("WHMT-PERSONAL Zombie");
        }, (Supplier<?>) () -> hover(sample().records.get(0)));
        t.act(() -> write(lockFile, "{\"lockedFeatures\": [\"templates.hover\"]}"));
        t.cmd("/whm reload", "reloaded", 60);
        t.waitFor("locked template: the personal one is ignored", () -> hover(sample().records.get(0)).get(0).equals("WHMT-HOVER Zombie"), 60);
        // switches switched off by the server leave the lines empty
        t.cmd("/whm server config showAttackerWeapon false", "showAttackerWeapon", 60);
        t.waitFor("server switch off: the weapon line disappears", () -> !starts(hover(sample().records.get(0)), "Weapon:"), 60);
        t.cmd("/whm server config showAttackerWeapon true", "showAttackerWeapon", 60);
        t.waitFor("server switch on again: weapon line back", () -> starts(hover(sample().records.get(0)), "Weapon:"), 60);
        // ---- death messages
        t.check("death messages: the most specific key wins", () -> {
            DamageRecord gen = new DamageRecord(0, SelfTest.id("outOfWorld"), "outOfWorld", "", "", "", "", "minecraft", "Minecraft", 1, 1, 0, 0, -1, "", "", "", "", 1, 1, 20, "");
            DamageRecord fall = new DamageRecord(0, SelfTest.id("fall"), "fall", "", "", "", "", "minecraft", "Minecraft", 1, 1, 0, 0, -1, "", "", "", "", 1, 1, 20, "");
            String a = DeathMessages.find(WhmConfig.pack.deathMessages, gen);
            String b = DeathMessages.find(WhmConfig.pack.deathMessages, fall);
            return a != null && a.contains("WHMT-DM-VOID") && b != null && b.contains("WHMT-DM-ANY");
        }, (Supplier<?>) () -> WhmConfig.pack.deathMessages);
        t.clearChat();
        t.act(() -> p.command("/whm test " + SelfTest.id("outOfWorld")));
        t.waitFor("/whm test <void id>", () -> t.chatHas("Chat preview"), 60);
        t.check("/whm test previews the death message", () -> t.chatHas("Death message: WHMT-DM-VOID " + p.playerName()), (Supplier<?>) t::chatText);
        t.srv(Act.HEAL);
        t.sleep(25);
        t.clearChat();
        t.srv(Act.VOID, 1000f);
        t.waitFor("death screen", p::deathScreen, 100);
        t.waitFor("custom death message in chat (variables filled)", () -> t.chatHas("WHMT-DM-VOID " + p.playerName() + " (") && t.chatHas("hearts)"), 100);
        t.check("vanilla death message replaced (not shown)", () -> !t.chatHas("fell out of the world"), (Supplier<?>) t::chatText);
        t.sleep(30);
        t.screenshot("whm_selftest_v13_deathscreen");
        t.act(p::respawn);
        t.waitFor("respawned", () -> p.health() > 0f && p.noScreen(), 200);
        t.waitFor("respawn summary template", () -> t.chatHas("WHMT-SUM Killing Blow"), 100);
        t.cmd("/whm copyrecap", "Death recap copied", 40);
        t.waitFor("copy template used for [Copy Recap] (with the equipment block)", () -> p.clipboard().contains("WHMT-COPY " + p.playerName())
            && p.clipboard().contains("WHMT-LINE") && p.clipboard().contains("Max health"), 40);
        // no entries: vanilla message unchanged
        t.act(() -> write(packFile, pack(false)));
        t.cmd("/whm reload", "reloaded", 60);
        t.waitFor("death messages removed (server info)", () -> ClientCore.serverDeathMessages.isEmpty() && WhmConfig.pack.deathMessages.isEmpty(), 60);
        t.sleep(100);
        t.clearChat();
        t.srv(Act.GENERIC, 1000f);
        t.waitFor("death screen", p::deathScreen, 100);
        t.waitFor("no deathMessages: the vanilla message is unchanged", () -> t.chatHas(p.playerName() + " died"), 100);
        t.check("no custom death message without entries", () -> !t.chatHas("WHMT-DM"), (Supplier<?>) t::chatText);
        t.sleep(25);
        t.act(p::respawn);
        t.waitFor("respawned", () -> p.health() > 0f && p.noScreen(), 200);
        // ---- template editor
        t.act(() -> p.command("/whm template"));
        t.waitFor("/whm template opens the editor", () -> p.templateEditorOpen() && TemplateEditor.current != null, 60);
        t.sleep(10);
        t.screenshot("whm_selftest_editor");
        t.act(() -> {
            TemplateEditor e = TemplateEditor.current;
            e.select(Template.LINE);
            e.focus(0);
            e.field(0).cursor = e.field(0).text.length();
            e.field(0).anchor = e.field(0).cursor;
            for (char c : " WHMT-TYPED".toCharArray()) e.typed(c);
        });
        t.sleep(3);
        t.check("editor: typing changes the live preview", () -> any(TemplateEditor.current.previewText(), "WHMT-TYPED"),
            (Supplier<?>) () -> TemplateEditor.current.previewText());
        t.check("editor: backspace / home / selection", () -> {
            TemplateEditor e = TemplateEditor.current;
            e.typed('Q');
            e.key(TemplateEditor.K_BACKSPACE, false, false);
            String before = e.field(0).text;
            e.key(TemplateEditor.K_HOME, false, false);
            e.key(TemplateEditor.K_END, false, true);
            boolean sel = e.field(0).selected().equals(before);
            e.key(TemplateEditor.K_END, false, false);
            return before.endsWith("WHMT-TYPED") && sel;
        }, (Supplier<?>) () -> TemplateEditor.current.field(0).text);
        t.check("editor: clicking a variable inserts it at the cursor", () -> {
            TemplateEditor e = TemplateEditor.current;
            int[] a = e.area("var:damage");
            if (a == null) return false;
            e.mouseDown(0, (a[0] + a[2]) / 2.0, (a[1] + a[3]) / 2.0, false);
            return e.field(0).text.endsWith("WHMT-TYPED{damage}");
        }, (Supplier<?>) () -> TemplateEditor.current.field(0).text);
        t.sleep(3);
        t.check("editor: preview shows the inserted variable's value", () -> any(TemplateEditor.current.previewText(), "WHMT-TYPED6.3")
            || any(TemplateEditor.current.previewText(), "WHMT-TYPED"), (Supplier<?>) () -> TemplateEditor.current.previewText());
        t.screenshot("whm_selftest_editor_typed");
        t.check("editor: Save writes the personal template", () -> {
            TemplateEditor e = TemplateEditor.current;
            int[] a = e.area("save");
            e.mouseDown(0, a[0] + 2, a[1] + 2, false);
            Template tp = WhmConfig.client.templates.get(Template.LINE);
            return read(Templates.personalFile()).contains("WHMT-TYPED{damage}") && tp != null && tp.lines.get(0).contains("WHMT-TYPED")
                && any(plain(java.util.Collections.singletonList(ClientCore.formatter().line(sample().key(), sample().time))), "WHMT-TYPED");
        }, (Supplier<?>) () -> read(Templates.personalFile()));
        t.check("editor: Save to pack (single player with cheats) writes pack/templates/line.json", () -> {
            TemplateEditor e = TemplateEditor.current;
            int[] a = e.area("pack");
            if (a == null || !e.canSaveToPack()) return false;
            e.mouseDown(0, a[0] + 2, a[1] + 2, false);
            return read(packLine).contains("WHMT-TYPED{damage}");
        }, null);
        t.waitFor("editor: the pack folder was reloaded with the new file", () -> WhmConfig.pack.files.contains("templates/line.json"), 60);
        t.check("editor: Reset removes the personal template", () -> {
            TemplateEditor e = TemplateEditor.current;
            int[] a = e.area("reset");
            e.mouseDown(0, a[0] + 2, a[1] + 2, false);
            return WhmConfig.client.templates.get(Template.LINE) == null && "pack".equals(e.source());
        }, (Supplier<?>) () -> TemplateEditor.current.source());
        t.check("editor: a locked template is read-only", () -> {
            TemplateEditor e = TemplateEditor.current;
            e.select(Template.HOVER);
            String before = e.field(0).text;
            e.typed('Z');
            e.insertVariable("damage");
            return e.isLocked() && e.field(0).text.equals(before);
        }, null);
        t.sleep(3);
        t.check("editor: no Save / Reset for a locked template", () -> TemplateEditor.current.area("save") == null && TemplateEditor.current.area("reset") == null, null);
        t.screenshot("whm_selftest_editor_locked");
        t.check("editor: the variable list starts with the damage group", () -> TemplateEditor.current.area("var:damage") != null
            && TemplateEditor.current.area("var:recap") == null, null);
        t.act(() -> {
            TemplateEditor e = TemplateEditor.current;
            int[] a = e.area("varlist");
            for (int i = 0; i < 120; i++) e.scroll((a[0] + a[2]) / 2.0, (a[1] + a[3]) / 2.0, -1);
        });
        t.sleep(3);
        t.check("editor: the variable list scrolls to the last variable", () -> TemplateEditor.current.area("var:recap") != null, null);
        t.check("editor: a clicked variable is described in the status line", () -> {
            TemplateEditor e = TemplateEditor.current;
            int[] a = e.area("var:recap");
            e.mouseDown(0, (a[0] + a[2]) / 2.0, (a[1] + a[3]) / 2.0, false);
            return "recap".equals(e.selectedVariable());
        }, null);
        t.sleep(3);
        t.check("editor: status line shows the description", () -> TemplateEditor.current.statusLine().startsWith("{recap} ")
            && TemplateEditor.current.statusLine().length() > 10, (Supplier<?>) () -> TemplateEditor.current.statusLine());
        t.check("editor: death messages tab for pack makers", () -> {
            TemplateEditor e = TemplateEditor.current;
            e.select(Template.DEATH);
            return Template.DEATH.equals(e.output());
        }, null);
        t.act(() -> {
            TemplateEditor e = TemplateEditor.current;
            for (int i = 0; i < 25; i++) e.key(TemplateEditor.K_ENTER, false, false);
        });
        t.sleep(3);
        t.check("editor: the template lines scroll to the focused line", () -> {
            TemplateEditor e = TemplateEditor.current;
            return e.lineCount() > e.visibleLines() && e.area("field:0") == null && e.area("field:25") != null;
        }, (Supplier<?>) () -> TemplateEditor.current.lineCount() + " lines, " + TemplateEditor.current.visibleLines() + " visible");
        t.screenshot("whm_selftest_editor_scrolled");
        t.act(() -> TemplateEditor.current.key(TemplateEditor.K_ESCAPE, false, false));
        t.waitFor("editor: Escape closes it", () -> !p.templateEditorOpen(), 40);
        t.act(p::pressConfigTemplatesButton);
        t.waitFor("config screen button opens the editor", p::templateEditorOpen, 40);
        t.act(p::closeScreen);
        t.sleep(5);
        // ---- clean up
        t.act(() -> {
            packFile.delete();
            lockFile.delete();
            new File(whm(), "pack/test13").delete();
            packLine.delete();
            try {
                Templates.resetPersonal();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        t.cmd("/whm reload", "reloaded", 60);
        t.waitFor("templates gone after clean up", () -> ClientCore.formatter().o.template(Template.LINE) == null && ClientCore.serverTemplates.isEmpty(), 60);
    }
}
