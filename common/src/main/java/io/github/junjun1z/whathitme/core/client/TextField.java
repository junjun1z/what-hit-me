package io.github.junjun1z.whathitme.core.client;

/**
 * A single-line text field drawn with {@link Gfx}: cursor, selection, clipboard (through the client port), typing,
 * backspace / delete, home / end, word jumps with ctrl. Key codes are the editor's own ({@link TemplateEditor}).
 */
public final class TextField {

    public String text;
    public int cursor;
    /** Other end of the selection (== cursor: nothing selected) */
    public int anchor;
    /** First visible character */
    private int scroll;
    public boolean editable = true;

    public TextField(String text) {
        this.text = text == null ? "" : text;
        this.cursor = this.text.length();
        this.anchor = cursor;
    }

    public boolean hasSelection() {
        return anchor != cursor;
    }

    public String selected() {
        return text.substring(Math.min(anchor, cursor), Math.max(anchor, cursor));
    }

    private void deleteSelection() {
        int a = Math.min(anchor, cursor);
        int b = Math.max(anchor, cursor);
        text = text.substring(0, a) + text.substring(b);
        cursor = a;
        anchor = a;
    }

    /** Inserts text at the cursor (replacing the selection); line breaks become spaces */
    public void insert(String s) {
        if (!editable || s == null) return;
        s = s.replace("\r", "").replace('\n', ' ');
        if (hasSelection()) deleteSelection();
        text = text.substring(0, cursor) + s + text.substring(cursor);
        cursor += s.length();
        anchor = cursor;
    }

    public void typed(char c) {
        if (c < 32 || c == 127) return;
        insert(String.valueOf(c));
    }

    private void move(int to, boolean shift) {
        cursor = Math.max(0, Math.min(text.length(), to));
        if (!shift) anchor = cursor;
    }

    private int wordLeft() {
        int i = cursor;
        while (i > 0 && text.charAt(i - 1) == ' ') i--;
        while (i > 0 && text.charAt(i - 1) != ' ') i--;
        return i;
    }

    private int wordRight() {
        int i = cursor;
        while (i < text.length() && text.charAt(i) != ' ') i++;
        while (i < text.length() && text.charAt(i) == ' ') i++;
        return i;
    }

    /** @return true when the key was used */
    public boolean key(int key, boolean ctrl, boolean shift) {
        ClientPort port = ClientCore.port;
        switch (key) {
            case TemplateEditor.K_LEFT:
                move(ctrl ? wordLeft() : hasSelection() && !shift ? Math.min(anchor, cursor) : cursor - 1, shift);
                return true;
            case TemplateEditor.K_RIGHT:
                move(ctrl ? wordRight() : hasSelection() && !shift ? Math.max(anchor, cursor) : cursor + 1, shift);
                return true;
            case TemplateEditor.K_HOME:
                move(0, shift);
                return true;
            case TemplateEditor.K_END:
                move(text.length(), shift);
                return true;
            case TemplateEditor.K_BACKSPACE:
                if (!editable) return true;
                if (hasSelection()) deleteSelection();
                else if (cursor > 0) {
                    int from = ctrl ? wordLeft() : cursor - 1;
                    text = text.substring(0, from) + text.substring(cursor);
                    cursor = from;
                    anchor = from;
                }
                return true;
            case TemplateEditor.K_DELETE:
                if (!editable) return true;
                if (hasSelection()) deleteSelection();
                else if (cursor < text.length()) {
                    int to = ctrl ? wordRight() : cursor + 1;
                    text = text.substring(0, cursor) + text.substring(to);
                }
                return true;
            case TemplateEditor.K_A:
                if (!ctrl) return false;
                anchor = 0;
                cursor = text.length();
                return true;
            case TemplateEditor.K_C:
                if (!ctrl) return false;
                if (hasSelection() && port != null) port.setClipboard(selected());
                return true;
            case TemplateEditor.K_X:
                if (!ctrl) return false;
                if (hasSelection() && port != null && editable) {
                    port.setClipboard(selected());
                    deleteSelection();
                }
                return true;
            case TemplateEditor.K_V:
                if (!ctrl) return false;
                if (port != null) insert(port.getClipboard());
                return true;
            default:
                return false;
        }
    }

    private static String shown(String s) {
        return s.replace('§', '&');
    }

    /** Cursor position from a click at x (relative to the text start) */
    public void click(Gfx g, int relX, boolean shift) {
        int pos = scroll;
        while (pos < text.length()) {
            int wNext = g.width(shown(text.substring(scroll, pos + 1)));
            int wHere = g.width(shown(text.substring(scroll, pos)));
            if (relX < (wHere + wNext) / 2) break;
            pos++;
        }
        move(pos, shift);
    }

    private void fitScroll(Gfx g, int width) {
        if (scroll > text.length()) scroll = text.length();
        if (cursor < scroll) scroll = cursor;
        while (scroll < cursor && g.width(shown(text.substring(scroll, cursor))) > width) scroll++;
    }

    /**
     * Draws the field; placeholders are colored (known aqua, unknown red), color codes grey.
     *
     * @param known tells whether a canonical variable name is known
     */
    public void draw(Gfx g, int x, int y, int w, int h, boolean focused, boolean blink, java.util.function.Predicate<String> known) {
        g.fill(x, y, x + w, y + h, focused ? 0xFFA0A0A0 : 0xFF606060);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, editable ? 0xFF000000 : 0xFF202020);
        int inner = w - 6;
        fitScroll(g, inner);
        String vis = text.substring(scroll);
        // cut what does not fit
        int end = vis.length();
        while (end > 0 && g.width(shown(vis.substring(0, end))) > inner) end--;
        vis = vis.substring(0, end);
        int tx = x + 3;
        int ty = y + (h - 8) / 2;
        if (hasSelection()) {
            int a = Math.max(scroll, Math.min(anchor, cursor)) - scroll;
            int b = Math.min(scroll + end, Math.max(anchor, cursor)) - scroll;
            if (b > a) g.fill(tx + g.width(shown(vis.substring(0, a))), ty - 1, tx + g.width(shown(vis.substring(0, b))), ty + 9, 0xFF3050A0);
        }
        // colored segments
        int i = 0;
        int cx = tx;
        int base = editable ? 0xE0E0E0 : 0x909090;
        while (i < vis.length()) {
            int color = base;
            int j;
            char c = vis.charAt(i);
            io.github.junjun1z.whathitme.core.text.Template.Placeholder p = c == '{' ? io.github.junjun1z.whathitme.core.text.Template.placeholderAt(vis, i) : null;
            if (p != null) {
                j = p.end;
                color = known.test(p.name) ? 0x55FFFF : 0xFF5555;
            } else if ((c == '&' || c == '§') && i + 1 < vis.length()) {
                j = i + 2;
                color = 0x808080;
            } else {
                j = i + 1;
                while (j < vis.length() && vis.charAt(j) != '{' && vis.charAt(j) != '&' && vis.charAt(j) != '§') j++;
            }
            String seg = shown(vis.substring(i, j));
            g.text(seg, cx, ty, color);
            cx += g.width(seg);
            i = j;
        }
        if (focused && blink && editable) {
            int c = Math.max(0, Math.min(end, cursor - scroll));
            int px = tx + g.width(shown(vis.substring(0, c)));
            g.fill(px, ty - 1, px + 1, ty + 9, 0xFFFFFFFF);
        }
    }
}
