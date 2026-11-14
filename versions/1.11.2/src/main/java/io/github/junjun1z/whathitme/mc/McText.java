package io.github.junjun1z.whathitme.mc;

import io.github.junjun1z.whathitme.core.text.Fmt;
import io.github.junjun1z.whathitme.core.text.Lang;
import io.github.junjun1z.whathitme.core.text.Txt;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

/** Txt (core text model) to Minecraft 1.11 ITextComponent, and the 1.11 translation lookup. */
public final class McText {

    private McText() {}

    public static ITextComponent of(Txt t) {
        ITextComponent c;
        switch (t.kind) {
            case TRANSLATE: {
                Object[] a = t.args();
                for (int i = 0; i < a.length; i++) {
                    if (a[i] instanceof Txt) a[i] = of((Txt) a[i]);
                    else a[i] = String.valueOf(a[i]);
                }
                c = new TextComponentTranslation(t.text, a);
                break;
            }
            case JSON: {
                ITextComponent j = null;
                try {
                    j = ITextComponent.Serializer.jsonToComponent(t.text);
                } catch (Exception ignored) {
                    // unreadable message
                }
                c = j == null ? new TextComponentString("") : j;
                break;
            }
            default:
                c = new TextComponentString(t.text);
        }
        Style s = c.getStyle();
        if (t.color() != null) s.setColor(color(t.color()));
        if (t.bold() != null) s.setBold(t.bold());
        if (t.underlined() != null) s.setUnderlined(t.underlined());
        if (t.clickAction() != null) {
            s.setClickEvent(new ClickEvent(t.clickAction() == Txt.Click.RUN_COMMAND ? ClickEvent.Action.RUN_COMMAND : ClickEvent.Action.SUGGEST_COMMAND,
                t.clickValue()));
        }
        if (t.hoverText() != null) s.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, of(t.hoverText())));
        for (Txt sib : t.siblings()) c.appendSibling(of(sib));
        return c;
    }

    public static TextFormatting color(Fmt f) {
        return TextFormatting.valueOf(f.name());
    }

    /** The client's current language (and the server's English on a dedicated server) */
    @SuppressWarnings("deprecation")
    public static final Lang LANG = new Lang() {
        @Override
        public String pattern(String key) {
            if (key == null || key.isEmpty()) return null;
            return net.minecraft.util.text.translation.I18n.canTranslate(key) ? net.minecraft.util.text.translation.I18n.translateToLocal(key) : null;
        }

        @Override
        public String jsonText(String json) {
            try {
                ITextComponent c = ITextComponent.Serializer.jsonToComponent(json);
                return c == null ? "" : c.getUnformattedText();
            } catch (Exception e) {
                return "";
            }
        }
    };
}
