package io.github.junjun1z.whathitme.mc;

import io.github.junjun1z.whathitme.core.text.Lang;
import io.github.junjun1z.whathitme.core.text.Txt;
import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

/** Txt (core text model) to Minecraft 1.15.2 Components, and the translation lookup. */
public final class McText {

    private McText() {}

    public static Component of(Txt t) {
        Component c;
        switch (t.kind) {
            case TRANSLATE: {
                Object[] a = t.args();
                for (int i = 0; i < a.length; i++) {
                    if (a[i] instanceof Txt) a[i] = of((Txt) a[i]);
                    else a[i] = String.valueOf(a[i]);
                }
                c = new TranslatableComponent(t.text, a);
                break;
            }
            case JSON: {
                Component j = fromJson(t.text);
                c = j == null ? new TextComponent("") : j.deepCopy();
                break;
            }
            default:
                c = new TextComponent(t.text);
        }
        // Style is mutable on this version
        Style s = c.getStyle();
        if (t.color() != null) s.setColor(ChatFormatting.valueOf(t.color().name()));
        if (t.bold() != null) s.setBold(t.bold());
        if (Boolean.TRUE.equals(t.underlined())) s.setUnderlined(Boolean.TRUE);
        if (t.clickAction() != null) {
            s.setClickEvent(new ClickEvent(t.clickAction() == Txt.Click.RUN_COMMAND ? ClickEvent.Action.RUN_COMMAND : ClickEvent.Action.SUGGEST_COMMAND,
                t.clickValue()));
        }
        if (t.hoverText() != null) s.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, of(t.hoverText())));
        for (Txt sib : t.siblings()) c.append(of(sib));
        return c;
    }

    public static Component fromJson(String json) {
        try {
            return Component.Serializer.fromJson(json);
        } catch (Exception e) {
            return null;
        }
    }

    public static String toJson(Component c) {
        return Component.Serializer.toJson(c);
    }

    /** {translation key, plain text of argument 2} of a translatable JSON text, or null */
    public static String[] translationOf(String json) {
        Component c = fromJson(json);
        if (!(c instanceof TranslatableComponent)) return null;
        TranslatableComponent t = (TranslatableComponent) c;
        Object[] args = t.getArgs();
        String attacker = args.length > 1 ? (args[1] instanceof Component ? ((Component) args[1]).getString() : String.valueOf(args[1])) : "";
        return new String[] {t.getKey(), attacker};
    }

    /** The current language (client language, or English on a server) */
    public static final Lang LANG = new Lang() {
        @Override
        public String pattern(String key) {
            if (key == null || key.isEmpty()) return null;
            // the client pushes its selected language into this instance (Language#forceData)
            String s = Language.getInstance().getElement(key);
            return s == null || s.equals(key) ? null : s;
        }

        @Override
        public String jsonText(String json) {
            Component c = fromJson(json);
            return c == null ? "" : c.getString();
        }
    };
}
