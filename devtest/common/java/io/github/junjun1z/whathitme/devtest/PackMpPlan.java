package io.github.junjun1z.whathitme.devtest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import io.github.junjun1z.whathitme.core.client.ClientCore;
import io.github.junjun1z.whathitme.core.client.PackEditor;
import io.github.junjun1z.whathitme.core.pack.PackClient;
import io.github.junjun1z.whathitme.core.pack.PackModel;

/**
 * Pack editor on a dedicated server (mode mp-pack): the operator edits, the server writes and broadcasts the change;
 * after /deop the client refuses /whm pack and the server refuses edits sent anyway.
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

    void plan() {
        t.waitFor("connected to the server", p::connected, 2400);
        t.waitFor("server has the mod", () -> ClientCore.serverHasMod, 400);
        t.waitFor("operator", () -> ClientCore.port.hasOpPermission(), 400);
        t.act(() -> p.command("/whm pack"));
        t.waitFor("editor open (operator, dedicated server)", () -> p.packEditorOpen() && e() != null && e().loaded() && !e().personal(), 200);
        boolean adv = io.github.junjun1z.whmdriver.VanillaCommands.of(p.mcVersion()).atLeast(1, 12, 0);
        t.check("server lists: damage ids, advancements (1.12+)", () -> !e().model().damage.isEmpty() && (!adv || !e().model().conds.get("advancement").isEmpty()), null);
        op("create category pemp/mp", () -> e().createCategory("pemp", "mp"));
        t.act(() -> {
            e().selectTab("renames");
            e().selectCategory("pemp/mp.json");
            e().newEntry();
            e().field("key").text = "minecraft:lava";
            e().field("value").text = "WHMT-MP Lava";
        });
        op("rename saved on the server", () -> e().done());
        t.check("category in the server state", () -> {
            PackModel.Cat c = e().model().cat("pemp/mp.json");
            return c != null && PackModel.count(c, "renameMap") == 1;
        }, null);
        t.waitFor("server broadcast the new pack to the clients", () -> ClientCore.serverInfo != null
            && ClientCore.serverInfo.packRenames.contains("minecraft:lava = WHMT-MP Lava"), 200);
        t.act(p::closeScreen);
        t.cmd("/whm test minecraft:lava", "WHMT-MP Lava", 100);
        t.screenshot("whm_pack_mp");
        // without permission
        t.act(() -> p.say("/deop " + p.playerName()));
        if (ClientCore.port.knowsOpLevel()) {
            t.waitFor("no longer operator", () -> !ClientCore.port.hasOpPermission(), 400);
        } else {
            // 1.7.10 / 1.8: the client is not told, the server answers the open request
            t.sleep(40);
        }
        t.clearChat();
        t.act(() -> p.command("/whm pack"));
        t.waitFor("client refuses /whm pack without permission", () -> t.chatHas("Only operators"), 100);
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
        t.act(() -> p.command("/whm pack personal"));
        t.waitFor("personal mode still opens", () -> p.packEditorOpen() && e() != null && e().personal(), 100);
        t.act(p::closeScreen);
        t.sleep(10);
        t.act(p::pressConfigPackButton);
        t.waitFor("config button opens the own lists without permission", () -> p.packEditorOpen() && e() != null && e().personal() && e().loaded(), 200);
        t.act(p::closeScreen);
        t.sleep(20);
    }
}
