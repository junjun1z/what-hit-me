package io.github.junjun1z.whathitme.core.text;

/**
 * Translation lookup of one side (client language, server English, ...). Implemented per Minecraft version
 * (I18n / Language) and by {@link Msg#ENGLISH_LANG} for our own English file.
 */
public interface Lang {

    /** The translation pattern of a key ("Damage: %s"), or null when the key is unknown */
    String pattern(String key);

    default boolean has(String key) {
        return key != null && !key.isEmpty() && pattern(key) != null;
    }

    /** Translated text of a key without arguments, the key itself when unknown */
    default String translate(String key) {
        String p = key == null ? null : pattern(key);
        return p == null ? (key == null ? "" : key) : p;
    }

    /** Plain text of a native JSON text component (a death message), "" when it cannot be read */
    default String jsonText(String json) {
        return "";
    }
}
