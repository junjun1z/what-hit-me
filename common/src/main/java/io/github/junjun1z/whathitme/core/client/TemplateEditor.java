package io.github.junjun1z.whathitme.core.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.junjun1z.whathitme.core.Safe;
import io.github.junjun1z.whathitme.core.cmd.TemplatePreview;
import io.github.junjun1z.whathitme.core.config.Templates;
import io.github.junjun1z.whathitme.core.config.WhmConfig;
import io.github.junjun1z.whathitme.core.data.DamageRecord;
import io.github.junjun1z.whathitme.core.data.Snapshot;
import io.github.junjun1z.whathitme.core.net.Packets;
import io.github.junjun1z.whathitme.core.text.DeathMessages;
import io.github.junjun1z.whathitme.core.text.DisplayOptions;
import io.github.junjun1z.whathitme.core.text.Fmt;
import io.github.junjun1z.whathitme.core.text.Lang;
import io.github.junjun1z.whathitme.core.text.Msg;
import io.github.junjun1z.whathitme.core.text.RecapFormatter;
import io.github.junjun1z.whathitme.core.text.Template;
import io.github.junjun1z.whathitme.core.text.TemplateVars;
import io.github.junjun1z.whathitme.core.text.Txt;

/**
 * The in-game template editor (/whm template): output tabs, the template as single-line fields, a live preview drawn by
 * the real formatter / panel code, a clickable variable list, Save / Save to pack / Reset / Cancel. Everything is drawn
 * with {@link Gfx}; a version only needs a screen that forwards drawing, mouse, keys and typed characters and closes
 * itself when {@link #wantsClose()} is true.
 */
public final class TemplateEditor {

    // key codes the screen host maps its own keys to
    public static final int K_BACKSPACE = 1;
    public static final int K_DELETE = 2;
    public static final int K_LEFT = 3;
    public static final int K_RIGHT = 4;
    public static final int K_UP = 5;
    public static final int K_DOWN = 6;
    public static final int K_HOME = 7;
    public static final int K_END = 8;
    public static final int K_ENTER = 9;
    public static final int K_TAB = 10;
    public static final int K_ESCAPE = 11;
    public static final int K_A = 20;
    public static final int K_C = 21;
    public static final int K_V = 22;
    public static final int K_X = 23;

    private static final int M = 6;
    private static final int ROW = 14;
    private static final int VAR_W = 118;
    private static final int VAR_ROW = 10;

    /** The open editor (self-test), null when none */
    public static volatile TemplateEditor current;

    private final List<String> outputs = new ArrayList<>();
    private final boolean canPack;
    private int out;
    private final List<TextField> fields = new ArrayList<>();
    private int focus;
    private int lineScroll;
    private int varScroll;
    private boolean sample;
    private String status = "";
    private int statusColor = 0x55FF55;
    private boolean close;
    /** Where the shown lines came from: personal, pack or example */
    private String source = "";
    private boolean lockedNow;
    private long opened = System.currentTimeMillis();

    /** Clickable areas of the last frame: id -> {x0, y0, x1, y1} */
    private final Map<String, int[]> areas = new LinkedHashMap<>();
    /** Plain text of the preview of the last frame (self-test) */
    private final List<String> previewText = new ArrayList<>();
    private int lastW = 400;
    private int lastH = 240;
    private Gfx lastG;

    public TemplateEditor() {
        canPack = Safe.get("permission", () -> ClientCore.port.isSingleplayer() && ClientCore.op(), false);
        for (String o : Template.OUTPUTS) outputs.add(o);
        if (canPack) outputs.add(Template.DEATH);
        Snapshot real = ClientCore.latestDeath();
        sample = real == null || real.key() == null;
        load(0);
        current = this;
    }

    private static String tr(String key, Object... args) {
        return Msg.LOCAL.text(key, args);
    }

    public String output() {
        return outputs.get(out);
    }

    public boolean wantsClose() {
        return close;
    }

    /** The screen was closed by the host */
    public void closed() {
        if (current == this) current = null;
    }

    public boolean canSaveToPack() {
        return canPack;
    }

    public boolean isLocked() {
        return lockedNow;
    }

    public String source() {
        return source;
    }

    public List<String> lines() {
        List<String> l = new ArrayList<>();
        for (TextField f : fields) l.add(f.text);
        while (!l.isEmpty() && l.get(l.size() - 1).trim().isEmpty()) l.remove(l.size() - 1);
        return l;
    }

    public List<String> previewText() {
        return new ArrayList<>(previewText);
    }

    /** Area of a clickable element of the last frame (button ids: save, pack, reset, cancel, sample, add, tab:&lt;output&gt;, var:&lt;name&gt;, field:&lt;i&gt;) */
    public int[] area(String id) {
        return areas.get(id);
    }

    public void select(String output) {
        int i = outputs.indexOf(output);
        if (i >= 0) load(i);
    }

    private void load(int idx) {
        out = idx;
        String o = output();
        List<String> lines;
        lockedNow = !Template.DEATH.equals(o) && Templates.locked(o, ClientCore::locked);
        if (Template.DEATH.equals(o)) {
            lines = new ArrayList<>();
            for (Map.Entry<String, String> e : ClientCore.packDeathMessages().entrySet()) lines.add(e.getKey() + " = " + e.getValue());
            source = lines.isEmpty() ? "example" : "pack";
            if (lines.isEmpty()) lines.addAll(Templates.EXAMPLES.get(o));
        } else {
            Template personal = lockedNow ? null : WhmConfig.client.templates.get(o);
            Template pack = ClientCore.packTemplates().get(o);
            if (personal != null) {
                lines = personal.lines;
                source = "personal";
            } else if (pack != null) {
                lines = pack.lines;
                source = "pack";
            } else {
                lines = Templates.EXAMPLES.get(o);
                source = "example";
            }
        }
        fields.clear();
        for (String l : lines) fields.add(new TextField(l));
        if (fields.isEmpty()) fields.add(new TextField(""));
        for (TextField f : fields) f.editable = !lockedNow;
        focus = 0;
        lineScroll = 0;
    }

    private void flash(String key, boolean ok, Object... args) {
        status = tr(key, args);
        statusColor = ok ? 0x55FF55 : 0xFF5555;
    }

    // ------------------------------------------------------------------ actions

    public void save() {
        if (lockedNow || Template.DEATH.equals(output())) return;
        try {
            Templates.savePersonal(output(), lines());
            WhmConfig.reloadClient();
            source = "personal";
            flash("whathitme.editor.saved", true);
        } catch (Exception e) {
            flash("whathitme.template.savefailed", false, String.valueOf(e.getMessage()));
        }
    }

    public void saveToPack() {
        if (!canPack) return;
        try {
            Templates.savePack(output(), lines());
            WhmConfig.reloadCommon();
            if (ClientCore.serverHasMod) ClientCore.port.send(new Packets.Request(Packets.Request.RELOAD, ""));
            flash("whathitme.editor.savedpack", true, Templates.packFile(output()).getName());
        } catch (Exception e) {
            flash("whathitme.template.savefailed", false, String.valueOf(e.getMessage()));
        }
    }

    public void reset() {
        if (lockedNow || Template.DEATH.equals(output())) return;
        try {
            Templates.savePersonal(output(), null);
            WhmConfig.reloadClient();
            load(out);
            flash("whathitme.template.resetdone", true);
        } catch (Exception e) {
            flash("whathitme.template.savefailed", false, String.valueOf(e.getMessage()));
        }
    }

    public void toggleSample() {
        Snapshot real = ClientCore.latestDeath();
        sample = !sample || real == null || real.key() == null;
    }

    public boolean usesSample() {
        return sample;
    }

    /** Inserts {name} at the cursor of the focused line */
    public void insertVariable(String name) {
        if (lockedNow || fields.isEmpty()) return;
        fields.get(Math.min(focus, fields.size() - 1)).insert("{" + name + "}");
    }

    public void focus(int line) {
        if (line >= 0 && line < fields.size()) focus = line;
        ensureVisible();
    }

    public TextField field(int i) {
        return fields.get(i);
    }

    public int lineCount() {
        return fields.size();
    }

    private void addLine(int after) {
        if (lockedNow) return;
        fields.add(after + 1, new TextField(""));
        focus = after + 1;
        ensureVisible();
    }

    private void removeLine(int i) {
        if (lockedNow) return;
        fields.remove(i);
        if (fields.isEmpty()) fields.add(new TextField(""));
        focus = Math.min(focus, fields.size() - 1);
    }

    private void moveLine(int i, int d) {
        if (lockedNow) return;
        int j = i + d;
        if (j < 0 || j >= fields.size()) return;
        TextField t = fields.remove(i);
        fields.add(j, t);
        focus = j;
        ensureVisible();
    }

    private int visibleRows() {
        return Math.max(3, Math.min(14, (int) (lastH * 0.42f - 24) / ROW));
    }

    private void ensureVisible() {
        int rows = visibleRows();
        if (focus < lineScroll) lineScroll = focus;
        if (focus >= lineScroll + rows) lineScroll = focus - rows + 1;
        lineScroll = Math.max(0, Math.min(lineScroll, Math.max(0, fields.size() - rows)));
    }

    // ------------------------------------------------------------------ input

    public boolean key(int key, boolean ctrl, boolean shift) {
        return Safe.get("template editor", () -> keyImpl(key, ctrl, shift), false);
    }

    private boolean keyImpl(int key, boolean ctrl, boolean shift) {
        switch (key) {
            case K_ESCAPE:
                close = true;
                return true;
            case K_UP:
                focus(Math.max(0, focus - 1));
                return true;
            case K_DOWN:
            case K_TAB:
                focus(Math.min(fields.size() - 1, focus + 1));
                return true;
            case K_ENTER:
                addLine(focus);
                return true;
            default:
                return !fields.isEmpty() && fields.get(focus).key(key, ctrl, shift);
        }
    }

    public boolean typed(char c) {
        return Safe.get("template editor", () -> {
            if (fields.isEmpty() || lockedNow) return false;
            fields.get(focus).typed(c);
            return true;
        }, false);
    }

    private static boolean in(int[] a, double x, double y) {
        return a != null && x >= a[0] && x < a[2] && y >= a[1] && y < a[3];
    }

    public boolean mouseDown(int button, double mx, double my, boolean shift) {
        return Safe.get("template editor", () -> mouseImpl(button, mx, my, shift), false);
    }

    private boolean mouseImpl(int button, double mx, double my, boolean shift) {
        if (button != 0) return false;
        for (Map.Entry<String, int[]> e : new ArrayList<>(areas.entrySet())) {
            if (!in(e.getValue(), mx, my) || e.getKey().equals("varlist")) continue;
            String id = e.getKey();
            if (id.startsWith("tab:")) select(id.substring(4));
            else if (id.startsWith("var:")) insertVariable(id.substring(4));
            else if (id.startsWith("up:")) moveLine(Integer.parseInt(id.substring(3)), -1);
            else if (id.startsWith("down:")) moveLine(Integer.parseInt(id.substring(5)), 1);
            else if (id.startsWith("del:")) removeLine(Integer.parseInt(id.substring(4)));
            else if (id.startsWith("field:")) {
                int i = Integer.parseInt(id.substring(6));
                focus = i;
                if (lastG != null) fields.get(i).click(lastG, (int) mx - e.getValue()[0] - 3, shift);
            } else if (id.equals("add")) addLine(fields.size() - 1);
            else if (id.equals("save")) save();
            else if (id.equals("pack")) saveToPack();
            else if (id.equals("reset")) reset();
            else if (id.equals("sample")) toggleSample();
            else if (id.equals("cancel")) close = true;
            return true;
        }
        return false;
    }

    public boolean scroll(double mx, double my, double delta) {
        int d = delta > 0 ? -1 : 1;
        int[] vars = areas.get("varlist");
        if (in(vars, mx, my)) {
            varScroll = Math.max(0, Math.min(TemplateVars.KNOWN.size() - 1, varScroll + d));
        } else {
            lineScroll = Math.max(0, Math.min(Math.max(0, fields.size() - visibleRows()), lineScroll + d));
        }
        return true;
    }

    // ------------------------------------------------------------------ drawing

    public void render(Gfx g, int w, int h, int mx, int my) {
        try {
            renderImpl(g, w, h, mx, my);
        } catch (Throwable t) {
            Safe.warn("template editor", t);
        }
    }

    private boolean button(Gfx g, String id, String label, int x, int y, int bw, boolean enabled, int mx, int my) {
        int[] a = {x, y, x + bw, y + 13};
        boolean over = enabled && in(a, mx, my);
        g.fill(x, y, x + bw, y + 13, enabled ? (over ? 0xFFA0A0A0 : 0xFF707070) : 0xFF404040);
        g.fill(x + 1, y + 1, x + bw - 1, y + 12, enabled ? (over ? 0xFF5A5A8C : 0xFF3A3A4A) : 0xFF262626);
        g.text(label, x + (bw - g.width(label)) / 2, y + 3, enabled ? (over ? 0xFFFFA0 : 0xFFFFFF) : 0x707070);
        if (enabled) areas.put(id, a);
        return over;
    }

    private void renderImpl(Gfx g, int w, int h, int mx, int my) {
        lastG = g;
        lastW = w;
        lastH = h;
        areas.clear();
        g.fill(0, 0, w, h, 0xC0101018);
        boolean blink = ((System.currentTimeMillis() - opened) / 500) % 2 == 0;
        // ---- tabs
        int x = M;
        for (int i = 0; i < outputs.size(); i++) {
            String label = tr("whathitme.editor.out." + outputs.get(i));
            int bw = g.width(label) + 10;
            int[] a = {x, 4, x + bw, 17};
            boolean sel = i == out;
            g.fill(x, 4, x + bw, 17, sel ? 0xFFC0A040 : 0xFF505050);
            g.fill(x + 1, 5, x + bw - 1, 17, sel ? 0xFF403010 : 0xFF202020);
            g.text(label, x + 5, 7, sel ? 0xFFFF55 : 0xCCCCCC);
            areas.put("tab:" + outputs.get(i), a);
            x += bw + 2;
        }
        String title = tr("whathitme.editor.title");
        if (x + g.width(title) + M < w) g.text(Fmt.GOLD + title, w - M - g.width(title), 7, 0xFFFFFF);
        // ---- lines
        int top = 22;
        int rows = visibleRows();
        int varX = w - VAR_W - M;
        int listRight = varX - M;
        int bw = 12;
        int fieldW = Math.max(60, listRight - M - 3 * (bw + 1) - 18);
        ensureVisible();
        for (int r = 0; r < rows; r++) {
            int i = lineScroll + r;
            if (i >= fields.size()) break;
            int y = top + r * ROW;
            g.text(Fmt.DARK_GRAY + String.valueOf(i + 1), M, y + 3, 0xFFFFFF);
            int fx = M + 16;
            fields.get(i).draw(g, fx, y, fieldW, ROW - 1, i == focus, blink, TemplateVars::isKnown);
            areas.put("field:" + i, new int[] {fx, y, fx + fieldW, y + ROW - 1});
            int bx = fx + fieldW + 2;
            button(g, "up:" + i, "↑", bx, y, bw, !lockedNow && i > 0, mx, my);
            button(g, "down:" + i, "↓", bx + bw + 1, y, bw, !lockedNow && i < fields.size() - 1, mx, my);
            button(g, "del:" + i, "✕", bx + 2 * (bw + 1), y, bw, !lockedNow, mx, my);
        }
        int listBottom = top + rows * ROW;
        button(g, "add", tr("whathitme.editor.addline"), M + 16, listBottom, 70, !lockedNow, mx, my);
        String info = lockedNow ? Fmt.RED + tr("whathitme.editor.locked") : Fmt.GRAY + tr("whathitme.editor.source." + source);
        g.text(LegacyText.trim(g, info, listRight - M - 92), M + 92, listBottom + 3, 0xFFFFFF);
        // ---- variables
        int varTop = top;
        int varBottom = listBottom + 13;
        g.fill(varX, varTop, varX + VAR_W, varBottom, 0x80000000);
        areas.put("varlist", new int[] {varX, varTop, varX + VAR_W, varBottom});
        List<String> names = new ArrayList<>(TemplateVars.KNOWN.keySet());
        int vy = varTop + 2;
        String hoveredVar = null;
        for (int i = varScroll; i < names.size() && vy + VAR_ROW <= varBottom; i++) {
            String n = names.get(i);
            int[] a = {varX, vy, varX + VAR_W, vy + VAR_ROW};
            boolean over = in(a, mx, my);
            if (over) {
                g.fill(a[0], a[1], a[2], a[3], 0x40FFFFFF);
                hoveredVar = n;
            }
            g.text("{" + n + "}", varX + 3, vy + 1, over ? 0xFFFF55 : 0x55FFFF);
            if (!lockedNow) areas.put("var:" + n, a);
            vy += VAR_ROW;
        }
        // ---- preview
        int py = varBottom + 6;
        String sampleLabel = sample ? tr("whathitme.editor.preview.sample") : tr("whathitme.editor.preview.real");
        g.text(Fmt.GOLD + tr("whathitme.editor.preview") + Fmt.GRAY + " (" + sampleLabel + ")", M, py, 0xFFFFFF);
        int areaTop = py + 12;
        int areaBottom = h - 22;
        drawPreview(g, M, areaTop, w - 2 * M, Math.max(20, areaBottom - areaTop), mx, my, w, h);
        // ---- bottom buttons
        int by = h - 18;
        int b = M;
        boolean death = Template.DEATH.equals(output());
        button(g, "save", tr("whathitme.editor.save"), b, by, 60, !lockedNow && !death, mx, my);
        b += 62;
        button(g, "pack", tr("whathitme.editor.savepack"), b, by, 80, canPack, mx, my);
        b += 82;
        button(g, "reset", tr("whathitme.editor.reset"), b, by, 70, !lockedNow && !death, mx, my);
        b += 72;
        button(g, "sample", tr(sample ? "whathitme.editor.usereal" : "whathitme.editor.usesample"), b, by, 80, true, mx, my);
        b += 82;
        button(g, "cancel", tr("whathitme.editor.cancel"), b, by, 60, true, mx, my);
        b += 64;
        if (!status.isEmpty()) g.text(LegacyText.trim(g, status, Math.max(0, w - b - M)), b, by + 3, statusColor);
        if (hoveredVar != null) {
            List<String> tip = new ArrayList<>();
            tip.add(Fmt.AQUA + "{" + hoveredVar + "}");
            String key = "whathitme.var." + hoveredVar;
            tip.add(Msg.LOCAL.has(key) ? tr(key) : TemplateVars.KNOWN.get(hoveredVar));
            g.tooltip(tip, mx, my, w, h);
        }
    }

    private Snapshot previewData() {
        Snapshot real = ClientCore.latestDeath();
        if (sample || real == null || real.key() == null) return TemplatePreview.sample(System.currentTimeMillis());
        return real;
    }

    private void drawPreview(Gfx g, int x, int y, int pw, int ph, int mx, int my, int sw, int sh) {
        previewText.clear();
        Lang lang = Msg.LOCAL.lang();
        String o = output();
        Snapshot s = previewData();
        DisplayOptions base = ClientCore.options();
        g.fill(x, y, x + pw, y + ph, 0x40000000);
        if (Template.DEATH.equals(o)) {
            RecapFormatter f = new RecapFormatter(Msg.LOCAL, base);
            String line = fields.get(focus).text;
            int eq = line.indexOf('=');
            DamageRecord k = s.key();
            List<String> out = new ArrayList<>();
            if (eq > 0 && k != null) {
                Txt t = DeathMessages.render(f, line.substring(eq + 1).trim(), k, base.player.isEmpty() ? "Steve" : base.player, s.deathMessage);
                out.add(t.toLegacy(lang));
                Map<String, String> one = new LinkedHashMap<>();
                one.put(line.substring(0, eq).trim(), "x");
                boolean match = DeathMessages.find(one, k) != null;
                out.add(Fmt.GRAY + tr(match ? "whathitme.editor.dm.match" : "whathitme.editor.dm.nomatch", k.damageId));
            } else {
                out.add(Fmt.GRAY + tr("whathitme.editor.dm.format"));
            }
            chatBox(g, out, x, y, pw, ph);
            return;
        }
        RecapFormatter f = new RecapFormatter(Msg.LOCAL, base.withTemplate(o, Template.of(lines())));
        DamageRecord k = s.key();
        switch (o) {
            case Template.HOVER: {
                List<String> tip = new ArrayList<>();
                if (k != null) for (Txt t : f.hoverLines(k)) tip.add(t.toLegacy(lang));
                for (String t : tip) previewText.add(Fmt.strip(t));
                // drawn like the death screen tooltip, at the top left of the preview area
                g.push();
                g.tooltip(tip, x - 12 + 4, y + 12 + 4, sw, sh);
                g.pop();
                break;
            }
            case Template.LINE: {
                List<DamageRecord> recs = f.visible(s.records, 20);
                for (DamageRecord r : recs) {
                    for (String row : DeathPanel.rowsFor(g, f, r, s.time, lang, DeathPanel.width() - 12)) previewText.add(Fmt.strip(row));
                }
                DeathPanel.drawPreview(g, f, recs, s.time, x + 2, y + 2, Math.max(1, (ph - 10) / 10), mx, my, sw, sh);
                break;
            }
            case Template.SUMMARY:
            case Template.ALERT: {
                List<Txt> lines = Template.SUMMARY.equals(o) ? f.respawnSummary(s)
                    : java.util.Collections.singletonList(f.alert(new Snapshot(Snapshot.TOTEM, s.time, s.records, s.keyIndex, "", s.gear), 1));
                List<String> legacy = new ArrayList<>();
                for (Txt t : lines) for (String part : t.toLegacy(lang).split("\n")) legacy.add(part);
                chatBox(g, legacy, x, y, pw, ph);
                break;
            }
            default: {
                List<String> plain = new ArrayList<>();
                for (String l : f.copyText(s, s.time, lang).split("\n")) plain.add(l);
                int cy = y + 3;
                for (String l : plain) {
                    previewText.add(l);
                    for (String part : LegacyText.wrap(g, l, pw - 8)) {
                        if (cy + 9 > y + ph) return;
                        g.text(part, x + 4, cy, 0xDDDDDD);
                        cy += 9;
                    }
                }
            }
        }
    }

    private void chatBox(Gfx g, List<String> lines, int x, int y, int pw, int ph) {
        int width = Math.min(320, pw - 4);
        int cy = y + 2;
        for (String l : lines) {
            previewText.add(Fmt.strip(l));
            for (String part : LegacyText.wrap(g, l, width)) {
                if (cy + 9 > y + ph) return;
                g.fill(x + 2, cy - 1, x + 2 + width + 4, cy + 8, 0x80000000);
                g.text(part, x + 4, cy, 0xFFFFFF);
                cy += 9;
            }
        }
    }
}
