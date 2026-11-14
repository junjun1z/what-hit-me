package io.github.junjun1z.whathitme.mc;

import io.github.junjun1z.whathitme.core.text.Fmt;
import io.github.junjun1z.whathitme.core.text.Lang;
import io.github.junjun1z.whathitme.core.text.Txt;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;

/** Txt (core text model) to Minecraft 1.8 IChatComponent, and the 1.8 translation lookup. */
public final class McText {

    private McText() {}

    public static IChatComponent of(Txt t) {
        IChatComponent c;
        switch (t.kind) {
            case TRANSLATE: {
                Object[] a = t.args();
                for (int i = 0; i < a.length; i++) {
                    if (a[i] instanceof Txt) a[i] = of((Txt) a[i]);
                    else a[i] = String.valueOf(a[i]);
                }
                c = new ChatComponentTranslation(t.text, a);
                break;
            }
            case JSON: {
                IChatComponent j = null;
                try {
                    j = IChatComponent.Serializer.jsonToComponent(t.text);
                } catch (Exception ignored) {
                    // unreadable message
                }
                c = j == null ? new ChatComponentText("") : j;
                break;
            }
            default:
                c = new ChatComponentText(t.text);
        }
        ChatStyle s = c.getChatStyle();
        if (t.color() != null) s.setColor(color(t.color()));
        if (t.bold() != null) s.setBold(t.bold());
        if (t.underlined() != null) s.setUnderlined(t.underlined());
        if (t.clickAction() != null) {
            s.setChatClickEvent(new ClickEvent(t.clickAction() == Txt.Click.RUN_COMMAND ? ClickEvent.Action.RUN_COMMAND : ClickEvent.Action.SUGGEST_COMMAND,
                t.clickValue()));
        }
        if (t.hoverText() != null) s.setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, of(t.hoverText())));
        for (Txt sib : t.siblings()) c.appendSibling(of(sib));
        return c;
    }

    public static EnumChatFormatting color(Fmt f) {
        return EnumChatFormatting.valueOf(f.name());
    }

    /** The client's current language (and the server's English on a dedicated server) */
    @SuppressWarnings("deprecation")
    public static final Lang LANG = new Lang() {
        @Override
        public String pattern(String key) {
            if (key == null || key.isEmpty()) return null;
            return net.minecraft.util.StatCollector.canTranslate(key) ? net.minecraft.util.StatCollector.translateToLocal(key) : null;
        }

        @Override
        public String jsonText(String json) {
            try {
                IChatComponent c = IChatComponent.Serializer.jsonToComponent(json);
                return c == null ? "" : c.getUnformattedText();
            } catch (Exception e) {
                return "";
            }
        }
    };
}
