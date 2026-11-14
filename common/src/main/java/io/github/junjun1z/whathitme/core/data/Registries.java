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
}
