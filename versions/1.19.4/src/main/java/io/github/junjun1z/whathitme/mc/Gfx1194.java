package io.github.junjun1z.whathitme.mc;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.vertex.PoseStack;

import io.github.junjun1z.whathitme.core.client.Gfx;
import io.github.junjun1z.whathitme.core.client.LegacyText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Death screen drawing on 1.19.4 (PoseStack + GuiComponent, before GuiGraphics). One instance per frame. */
public final class Gfx1194 implements Gfx {

    private final PoseStack ps;
    private final Screen screen;
    private final Font font = Minecraft.getInstance().font;

    public Gfx1194(PoseStack ps, Screen screen) {
        this.ps = ps;
        this.screen = screen;
    }

    @Override
    public int width(String s) {
        return font.width(s);
    }

    @Override
    public void fill(int x0, int y0, int x1, int y1, int argb) {
        GuiComponent.fill(ps, x0, y0, x1, y1, argb);
    }

    @Override
    public void text(String s, int x, int y, int rgb) {
        font.drawShadow(ps, s, x, y, 0xFF000000 | rgb);
    }

    @Override
    public void push() {
        ps.pushPose();
    }

    @Override
    public void translate(float x, float y, float z) {
        ps.translate(x, y, z);
    }

    @Override
    public void scale(float s) {
        ps.scale(s, s, 1f);
    }

    @Override
    public void pop() {
        ps.popPose();
    }

    @Override
    public void tooltip(List<String> lines, int mouseX, int mouseY, int screenW, int screenH) {
        List<Component> c = new ArrayList<>();
        for (String l : lines) for (String w : LegacyText.wrap(this, l, 260)) c.add(McText.literal(w));
        ps.pushPose();
        ps.translate(0, 0, 310f);
        screen.renderComponentTooltip(ps, c, mouseX, mouseY);
        ps.popPose();
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
