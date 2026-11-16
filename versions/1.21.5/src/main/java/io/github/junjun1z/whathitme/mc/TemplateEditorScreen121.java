package io.github.junjun1z.whathitme.mc;

import com.mojang.blaze3d.platform.InputConstants;

import io.github.junjun1z.whathitme.core.client.TemplateEditor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Screen host of the core template editor on 1.21.5: forwards drawing, mouse, keys and typed characters. */
public final class TemplateEditorScreen121 extends Screen {

    private final Screen parent;
    public final TemplateEditor editor = new TemplateEditor();

    public TemplateEditorScreen121(Screen parent) {
        super(Component.literal("What Hit Me? Templates"));
        this.parent = parent;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        editor.render(new Gfx121(g), width, height, mouseX, mouseY);
        if (editor.wantsClose()) onClose();
    }

    @Override
    public void onClose() {
        editor.closed();
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public void removed() {
        editor.closed();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return editor.mouseDown(button, mouseX, mouseY, hasShiftDown()) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double h, double v) {
        return v != 0 && editor.scroll(mouseX, mouseY, v);
    }

    private static int map(int key) {
        switch (key) {
            case InputConstants.KEY_BACKSPACE:
                return TemplateEditor.K_BACKSPACE;
            case InputConstants.KEY_DELETE:
                return TemplateEditor.K_DELETE;
            case InputConstants.KEY_LEFT:
                return TemplateEditor.K_LEFT;
            case InputConstants.KEY_RIGHT:
                return TemplateEditor.K_RIGHT;
            case InputConstants.KEY_UP:
                return TemplateEditor.K_UP;
            case InputConstants.KEY_DOWN:
                return TemplateEditor.K_DOWN;
            case InputConstants.KEY_HOME:
                return TemplateEditor.K_HOME;
            case InputConstants.KEY_END:
                return TemplateEditor.K_END;
            case InputConstants.KEY_RETURN:
            case InputConstants.KEY_NUMPADENTER:
                return TemplateEditor.K_ENTER;
            case InputConstants.KEY_TAB:
                return TemplateEditor.K_TAB;
            case InputConstants.KEY_ESCAPE:
                return TemplateEditor.K_ESCAPE;
            case InputConstants.KEY_A:
                return TemplateEditor.K_A;
            case InputConstants.KEY_C:
                return TemplateEditor.K_C;
            case InputConstants.KEY_V:
                return TemplateEditor.K_V;
            case InputConstants.KEY_X:
                return TemplateEditor.K_X;
            default:
                return 0;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int k = map(keyCode);
        if (k != 0 && editor.key(k, hasControlDown(), hasShiftDown())) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (c >= 32 && c != 127 && c != '\u00a7') return editor.typed(c);
        return false;
    }
}
