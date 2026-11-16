package io.github.junjun1z.whathitme.mc;

import java.util.ArrayList;
import java.util.List;

import io.github.junjun1z.whathitme.core.Safe;
import io.github.junjun1z.whathitme.core.config.Setting;
import io.github.junjun1z.whathitme.core.config.WhmConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Settings of whathitme-client.cfg and whathitme-common.cfg, one page at a time. Keys locked by the modpack
 * (lockedFeatures) are shown but cannot be changed.
 */
public final class ConfigScreen121 extends Screen {

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

    public ConfigScreen121(Screen parent) {
        super(Component.literal("What Hit Me?"));
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
            return Button.builder(Component.literal(onOff(v)), b -> toggle(r, b)).bounds(x, y, w, 20).build();
        }
        if (r.s.valid != null && r.s.valid.length > 0) {
            return Button.builder(Component.literal(v), b -> cycle(r, b)).bounds(x, y, w, 20).build();
        }
        EditBox e = new EditBox(font, x + 1, y + 1, w - 2, 18, Component.literal(r.s.key));
        e.setMaxLength(32767);
        e.setValue(v);
        r.shown = v;
        return e;
    }

    private void toggle(Row r, Button b) {
        r.base.set(r.s.key, "toggle");
        b.setMessage(Component.literal(onOff(r.base.get(r.s.key))));
    }

    private void cycle(Row r, Button b) {
        String cur = r.base.get(r.s.key);
        int i = 0;
        while (i < r.s.valid.length && !r.s.valid[i].equalsIgnoreCase(cur)) i++;
        r.base.set(r.s.key, r.s.valid[(i + 1) % r.s.valid.length]);
        b.setMessage(Component.literal(r.base.get(r.s.key)));
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
            if (i >= from && i < from + perPage) addRenderableWidget(r.widget);
        }
        int y = height - 26;
        Button prev = addRenderableWidget(Button.builder(Component.literal("<"), b -> turn(-1)).bounds(width / 2 - 155, y, 70, 20).build());
        Button next = addRenderableWidget(Button.builder(Component.literal(">"), b -> turn(1)).bounds(width / 2 - 80, y, 70, 20).build());
        prev.active = page > 0;
        next.active = page < pages - 1;
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose()).bounds(width / 2 + 5, y, 150, 20).build());
        String tl = net.minecraft.client.resources.language.I18n.get("whathitme.config.templates");
        int tw = Math.max(70, font.width(tl) + 12);
        templates = addRenderableWidget(Button.builder(Component.literal(tl), b -> {
            commit();
            minecraft.setScreen(new TemplateEditorScreen121(this));
        }).bounds(width - tw - 4, 4, tw, 20).build());
    }

    private Button templates;

    /** The "Templates..." button (self-test) */
    public Button templatesButton() {
        return templates;
    }

    private void turn(int d) {
        commit();
        page += d;
        init(width, height);
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
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        String title = "What Hit Me?  (" + (page + 1) + "/" + Math.max(1, (rows.size() + perPage - 1) / perPage) + ")";
        g.drawString(font, title, (width - font.width(title)) / 2, 12, 0xFFFFFFFF);
        int from = page * perPage;
        String comment = null;
        for (int i = from; i < Math.min(rows.size(), from + perPage); i++) {
            Row r = rows.get(i);
            int y = 30 + (i - from) * ROW;
            if (r.s == null) {
                g.drawString(font, r.header, width / 2 - 155, y + 6, 0xFFFFFF55);
                continue;
            }
            boolean locked = WhmConfig.isLocked(r.s.key);
            g.drawString(font, fit(r.s.key + (locked ? " (locked)" : ""), width / 2 - 10 - (width / 2 - 155)), width / 2 - 155, y + 6, locked ? 0xFF808080 : 0xFFE0E0E0);
            if (mouseY >= y && mouseY < y + ROW && mouseX < width / 2 + 10) comment = r.s.comment;
        }
        if (comment != null) g.drawString(font, fit(comment, width - 20), 10, height - 40, 0xFFA0A0A0);
    }

    @Override
    public void onClose() {
        commit();
        minecraft.setScreen(parent);
    }
}
