package io.github.junjun1z.whathitme.core.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;
import java.util.function.Predicate;

import io.github.junjun1z.whathitme.core.text.Fmt;

/**
 * Immediate-mode widgets on {@link Gfx} for the core screens (same look as the template editor): buttons, tabs,
 * checkboxes, text fields, scroll bars, scrollable lists. Every frame the screen draws its widgets again; each
 * clickable widget registers an area with an id and an action, the newest area wins a click.
 */
public final class Ui {

    /** Button / field height */
    public static final int BH = 13;
    /** List row height */
    public static final int ROW = 11;

    static final class Area {
        final int[] r;
        final Runnable click;

        Area(int[] r, Runnable click) {
            this.r = r;
            this.click = click;
        }
    }

    public Gfx g;
    public int mx;
    public int my;
    /** Position of the click being handled (text fields place the cursor with it) */
    public double clickX;
    public double clickY;
    public boolean shift;
    private final Map<String, Area> areas = new LinkedHashMap<>();
    private final List<Object[]> wheels = new ArrayList<>();

    public void begin(Gfx g, int mx, int my) {
        this.g = g;
        this.mx = mx;
        this.my = my;
        areas.clear();
        wheels.clear();
    }

    /** Drops every area registered so far (a modal layer is drawn on top) */
    public void clearAreas() {
        areas.clear();
        wheels.clear();
    }

    public int[] area(String id) {
        Area a = areas.get(id);
        return a == null ? null : a.r;
    }

    public List<String> ids() {
        return new ArrayList<>(areas.keySet());
    }

    public static boolean in(int[] r, double x, double y) {
        return r != null && x >= r[0] && x < r[2] && y >= r[1] && y < r[3];
    }

    public boolean hover(int x0, int y0, int x1, int y1) {
        return mx >= x0 && mx < x1 && my >= y0 && my < y1;
    }

    public void onClick(String id, int x0, int y0, int x1, int y1, Runnable r) {
        areas.remove(id);
        areas.put(id, new Area(new int[] {x0, y0, x1, y1}, r));
    }

    public void onWheel(int x0, int y0, int x1, int y1, IntConsumer step) {
        wheels.add(new Object[] {new int[] {x0, y0, x1, y1}, step});
    }

    /** Runs the newest area under the point; false when there is none */
    public boolean click(double x, double y, boolean shift) {
        List<Map.Entry<String, Area>> list = new ArrayList<>(areas.entrySet());
        for (int i = list.size() - 1; i >= 0; i--) {
            Area a = list.get(i).getValue();
            if (in(a.r, x, y)) {
                clickX = x;
                clickY = y;
                this.shift = shift;
                if (a.click != null) a.click.run();
                return true;
            }
        }
        return false;
    }

    /** Clicks the area with this id at its center (self-test); false when it is not shown */
    public boolean press(String id) {
        Area a = areas.get(id);
        if (a == null) return false;
        clickX = (a.r[0] + a.r[2]) / 2.0;
        clickY = (a.r[1] + a.r[3]) / 2.0;
        shift = false;
        if (a.click != null) a.click.run();
        return true;
    }

    public boolean wheel(double x, double y, double delta) {
        for (int i = wheels.size() - 1; i >= 0; i--) {
            Object[] w = wheels.get(i);
            if (in((int[]) w[0], x, y)) {
                ((IntConsumer) w[1]).accept(delta > 0 ? -1 : 1);
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------ drawing

    public void text(String s, int x, int y, int rgb) {
        g.text(s, x, y, rgb);
    }

    /** Text cut to a width */
    public void text(String s, int x, int y, int w, int rgb) {
        if (w <= 0) return;
        g.text(LegacyText.trim(g, s, w), x, y, rgb);
    }

    public int width(String s) {
        return g.width(s);
    }

    public void fill(int x0, int y0, int x1, int y1, int argb) {
        g.fill(x0, y0, x1, y1, argb);
    }

    /** A frame with a dark inside */
    public void panel(int x0, int y0, int x1, int y1) {
        g.fill(x0, y0, x1, y1, 0xFF505060);
        g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, 0xE0141420);
    }

    public int buttonWidth(String label) {
        return Math.max(BH + 4, g.width(label) + 10);
    }

    /** A button; returns its width */
    public int button(String id, String label, int x, int y, int w, boolean enabled, Runnable r) {
        boolean over = enabled && hover(x, y, x + w, y + BH);
        g.fill(x, y, x + w, y + BH, enabled ? (over ? 0xFFA0A0A0 : 0xFF707070) : 0xFF404040);
        g.fill(x + 1, y + 1, x + w - 1, y + BH - 1, enabled ? (over ? 0xFF5A5A8C : 0xFF3A3A4A) : 0xFF262626);
        String l = LegacyText.trim(g, label, w - 4);
        g.text(l, x + (w - g.width(l)) / 2, y + 3, enabled ? (over ? 0xFFFFA0 : 0xFFFFFF) : 0x707070);
        if (enabled) onClick(id, x, y, x + w, y + BH, r);
        return w;
    }

    /** A tab of a tab row; returns its width */
    public int tab(String id, String label, int x, int y, int w, boolean selected, Runnable r) {
        g.fill(x, y, x + w, y + BH, selected ? 0xFFC0A040 : 0xFF505050);
        g.fill(x + 1, y + 1, x + w - 1, y + BH, selected ? 0xFF403010 : 0xFF202020);
        boolean over = !selected && hover(x, y, x + w, y + BH);
        g.text(LegacyText.trim(g, label, w - 6), x + 5, y + 3, selected ? 0xFFFF55 : over ? 0xFFFFA0 : 0xCCCCCC);
        onClick(id, x, y, x + w, y + BH, r);
        return w;
    }

    /**
     * A row of tabs wrapped to more rows when needed
     *
     * @return the y below the last row
     */
    public int tabs(String prefix, List<String> ids, List<String> labels, int selected, int x, int y, int w, IntConsumer pick) {
        int cx = x;
        for (int i = 0; i < ids.size(); i++) {
            int bw = Math.min(g.width(labels.get(i)) + 10, w);
            if (cx > x && cx + bw > x + w) {
                cx = x;
                y += BH + 2;
            }
            final int idx = i;
            tab(prefix + ids.get(i), labels.get(i), cx, y, bw, i == selected, () -> pick.accept(idx));
            cx += bw + 2;
        }
        return y + BH + 2;
    }

    /** A checkbox with a label; returns its width */
    public int checkbox(String id, String label, int x, int y, int maxW, boolean value, boolean enabled, Runnable toggle) {
        int w = Math.min(maxW, 12 + (label.isEmpty() ? 0 : g.width(label) + 3));
        boolean over = enabled && hover(x, y, x + w, y + 10);
        g.fill(x, y, x + 10, y + 10, over ? 0xFFE0E0E0 : enabled ? 0xFFA0A0A0 : 0xFF505050);
        g.fill(x + 1, y + 1, x + 9, y + 9, 0xFF101010);
        if (value) g.fill(x + 3, y + 3, x + 7, y + 7, enabled ? 0xFF55FF55 : 0xFF407040);
        if (!label.isEmpty()) g.text(LegacyText.trim(g, label, maxW - 13), x + 13, y + 1, enabled ? (over ? 0xFFFFA0 : 0xE0E0E0) : 0x808080);
        if (enabled) onClick(id, x, y - 1, x + w, y + 11, toggle);
        return w;
    }

    /** A text field; clicking focuses it and places the cursor */
    public void field(String id, TextField f, int x, int y, int w, boolean focused, boolean blink, Predicate<String> known, Runnable focus) {
        f.draw(g, x, y, w, BH, focused, blink, known == null ? n -> true : known, known == null);
        onClick(id, x, y, x + w, y + BH, () -> {
            if (focus != null) focus.run();
            f.click(g, (int) clickX - x - 3, shift);
        });
    }

    /** A thin scroll bar: first visible item, visible count, total */
    public void scrollBar(int x, int y0, int y1, int first, int visible, int total) {
        if (total <= visible || y1 - y0 < 4) return;
        g.fill(x, y0, x + 2, y1, 0x60FFFFFF);
        int len = Math.max(4, (y1 - y0) * visible / total);
        int pos = y0 + (y1 - y0 - len) * first / Math.max(1, total - visible);
        g.fill(x, pos, x + 2, pos + len, 0xFFE0E0E0);
    }

    /** Color code prefix for a status color */
    public static String color(boolean ok) {
        return ok ? Fmt.GREEN.toString() : Fmt.RED.toString();
    }
}
