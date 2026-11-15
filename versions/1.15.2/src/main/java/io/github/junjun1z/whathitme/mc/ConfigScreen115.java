package io.github.junjun1z.whathitme.mc;

import java.util.ArrayList;
import java.util.List;

import io.github.junjun1z.whathitme.core.Safe;
import io.github.junjun1z.whathitme.core.config.Setting;
import io.github.junjun1z.whathitme.core.config.WhmConfig;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;

/**
 * Settings of whathitme-client.cfg and whathitme-common.cfg, one page at a time. Keys locked by the modpack
 * (lockedFeatures) are shown but cannot be changed.
 */
public final class ConfigScreen115 extends Screen {

    private static final int ROW = 22;

    private static final class Row {
        final WhmConfig.Base base;
        final Setting s;
        final String header;
        AbstractWidget widget;
        String shown = "";

        Row(WhmConfig.Base base, Setting s, String header) {
            this.base = base;
            this.s = s;
            this.header = header;
        }
    }

    private final Screen parent;
    private final List<Row> rows = new ArrayList<>();
    private int page;
    private int perPage = 1;

    public ConfigScreen115(Screen parent) {
        super(new TextComponent("What Hit Me?"));
        this.parent = parent;
    }

    private void addRows(WhmConfig.Base base, String file) {
        if (base == null) return;
        rows.add(new Row(base, null, file));
        for (Setting s : base.settings()) rows.add(new Row(base, s, null));
    }

    private static String onOff(String v) {
        return "true".equalsIgnoreCase(v) ? "ON" : "OFF";
    }

    private AbstractWidget widget(Row r, int x, int y, int w) {
        String v = r.base.get(r.s.key);
        if (v == null) v = "";
        if (r.s.type == Setting.Type.BOOLEAN) {
            return new Button(x, y, w, 20, onOff(v), b -> {
                r.base.set(r.s.key, "toggle");
                b.setMessage(onOff(r.base.get(r.s.key)));
            });
        }
        if (r.s.valid != null && r.s.valid.length > 0) {
            return new Button(x, y, w, 20, v, b -> {
                String cur = r.base.get(r.s.key);
                int i = 0;
                while (i < r.s.valid.length && !r.s.valid[i].equalsIgnoreCase(cur)) i++;
                r.base.set(r.s.key, r.s.valid[(i + 1) % r.s.valid.length]);
                b.setMessage(r.base.get(r.s.key));
            });
        }
        EditBox e = new EditBox(font, x + 1, y + 1, w - 2, 18, r.s.key);
        e.setMaxLength(32767);
        e.setValue(v);
        r.shown = v;
        return e;
    }

    /** Stores the text fields that were changed (invalid values are dropped) */
    private void commit() {
        for (Row r : rows) {
            if (!(r.widget instanceof EditBox) || !r.widget.active) continue;
            String v = ((EditBox) r.widget).getValue();
            if (!v.equals(r.shown)) {
                Safe.run("config screen", () -> r.base.set(r.s.key, v));
                r.shown = v;
            }
        }
    }

    @Override
    protected void init() {
        commit();
        rows.clear();
        addRows(WhmConfig.client, "whathitme-client.cfg");
        addRows(WhmConfig.common, "whathitme-common.cfg");
        perPage = Math.max(1, (height - 70) / ROW);
        int pages = (rows.size() + perPage - 1) / perPage;
        page = Math.max(0, Math.min(page, pages - 1));
        int from = page * perPage;
        int w = Math.min(160, width / 2 - 20);
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            if (r.s == null) continue;
            r.widget = widget(r, width / 2 + 10, 30 + (i - from) * ROW, w);
            r.widget.active = !WhmConfig.isLocked(r.s.key);
            if (i >= from && i < from + perPage) addButton(r.widget);
        }
        int y = height - 26;
        Button prev = addButton(new Button(width / 2 - 155, y, 70, 20, "<", b -> turn(-1)));
        Button next = addButton(new Button(width / 2 - 80, y, 70, 20, ">", b -> turn(1)));
        prev.active = page > 0;
        next.active = page < pages - 1;
        addButton(new Button(width / 2 + 5, y, 150, 20, "Done", b -> onClose()));
    }

    private void turn(int d) {
        commit();
        page += d;
        init(minecraft, width, height);
    }

    /** Whether the entry of a key can be changed (null = no such entry); for the self-test */
    public Boolean entryActive(String key) {
        for (Row r : rows) if (r.s != null && r.s.key.equals(key) && r.widget != null) return r.widget.active;
        return null;
    }

    private String fit(String s, int w) {
        if (font.width(s) <= w) return s;
        while (s.length() > 1 && font.width(s + "...") > w) s = s.substring(0, s.length() - 1);
        return s + "...";
    }

    @Override
    public void render(int mouseX, int mouseY, float partial) {
        renderBackground();
        String title = "What Hit Me?  (" + (page + 1) + "/" + Math.max(1, (rows.size() + perPage - 1) / perPage) + ")";
        font.drawShadow(title, (width - font.width(title)) / 2f, 12, 0xFFFFFF);
        int from = page * perPage;
        String comment = null;
        for (int i = from; i < Math.min(rows.size(), from + perPage); i++) {
            Row r = rows.get(i);
            int y = 30 + (i - from) * ROW;
            if (r.s == null) {
                font.drawShadow(r.header, width / 2f - 155, y + 6, 0xFFFF55);
                continue;
            }
            boolean locked = WhmConfig.isLocked(r.s.key);
            font.drawShadow(fit(r.s.key + (locked ? " (locked)" : ""), width / 2 - 10 - (width / 2 - 155)), width / 2f - 155, y + 6,
                locked ? 0x808080 : 0xE0E0E0);
            if (mouseY >= y && mouseY < y + ROW && mouseX < width / 2 + 10) comment = r.s.comment;
        }
        if (comment != null) font.drawShadow(fit(comment, width - 20), 10, height - 40, 0xA0A0A0);
        super.render(mouseX, mouseY, partial);
    }

    @Override
    public void onClose() {
        commit();
        minecraft.setScreen(parent);
    }
}
