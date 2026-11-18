package io.github.junjun1z.whathitme.devtest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import io.github.junjun1z.whathitme.core.client.ClientCore;
import io.github.junjun1z.whathitme.core.client.PackEditor;
import io.github.junjun1z.whathitme.core.pack.PackClient;
import io.github.junjun1z.whathitme.core.pack.PackModel;

/**
 * Pack editor on a dedicated server with two modded clients. a (operator): edits, sees b's change as a notice.
 * b (Alex, not op): refused by the client and by the server, then made op by a, sees a's edit applied and notified,
 * edits the same category.
 */
final class PackMpPlan {

    private final SelfTest t;
    private final DevPort p;
    private int before;
    private volatile String denied = "";

    PackMpPlan(SelfTest t) {
        this.t = t;
        this.p = t.port;
    }

    private static PackEditor e() {
        return PackEditor.current;
    }

    private void op(String name, Runnable action) {
        t.act(() -> {
            before = e() == null ? 0 : e().results();
            action.run();
        });
        t.waitFor(name + ": server answered", () -> e() != null && e().results() > before, 200);
        t.check(name + ": ok", () -> e().lastResult().startsWith("ok"), (Supplier<?>) () -> e().lastResult() + " / " + e().status());
        t.sleep(5);
    }

    void plan(boolean a) {
        t.waitFor("connected to the server", p::connected, 2400);
        t.waitFor("server has the mod", () -> ClientCore.serverHasMod, 400);
        if (a) planA();
        else planB();
    }

    private void planA() {
        t.waitFor("second player Alex online", () -> p.inPlayerList("Alex"), 6000);
        t.waitFor("Alex was refused (chat WHMT-B-READY)", () -> t.chatHas("WHMT-B-READY"), 3000);
        t.act(() -> p.say("/op Alex"));
        t.sleep(40);
        t.act(() -> p.command("/whm pack"));
        t.waitFor("editor open (operator)", () -> p.packEditorOpen() && e() != null && e().loaded() && !e().personal(), 200);
        t.waitFor("Alex has the editor open too", () -> e().model().editors.contains("Alex"), 3000);
        op("create category pemp/mp", () -> e().createCategory("pemp", "mp"));
        t.act(() -> {
            e().selectTab("renames");
            e().selectCategory("pemp/mp.json");
            e().newEntry();
            e().field("key").text = "minecraft:lava";
            e().field("value").text = "WHMT-MP Lava";
        });
        op("rename saved on the server", () -> e().done());
        t.act(() -> p.say("WHMT-A-SAVED"));
        t.waitFor("notice: Alex changed the category", () -> e().lastNotice().startsWith("Alex") && e().lastNotice().contains("pemp/mp.json"), 3000);
        t.waitFor("Alex's hint arrived in the state", () -> {
            PackModel.Cat c = e().model().cat("pemp/mp.json");
            return c != null && PackModel.count(c, "hintMap") == 1;
        }, 200);
        t.screenshot("whm_pack_mp_notice");
        t.waitFor("Alex finished (chat WHMT-B-DONE)", () -> t.chatHas("WHMT-B-DONE"), 3000);
        op("clean up: delete category", () -> {
            e().selectCategory("pemp/mp.json");
            e().deleteSelected();
        });
        op("clean up: delete folder", () -> {
            e().selectNode("dir:pemp");
            e().deleteSelected();
        });
        t.act(() -> p.say("/deop Alex"));
        t.act(p::closeScreen);
        t.sleep(20);
    }

    private void planB() {
        t.check("Alex is not an operator", () -> !ClientCore.port.hasOpPermission(), null);
        t.clearChat();
        t.act(() -> p.command("/whm pack"));
        t.waitFor("client refuses /whm pack without permission", () -> t.chatHas("Only operators"), 100);
        t.check("no editor opened", () -> !p.packEditorOpen(), null);
        // a modified client could still send edits: the server must refuse them
        t.act(() -> {
            PackClient c = new PackClient();
            c.listen(m -> {
                if ("denied".equals(m.get("type"))) denied = "denied";
            });
            Map<String, Object> op = new LinkedHashMap<>();
            op.put("op", "cat.create");
            op.put("path", "hacked.json");
            c.send(op);
        });
        t.waitFor("server refuses an edit from a non-operator", () -> "denied".equals(denied), 200);
        t.act(() -> p.say("WHMT-B-READY"));
        t.waitFor("made operator by the other player", () -> ClientCore.port.hasOpPermission(), 3000);
        t.act(() -> p.command("/whm pack"));
        t.waitFor("editor open after /op", () -> p.packEditorOpen() && e() != null && e().loaded() && !e().personal(), 200);
        t.waitFor("the operator's save arrived (chat WHMT-A-SAVED)", () -> t.chatHas("WHMT-A-SAVED"), 3000);
        t.waitFor("rename applied for this player (server info)", () -> ClientCore.serverInfo != null && ClientCore.serverInfo.packRenames.contains("minecraft:lava = WHMT-MP Lava"), 200);
        t.check("notice names the other editor and the file", () -> e().lastNotice().startsWith("_junjun1z") || e().lastNotice().contains("pemp/mp.json"),
            (Supplier<?>) () -> e().lastNotice());
        t.screenshot("whm_pack_mp_b");
        t.cmd("/whm test minecraft:lava", "WHMT-MP Lava", 100);
        t.act(() -> {
            e().selectTab("hints");
            e().selectCategory("pemp/mp.json");
            e().newEntry();
            e().field("key").text = "minecraft:lava";
            e().field("value").text = "WHMT-MP hint";
        });
        op("hint saved by the second editor", () -> e().done());
        t.sleep(40);
        t.act(p::closeScreen);
        t.act(() -> p.say("WHMT-B-DONE"));
        t.sleep(100);
    }
}
