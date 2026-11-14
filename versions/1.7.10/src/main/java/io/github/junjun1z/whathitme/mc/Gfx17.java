package io.github.junjun1z.whathitme.mc;

import java.util.List;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import io.github.junjun1z.whathitme.core.client.Gfx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;

/** Death screen drawing on 1.7.10 (GL11, Gui.drawRect, FontRenderer, a vanilla-style tooltip). */
public final class Gfx17 implements Gfx {

    public static final Gfx17 INSTANCE = new Gfx17();

    private Gfx17() {}

    private static FontRenderer font() {
        return Minecraft.getMinecraft().fontRenderer;
    }

    @Override
    public int width(String s) {
        return font().getStringWidth(s);
    }

    @Override
    public void fill(int x0, int y0, int x1, int y1, int argb) {
        Gui.drawRect(x0, y0, x1, y1, argb);
    }

    @Override
    public void text(String s, int x, int y, int rgb) {
        font().drawStringWithShadow(s, x, y, rgb);
    }

    @Override
    public void push() {
        GL11.glPushMatrix();
    }

    @Override
    public void translate(float x, float y, float z) {
        GL11.glTranslatef(x, y, z);
    }

    @Override
    public void scale(float s) {
        GL11.glScalef(s, s, 1f);
    }

    @Override
    public void pop() {
        GL11.glPopMatrix();
    }

    /** Like GuiScreen#drawHoveringText (1.7.10 Forge has no GuiUtils.drawHoveringText): wrapped, kept on screen */
    @Override
    public void tooltip(List<String> lines, int mouseX, int mouseY, int screenW, int screenH) {
        if (lines.isEmpty()) return;
        FontRenderer f = font();
        List<String> wrapped = new java.util.ArrayList<>();
        for (String l : lines) {
            for (Object o : f.listFormattedStringToWidth(l, 260)) wrapped.add((String) o);
        }
        int w = 0;
        for (String l : wrapped) w = Math.max(w, f.getStringWidth(l));
        int h = wrapped.size() == 1 ? 8 : 8 + (wrapped.size() - 1) * 10;
        int x = mouseX + 12;
        int y = mouseY - 12;
        if (x + w + 4 > screenW) x = Math.max(4, mouseX - 16 - w);
        if (y + h + 6 > screenH) y = screenH - h - 6;
        if (y < 4) y = 4;
        GL11.glPushMatrix();
        GL11.glTranslatef(0, 0, 310f);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        int bg = 0xF0100010;
        Gui.drawRect(x - 3, y - 4, x + w + 3, y - 3, bg);
        Gui.drawRect(x - 3, y + h + 3, x + w + 3, y + h + 4, bg);
        Gui.drawRect(x - 3, y - 3, x + w + 3, y + h + 3, bg);
        Gui.drawRect(x - 4, y - 3, x - 3, y + h + 3, bg);
        Gui.drawRect(x + w + 3, y - 3, x + w + 4, y + h + 3, bg);
        int b = 0x505000FF;
        Gui.drawRect(x - 3, y - 2, x - 2, y + h + 2, b);
        Gui.drawRect(x + w + 2, y - 2, x + w + 3, y + h + 2, b);
        Gui.drawRect(x - 3, y - 3, x + w + 3, y - 2, b);
        Gui.drawRect(x - 3, y + h + 2, x + w + 3, y + h + 3, b);
        for (int i = 0; i < wrapped.size(); i++) {
            f.drawStringWithShadow(wrapped.get(i), x, y, 0xFFFFFF);
            y += i == 0 ? 10 : 10;
        }
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glPopMatrix();
    }

    @Override
    public int guiScale() {
        Minecraft mc = Minecraft.getMinecraft();
        return new ScaledResolution(mc, mc.displayWidth, mc.displayHeight).getScaleFactor();
    }

    @Override
    public boolean leftDown() {
        return Mouse.isButtonDown(0);
    }
}
