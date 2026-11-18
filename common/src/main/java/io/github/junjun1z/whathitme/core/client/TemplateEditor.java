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
public final class TemplateEditor implements CoreScreen {

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

    /** The variable list: "#group" headers followed by their variables */
    private static final List<String> VAR_ITEMS = new ArrayList<>();

    static {
        String[] groups = {"damage", "type", "attacker", "context", "other"};
        for (String g : groups) {
            VAR_ITEMS.add("#" + g);
            for (Map.Entry<String, String> e : TemplateVars.KNOWN.entrySet()) {
                String vg = e.getValue();
                boolean main = vg.equals("damage") || vg.equals("type") || vg.equals("attacker") || vg.equals("context");
                if (main ? vg.equals(g) : g.equals("other")) VAR_ITEMS.add(e.getKey());
            }
        }
    }

    /** The variable list with the server's script variables as an extra group */
    static List<String> varItems() {
        Map<String, String> sv = ClientCore.scriptVariables();
        if (sv.isEmpty()) return VAR_ITEMS;
        List<String> out = new ArrayList<>(VAR_ITEMS);
        out.add("#script");
        for (String n : sv.keySet()) out.add(io.github.junjun1z.whathitme.core.text.Template.SCRIPT + n);
        return out;
    }

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
    private long statusAt;
    private String statusLine = "";
    private String selectedVar;
    private int rowsNow = 5;
    private int varFit = 8;
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

    /** Pack editor mode: one output, its lines; Done hands the lines back (null = normal editor) */
    private final java.util.function.Consumer<List<String>> packDone;
    private final List<String> packLines;

    public TemplateEditor() {
        canPack = Safe.get("permission", () -> ClientCore.port.isSingleplayer() && ClientCore.op(), false);
        packDone = null;
        packLines = null;
        for (String o : Template.OUTPUTS) outputs.add(o);
        if (canPack) outputs.add(Template.DEATH);
        Snapshot real = ClientCore.latestDeath();
        sample = real == null || real.key() == null;
        load(0);
        current = this;
    }

    /** The template of one pack category entry (pack editor, templates tab): Done returns the lines, nothing is saved here */
    public TemplateEditor(String output, List<String> lines, java.util.function.Consumer<List<String>> done) {
        canPack = false;
        packDone = done;
        packLines = new ArrayList<>(lines == null ? new ArrayList<String>() : lines);
        outputs.add(output);
        Snapshot real = ClientCore.latestDeath();
        sample = real == null || real.key() == null;
        load(0);
        current = this;
    }

    /** Pack editor mode: hands the lines back and closes */
    public void done() {
        if (packDone == null) return;
        packDone.accept(lines());
        close = true;
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
        lockedNow = packDone == null && !Template.DEATH.equals(o) && Templates.locked(o, ClientCore::locked);
        if (packDone != null) {
            lines = packLines.isEmpty() ? Templates.EXAMPLES.get(o) : packLines;
            source = packLines.isEmpty() ? "example" : "pack";
            if (lines == null) lines = new ArrayList<>();
        } else if (Template.DEATH.equals(o)) {
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
        statusAt = System.currentTimeMillis();
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

    private void ensureVisible() {
        int rows = rowsNow;
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
            else if (id.startsWith("var:")) {
                selectedVar = id.substring(4);
                statusAt = 0;
                insertVariable(selectedVar);
            }
            else if (id.startsWith("up:")) moveLine(Integer.parseInt(id.substring(3)), -1);
            else if (id.startsWith("down:")) moveLine(Integer.parseInt(id.substring(5)), 1);
            else if (id.startsWith("del:")) removeLine(Integer.parseInt(id.substring(4)));
            else if (id.startsWith("field:")) {
                int i = Integer.parseInt(id.substring(6));
                focus = i;
                if (lastG != null) fields.get(i).click(lastG, (int) mx - e.getValue()[0] - 3, shift);
            } else if (id.equals("add")) addLine(fields.size() - 1);
            else if (id.equals("done")) done();
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
            varScroll = Math.max(0, Math.min(Math.max(0, varItems().size() - varFit), varScroll + d));
        } else {
            lineScroll = Math.max(0, Math.min(Math.max(0, fields.size() - rowsNow), lineScroll + d));
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
        String l = LegacyText.trim(g, label, bw - 4);
        g.text(l, x + (bw - g.width(l)) / 2, y + 3, enabled ? (over ? 0xFFFFA0 : 0xFFFFFF) : 0x707070);
        if (enabled) areas.put(id, a);
        return over;
    }

    /** A thin scroll bar: first visible item, visible count, total */
    private static void scrollBar(Gfx g, int x, int y0, int y1, int first, int visible, int total) {
        if (total <= visible || y1 - y0 < 4) return;
        g.fill(x, y0, x + 2, y1, 0x60FFFFFF);
        int len = Math.max(4, (y1 - y0) * visible / total);
        int pos = y0 + (y1 - y0 - len) * first / Math.max(1, total - visible);
        g.fill(x, pos, x + 2, pos + len, 0xFFE0E0E0);
    }

    private String groupLabel(String group) {
        return tr("whathitme.editor.group." + group);
    }

    /** One-line description of a variable */
    public static String describe(String name) {
        if (TemplateVars.isScript(name)) {
            String d = ClientCore.scriptVariables().get(name.substring(io.github.junjun1z.whathitme.core.text.Template.SCRIPT.length()));
            return d == null || d.isEmpty() ? tr("whathitme.var.script") : d;
        }
        String key = "whathitme.var." + name;
        return Msg.LOCAL.has(key) ? tr(key) : String.valueOf(TemplateVars.KNOWN.get(name));
    }

    public String selectedVariable() {
        return selectedVar;
    }

    /** Plain text of the status line of the last frame (self-test) */
    public String statusLine() {
        return Fmt.strip(statusLine);
    }

    /** Number of template lines that fit in the last frame */
    public int visibleLines() {
        return rowsNow;
    }

    private void renderImpl(Gfx g, int w, int h, int mx, int my) {
        lastG = g;
        lastW = w;
        lastH = h;
        areas.clear();
        g.fill(0, 0, w, h, 0xC0101018);
        boolean blink = ((System.currentTimeMillis() - opened) / 500) % 2 == 0;
        // tabs, wrapped to more rows in narrow windows
        int x = M;
        int ty = 4;
        for (int i = 0; i < outputs.size(); i++) {
            String label = tr("whathitme.editor.out." + outputs.get(i));
            int bw = Math.min(g.width(label) + 10, w - 2 * M);
            if (x > M && x + bw > w - M) {
                x = M;
                ty += 15;
            }
            int[] a = {x, ty, x + bw, ty + 13};
            boolean sel = i == out;
            g.fill(x, ty, x + bw, ty + 13, sel ? 0xFFC0A040 : 0xFF505050);
            g.fill(x + 1, ty + 1, x + bw - 1, ty + 13, sel ? 0xFF403010 : 0xFF202020);
            g.text(LegacyText.trim(g, label, bw - 6), x + 5, ty + 3, sel ? 0xFFFF55 : 0xCCCCCC);
            areas.put("tab:" + outputs.get(i), a);
            x += bw + 2;
        }
        String title = tr("whathitme.editor.title");
        if (x + g.width(title) + M + 6 < w) g.text(Fmt.GOLD + title, w - M - g.width(title), ty + 3, 0xFFFFFF);
        int top = ty + 18;
        // bottom buttons, wrapped to more rows in narrow windows
        boolean death = Template.DEATH.equals(output());
        String[][] btns = packDone != null ? new String[][] {
            {"done", tr("whathitme.pack.done"), "true"},
            {"sample", tr(sample ? "whathitme.editor.usereal" : "whathitme.editor.usesample"), "true"},
            {"cancel", tr("whathitme.pack.cancel"), "true"}} : new String[][] {
            {"save", tr("whathitme.editor.save"), String.valueOf(!lockedNow && !death)},
            {"pack", tr("whathitme.editor.savepack"), String.valueOf(canPack)},
            {"reset", tr("whathitme.editor.reset"), String.valueOf(!lockedNow && !death)},
            {"sample", tr(sample ? "whathitme.editor.usereal" : "whathitme.editor.usesample"), "true"},
            {"cancel", tr("whathitme.editor.cancel"), "true"}};
        int[] bx = new int[btns.length];
        int[] brow = new int[btns.length];
        int[] bwid = new int[btns.length];
        int rowsB = 1;
        x = M;
        for (int i = 0; i < btns.length; i++) {
            bwid[i] = Math.min(Math.max(44, g.width(btns[i][1]) + 12), w - 2 * M);
            if (x > M && x + bwid[i] > w - M) {
                x = M;
                rowsB++;
            }
            bx[i] = x;
            brow[i] = rowsB - 1;
            x += bwid[i] + 2;
        }
        int btnTop = h - 17 - (rowsB - 1) * 15;
        int statusY = btnTop - 11;
        int bottom = statusY - 3;
        // template lines: about half of the space between tabs and status line, the rest is the preview
        int avail = bottom - top;
        int rows = Math.min(14, (avail * 55 / 100 - 14) / ROW);
        rows = Math.max(1, Math.min(rows, (avail - 44 - 14) / ROW));
        rows = Math.max(1, rows);
        rowsNow = rows;
        int varW = Math.max(96, Math.min(140, w * 36 / 100));
        int varX = w - varW - M;
        int listRight = varX - M;
        int bw = 12;
        int fx = M + 16;
        int fieldW = Math.max(40, listRight - fx - 3 * (bw + 1) - 6);
        ensureVisible();
        for (int r = 0; r < rows; r++) {
            int i = lineScroll + r;
            if (i >= fields.size()) break;
            int y = top + r * ROW;
            g.text(Fmt.DARK_GRAY + String.valueOf(i + 1), M, y + 3, 0xFFFFFF);
            fields.get(i).draw(g, fx, y, fieldW, ROW - 1, i == focus, blink, TemplateVars::isKnown);
            areas.put("field:" + i, new int[] {fx, y, fx + fieldW, y + ROW - 1});
            int b0 = fx + fieldW + 2;
            button(g, "up:" + i, "↑", b0, y, bw, !lockedNow && i > 0, mx, my);
            button(g, "down:" + i, "↓", b0 + bw + 1, y, bw, !lockedNow && i < fields.size() - 1, mx, my);
            button(g, "del:" + i, "✕", b0 + 2 * (bw + 1), y, bw, !lockedNow, mx, my);
        }
        int listBottom = top + rows * ROW;
        scrollBar(g, listRight - 2, top, listBottom - 1, lineScroll, rows, fields.size());
        areas.put("lines", new int[] {M, top, listRight, listBottom});
        String addLabel = tr("whathitme.editor.addline");
        int addW = Math.max(40, g.width(addLabel) + 12);
        button(g, "add", addLabel, fx, listBottom, addW, !lockedNow, mx, my);
        String info = lockedNow ? Fmt.RED + tr("whathitme.editor.locked") : Fmt.GRAY + tr("whathitme.editor.source." + source);
        if (fields.size() > rows) info += Fmt.DARK_GRAY + "  " + (lineScroll + 1) + "-" + Math.min(fields.size(), lineScroll + rows) + "/" + fields.size();
        g.text(LegacyText.trim(g, info, Math.max(0, listRight - fx - addW - 6)), fx + addW + 4, listBottom + 3, 0xFFFFFF);
        // variables, grouped, scrollable
        int varTop = top;
        int varBottom = listBottom + 13;
        g.fill(varX, varTop, varX + varW, varBottom, 0x80000000);
        areas.put("varlist", new int[] {varX, varTop, varX + varW, varBottom});
        int fit = Math.max(1, (varBottom - varTop - 4) / VAR_ROW);
        varFit = fit;
        List<String> items = varItems();
        varScroll = Math.max(0, Math.min(varScroll, Math.max(0, items.size() - fit)));
        int vy = varTop + 2;
        String hoveredVar = null;
        for (int i = varScroll; i < items.size() && i < varScroll + fit; i++) {
            String n = items.get(i);
            if (n.startsWith("#")) {
                g.text(LegacyText.trim(g, Fmt.GOLD + groupLabel(n.substring(1)), varW - 8), varX + 3, vy + 1, 0xFFFFFF);
                vy += VAR_ROW;
                continue;
            }
            int[] a = {varX, vy, varX + varW - 3, vy + VAR_ROW};
            boolean over = in(a, mx, my);
            if (n.equals(selectedVar)) g.fill(a[0], a[1], a[2], a[3], 0x5055FFFF);
            if (over) {
                g.fill(a[0], a[1], a[2], a[3], 0x40FFFFFF);
                hoveredVar = n;
            }
            g.text(LegacyText.trim(g, "{" + n + "}", varW - 14), varX + 9, vy + 1, over ? 0xFFFF55 : 0x55FFFF);
            areas.put("var:" + n, a);
            vy += VAR_ROW;
        }
        scrollBar(g, varX + varW - 3, varTop + 1, varBottom - 1, varScroll, fit, items.size());
        // preview
        int py = varBottom + 5;
        String sampleLabel = sample ? tr("whathitme.editor.preview.sample") : tr("whathitme.editor.preview.real");
        g.text(LegacyText.trim(g, Fmt.GOLD + tr("whathitme.editor.preview") + Fmt.GRAY + " (" + sampleLabel + ")" + ruleNote(), w - 2 * M), M, py, 0xFFFFFF);
        int areaTop = py + 11;
        drawPreview(g, M, areaTop, w - 2 * M, Math.max(12, bottom - areaTop), mx, my, w, h);
        // status line: a fresh message, else the selected variable
        if (!status.isEmpty() && System.currentTimeMillis() - statusAt < 6000) {
            statusLine = (statusColor == 0x55FF55 ? Fmt.GREEN : Fmt.RED) + status;
        } else if (selectedVar != null) {
            statusLine = Fmt.AQUA + "{" + selectedVar + "}" + Fmt.GRAY + " " + describe(selectedVar);
        } else {
            statusLine = Fmt.DARK_GRAY + tr("whathitme.editor.hint");
        }
        g.text(LegacyText.trim(g, statusLine, w - 2 * M), M, statusY, 0xFFFFFF);
        for (int i = 0; i < btns.length; i++) {
            button(g, btns[i][0], btns[i][1], bx[i], btnTop + brow[i] * 15, bwid[i], Boolean.parseBoolean(btns[i][2]), mx, my);
        }
        if (hoveredVar != null) {
            List<String> tip = new ArrayList<>();
            tip.add(Fmt.AQUA + "{" + hoveredVar + "}");
            tip.addAll(LegacyText.wrap(g, describe(hoveredVar), Math.max(80, Math.min(220, w - 40))));
            g.tooltip(tip, mx, my, w, h);
        }
    }

    /** " - progression rule ..." when the server applied a rule to the previewed killing blow, else "" */
    public String ruleNote() {
        DamageRecord k = previewData().key();
        io.github.junjun1z.whathitme.core.data.RuleMark mk = k == null ? null : k.mark;
        if (mk == null || !mk.isRule()) return "";
        List<String> parts = new ArrayList<>(mk.hiddenNames());
        if (mk.enforced()) parts.add(0, "enforce");
        return Fmt.LIGHT_PURPLE + " - " + tr("whathitme.editor.rule", parts.isEmpty() ? "-" : String.join(", ", parts));
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
                // wrapped and cut to the preview area so it never covers the buttons
                List<String> shown = new ArrayList<>();
                for (String t : tip) shown.addAll(LegacyText.wrap(g, t, Math.max(60, Math.min(250, pw - 16))));
                int max = Math.max(1, (ph - 10) / 10);
                if (shown.size() > max) {
                    shown = new ArrayList<>(shown.subList(0, Math.max(1, max - 1)));
                    shown.add(Fmt.DARK_GRAY + "...");
                }
                // drawn like the death screen tooltip, at the top left of the preview area
                g.push();
                g.tooltip(shown, x - 12 + 4, y + 12 + 4, sw, sh);
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
