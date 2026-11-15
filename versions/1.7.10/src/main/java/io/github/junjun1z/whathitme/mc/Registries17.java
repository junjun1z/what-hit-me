package io.github.junjun1z.whathitme.mc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.data.Entry;
import io.github.junjun1z.whathitme.core.data.Registries;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

/** Minecraft 1.7.10 registries (EntityList names, Potion.potionTypes) and active effects. */
public final class Registries17 implements Registries {

    public static final Registries17 INSTANCE = new Registries17();

    /** Damage type names of vanilla 1.7.10 (melee "mob" / "player" excluded) */
    private static final List<String> VANILLA = Arrays.asList("inFire", "lightningBolt", "onFire", "lava", "inWall", "drown", "starve", "cactus", "fall",
        "outOfWorld", "generic", "magic", "wither", "anvil", "fallingBlock", "arrow", "thrown", "indirectMagic", "thorns", "explosion", "explosion.player",
        "fireball");

    /** Registry names (1.9+) of the vanilla potions by numeric id */
    private static final String[] VANILLA_POTIONS = {null, "speed", "slowness", "haste", "mining_fatigue", "strength", "instant_health", "instant_damage",
        "jump_boost", "nausea", "regeneration", "resistance", "fire_resistance", "water_breathing", "invisibility", "blindness", "night_vision", "hunger",
        "weakness", "poison", "wither", "health_boost", "absorption", "saturation"};

    private Registries17() {}

    private static String tr(String key) {
        if (key == null || key.isEmpty()) return "";
        return net.minecraft.util.StatCollector.translateToLocal(key);
    }

    @Override
    public String translate(String key) {
        return tr(key);
    }

    /** "minecraft:poison" for vanilla, "&lt;mod&gt;:&lt;name&gt;" for mod potions */
    static String potionId(Potion p) {
        if (p.id > 0 && p.id < VANILLA_POTIONS.length && p.getClass().getName().startsWith("net.minecraft.")) return "minecraft:" + VANILLA_POTIONS[p.id];
        String name = p.getName() == null ? "potion" + p.id : p.getName();
        if (name.startsWith("potion.")) name = name.substring(7);
        String mod = Facts.TYPES.modOfClass(p.getClass());
        return (mod == null || mod.isEmpty() ? "unknown" : mod) + ":" + EntityIds.snake(name.replace('.', '_')).toLowerCase(Locale.ROOT);
    }

    private static volatile java.lang.reflect.Field badField;

    /**
     * Potion#isBadEffect is client-only on 1.7.10 (missing on a dedicated server): read its field, the only final
     * boolean of Potion
     */
    static boolean bad(Potion p) {
        try {
            java.lang.reflect.Field f = badField;
            if (f == null) {
                for (java.lang.reflect.Field c : Potion.class.getDeclaredFields()) {
                    if (c.getType() == boolean.class && java.lang.reflect.Modifier.isFinal(c.getModifiers()) && !java.lang.reflect.Modifier.isStatic(c.getModifiers())) {
                        c.setAccessible(true);
                        f = c;
                        break;
                    }
                }
                if (f == null) return false;
                badField = f;
            }
            return f.getBoolean(p);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<Entry> mobs() {
        List<Entry> out = new ArrayList<>();
        for (Object o : EntityList.stringToClassMapping.keySet()) {
            String old = (String) o;
            String id = EntityIds.of(old);
            String ns = id.substring(0, id.indexOf(':'));
            String key = "entity." + old + ".name";
            String name = tr(key);
            out.add(new Entry(Entry.MOB, id, name.equals(key) ? old : name, key, ns, Platform.modName(ns), 0, 0, 0, 0, false, ""));
        }
        return out;
    }

    @Override
    public List<Entry> effects() {
        List<Entry> out = new ArrayList<>();
        for (Potion p : Potion.potionTypes) {
            if (p == null) continue;
            String id = potionId(p);
            String ns = id.substring(0, id.indexOf(':'));
            String key = p.getName();
            out.add(new Entry(Entry.EFFECT, id, tr(key), key, ns, Platform.modName(ns), bad(p) ? 1 : 0, 0, 0, 0, false, ""));
        }
        return out;
    }

    @Override
    public List<String> livingEntityIds() {
        List<String> out = new ArrayList<>();
        for (Object o : EntityList.stringToClassMapping.entrySet()) {
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            Object c = e.getValue();
            if (c instanceof Class && EntityLivingBase.class.isAssignableFrom((Class<?>) c)) out.add(EntityIds.of((String) e.getKey()));
        }
        return out;
    }

    /** Every static DamageSource field of DamageSource (vanilla) with its exact properties, plus the other vanilla type names */
    @Override
    public List<Entry> damageTypeEntries() {
        List<Entry> out = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (java.lang.reflect.Field f : net.minecraft.util.DamageSource.class.getDeclaredFields()) {
            try {
                if (!java.lang.reflect.Modifier.isStatic(f.getModifiers()) || f.getType() != net.minecraft.util.DamageSource.class) continue;
                f.setAccessible(true);
                net.minecraft.util.DamageSource src = (net.minecraft.util.DamageSource) f.get(null);
                if (src == null) continue;
                String type = src.getDamageType();
                String id = Facts.TYPES.typeId(type, src.getClass(), true);
                if (!seen.add(id)) continue;
                int c = id.indexOf(':');
                String mod = c > 0 ? id.substring(0, c) : "minecraft";
                out.add(new Entry(Entry.DAMAGE, id, type, "", mod, Platform.modName(mod), Facts.sourceFlags(src), 0, 0, 0, false, ""));
            } catch (Throwable ignored) {
                // skip odd fields
            }
        }
        for (Entry e : Registries.super.damageTypeEntries()) if (seen.add(e.id)) out.add(e);
        return out;
    }

    @Override
    public List<String> vanillaDamageIds() {
        List<String> out = new ArrayList<>();
        for (String t : VANILLA) out.add("minecraft:" + t);
        return out;
    }

    /** Active potion effects of an entity */
    public static List<Entry> effects(EntityLivingBase entity) {
        List<Entry> out = new ArrayList<>();
        for (Object o : entity.getActivePotionEffects()) {
            PotionEffect pe = (PotionEffect) o;
            Potion p = pe.getPotionID() >= 0 && pe.getPotionID() < Potion.potionTypes.length ? Potion.potionTypes[pe.getPotionID()] : null;
            if (p == null) continue;
            String id = potionId(p);
            String mod = id.substring(0, id.indexOf(':'));
            boolean milk = false;
            StringBuilder cur = new StringBuilder();
            try {
                for (ItemStack s : pe.getCurativeItems()) {
                    if (s == null) continue;
                    if (s.getItem() == net.minecraft.init.Items.milk_bucket) milk = true;
                    if (cur.length() > 0) cur.append(", ");
                    cur.append(s.getDisplayName());
                }
            } catch (Throwable ignored) {
                // a mod returning odd curative items
            }
            out.add(new Entry(Entry.ACTIVE_EFFECT, id, tr(p.getName()), p.getName(), mod, Platform.modName(mod), bad(p) ? 1 : 0, 0, pe.getAmplifier(),
                pe.getDuration(), milk, cur.toString()));
        }
        return out;
    }
}
