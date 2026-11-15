package io.github.junjun1z.whathitme.mc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.data.Entry;
import io.github.junjun1z.whathitme.core.data.Registries;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;

/** Minecraft 1.8 registries (EntityList names, Potion.potionTypes) and active effects. */
public final class Registries18 implements Registries {

    public static final Registries18 INSTANCE = new Registries18();

    /** Damage type names of vanilla 1.8 (melee "mob" / "player" excluded) */
    private static final List<String> VANILLA = Arrays.asList("inFire", "lightningBolt", "onFire", "lava", "inWall", "drown",
        "starve", "cactus", "fall", "outOfWorld", "generic", "magic", "wither", "anvil", "fallingBlock",
        "arrow", "thrown", "indirectMagic", "thorns", "explosion", "explosion.player", "fireball");

    private Registries18() {}

    @SuppressWarnings("deprecation")
    private static String tr(String key) {
        if (key == null || key.isEmpty()) return "";
        return net.minecraft.util.StatCollector.translateToLocal(key);
    }

    @Override
    public String translate(String key) {
        return tr(key);
    }

    @Override
    public List<Entry> mobs() {
        List<Entry> out = new ArrayList<>();
        for (String old : EntityList.stringToClassMapping.keySet()) {
            String id = EntityIds.of(old);
            String ns = id.substring(0, id.indexOf(':'));
            String key = "entity." + old + ".name";
            String name = tr(key);
            out.add(new Entry(Entry.MOB, id, name.equals(key) ? old : name, key, ns, Platform.modName(ns), 0, 0, 0, 0, false, ""));
        }
        return out;
    }

    private static volatile Map<Potion, ResourceLocation> potionIds;

    /** Registry name of a potion (1.8 only maps names to potions) */
    static ResourceLocation potionId(Potion p) {
        Map<Potion, ResourceLocation> m = potionIds;
        if (m == null) {
            m = new HashMap<>();
            for (ResourceLocation r : Potion.getPotionLocations()) {
                Potion q = Potion.getPotionFromResourceLocation(r.toString());
                if (q != null) m.put(q, r);
            }
            potionIds = m;
        }
        return m.get(p);
    }

    @Override
    public List<Entry> effects() {
        List<Entry> out = new ArrayList<>();
        for (Potion p : Potion.potionTypes) {
            if (p == null) continue;
            ResourceLocation id = potionId(p);
            if (id == null) continue;
            String key = p.getName();
            out.add(new Entry(Entry.EFFECT, id.toString(), tr(key), key, id.getResourceDomain(), Platform.modName(id.getResourceDomain()), p.isBadEffect() ? 1 : 0,
                0, 0, 0, false, ""));
        }
        return out;
    }

    @Override
    public List<String> livingEntityIds() {
        List<String> out = new ArrayList<>();
        for (java.util.Map.Entry<String, Class<? extends Entity>> e : EntityList.stringToClassMapping.entrySet()) {
            Class<? extends Entity> c = e.getValue();
            if (c != null && EntityLivingBase.class.isAssignableFrom(c)) out.add(EntityIds.of(e.getKey()));
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
        Collection<PotionEffect> list = entity.getActivePotionEffects();
        for (PotionEffect pe : list) {
            Potion p = pe.getPotionID() >= 0 && pe.getPotionID() < Potion.potionTypes.length ? Potion.potionTypes[pe.getPotionID()] : null;
            if (p == null) continue;
            ResourceLocation id = potionId(p);
            String mod = id == null ? "minecraft" : id.getResourceDomain();
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
            out.add(new Entry(Entry.ACTIVE_EFFECT, id == null ? p.getName() : id.toString(), tr(p.getName()), p.getName(), mod, Platform.modName(mod),
                p.isBadEffect() ? 1 : 0, 0, pe.getAmplifier(), pe.getDuration(), milk, cur.toString()));
        }
        return out;
    }
}
