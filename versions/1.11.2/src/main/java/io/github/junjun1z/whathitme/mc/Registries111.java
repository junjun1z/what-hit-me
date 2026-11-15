package io.github.junjun1z.whathitme.mc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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

/** Minecraft 1.11 registries (EntityList, Potion.REGISTRY) and active effects. */
public final class Registries111 implements Registries {

    public static final Registries111 INSTANCE = new Registries111();

    /** Damage type names of vanilla 1.11 (melee "mob" / "player" excluded) */
    private static final List<String> VANILLA = Arrays.asList("inFire", "lightningBolt", "onFire", "lava", "hotFloor", "inWall", "cramming", "drown",
        "starve", "cactus", "fall", "flyIntoWall", "outOfWorld", "generic", "magic", "wither", "anvil", "fallingBlock", "dragonBreath", "fireworks",
        "arrow", "thrown", "indirectMagic", "thorns", "explosion", "explosion.player", "fireball");

    private Registries111() {}

    @SuppressWarnings("deprecation")
    private static String tr(String key) {
        if (key == null || key.isEmpty()) return "";
        return net.minecraft.util.text.translation.I18n.translateToLocal(key);
    }

    @Override
    public String translate(String key) {
        return tr(key);
    }

    @Override
    public List<Entry> mobs() {
        List<Entry> out = new ArrayList<>();
        for (ResourceLocation id : EntityList.getEntityNameList()) {
            String old = EntityList.getTranslationName(id);
            if (old == null) old = id.getResourcePath();
            String key = "entity." + old + ".name";
            String name = tr(key);
            out.add(new Entry(Entry.MOB, id.toString(), name.equals(key) ? old : name, key, id.getResourceDomain(), Platform.modName(id.getResourceDomain()), 0,
                0, 0, 0, false, ""));
        }
        return out;
    }

    @Override
    public List<Entry> effects() {
        List<Entry> out = new ArrayList<>();
        for (Potion p : Potion.REGISTRY) {
            ResourceLocation id = Potion.REGISTRY.getNameForObject(p);
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
        for (ResourceLocation id : EntityList.getEntityNameList()) {
            Class<? extends Entity> c = EntityList.getClass(id);
            if (c != null && EntityLivingBase.class.isAssignableFrom(c)) out.add(id.toString());
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
            Potion p = pe.getPotion();
            ResourceLocation id = Potion.REGISTRY.getNameForObject(p);
            String mod = id == null ? "minecraft" : id.getResourceDomain();
            boolean milk = false;
            StringBuilder cur = new StringBuilder();
            try {
                for (ItemStack s : pe.getCurativeItems()) {
                    if (s.isEmpty()) continue;
                    if (s.getItem() == net.minecraft.init.Items.MILK_BUCKET) milk = true;
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

    /** DamageSource and its vanilla subclasses as mods reference them, and every mod file */
    @Override
    public io.github.junjun1z.whathitme.core.data.DamageIdScanner.Input scanInput() {
        List<String> base = new ArrayList<>();
        for (Class<?> c : new Class<?>[] {net.minecraft.util.DamageSource.class, net.minecraft.util.EntityDamageSource.class,
            net.minecraft.util.EntityDamageSourceIndirect.class}) base.add(c.getName().replace('.', '/'));
        java.util.Map<java.io.File, String> files = new java.util.LinkedHashMap<>();
        for (net.minecraftforge.fml.common.ModContainer mc : net.minecraftforge.fml.common.Loader.instance().getActiveModList()) {
            String id = mc.getModId();
            if ("minecraft".equalsIgnoreCase(id) || "mcp".equalsIgnoreCase(id) || "FML".equalsIgnoreCase(id) || "forge".equalsIgnoreCase(id)
                || Platform.MODID.equals(id)) continue;
            java.io.File f = mc.getSource();
            if (f == null || !f.exists()) continue;
            try {
                files.putIfAbsent(f.getCanonicalFile(), id);
            } catch (Exception ignored) {
                // skip
            }
        }
        return new io.github.junjun1z.whathitme.core.data.DamageIdScanner.Input(base, files, net.minecraftforge.fml.common.Loader.instance().getIndexedModList().keySet(),
            io.github.junjun1z.whathitme.core.data.DamageIdScanner.OLD_FORGE_SETTERS);
    }
}
