package io.github.junjun1z.whathitme.core.data;

import java.util.List;

/**
 * The game registries one side can read: mobs, effects, vanilla damage ids and translations. Implemented per
 * Minecraft version; used on demand only (search, dump, tab completion).
 */
public interface Registries {

    /** Every registered entity as {@link Entry#MOB} (name in this side's language, nameKey = translation key) */
    List<Entry> mobs();

    /** Every registered effect as {@link Entry#EFFECT} (flags 1 = harmful) */
    List<Entry> effects();

    /** "mob:&lt;id&gt;" is listed in the hint template for these: every living entity id */
    List<String> livingEntityIds();

    /** Damage ids of the vanilla damage types without the melee ones, e.g. minecraft:fall */
    List<String> vanillaDamageIds();

    /** Translation of a key on this side, "" when unknown */
    String translate(String key);

    /**
     * Every damage type of the game as {@link Entry#DAMAGE} (id, name = msgId, flags = property flags), for the catalog
     * cache. Default: the vanilla ids with the vanilla property flags.
     */
    default List<Entry> damageTypeEntries() {
        List<Entry> out = new java.util.ArrayList<>();
        for (String id : vanillaDamageIds()) {
            int c = id.indexOf(':');
            String type = c >= 0 ? id.substring(c + 1) : id;
            String mod = c > 0 ? id.substring(0, c) : "minecraft";
            out.add(new Entry(Entry.DAMAGE, id, type, "", mod, io.github.junjun1z.whathitme.core.Platform.modName(mod),
                io.github.junjun1z.whathitme.core.client.FallbackCore.typeFlags(type), 0, 0, 0, false, ""));
        }
        return out;
    }
}
