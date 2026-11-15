package io.github.junjun1z.whathitme.mc;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.client.Gfx;
import io.github.junjun1z.whathitme.core.client.LegacyText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;

/**
 * Death screen drawing on 26.x (GuiGraphicsExtractor: deferred rendering in strata, 2D pose stack). One instance per
 * frame.
 */
public final class Gfx121 implements Gfx {

    private final GuiGraphicsExtractor g;
    private final Font font = Minecraft.getInstance().font;

    public Gfx121(GuiGraphicsExtractor g) {
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
        g.text(font, s, x, y, 0xFF000000 | rgb, true);
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

    private static Method tooltip;
    private static boolean tooltipBroken;

    /** GuiGraphicsExtractor.tooltip(Font, List, x, y, positioner, style[, extraSpaceAfterFirstLine]): 26.3 added the flag */
    private static Method tooltipMethod() {
        if (tooltipBroken) return null;
        if (tooltip == null) {
            for (Method m : GuiGraphicsExtractor.class.getMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (m.getName().equals("tooltip") && (p.length == 6 || p.length == 7) && p[0] == Font.class && p[1] == List.class) tooltip = m;
            }
            if (tooltip == null) tooltipBroken = true;
        }
        return tooltip;
    }

    @Override
    public void tooltip(List<String> lines, int mouseX, int mouseY, int screenW, int screenH) {
        List<Component> text = new ArrayList<>();
        for (String l : lines) for (String w : LegacyText.wrap(this, l, 260)) text.add(Component.literal(w));
        if (text.isEmpty()) return;
        // setTooltipForNextFrame() is only drawn by the screen's own pass, which is over when the loaders' "after
        // render" events run: draw the tooltip right away on a new stratum instead (what the deferred pass does)
        Method m = tooltipMethod();
        if (m != null) {
            List<ClientTooltipComponent> c = new ArrayList<>();
            for (Component t : text) c.add(ClientTooltipComponent.create(t.getVisualOrderText()));
            try {
                g.nextStratum();
                if (m.getParameterCount() == 7) m.invoke(g, font, c, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null, false);
                else m.invoke(g, font, c, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
                return;
            } catch (Throwable t) {
                tooltipBroken = true;
                Platform.log.warn("Death panel tooltips: direct drawing failed, using the deferred tooltip", t);
            }
        }
        g.setComponentTooltipForNextFrame(font, text, mouseX, mouseY);
    }

    @Override
    public int guiScale() {
        return (int) Math.round(Minecraft.getInstance().getWindow().getGuiScale());
    }

    private static Method mouseState;
    private static boolean sdl;

    @Override
    public boolean leftDown() {
        // ask the window library: SDL in 26.2+, GLFW before
        try {
            if (mouseState == null) {
                try {
                    mouseState = Class.forName("org.lwjgl.sdl.SDLMouse").getMethod("SDL_GetMouseState", java.nio.FloatBuffer.class, java.nio.FloatBuffer.class);
                    sdl = true;
                } catch (ClassNotFoundException e) {
                    mouseState = Class.forName("org.lwjgl.glfw.GLFW").getMethod("glfwGetMouseButton", long.class, int.class);
                    sdl = false;
                }
            }
            if (sdl) return ((Integer) mouseState.invoke(null, null, null) & 1) != 0;
            return (Integer) mouseState.invoke(null, Minecraft.getInstance().getWindow().handle(), 0) == 1;
        } catch (Throwable t) {
            return true; // unknown: never end a drag early
        }
    }
}
