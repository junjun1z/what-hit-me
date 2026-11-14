package io.github.junjun1z.whathitme.mc;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.systems.RenderSystem;

import io.github.junjun1z.whathitme.core.client.Gfx;
import io.github.junjun1z.whathitme.core.client.LegacyText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;

/** Death screen drawing on 1.15 (GL matrix stack, no PoseStack; tooltips through the screen). One instance per frame. */
public final class Gfx115 implements Gfx {

    private final Screen screen;
    private final Font font = Minecraft.getInstance().font;

    public Gfx115(Screen screen) {
        this.screen = screen;
    }

    @Override
    public int width(String s) {
        return font.width(s);
    }

    @Override
    public void fill(int x0, int y0, int x1, int y1, int argb) {
        GuiComponent.fill(x0, y0, x1, y1, argb);
        RenderSystem.color4f(1f, 1f, 1f, 1f);
    }

    @Override
    public void text(String s, int x, int y, int rgb) {
        font.drawShadow(s, x, y, 0xFF000000 | rgb);
        RenderSystem.color4f(1f, 1f, 1f, 1f);
    }

    @Override
    public void push() {
        RenderSystem.pushMatrix();
    }

    @Override
    public void translate(float x, float y, float z) {
        RenderSystem.translatef(x, y, z);
    }

    @Override
    public void scale(float s) {
        RenderSystem.scalef(s, s, 1f);
    }

    @Override
    public void pop() {
        RenderSystem.popMatrix();
    }

    @Override
    public void tooltip(List<String> lines, int mouseX, int mouseY, int screenW, int screenH) {
        List<String> c = new ArrayList<>();
        for (String l : lines) c.addAll(LegacyText.wrap(this, l, 260));
        RenderSystem.pushMatrix();
        RenderSystem.translatef(0f, 0f, 310f);
        screen.renderTooltip(c, mouseX, mouseY);
        RenderSystem.disableLighting();
        RenderSystem.color4f(1f, 1f, 1f, 1f);
        RenderSystem.popMatrix();
    }

    @Override
    public int guiScale() {
        return (int) Math.round(Minecraft.getInstance().getWindow().getGuiScale());
    }

    @Override
    public boolean leftDown() {
        return GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
    }
}
