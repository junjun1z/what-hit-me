package io.github.junjun1z.whathitme.core.client;

import java.util.ArrayList;
import java.util.List;

import io.github.junjun1z.whathitme.core.config.WhmConfig;
import io.github.junjun1z.whathitme.core.config.WhmConfig.Flag;
import io.github.junjun1z.whathitme.core.data.DamageRecord;
import io.github.junjun1z.whathitme.core.data.Snapshot;
import io.github.junjun1z.whathitme.core.text.Fmt;
import io.github.junjun1z.whathitme.core.text.Lang;
import io.github.junjun1z.whathitme.core.text.Msg;
import io.github.junjun1z.whathitme.core.text.RecapFormatter;
import io.github.junjun1z.whathitme.core.text.Txt;

/**
 * The recap panel drawn over the death screen: draggable header, [-] minimize, wheel scroll, Copy button, tooltips.
 * The loader calls {@link #render} after the death screen is drawn and forwards mouse input to
 * {@link #mouseDown}, {@link #mouseUp} and {@link #scroll} (cancelling the event when they return true).
 */
public final class DeathPanel {

    public static final DeathPanel INSTANCE = new DeathPanel();

    private static final int W = 250;
    private static final int HEADER = 14;
    private static final int LINE = 10;
    private static final int PAD = 4;

    private int scroll;
    private boolean dragging;
    private long lastDragEvent;
    private int dragDX;
    private int dragDY;
    private long openedAt;
    private String flash = "";
    private long flashUntil;

    // last layout (GUI coordinates)
    private int px;
    private int py;
    private int ph;
    private float sc = 1f;
    private int baseX;
    private int baseY;
    private int dragX;
    private int dragY;
    /** Panel-space y of the first damage line, and the records shown */
    private int linesTop;
    private final List<DamageRecord> shown = new ArrayList<>();
    /** Widths of the header buttons of the last frame */
    private int lastMinX = W;
    private int lastCopyX = W;

    private DeathPanel() {}

    void onOpen() {
        scroll = 0;
        dragging = false;
        openedAt = System.currentTimeMillis();
    }

    void onNewData() {
        scroll = 0;
    }

    private static String tr(String key, Object... args) {
        return Msg.LOCAL.text(key, args);
    }

    private static Lang lang() {
        return Msg.LOCAL.lang();
    }

    /** The recap that belongs to this death screen, or null while waiting for it */
    private Snapshot data() {
        Snapshot s = ClientCore.currentDeath;
        if (s == null) return null;
        if (s.time < openedAt - 15000) return null; // an older death
        return s;
    }

    public void render(Gfx g, int screenW, int screenH, int mouseX, int mouseY) {
        try {
            renderImpl(g, screenW, screenH, mouseX, mouseY);
        } catch (Throwable t) {
            io.github.junjun1z.whathitme.core.Safe.warn("death panel", t);
        }
    }

    private void renderImpl(Gfx g, int screenW, int screenH, int mouseX, int mouseY) {
        RecapFormatter f = ClientCore.formatter();
        if (!f.o.on(Flag.DEATH_SCREEN_PANEL)) return;
        WhmConfig.Client cfg = WhmConfig.client;
        Lang lang = lang();
        Snapshot s = data();
        boolean min = cfg.minimized;
        int inner = W - PAD * 2;

        // ---- content (panel space)
        List<String> top = new ArrayList<>();
        List<String> footer = new ArrayList<>();
        shown.clear();
        DamageRecord key = null;
        if (s == null) {
            boolean waiting = ClientCore.serverHasMod || ClientCore.port.isSingleplayer();
            top.add(Fmt.GRAY + tr(waiting ? "whathitme.panel.waiting" : "whathitme.panel.nodata"));
        } else {
            key = s.key();
            String kill = key != null
                ? Fmt.DARK_RED + "☠ " + Fmt.RED + f.displayName(key).toLegacy(lang) + Fmt.RED
                    + (f.o.on(Flag.DAMAGE_AMOUNT) ? " [" + tr("whathitme.hearts", RecapFormatter.hearts(key.taken())) + "]" : "")
                : Fmt.DARK_RED + "☠ " + Fmt.RED + tr("whathitme.unknown");
            top.addAll(LegacyText.wrap(g, kill, inner, 2));
            // the vanilla death message is already shown above the buttons
            if (key == null) {
                Txt dm = f.deathMessage(s);
                if (dm != null) for (String l : LegacyText.wrap(g, Fmt.GRAY + dm.toPlain(lang), inner, 2)) top.add(Fmt.GRAY + l);
            }
            if (key != null && f.o.on(Flag.DAMAGE_PROPERTIES) && (key.flags & DamageRecord.PROPERTY_MASK) != 0) {
                top.addAll(LegacyText.wrap(g, Fmt.DARK_AQUA + f.properties(key.flags).toPlain(lang), inner, 2));
            }
            shown.addAll(f.visible(s.records, 100));
            footer.addAll(LegacyText.wrap(g, f.total(s.records, s.time).toLegacy(lang), inner, 2));
            Txt vital = f.vital(key);
            if (vital != null) top.addAll(LegacyText.wrap(g, Fmt.LIGHT_PURPLE + vital.toPlain(lang), inner, 2));
            Txt tip = key != null && f.o.on(Flag.HINTS_AND_TIPS) ? f.tip(key) : null;
            if (tip != null) {
                for (String l : LegacyText.wrap(g, Fmt.GREEN + "✎ " + tip.toPlain(lang), inner, 3)) footer.add(Fmt.GREEN + l);
            }
            if (key != null && key.has(DamageRecord.ESTIMATED)) {
                footer.add(Fmt.GOLD + tr("whathitme.badge.estimated") + " " + Fmt.GRAY + tr("whathitme.panel.estimated"));
            }
        }
        // rows of each record: one line, or the wrapped lines of a line template
        List<List<String>> rows = new ArrayList<>();
        long ref0 = s == null ? 0L : s.time;
        for (DamageRecord r : shown) rows.add(rowsFor(g, f, r, ref0, lang, inner - 4));
        int maxLines = Math.max(1, cfg.maxVisibleLines);
        int maxScroll = shown.size();
        int used = 0;
        while (maxScroll > 0 && used + rows.get(maxScroll - 1).size() <= maxLines) used += rows.get(--maxScroll).size();
        if (maxScroll == shown.size() && maxScroll > 0) maxScroll--;
        if (scroll > maxScroll) scroll = maxScroll;
        if (scroll < 0) scroll = 0;
        int visible = 0;
        int visibleRows = 0;
        while (scroll + visible < shown.size()) {
            int n = rows.get(scroll + visible).size();
            if (visible > 0 && visibleRows + n > maxLines) break;
            visibleRows += n;
            visible++;
        }
        int h = HEADER;
        if (!min) {
            h += PAD + top.size() * LINE + (shown.isEmpty() ? 0 : 3 + visibleRows * LINE) + 3 + footer.size() * LINE + PAD;
        }

        // ---- position
        if (!dragging) sc = fitScale(cfg, g, screenW, screenH, h);
        int pw = Math.round(W * sc);
        ph = Math.round(h * sc);
        int[] base = base(cfg.anchorPoint, screenW, screenH, pw, ph);
        baseX = base[0];
        baseY = base[1];
        if (dragging) {
            dragX = mouseX - dragDX;
            dragY = mouseY - dragDY;
            // ends a drag whose release was lost (e.g. outside the window)
            if (g.leftDown()) {
                lastDragEvent = System.currentTimeMillis();
            } else if (System.currentTimeMillis() - lastDragEvent > 1500) {
                endDrag();
            }
        }
        int x = dragging ? dragX : baseX + cfg.offsetX;
        int y = dragging ? dragY : baseY + cfg.offsetY;
        x = Math.max(0, Math.min(screenW - pw, x));
        y = Math.max(0, Math.min(screenH - ph, y));
        px = x;
        py = y;

        // ---- draw
        g.push();
        g.translate(x, y, 300f);
        g.scale(sc);
        int alpha = Math.round(Math.max(0f, Math.min(1f, cfg.backgroundOpacity)) * 255f);
        g.fill(0, 0, W, h, (alpha << 24) | 0x100808);
        g.fill(0, 0, W, HEADER, (Math.max(alpha, 160) << 24) | 0x6B1010);
        g.fill(0, h - 1, W, h, 0x80FF5555);
        g.text(Fmt.GOLD + "❤ " + Fmt.BOLD + tr("whathitme.panel.title"), PAD, 3, 0xFFFFFF);
        int lx = (int) ((mouseX - x) / sc);
        int ly = (int) ((mouseY - y) / sc);
        String minLabel = min ? "[+]" : "[-]";
        int minX = W - PAD - g.width(minLabel);
        lastMinX = minX;
        g.text(minLabel, minX, 3, hover(lx, ly, minX, 0, W, HEADER) ? 0xFFFF55 : 0xDDDDDD);
        String copy = "[" + tr("whathitme.panel.copy") + "]";
        int copyX = minX - 4 - g.width(copy);
        lastCopyX = copyX;
        boolean canCopy = s != null && f.o.on(Flag.ACTION_BUTTONS);
        if (canCopy) g.text(copy, copyX, 3, hover(lx, ly, copyX, 0, minX - 2, HEADER) ? 0xFFFF55 : 0x55FFFF);
        DamageRecord hovered = null;
        if (!min) {
            int cy = HEADER + PAD;
            for (String l : top) {
                g.text(l, PAD, cy, 0xFFFFFF);
                cy += LINE;
            }
            if (!shown.isEmpty()) {
                cy += 1;
                g.fill(PAD, cy, W - PAD, cy + 1, 0x40FFFFFF);
                cy += 2;
                linesTop = cy;
                long ref = s.time;
                for (int i = 0; i < visible; i++) {
                    DamageRecord r = shown.get(scroll + i);
                    List<String> rr = rows.get(scroll + i);
                    int rh = rr.size() * LINE;
                    boolean over = hover(lx, ly, 0, cy, W, cy + rh);
                    if (over) {
                        g.fill(1, cy - 1, W - 1, cy + rh - 1, 0x30FFFFFF);
                        hovered = r;
                    }
                    for (String row : rr) {
                        g.text(LegacyText.trim(g, row, inner - (maxScroll > 0 ? 4 : 0)), PAD, cy, 0xFFFFFF);
                        cy += LINE;
                    }
                }
                if (maxScroll > 0) {
                    int trackTop = linesTop;
                    int trackH = visibleRows * LINE - 1;
                    int barH = Math.max(6, trackH * visible / shown.size());
                    int barY = trackTop + (trackH - barH) * scroll / maxScroll;
                    g.fill(W - 3, trackTop, W - 1, trackTop + trackH, 0x40FFFFFF);
                    g.fill(W - 3, barY, W - 1, barY + barH, 0xC0FFFFFF);
                }
            }
            cy += 1;
            g.fill(PAD, cy, W - PAD, cy + 1, 0x40FFFFFF);
            cy += 2;
            for (String l : footer) {
                g.text(l, PAD, cy, 0xFFFFFF);
                cy += LINE;
            }
        }
        g.pop();

        if (System.currentTimeMillis() < flashUntil) {
            g.push();
            g.translate(0, 0, 300f);
            g.text(flash, x, Math.min(screenH - 10, y + ph + 2), 0x55FF55);
            g.pop();
        }
        if (hovered != null && !dragging) {
            List<String> tip = new ArrayList<>();
            for (Txt c : f.hoverLines(hovered)) tip.add(c.toLegacy(lang));
            g.tooltip(tip, mouseX, mouseY, screenW, screenH);
        }
    }

    /** The panel rows of one record: the built-in line, or the lines of a line template wrapped to width */
    static List<String> rowsFor(Gfx g, RecapFormatter f, DamageRecord r, long ref, Lang lang, int width) {
        List<String> out = new ArrayList<>();
        io.github.junjun1z.whathitme.core.text.Template t = f.o.template(io.github.junjun1z.whathitme.core.text.Template.LINE);
        if (t != null && !r.has(DamageRecord.ARMOR_BREAK)) {
            for (Txt line : t.render(f.m, new io.github.junjun1z.whathitme.core.text.TemplateVars(f, r, null, ref))) {
                out.addAll(LegacyText.wrap(g, line.toLegacy(lang), width, 3));
            }
        }
        if (out.isEmpty()) out.add(panelLine(f, r, ref, lang));
        return out;
    }

    /**
     * Draws damage lines like the panel does (template editor preview): background, rows, the tooltip of the hovered
     * record. Returns the height used.
     */
    public static int drawPreview(Gfx g, RecapFormatter f, List<DamageRecord> records, long ref, int x, int y, int maxRows, int mouseX, int mouseY,
                                  int screenW, int screenH) {
        Lang lang = lang();
        int inner = W - PAD * 2;
        List<List<String>> rows = new ArrayList<>();
        int total = 0;
        for (DamageRecord r : records) {
            List<String> rr = rowsFor(g, f, r, ref, lang, inner - 4);
            if (total + rr.size() > maxRows && total > 0) break;
            rows.add(rr);
            total += rr.size();
        }
        int h = PAD * 2 + total * LINE;
        g.fill(x, y, x + W, y + h, 0xC0100808);
        g.fill(x, y + h - 1, x + W, y + h, 0x80FF5555);
        int cy = y + PAD;
        DamageRecord hovered = null;
        for (int i = 0; i < rows.size(); i++) {
            int rh = rows.get(i).size() * LINE;
            if (mouseX >= x && mouseX < x + W && mouseY >= cy && mouseY < cy + rh) {
                g.fill(x + 1, cy - 1, x + W - 1, cy + rh - 1, 0x30FFFFFF);
                hovered = records.get(i);
            }
            for (String row : rows.get(i)) {
                g.text(LegacyText.trim(g, row, inner), x + PAD, cy, 0xFFFFFF);
                cy += LINE;
            }
        }
        if (hovered != null) {
            List<String> tip = new ArrayList<>();
            for (Txt c : f.hoverLines(hovered)) tip.add(c.toLegacy(lang));
            g.tooltip(tip, mouseX, mouseY, screenW, screenH);
        }
        return h;
    }

    public static int width() {
        return W;
    }

    private static String panelLine(RecapFormatter f, DamageRecord r, long ref, Lang lang) {
        StringBuilder sb = new StringBuilder();
        if (r.has(DamageRecord.KILLING)) sb.append(Fmt.DARK_RED).append("☠ ");
        if (f.o.on(Flag.TIMESTAMP)) sb.append(Fmt.DARK_GRAY).append('-').append(RecapFormatter.age(ref - r.time)).append(' ');
        sb.append(Fmt.WHITE).append(f.displayName(r).toLegacy(lang));
        if (r.has(DamageRecord.ARMOR_BREAK)) return sb.toString();
        if (f.o.on(Flag.DAMAGE_AMOUNT)) sb.append(Fmt.RED).append(" -").append(tr("whathitme.hearts", RecapFormatter.hearts(r.taken())));
        if (f.o.on(Flag.REFLECTED_BADGE) && r.has(DamageRecord.REFLECTED)) sb.append(Fmt.YELLOW).append(" ⚡");
        if (f.o.on(Flag.HIT_COUNT) && r.hits > 1) sb.append(Fmt.YELLOW).append(" x").append(r.hits);
        if (r.has(DamageRecord.TOTEM)) sb.append(Fmt.GOLD).append(" ★");
        if (f.o.on(Flag.ATTACKER_INVISIBILITY) && r.has(DamageRecord.INVISIBLE)) sb.append(Fmt.LIGHT_PURPLE).append(" ✦");
        return sb.toString();
    }

    /**
     * The configured scale, made smaller when the panel would cover the Respawn / Title screen buttons (large GUI scale,
     * small window) or not fit on the screen. Never below one screen pixel per font pixel.
     */
    private static float fitScale(WhmConfig.Client cfg, Gfx g, int sw, int sh, int h) {
        float s = cfg.scale;
        int factor = g.guiScale();
        float floor = Math.max(0.25f, Math.min(s, 1f / Math.max(1, factor)));
        int pw = Math.round(W * s);
        int ph = Math.round(h * s);
        int[] b = base(cfg.anchorPoint, sw, sh, pw, ph);
        int x0 = Math.max(0, Math.min(sw - pw, b[0] + cfg.offsetX));
        int left = sw / 2 - 102; // the death screen buttons are 200 wide, centered
        int right = sw / 2 + 102;
        if (x0 < left && x0 + pw > left) {
            s = Math.min(s, (left - x0) / (float) W);
        } else if (x0 + pw > right && x0 > left && x0 < right && cfg.anchorPoint != null && cfg.anchorPoint.endsWith("_RIGHT")) {
            s = Math.min(s, (x0 + pw - right) / (float) W);
        }
        if (h * s > sh - 4) s = (sh - 4) / (float) h;
        return Math.max(floor, s);
    }

    private static boolean hover(int lx, int ly, int x0, int y0, int x1, int y1) {
        return lx >= x0 && lx < x1 && ly >= y0 && ly < y1;
    }

    private static int[] base(String anchor, int sw, int sh, int pw, int ph) {
        switch (anchor == null ? "" : anchor) {
            case "TOP_LEFT":
                return new int[] {0, 0};
            case "TOP_RIGHT":
                return new int[] {sw - pw, 0};
            case "BOTTOM_LEFT":
                return new int[] {0, sh - ph};
            case "BOTTOM_RIGHT":
                return new int[] {sw - pw, sh - ph};
            case "CENTER_RIGHT":
                return new int[] {sw - pw, (sh - ph) / 2};
            case "CENTER_LEFT":
            default:
                return new int[] {0, (sh - ph) / 2};
        }
    }

    private void endDrag() {
        dragging = false;
        WhmConfig.client.saveLayout(px - baseX, py - baseY, WhmConfig.client.minimized);
    }

    private boolean inside(double mx, double my) {
        return mx >= px && mx < px + Math.round(W * sc) && my >= py && my < py + ph;
    }

    /** Mouse button pressed on the death screen. @return true when the panel used it (cancel the event) */
    public boolean mouseDown(int button, double mx, double my) {
        try {
            return mouseDownImpl(button, mx, my);
        } catch (Throwable t) {
            io.github.junjun1z.whathitme.core.Safe.warn("death panel", t);
            return false;
        }
    }

    private boolean mouseDownImpl(int button, double mx, double my) {
        if (!ClientCore.on(Flag.DEATH_SCREEN_PANEL)) return false;
        if (!inside(mx, my)) return false;
        if (button != 0) return true; // do not click through the panel
        int lx = (int) ((mx - px) / sc);
        int ly = (int) ((my - py) / sc);
        if (ly < HEADER && lx >= lastMinX - 2) {
            WhmConfig.client.saveLayout(WhmConfig.client.offsetX, WhmConfig.client.offsetY, !WhmConfig.client.minimized);
            return true;
        }
        if (ly < HEADER && lx >= lastCopyX - 2 && data() != null && ClientCore.on(Flag.ACTION_BUTTONS)) {
            copyRecap();
            return true;
        }
        if (ly < HEADER) {
            dragging = true;
            lastDragEvent = System.currentTimeMillis();
            dragDX = (int) mx - px;
            dragDY = (int) my - py;
            dragX = px;
            dragY = py;
            return true;
        }
        return true;
    }

    /** Mouse button released. @return true when the panel used it */
    public boolean mouseUp(int button, double mx, double my, int screenW, int screenH) {
        try {
            return mouseUpImpl(button, mx, my, screenW, screenH);
        } catch (Throwable t) {
            io.github.junjun1z.whathitme.core.Safe.warn("death panel", t);
            return false;
        }
    }

    private boolean mouseUpImpl(int button, double mx, double my, int screenW, int screenH) {
        if (button == 0 && dragging) {
            px = Math.max(0, Math.min(screenW - Math.round(W * sc), (int) mx - dragDX));
            py = Math.max(0, Math.min(screenH - ph, (int) my - dragDY));
            endDrag();
            return true;
        }
        return ClientCore.on(Flag.DEATH_SCREEN_PANEL) && inside(mx, my);
    }

    /** Mouse moved while a button is held (optional: the drag also follows the mouse while rendering) */
    public boolean mouseDrag(double mx, double my) {
        try {
            return mouseDragImpl(mx, my);
        } catch (Throwable t) {
            io.github.junjun1z.whathitme.core.Safe.warn("death panel", t);
            return false;
        }
    }

    private boolean mouseDragImpl(double mx, double my) {
        if (!dragging) return false;
        lastDragEvent = System.currentTimeMillis();
        dragX = (int) mx - dragDX;
        dragY = (int) my - dragDY;
        return true;
    }

    /** Mouse wheel. @return true when the panel used it */
    public boolean scroll(double mx, double my, double delta) {
        try {
            return scrollImpl(mx, my, delta);
        } catch (Throwable t) {
            io.github.junjun1z.whathitme.core.Safe.warn("death panel", t);
            return false;
        }
    }

    private boolean scrollImpl(double mx, double my, double delta) {
        if (!ClientCore.on(Flag.DEATH_SCREEN_PANEL) || !inside(mx, my) || delta == 0) return false;
        scroll += delta > 0 ? -1 : 1;
        return true;
    }

    public boolean isDragging() {
        return dragging;
    }

    void copyRecap() {
        if (data() == null) return;
        if (!ClientCore.copyRecap()) return;
        flash = tr("whathitme.copyrecap.done");
        flashUntil = System.currentTimeMillis() + 2500;
    }
}
