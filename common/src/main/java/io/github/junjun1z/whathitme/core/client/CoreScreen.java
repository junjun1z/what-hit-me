package io.github.junjun1z.whathitme.core.client;

/**
 * A screen drawn and driven entirely by the core (template editor, pack editor). A version only needs one thin host
 * screen that forwards drawing, mouse, wheel, keys (mapped to the {@link TemplateEditor} K_* codes) and typed
 * characters, and goes back to its parent when {@link #wantsClose()} is true.
 */
public interface CoreScreen {

    void render(Gfx g, int width, int height, int mouseX, int mouseY);

    boolean mouseDown(int button, double x, double y, boolean shift);

    boolean scroll(double x, double y, double delta);

    boolean key(int key, boolean ctrl, boolean shift);

    boolean typed(char c);

    boolean wantsClose();

    /** The host screen was removed */
    void closed();
}
