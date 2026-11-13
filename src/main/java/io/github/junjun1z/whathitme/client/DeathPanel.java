package io.github.junjun1z.whathitme.client;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Mouse;

import io.github.junjun1z.whathitme.config.WhmConfig;
import io.github.junjun1z.whathitme.config.WhmConfig.Flag;
import io.github.junjun1z.whathitme.data.DamageRecord;
import io.github.junjun1z.whathitme.data.Snapshot;
import io.github.junjun1z.whathitme.text.Msg;
import io.github.junjun1z.whathitme.text.RecapFormatter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** The recap panel drawn over the death screen: draggable header, [-] minimize, wheel scroll, Copy button, tooltips. */
@SideOnly(Side.CLIENT)
public final class DeathPanel {

    public static final DeathPanel INSTANCE = new DeathPanel();

    private static final int W = 240;
    private static final int HEADER = 14;
    private static final int LINE = 10;
    private static final int PAD = 4;

    private int scroll;
    private boolean dragging;
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

    private DeathPanel() {}

    void onOpen() {
        scroll = 0;
        dragging = false;
        openedAt = System.currentTimeMillis();
    }

    void onNewData() {
        scroll = 0;
    }

    private static Minecraft mc() {
        return Minecraft.getMinecraft();
    }

    /** The recap that belongs to this death screen, or null while waiting for it */
    private Snapshot data() {
        Snapshot s = ClientState.currentDeath;
        if (s == null) return null;
        if (s.time < openedAt - 15000) return null; // an older death
        return s;
    }

    private List<String> wrap(FontRenderer fr, String text, int width, int maxLines) {
        List<String> l = fr.listFormattedStringToWidth(text, width);
        if (l.size() > maxLines) {
            l = new ArrayList<>(l.subList(0, maxLines));
            String last = l.get(maxLines - 1);
            l.set(maxLines - 1, fr.trimStringToWidth(last, width - fr.getStringWidth("...")) + "...");
        }
        return l;
    }

    void render(GuiScreen gui, int mouseX, int mouseY) {
        RecapFormatter f = ClientState.formatter();
        if (!f.o.on(Flag.DEATH_SCREEN_PANEL)) return;
        WhmConfig.Client cfg = WhmConfig.client;
        FontRenderer fr = mc().fontRenderer;
        Snapshot s = data();
        boolean min = cfg.minimized;
        int inner = W - PAD * 2;

        // ---- content (panel space)
        List<String> top = new ArrayList<>();
        List<String> footer = new ArrayList<>();
        shown.clear();
        DamageRecord key = null;
        if (s == null) {
            top.add(TextFormatting.GRAY + I18n.format(ClientState.serverHasMod || mc().isSingleplayer() ? "whathitme.panel.waiting" : "whathitme.panel.nodata"));
        } else {
            key = s.key();
            String kill = key != null
                ? TextFormatting.DARK_RED + "☠ " + TextFormatting.RED + f.displayName(key).getFormattedText() + TextFormatting.RED
                    + (f.o.on(Flag.DAMAGE_AMOUNT) ? " [" + I18n.format("whathitme.hearts", RecapFormatter.hearts(key.taken())) + "]" : "")
                : TextFormatting.DARK_RED + "☠ " + TextFormatting.RED + I18n.format("whathitme.unknown");
            top.addAll(wrap(fr, kill, inner, 2));
            ITextComponent dm = f.deathMessage(s);
            if (dm != null) for (String l : wrap(fr, TextFormatting.GRAY + dm.getUnformattedText(), inner, 2)) top.add(TextFormatting.GRAY + l);
            if (key != null && f.o.on(Flag.DAMAGE_PROPERTIES) && (key.flags & DamageRecord.PROPERTY_MASK) != 0) {
                top.addAll(wrap(fr, TextFormatting.DARK_AQUA + f.properties(key.flags).getUnformattedText(), inner, 2));
            }
            shown.addAll(f.visible(s.records, 100));
            footer.add(f.total(s.records, s.time).getFormattedText());
            if (key != null && f.o.on(Flag.HINTS_AND_TIPS)) {
                for (String l : wrap(fr, TextFormatting.GREEN + "✎ " + f.tip(key).getUnformattedText(), inner, 3)) footer.add(TextFormatting.GREEN + l);
            }
            if (key != null && key.has(DamageRecord.ESTIMATED)) footer.add(TextFormatting.GOLD + I18n.format("whathitme.badge.estimated") + " " + TextFormatting.GRAY + I18n.format("whathitme.panel.estimated"));
        }
        int visible = Math.min(cfg.maxVisibleLines, shown.size());
        int maxScroll = Math.max(0, shown.size() - visible);
        if (scroll > maxScroll) scroll = maxScroll;
        if (scroll < 0) scroll = 0;
        int h = HEADER;
        if (!min) {
            h += PAD + top.size() * LINE + (shown.isEmpty() ? 0 : 3 + visible * LINE) + 3 + footer.size() * LINE + PAD;
        }

        // ---- position
        sc = cfg.scale;
        int pw = Math.round(W * sc);
        ph = Math.round(h * sc);
        int[] base = base(cfg.anchorPoint, gui.width, gui.height, pw, ph);
        baseX = base[0];
        baseY = base[1];
        if (dragging) {
            if (!Mouse.isButtonDown(0)) {
                endDrag();
            } else {
                dragX = mouseX - dragDX;
                dragY = mouseY - dragDY;
            }
        }
        int x = dragging ? dragX : baseX + cfg.offsetX;
        int y = dragging ? dragY : baseY + cfg.offsetY;
        x = Math.max(0, Math.min(gui.width - pw, x));
        y = Math.max(0, Math.min(gui.height - ph, y));
        px = x;
        py = y;

        // ---- draw
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 300f);
        GlStateManager.scale(sc, sc, 1f);
        int alpha = Math.round(Math.max(0f, Math.min(1f, cfg.backgroundOpacity)) * 255f);
        Gui.drawRect(0, 0, W, h, (alpha << 24) | 0x100808);
        Gui.drawRect(0, 0, W, HEADER, (Math.max(alpha, 160) << 24) | 0x6B1010);
        Gui.drawRect(0, h - 1, W, h, 0x80FF5555);
        fr.drawStringWithShadow(TextFormatting.GOLD + "❤ " + TextFormatting.BOLD + I18n.format("whathitme.panel.title"), PAD, 3, 0xFFFFFF);
        int lx = (int) ((mouseX - x) / sc);
        int ly = (int) ((mouseY - y) / sc);
        String minLabel = min ? "[+]" : "[-]";
        int minX = W - PAD - fr.getStringWidth(minLabel);
        fr.drawStringWithShadow(minLabel, minX, 3, hover(lx, ly, minX, 0, W, HEADER) ? 0xFFFF55 : 0xDDDDDD);
        String copy = "[" + I18n.format("whathitme.panel.copy") + "]";
        int copyX = minX - 4 - fr.getStringWidth(copy);
        boolean canCopy = s != null && f.o.on(Flag.ACTION_BUTTONS);
        if (canCopy) fr.drawStringWithShadow(copy, copyX, 3, hover(lx, ly, copyX, 0, minX - 2, HEADER) ? 0xFFFF55 : 0x55FFFF);
        DamageRecord hovered = null;
        if (!min) {
            int cy = HEADER + PAD;
            for (String l : top) {
                fr.drawStringWithShadow(l, PAD, cy, 0xFFFFFF);
                cy += LINE;
            }
            if (!shown.isEmpty()) {
                cy += 1;
                Gui.drawRect(PAD, cy, W - PAD, cy + 1, 0x40FFFFFF);
                cy += 2;
                linesTop = cy;
                long ref = s.time;
                for (int i = 0; i < visible; i++) {
                    DamageRecord r = shown.get(scroll + i);
                    boolean over = hover(lx, ly, 0, cy, W, cy + LINE);
                    if (over) {
                        Gui.drawRect(1, cy - 1, W - 1, cy + LINE - 1, 0x30FFFFFF);
                        hovered = r;
                    }
                    fr.drawStringWithShadow(fr.trimStringToWidth(panelLine(f, r, ref), inner - (maxScroll > 0 ? 4 : 0)), PAD, cy, 0xFFFFFF);
                    cy += LINE;
                }
                if (maxScroll > 0) {
                    int trackTop = linesTop;
                    int trackH = visible * LINE - 1;
                    int barH = Math.max(6, trackH * visible / shown.size());
                    int barY = trackTop + (trackH - barH) * scroll / maxScroll;
                    Gui.drawRect(W - 3, trackTop, W - 1, trackTop + trackH, 0x40FFFFFF);
                    Gui.drawRect(W - 3, barY, W - 1, barY + barH, 0xC0FFFFFF);
                }
            }
            cy += 1;
            Gui.drawRect(PAD, cy, W - PAD, cy + 1, 0x40FFFFFF);
            cy += 2;
            for (String l : footer) {
                fr.drawStringWithShadow(l, PAD, cy, 0xFFFFFF);
                cy += LINE;
            }
        }
        GlStateManager.popMatrix();

        if (System.currentTimeMillis() < flashUntil) {
            fr.drawStringWithShadow(flash, x, Math.min(gui.height - 10, y + ph + 2), 0x55FF55);
        }
        if (hovered != null && !dragging) {
            List<String> tip = new ArrayList<>();
            for (ITextComponent c : f.hoverLines(hovered)) tip.add(c.getFormattedText());
            GlStateManager.pushMatrix();
            GlStateManager.translate(0, 0, 310f);
            GuiUtils.drawHoveringText(tip, mouseX, mouseY, gui.width, gui.height, 260, fr);
            GlStateManager.popMatrix();
        }
    }

    private static String panelLine(RecapFormatter f, DamageRecord r, long ref) {
        StringBuilder sb = new StringBuilder();
        if (r.has(DamageRecord.KILLING)) sb.append(TextFormatting.DARK_RED).append("☠ ");
        if (f.o.on(Flag.TIMESTAMP)) sb.append(TextFormatting.DARK_GRAY).append('-').append(RecapFormatter.age(ref - r.time)).append(' ');
        sb.append(TextFormatting.WHITE).append(f.displayName(r).getFormattedText());
        if (f.o.on(Flag.DAMAGE_AMOUNT)) sb.append(TextFormatting.RED).append(" -").append(I18n.format("whathitme.hearts", RecapFormatter.hearts(r.taken())));
        if (f.o.on(Flag.HIT_COUNT) && r.hits > 1) sb.append(TextFormatting.YELLOW).append(" x").append(r.hits);
        if (r.has(DamageRecord.TOTEM)) sb.append(TextFormatting.GOLD).append(" ★");
        if (f.o.on(Flag.ATTACKER_INVISIBILITY) && r.has(DamageRecord.INVISIBLE)) sb.append(TextFormatting.LIGHT_PURPLE).append(" ✦");
        return sb.toString();
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

    /** @return true when the event was used by the panel */
    boolean mouse(GuiScreen gui) {
        if (!ClientState.on(Flag.DEATH_SCREEN_PANEL)) return false;
        Minecraft mc = mc();
        int mx = Mouse.getEventX() * gui.width / mc.displayWidth;
        int my = gui.height - Mouse.getEventY() * gui.height / mc.displayHeight - 1;
        int lx = (int) ((mx - px) / sc);
        int ly = (int) ((my - py) / sc);
        boolean inside = mx >= px && mx < px + Math.round(W * sc) && my >= py && my < py + ph;
        int button = Mouse.getEventButton();
        boolean down = Mouse.getEventButtonState();
        int wheel = Mouse.getEventDWheel();
        if (button == 0 && !down && dragging) {
            px = Math.max(0, Math.min(gui.width - Math.round(W * sc), mx - dragDX));
            py = Math.max(0, Math.min(gui.height - ph, my - dragDY));
            endDrag();
            return true;
        }
        if (!inside) return false;
        if (wheel != 0) {
            scroll += wheel > 0 ? -1 : 1;
            return true;
        }
        if (button == 0 && down) {
            FontRenderer fr = mc.fontRenderer;
            String minLabel = WhmConfig.client.minimized ? "[+]" : "[-]";
            int minX = W - PAD - fr.getStringWidth(minLabel);
            String copy = "[" + I18n.format("whathitme.panel.copy") + "]";
            int copyX = minX - 4 - fr.getStringWidth(copy);
            if (ly < HEADER && lx >= minX - 2) {
                WhmConfig.client.saveLayout(WhmConfig.client.offsetX, WhmConfig.client.offsetY, !WhmConfig.client.minimized);
                return true;
            }
            if (ly < HEADER && lx >= copyX - 2 && data() != null && ClientState.on(Flag.ACTION_BUTTONS)) {
                copyRecap();
                return true;
            }
            if (ly < HEADER) {
                dragging = true;
                dragDX = mx - px;
                dragDY = my - py;
                dragX = px;
                dragY = py;
                return true;
            }
            return true; // do not click through the panel
        }
        return button >= 0 && inside;
    }

    void copyRecap() {
        Snapshot s = data();
        if (s == null) return;
        GuiScreen.setClipboardString(RecapFormatter.plain(new RecapFormatter(Msg.LOCAL, ClientState.options()).fullRecap(s, System.currentTimeMillis(), null)));
        flash = I18n.format("whathitme.copyrecap.done");
        flashUntil = System.currentTimeMillis() + 2500;
    }
}
