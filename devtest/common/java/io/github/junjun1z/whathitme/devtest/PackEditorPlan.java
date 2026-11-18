package io.github.junjun1z.whathitme.devtest;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.client.ClientCore;
import io.github.junjun1z.whathitme.core.client.PackEditor;
import io.github.junjun1z.whathitme.core.config.WhmConfig;
import io.github.junjun1z.whathitme.core.pack.PackModel;
import io.github.junjun1z.whathitme.core.pack.PackStore;
import io.github.junjun1z.whmdriver.VanillaCommands;

/**
 * Single player checks of the pack editor (/whm pack, DevPort.pack13()): categories, folders, every section, move,
 * order, on / off, memos, undo, server settings, personal mode; each save is checked in the JSON files and in game.
 */
final class PackEditorPlan {

    private final SelfTest t;
    private final DevPort p;
    private int before;

    private final boolean modern;

    PackEditorPlan(SelfTest t) {
        this.t = t;
        this.p = t.port;
        this.modern = VanillaCommands.of(p.mcVersion()).atLeast(1, 13, 0);
    }

    private String tagCmd(boolean add) {
        return modern ? "/tag @p " + (add ? "add" : "remove") + " whmt_vet" : "/scoreboard players tag @p " + (add ? "add" : "remove") + " whmt_vet";
    }

    private static File pack() {
        return new File(Platform.whmConfigDir(), "pack");
    }

    private static String read(String rel) {
        try {
            return new String(Files.readAllBytes(new File(pack(), rel).toPath()), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    private static PackEditor e() {
        return PackEditor.current;
    }

    /** Runs an editor action and waits for the server's answer (ok expected unless expectOk is false) */
    private void op(String name, Runnable action, boolean expectOk) {
        t.act(() -> {
            before = e() == null ? 0 : e().results();
            action.run();
        });
        t.waitFor(name + ": server answered", () -> e() != null && e().results() > before, 100);
        t.check(name + (expectOk ? ": ok" : ": refused"), () -> e().lastResult().startsWith(expectOk ? "ok" : "error"),
            (Supplier<?>) () -> e().lastResult() + " / " + e().status());
        t.sleep(4);
    }

    private void op(String name, Runnable action) {
        op(name, action, true);
    }

    private void shot(String name) {
        t.sleep(4);
        t.screenshot("whm_pack_" + name);
    }

    private void tab(String tab) {
        t.act(() -> e().selectTab(tab));
        t.sleep(2);
    }

    private void type(String field, String text) {
        t.act(() -> {
            e().field(field).text = text;
            e().field(field).cursor = text.length();
            e().field(field).anchor = text.length();
        });
    }

    private void newEntry(String tab) {
        tab(tab);
        t.act(() -> e().selectCategory("pe13/boss.json"));
        t.act(() -> e().newEntry());
        t.sleep(2);
    }

    void plan() {
        t.act(() -> t.info("pack editor section"));
        t.act(() -> {
            // leftovers of an aborted run
            deleteTree(new File(pack(), "pe13"));
            WhmConfig.reloadCommon();
        });
        t.clearChat();
        t.act(() -> p.command("/whm pack"));
        t.waitFor("/whm pack opens the editor", p::packEditorOpen, 60);
        t.waitFor("editor loaded the server state", () -> e() != null && e().loaded() && e().model().hasLists, 100);
        t.check("pack mode for an operator in single player", () -> !e().personal() && e().canEditPack() && PackStore.editors() == 1, null);
        t.check("existing pack files are categories (samples)", () -> e().model().cat("rules/_example.json") != null && e().model().cat("hints/_example.json") != null,
            (Supplier<?>) () -> e().model().cats.size());
        t.check("helper lists: damage ids, mobs, condition prefixes", () -> !e().model().damage.isEmpty() && !e().model().mobs.isEmpty()
            && e().model().conds.containsKey("advancement") && e().model().conds.containsKey("tag"), null);
        t.check("advancement values from the server", () -> !e().model().conds.get("advancement").isEmpty(), (Supplier<?>) () -> e().model().condProblems);
        shot("renames_empty");
        // ---- folder and category through the tree buttons and dialogs
        t.act(() -> e().openTreeAction("dir.new"));
        t.sleep(2);
        t.check("new folder dialog", () -> e().dialogOpen(), null);
        shot("dialog_newfolder");
        op("create folder pe13", () -> {
            e().dialogType("pe13");
            e().dialogOk();
        });
        t.check("folder exists", () -> new File(pack(), "pe13").isDirectory(), null);
        t.act(() -> e().selectNode("dir:pe13"));
        t.act(() -> e().openTreeAction("cat.new"));
        op("create category pe13/boss", () -> {
            e().dialogType("boss");
            e().dialogOk();
        });
        t.check("category file written", () -> new File(pack(), "pe13/boss.json").isFile() && e().model().cat("pe13/boss.json") != null, null);
        op("create category pe13/second", () -> e().createCategory("pe13", "second"));
        // ---- rename
        newEntry("renames");
        type("key", "minecraft:lava");
        type("value", "&cWHMT-PE Lava");
        type("memo", "memo of the rename");
        shot("rename_form");
        op("rename saved (Done)", () -> e().done());
        t.check("rename in the JSON file with its memo", () -> read("pe13/boss.json").contains("WHMT-PE Lava") && read("pe13/boss.json").contains("memo of the rename"),
            (Supplier<?>) () -> read("pe13/boss.json"));
        t.check("pack reloaded at once", () -> "&cWHMT-PE Lava".equals(WhmConfig.pack.renameMap.get("minecraft:lava")), (Supplier<?>) () -> WhmConfig.pack.renameMap);
        t.waitFor("client got the new server info", () -> ClientCore.serverInfo != null && ClientCore.serverInfo.packRenames.contains("minecraft:lava = &cWHMT-PE Lava"), 60);
        t.cmd("/whm test minecraft:lava", "WHMT-PE Lava", 60);
        // ---- hint, entered by clicking the form (area ids)
        newEntry("hints");
        t.act(() -> {
            e().field("key").text = "minecraft:lava";
            e().field("value").text = "WHMT-HINT swim up";
        });
        t.sleep(3);
        op("hint saved by clicking Done", () -> e().press("d.done"));
        t.check("hint in the file", () -> read("pe13/boss.json").contains("WHMT-HINT swim up"), null);
        t.cmd("/whm test minecraft:lava", "WHMT-HINT swim up", 60);
        // ---- rule with a condition and hide
        newEntry("rules");
        type("key", "minecraft:lava");
        type("name", "&kWHMT???");
        t.act(() -> {
            e().addCondition("unless", "tag:whmt_vet");
            e().setHide("damage_id", true);
        });
        t.act(() -> e().openConditionPicker());
        shot("dialog_condition");
        t.act(() -> e().dialogCancel());
        t.act(() -> e().openKeyBuilder());
        shot("dialog_keybuilder");
        t.act(() -> e().dialogCancel());
        shot("rule_form");
        op("rule saved", () -> e().done());
        t.check("rule in the file", () -> read("pe13/boss.json").contains("\"unless\"") && read("pe13/boss.json").contains("tag:whmt_vet")
            && WhmConfig.pack.rules.size() >= 1, (Supplier<?>) () -> read("pe13/boss.json"));
        t.cmd("/whm test minecraft:lava", "WHMT???", 60);
        t.check("rule hides the damage id in the test", () -> t.chatHas("damage_id"), (Supplier<?>) t::chatText);
        // live preview with --as / --assume (the server's /whm test code)
        t.act(() -> {
            e().togglePreview(true);
            e().setPreviewAs(p.playerName(), "tag:whmt_vet");
            e().runPreview();
        });
        t.waitFor("preview assuming the tag shows the rename", () -> String.join("\n", e().previewLines()).contains("WHMT-PE Lava"), 60);
        shot("rule_preview");
        t.act(() -> {
            e().setPreviewAs(p.playerName(), "");
            e().runPreview();
        });
        t.waitFor("preview without the tag shows the rule name", () -> String.join("\n", e().previewLines()).contains("WHMT???"), 60);
        t.act(() -> e().togglePreview(false));
        t.act(() -> p.say(tagCmd(true)));
        t.sleep(10);
        t.cmd("/whm test minecraft:lava", "WHMT-PE Lava", 60);
        t.act(() -> p.say(tagCmd(false)));
        t.sleep(5);
        // ---- death message
        newEntry("deathmessages");
        type("key", "minecraft:lava");
        type("value", "WHMT-DMPE {player} melted");
        op("death message saved", () -> e().done());
        t.check("death message in the pack", () -> WhmConfig.pack.deathMessages.containsKey("minecraft:lava"), null);
        t.cmd("/whm test minecraft:lava", "WHMT-DMPE", 60);
        // ---- death trigger
        newEntry("triggers");
        type("key", "minecraft:lava");
        type("command", "say WHMT-TRIG {player}");
        t.act(() -> e().setOnce(true));
        op("trigger saved", () -> e().done());
        t.check("trigger in the pack", () -> !WhmConfig.pack.triggers.isEmpty() && read("pe13/boss.json").contains("oncePerPlayer"), null);
        t.cmd("/whm test minecraft:lava", "WHMT-TRIG", 60);
        // ---- template (embedded template editor)
        newEntry("templates");
        t.act(() -> {
            e().field("key").text = "summary";
            e().setTemplateLines(Arrays.asList("&6WHMT-TPL {killing_blow}"));
        });
        op("template saved", () -> e().done());
        t.check("template in the pack", () -> WhmConfig.pack.templates.containsKey("summary") && read("pe13/boss.json").contains("WHMT-TPL"), null);
        t.act(() -> {
            for (PackModel.Ent en : e().entries()) if ("summary".equals(en.key)) e().selectEntry(en);
            e().openTemplateLinesEditor();
        });
        t.sleep(3);
        t.check("template lines open in the template editor", () -> e().child() != null && e().child().lines().get(0).contains("WHMT-TPL"), null);
        shot("template_child");
        t.act(() -> e().child().field(0).text = "&6WHMT-TPL2 {killing_blow}");
        op("template editor Done saves the entry", () -> e().child().done());
        t.check("template changed in the file", () -> read("pe13/boss.json").contains("WHMT-TPL2"), null);
        t.sleep(3);
        // ---- ignored + locked
        newEntry("ignored");
        type("key", "minecraft:cactus");
        op("ignored id saved", () -> e().done());
        t.check("ignored id in the pack", () -> WhmConfig.pack.ignored.contains("minecraft:cactus"), null);
        newEntry("locked");
        type("key", "showTimestamp");
        op("locked feature saved", () -> e().done());
        t.check("locked feature in the pack", () -> WhmConfig.pack.isLocked("showTimestamp"), null);
        t.waitFor("client knows the lock", () -> ClientCore.locked("showTimestamp"), 60);
        shot("locked");
        // ---- move an entry to another category
        tab("hints");
        t.act(() -> {
            e().selectCategory("pe13/boss.json");
            for (PackModel.Ent en : e().entries()) if ("minecraft:lava".equals(en.key)) e().selectEntry(en);
        });
        op("hint moved to pe13/second", () -> e().transferEntry("pe13/second.json"));
        t.check("hint moved in the files", () -> read("pe13/second.json").contains("WHMT-HINT") && !read("pe13/boss.json").contains("WHMT-HINT"), null);
        // ---- category order
        t.act(() -> e().selectCategory("pe13/second.json"));
        op("second moved up", () -> e().moveSelected(-1));
        t.check("load order: second before boss", () -> WhmConfig.pack.files.indexOf("pe13/second.json") >= 0
            && WhmConfig.pack.files.indexOf("pe13/second.json") < WhmConfig.pack.files.indexOf("pe13/boss.json"), (Supplier<?>) () -> WhmConfig.pack.files);
        t.check("order stored in the files, sample comments kept", () -> read("pe13/second.json").contains("\"order\"") && read("hints/_example.json").contains("// Custom tips"),
            (Supplier<?>) () -> read("hints/_example.json"));
        // ---- category off / on
        op("boss switched off", () -> e().setEnabled("pe13/boss.json", false));
        t.check("disabled category not loaded, file kept", () -> !WhmConfig.pack.renameMap.containsKey("minecraft:lava") && new File(pack(), "pe13/boss.json").isFile()
            && read("pe13/boss.json").contains("\"enabled\": false"), null);
        tab("renames");
        shot("tree_disabled");
        op("boss switched on", () -> e().setEnabled("pe13/boss.json", true));
        t.check("enabled again", () -> WhmConfig.pack.renameMap.containsKey("minecraft:lava"), null);
        // ---- memo + undo
        op("category memo", () -> e().setCategoryMemo("pe13/boss.json", "Boss category memo"));
        t.check("memo stored", () -> read("pe13/boss.json").contains("Boss category memo"), null);
        op("undo", () -> e().undo());
        t.check("undo removed the memo", () -> !read("pe13/boss.json").contains("Boss category memo"), null);
        // ---- validation: refused edits
        op("path traversal refused", () -> e().createCategory("", "../evil"), false);
        t.check("nothing written outside", () -> !new File(Platform.whmConfigDir(), "evil.json").exists(), null);
        // ---- server settings
        tab("settings");
        t.act(() -> e().selectSetting("historySize"));
        shot("settings");
        op("setting historySize", () -> e().applySetting("historySize", "61"));
        t.check("setting applied", () -> WhmConfig.common.historySize == 61, null);
        op("setting back", () -> e().applySetting("historySize", "50"));
        op("invalid setting refused", () -> e().applySetting("historySize", "abc"), false);
        // ---- screenshots of every tab
        for (String tb : Arrays.asList("renames", "hints", "rules", "deathmessages", "templates", "triggers", "ignored", "locked")) {
            tab(tb);
            t.act(() -> {
                e().selectCategory("pe13/boss.json");
                List<PackModel.Ent> l = e().entries();
                if (!l.isEmpty()) e().selectEntry(l.get(0));
            });
            shot("tab_" + tb);
        }
        t.act(() -> e().openTreeAction("node.delete"));
        shot("dialog_confirm");
        t.act(() -> e().dialogCancel());
        t.act(() -> e().openTreeAction("node.move"));
        shot("dialog_choice");
        t.act(() -> e().dialogCancel());
        // ---- personal mode
        t.act(() -> e().switchMode(true));
        t.waitFor("personal mode loaded", () -> e().personal() && e().loaded(), 40);
        t.check("personal mode has only renames / hints / ignored", () -> e().tabs().length == 3, null);
        t.act(() -> {
            e().selectTab("renames");
            e().newEntry();
        });
        type("key", "minecraft:cactus");
        type("value", "WHMT-PERSONAL Cactus");
        op("personal rename saved", () -> e().done());
        t.check("personal rename in whathitme-client.cfg", () -> WhmConfig.client.renameMap.containsKey("minecraft:cactus"), (Supplier<?>) () -> WhmConfig.client.renameMap);
        shot("personal");
        op("personal undo", () -> e().undo());
        t.check("personal rename removed by undo", () -> !WhmConfig.client.renameMap.containsKey("minecraft:cactus"), null);
        t.act(() -> e().key(io.github.junjun1z.whathitme.core.client.TemplateEditor.K_ESCAPE, false, false));
        t.waitFor("Escape closes the editor", () -> !p.packEditorOpen(), 40);
        t.act(p::pressConfigPackButton);
        t.waitFor("config screen button opens the pack editor", p::packEditorOpen, 40);
        t.act(p::closeScreen);
        t.sleep(5);
        t.check("editor session closed on the server", () -> PackStore.editors() == 0, null);
        // ---- clean up
        t.act(() -> {
            deleteTree(new File(pack(), "pe13"));
            for (String f : Arrays.asList("hints/_example.json", "renames/_example.json", "ignored/_example.json", "triggers/_example.json", "templates/_example.json",
                "deathmessages/_example.json", "rules/_example.json", "custom/_example.json")) {
                File x = new File(pack(), f);
                try {
                    String s = new String(Files.readAllBytes(x.toPath()), StandardCharsets.UTF_8);
                    String c = io.github.junjun1z.whathitme.core.pack.JsonPatch.set(s, "order", null);
                    Files.write(x.toPath(), c.getBytes(StandardCharsets.UTF_8));
                } catch (Exception ex) {
                    // missing sample
                }
            }
        });
        t.cmd("/whm reload", "reloaded", 60);
        t.check("clean after the pack editor plan", () -> !WhmConfig.pack.renameMap.containsKey("minecraft:lava") && WhmConfig.pack.rules.isEmpty(), null);
    }

    static void deleteTree(File f) {
        File[] kids = f.listFiles();
        if (kids != null) for (File k : kids) deleteTree(k);
        f.delete();
    }
}
