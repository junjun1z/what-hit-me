package io.github.junjun1z.whathitme.forge;

import java.io.IOException;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import io.github.junjun1z.whathitme.core.client.CoreScreen;
import io.github.junjun1z.whathitme.core.client.TemplateEditor;
import io.github.junjun1z.whathitme.mc.Gfx19;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Screen host of the core screens (template editor, pack editor) on 1.9.4: forwards drawing, mouse, keys and typed characters. */
@SideOnly(Side.CLIENT)
public final class GuiTemplateEditor extends GuiScreen {

    private final GuiScreen parent;
    /** The hosted screen */
    public final CoreScreen screen;
    /** The template editor when this hosts one, else null */
    public final TemplateEditor editor;

    public GuiTemplateEditor(GuiScreen parent) {
        this(parent, new TemplateEditor());
    }

    public GuiTemplateEditor(GuiScreen parent, CoreScreen screen) {
        this.parent = parent;
        this.screen = screen;
        this.editor = screen instanceof TemplateEditor ? (TemplateEditor) screen : null;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        screen.closed();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        screen.render(Gfx19.INSTANCE, width, height, mouseX, mouseY);
        if (screen.wantsClose()) mc.displayGuiScreen(parent);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        screen.mouseDown(button, mouseX, mouseY, isShiftKeyDown());
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            int mx = Mouse.getEventX() * width / mc.displayWidth;
            int my = height - Mouse.getEventY() * height / mc.displayHeight - 1;
            screen.scroll(mx, my, wheel);
        }
    }

    private static int map(int key) {
        switch (key) {
            case Keyboard.KEY_BACK:
                return TemplateEditor.K_BACKSPACE;
            case Keyboard.KEY_DELETE:
                return TemplateEditor.K_DELETE;
            case Keyboard.KEY_LEFT:
                return TemplateEditor.K_LEFT;
            case Keyboard.KEY_RIGHT:
                return TemplateEditor.K_RIGHT;
            case Keyboard.KEY_UP:
                return TemplateEditor.K_UP;
            case Keyboard.KEY_DOWN:
                return TemplateEditor.K_DOWN;
            case Keyboard.KEY_HOME:
                return TemplateEditor.K_HOME;
            case Keyboard.KEY_END:
                return TemplateEditor.K_END;
            case Keyboard.KEY_RETURN:
            case Keyboard.KEY_NUMPADENTER:
                return TemplateEditor.K_ENTER;
            case Keyboard.KEY_TAB:
                return TemplateEditor.K_TAB;
            case Keyboard.KEY_ESCAPE:
                return TemplateEditor.K_ESCAPE;
            case Keyboard.KEY_A:
                return TemplateEditor.K_A;
            case Keyboard.KEY_C:
                return TemplateEditor.K_C;
            case Keyboard.KEY_V:
                return TemplateEditor.K_V;
            case Keyboard.KEY_X:
                return TemplateEditor.K_X;
            default:
                return 0;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        int k = map(keyCode);
        if (k != 0 && screen.key(k, isCtrlKeyDown(), isShiftKeyDown())) return;
        if (ChatAllowedCharacters.isAllowedCharacter(typedChar)) screen.typed(typedChar);
    }
}
