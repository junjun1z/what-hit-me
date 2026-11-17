package io.github.junjun1z.whathitme.mc;

import io.github.junjun1z.whathitme.core.text.Lang;
import io.github.junjun1z.whathitme.core.text.Txt;
import java.util.function.Supplier;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;

/** Txt (core text model) to Minecraft 1.21.10 Components, and the translation lookup. */
public final class McText {

    /** Registries for JSON texts with item / entity data: the server's or the client's, set by the loader */
    public static volatile Supplier<HolderLookup.Provider> registries = () -> null;

    private McText() {}

    private static HolderLookup.Provider provider() {
        HolderLookup.Provider p = null;
        try {
            p = registries.get();
        } catch (Throwable ignored) {
            // none
        }
        return p != null ? p : RegistryAccess.EMPTY;
    }

    public static MutableComponent of(Txt t) {
        MutableComponent c;
        switch (t.kind) {
            case TRANSLATE: {
                Object[] a = t.args();
                for (int i = 0; i < a.length; i++) {
                    if (a[i] instanceof Txt) a[i] = of((Txt) a[i]);
                    else a[i] = String.valueOf(a[i]);
                }
                c = t.fallback() != null ? Component.translatableWithFallback(t.text, t.fallback(), a) : Component.translatable(t.text, a);
                break;
            }
            case JSON: {
                Component j = fromJson(t.text);
                c = j == null ? Component.literal("") : j.copy();
                break;
            }
            default:
                c = Component.literal(t.text);
        }
        Style s = c.getStyle();
        if (t.color() != null) s = s.withColor(ChatFormatting.valueOf(t.color().name()));
        if (t.bold() != null) s = s.withBold(t.bold());
        if (t.underlined() != null) s = s.withUnderlined(t.underlined());
        if (t.clickAction() != null) {
            s = s.withClickEvent(t.clickAction() == Txt.Click.RUN_COMMAND ? new ClickEvent.RunCommand(t.clickValue()) : new ClickEvent.SuggestCommand(t.clickValue()));
        }
        if (t.hoverText() != null) s = s.withHoverEvent(new HoverEvent.ShowText(of(t.hoverText())));
        c.setStyle(s);
        for (Txt sib : t.siblings()) c.append(of(sib));
        return c;
    }

    public static Component fromJson(String json) {
        try {
            return ComponentSerialization.CODEC.parse(provider().createSerializationContext(JsonOps.INSTANCE), JsonParser.parseString(json)).result().orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    public static String toJson(Component c) {
        return ComponentSerialization.CODEC.encodeStart(provider().createSerializationContext(JsonOps.INSTANCE), c).result().map(JsonElement::toString).orElse("");
    }

    /** {translation key, plain text of argument 2} of a translatable JSON text, or null */
    public static String[] translationOf(String json) {
        Component c = fromJson(json);
        if (c == null) return null;
        ComponentContents cc = c.getContents();
        if (!(cc instanceof TranslatableContents)) return null;
        TranslatableContents t = (TranslatableContents) cc;
        Object[] args = t.getArgs();
        String attacker = args.length > 1 ? (args[1] instanceof Component ? ((Component) args[1]).getString() : String.valueOf(args[1])) : "";
        return new String[] {t.getKey(), attacker};
    }

    /** The current language (client language, or English on a server) */
    public static final Lang LANG = new Lang() {
        @Override
        public String pattern(String key) {
            if (key == null || key.isEmpty()) return null;
            Language l = Language.getInstance();
            return l.has(key) ? l.getOrDefault(key) : null;
        }

        @Override
        public String jsonText(String json) {
            Component c = fromJson(json);
            return c == null ? "" : c.getString();
        }
    };
}
