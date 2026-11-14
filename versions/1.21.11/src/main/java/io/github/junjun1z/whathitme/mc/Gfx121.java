package io.github.junjun1z.whathitme.mc;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import io.github.junjun1z.whathitme.core.client.Gfx;
import io.github.junjun1z.whathitme.core.client.LegacyText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;

/**
 * Death screen drawing on 1.21.6 - 1.21.11 (GuiGraphics with deferred rendering in strata, 2D pose stack). One instance
 * per frame.
 */
public final class Gfx121 implements Gfx {

    private final GuiGraphics g;
    private final Font font = Minecraft.getInstance().font;

    public Gfx121(GuiGraphics g) {
        this.g = g;
    }

    @Override
    public int width(String s) {
        return font.width(s);
    }

    @Override
    public void fill(int x0, int y0, int x1, int y1, int argb) {
        g.fill(x0, y0, x1, y1, argb);
    }

    @Override
    public void text(String s, int x, int y, int rgb) {
        g.drawString(font, s, x, y, 0xFF000000 | rgb, true);
    }

    @Override
    public void push() {
        g.pose().pushMatrix();
    }

    @Override
    public void translate(float x, float y, float z) {
        // no depth any more: a raised layer is a new stratum (drawn over everything before it)
        if (z > 0) g.nextStratum();
        g.pose().translate(x, y);
    }

    @Override
    public void scale(float s) {
        g.pose().scale(s, s);
    }

    @Override
    public void pop() {
        g.pose().popMatrix();
    }

    @Override
    public void tooltip(List<String> lines, int mouseX, int mouseY, int screenW, int screenH) {
        List<ClientTooltipComponent> c = new ArrayList<>();
        for (String l : lines) for (String w : LegacyText.wrap(this, l, 260)) c.add(ClientTooltipComponent.create(Component.literal(w).getVisualOrderText()));
        if (c.isEmpty()) return;
        // setTooltipForNextFrame() is only drawn by the screen's own render pass, which is over when the loaders'
        // "after render" events run: draw the tooltip right away on a new stratum instead (what the deferred pass does)
        g.nextStratum();
        g.renderTooltip(font, c, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
    }

    @Override
    public int guiScale() {
        return (int) Math.round(Minecraft.getInstance().getWindow().getGuiScale());
    }

    @Override
    public boolean leftDown() {
        return GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
    }
}
