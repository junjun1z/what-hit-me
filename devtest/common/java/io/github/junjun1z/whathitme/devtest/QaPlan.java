package io.github.junjun1z.whathitme.devtest;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Supplier;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.client.ClientCore;
import io.github.junjun1z.whathitme.core.client.PackEditor;
import io.github.junjun1z.whathitme.devtest.DevPort.Act;

/**
 * QA script runner (mode "qa"): runs config/whathitme/qa.txt line by line. Lines: world NAME | connect | setup | mobs |
 * act ACT [amount] | cmd TEXT | say TEXT | wait TICKS | clear | expect TEXT | absent TEXT | count TEXT N | log | shot NAME |
 * respawn | dead | write REL|TEXT (\n = new line, path below the game dir) | delete REL | pack | packclose | pestatus |
 * info TEXT | serverinfo LIST N. Lines starting with # are comments.
 */
final class QaPlan {

    private final SelfTest t;
    private final DevPort p;

    QaPlan(SelfTest t) {
        this.t = t;
        this.p = t.port;
    }

    void plan() {
        List<String> lines;
        try {
            lines = Files.readAllLines(new File(Platform.whmConfigDir(), "qa.txt").toPath(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            t.ok("qa.txt readable", false, e);
            return;
        }
        for (String raw : lines) {
            String l = raw.trim();
            if (l.isEmpty() || l.startsWith("#")) continue;
            int sp = l.indexOf(' ');
            String c = sp < 0 ? l : l.substring(0, sp);
            String a = sp < 0 ? "" : l.substring(sp + 1);
            step(c, a);
        }
        t.act(() -> t.info("final chat:\n" + t.chatText()));
    }

    private static List<String> list(String name) {
        io.github.junjun1z.whathitme.core.net.Packets.ServerInfo i = ClientCore.serverInfo;
        if (i == null) return null;
        switch (name) {
            case "renames": return i.renames;
            case "hints": return i.hints;
            case "ignored": return i.ignored;
            case "packRenames": return i.packRenames;
            case "packHints": return i.packHints;
            case "templates": return i.templates;
            case "deathMessages": return i.deathMessages;
            default: return null;
        }
    }

    private static int infoList(String name) {
        List<String> l = list(name);
        return l == null ? -1 : l.size();
    }

    private static String lastOf(String name) {
        List<String> l = list(name);
        String s = l.get(l.size() - 1);
        return s.length() > 60 ? s.substring(0, 60) : s;
    }

    private static int count(String text, String s) {
        int n = 0;
        for (int i = text.indexOf(s); i >= 0; i = text.indexOf(s, i + 1)) n++;
        return n;
    }

    private void step(String c, String a) {
        switch (c) {
            case "world":
                t.loadWorld(a);
                t.waitFor("server info received", () -> ClientCore.serverHasMod, 400);
                break;
            case "connect":
                t.waitFor("connected", p::connected, 2400);
                t.sleep(60);
                break;
            case "setup":
                t.srv(Act.SETUP);
                break;
            case "mobs":
                t.srv(Act.SPAWN_MOBS);
                t.sleep(60);
                break;
            case "act": {
                String[] x = a.split(" ");
                Act act = Act.valueOf(x[0]);
                float amount = x.length > 1 ? Float.parseFloat(x[1]) : 0f;
                t.srv(act, amount);
                break;
            }
            case "cmd":
                t.act(() -> p.command(a));
                break;
            case "say":
                t.act(() -> p.say(a));
                break;
            case "wait":
                t.sleep(Integer.parseInt(a));
                break;
            case "clear":
                t.clearChat();
                break;
            case "expect":
                t.waitFor("chat has \"" + a + "\"", () -> t.chatHas(a), 100);
                break;
            case "absent":
                t.check("chat lacks \"" + a + "\"", () -> !t.chatHas(a), (Supplier<?>) t::chatText);
                break;
            case "count": {
                int k = a.lastIndexOf(' ');
                String s = a.substring(0, k);
                int n = Integer.parseInt(a.substring(k + 1));
                t.check("chat has \"" + s + "\" " + n + "x", () -> count(t.chatText(), s) == n, (Supplier<?>) () -> count(t.chatText(), s) + "x: " + t.chatText());
                break;
            }
            case "log":
                t.act(() -> t.info("chat:\n" + t.chatText()));
                break;
            case "shot":
                t.sleep(4);
                t.screenshot("whm_qa_" + a);
                break;
            case "dead":
                t.waitFor("death screen", p::deathScreen, 200);
                break;
            case "respawn":
                t.waitFor("death screen", p::deathScreen, 200);
                t.act(p::respawn);
                t.waitFor("respawned", () -> !p.deathScreen() && p.health() > 0, 200);
                break;
            case "write": {
                int bar = a.indexOf('|');
                String rel = a.substring(0, bar);
                String text = a.substring(bar + 1).replace("\\n", "\n");
                t.act(() -> {
                    try {
                        File f = new File(p.gameDir(), rel);
                        f.getParentFile().mkdirs();
                        Files.write(f.toPath(), text.getBytes(StandardCharsets.UTF_8));
                    } catch (Exception e) {
                        t.ok("write " + rel, false, e);
                    }
                });
                break;
            }
            case "delete":
                t.act(() -> new File(p.gameDir(), a).delete());
                break;
            case "pack":
                t.act(() -> p.command("/whm pack" + (a.isEmpty() ? "" : " " + a)));
                t.waitFor("pack editor open", p::packEditorOpen, 100);
                t.waitFor("pack editor loaded", () -> PackEditor.current != null && PackEditor.current.loaded(), 100);
                break;
            case "packclose":
                t.act(p::closeScreen);
                t.sleep(5);
                break;
            case "pestatus":
                t.act(() -> {
                    PackEditor e = PackEditor.current;
                    t.info("editor open=" + p.packEditorOpen() + (e == null ? "" : " status=" + e.status() + " last=" + e.lastResult()));
                });
                break;
            case "pecat":
                t.waitFor("editor shows category " + a, () -> PackEditor.current != null && PackEditor.current.model().cat(a) != null, 60);
                break;
            case "info":
                t.act(() -> t.info(a));
                break;
            case "serverinfo": {
                // serverinfo LIST N: the server info list (renames, hints, packRenames, packHints, ...) has N entries
                String[] x = a.split(" ");
                int n = Integer.parseInt(x[1]);
                t.waitFor("server info " + x[0] + " has " + n, () -> infoList(x[0]) == n, 200);
                t.act(() -> t.info("server info " + x[0] + ": " + infoList(x[0]) + (infoList(x[0]) > 0 ? ", last " + lastOf(x[0]) : "")));
                break;
            }
            default:
                t.ok("unknown qa line " + c, false, a);
        }
    }
}
