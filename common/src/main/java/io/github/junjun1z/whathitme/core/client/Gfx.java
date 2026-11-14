package io.github.junjun1z.whathitme.core.client;

import java.util.List;

/**
 * Drawing on the death screen, implemented per Minecraft version (GlStateManager + Gui on 1.12, GuiGraphics on 1.20+).
 * Text uses legacy section-sign color codes. Coordinates are GUI pixels.
 */
public interface Gfx {

    /** Width of a string with formatting codes */
    int width(String s);

    void fill(int x0, int y0, int x1, int y1, int argb);

    /** Text with shadow */
    void text(String s, int x, int y, int rgb);

    void push();

    void translate(float x, float y, float z);

    void scale(float s);

    void pop();

    /** A tooltip box at the mouse, above everything else */
    void tooltip(List<String> lines, int mouseX, int mouseY, int screenW, int screenH);

    /** GUI scale factor (screen pixels per GUI pixel) */
    int guiScale();

    /** Is the left mouse button held right now? */
    boolean leftDown();
}
