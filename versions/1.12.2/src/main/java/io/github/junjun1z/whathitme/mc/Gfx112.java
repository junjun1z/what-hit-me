package io.github.junjun1z.whathitme.mc;

import java.util.List;

import org.lwjgl.input.Mouse;

import io.github.junjun1z.whathitme.core.client.Gfx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.client.config.GuiUtils;

/** Death screen drawing on 1.12 (GlStateManager, Gui.drawRect, FontRenderer, Forge's tooltip). */
public final class Gfx112 implements Gfx {

    public static final Gfx112 INSTANCE = new Gfx112();

    private Gfx112() {}

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
        GlStateManager.pushMatrix();
    }

    @Override
    public void translate(float x, float y, float z) {
        GlStateManager.translate(x, y, z);
    }

    @Override
    public void scale(float s) {
        GlStateManager.scale(s, s, 1f);
    }

    @Override
    public void pop() {
        GlStateManager.popMatrix();
    }

    @Override
    public void tooltip(List<String> lines, int mouseX, int mouseY, int screenW, int screenH) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(0, 0, 310f);
        GuiUtils.drawHoveringText(lines, mouseX, mouseY, screenW, screenH, 260, font());
        GlStateManager.popMatrix();
    }

    @Override
    public int guiScale() {
        return new ScaledResolution(Minecraft.getMinecraft()).getScaleFactor();
    }

    @Override
    public boolean leftDown() {
        return Mouse.isButtonDown(0);
    }
}
