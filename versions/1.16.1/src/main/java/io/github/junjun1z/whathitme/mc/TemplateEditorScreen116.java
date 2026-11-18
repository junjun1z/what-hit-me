package io.github.junjun1z.whathitme.mc;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.vertex.PoseStack;

import io.github.junjun1z.whathitme.core.client.CoreScreen;
import io.github.junjun1z.whathitme.core.client.TemplateEditor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;

/** Screen host of the core screens (template editor, pack editor) on 1.16.1: forwards drawing, mouse, keys and typed characters. */
public final class TemplateEditorScreen116 extends Screen {

    private final Screen parent;
    /** The hosted screen */
    public final CoreScreen screen;
    /** The template editor when this hosts one, else null */
    public final TemplateEditor editor;

    public TemplateEditorScreen116(Screen parent) {
        this(parent, new TemplateEditor());
    }

    public TemplateEditorScreen116(Screen parent, CoreScreen screen) {
        super(new TextComponent("What Hit Me?"));
        this.parent = parent;
        this.screen = screen;
        this.editor = screen instanceof TemplateEditor ? (TemplateEditor) screen : null;
    }

    @Override
    protected void init() {
        minecraft.keyboardHandler.setSendRepeatsToGui(true);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        renderBackground(pose);
        screen.render(new Gfx116(pose, this), width, height, mouseX, mouseY);
        if (screen.wantsClose()) onClose();
    }

    @Override
    public void onClose() {
        screen.closed();
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public void removed() {
        minecraft.keyboardHandler.setSendRepeatsToGui(false);
        screen.closed();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return screen.mouseDown(button, mouseX, mouseY, hasShiftDown()) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return delta != 0 && screen.scroll(mouseX, mouseY, delta);
    }

    private static int map(int key) {
        switch (key) {
            case GLFW.GLFW_KEY_BACKSPACE:
                return TemplateEditor.K_BACKSPACE;
            case GLFW.GLFW_KEY_DELETE:
                return TemplateEditor.K_DELETE;
            case GLFW.GLFW_KEY_LEFT:
                return TemplateEditor.K_LEFT;
            case GLFW.GLFW_KEY_RIGHT:
                return TemplateEditor.K_RIGHT;
            case GLFW.GLFW_KEY_UP:
                return TemplateEditor.K_UP;
            case GLFW.GLFW_KEY_DOWN:
                return TemplateEditor.K_DOWN;
            case GLFW.GLFW_KEY_HOME:
                return TemplateEditor.K_HOME;
            case GLFW.GLFW_KEY_END:
                return TemplateEditor.K_END;
            case GLFW.GLFW_KEY_ENTER:
            case GLFW.GLFW_KEY_KP_ENTER:
                return TemplateEditor.K_ENTER;
            case GLFW.GLFW_KEY_TAB:
                return TemplateEditor.K_TAB;
            case GLFW.GLFW_KEY_ESCAPE:
                return TemplateEditor.K_ESCAPE;
            case GLFW.GLFW_KEY_A:
                return TemplateEditor.K_A;
            case GLFW.GLFW_KEY_C:
                return TemplateEditor.K_C;
            case GLFW.GLFW_KEY_V:
                return TemplateEditor.K_V;
            case GLFW.GLFW_KEY_X:
                return TemplateEditor.K_X;
            default:
                return 0;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int k = map(keyCode);
        if (k != 0 && screen.key(k, hasControlDown(), hasShiftDown())) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (c >= 32 && c != 127 && c != '\u00a7') return screen.typed(c);
        return false;
    }
}
