package io.github.junjun1z.whathitme.mc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.data.Entry;
import io.github.junjun1z.whathitme.core.data.Registries;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/** Forge 1.12 registries (entities, potions) and active effects. */
public final class Registries112 implements Registries {

    // registries are iterated directly: IForgeRegistry.getValuesCollection() does not exist in Forge for 1.12 / 1.12.1
    public static final Registries112 INSTANCE = new Registries112();

    /** Damage type names of vanilla 1.12 (melee "mob" / "player" excluded) */
    private static final List<String> VANILLA = Arrays.asList("inFire", "lightningBolt", "onFire", "lava", "hotFloor", "inWall", "cramming", "drown",
        "starve", "cactus", "fall", "flyIntoWall", "outOfWorld", "generic", "magic", "wither", "anvil", "fallingBlock", "dragonBreath", "fireworks",
        "arrow", "thrown", "indirectMagic", "thorns", "explosion", "explosion.player", "fireball", "sweeping");

    private Registries112() {}

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
        for (EntityEntry e : ForgeRegistries.ENTITIES) {
            ResourceLocation id = e.getRegistryName();
            if (id == null) continue;
            String key = "entity." + e.getName() + ".name";
            String name = tr(key);
            out.add(new Entry(Entry.MOB, id.toString(), name.equals(key) ? e.getName() : name, key, id.getNamespace(), Platform.modName(id.getNamespace()), 0, 0,
                0, 0, false, ""));
        }
        return out;
    }

    @Override
    public List<Entry> effects() {
        List<Entry> out = new ArrayList<>();
        for (Potion p : ForgeRegistries.POTIONS) {
            ResourceLocation id = p.getRegistryName();
            if (id == null) continue;
            String key = p.getName();
            out.add(new Entry(Entry.EFFECT, id.toString(), tr(key), key, id.getNamespace(), Platform.modName(id.getNamespace()), p.isBadEffect() ? 1 : 0, 0, 0, 0,
                false, ""));
        }
        return out;
    }

    @Override
    public List<String> livingEntityIds() {
        List<String> out = new ArrayList<>();
        for (EntityEntry ee : ForgeRegistries.ENTITIES) {
            if (ee.getRegistryName() == null || ee.getEntityClass() == null) continue;
            if (EntityLivingBase.class.isAssignableFrom(ee.getEntityClass())) out.add(ee.getRegistryName().toString());
        }
        return out;
    }

    @Override
    public List<String> vanillaDamageIds() {
        List<String> out = new ArrayList<>();
        for (String t : VANILLA) out.add("minecraft:" + t);
        return out;
    }

    /** Every static DamageSource field of DamageSource (vanilla) with its exact properties, plus the other vanilla type names */
    @Override
    public List<Entry> damageTypes() {
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
        for (Entry e : Registries.super.damageTypes()) if (seen.add(e.id)) out.add(e);
        return out;
    }

    /** Active potion effects of an entity */
    public static List<Entry> effects(EntityLivingBase entity) {
        List<Entry> out = new ArrayList<>();
        Collection<PotionEffect> list = entity.getActivePotionEffects();
        for (PotionEffect pe : list) {
            Potion p = pe.getPotion();
            ResourceLocation id = p.getRegistryName();
            String mod = id == null ? "minecraft" : id.getNamespace();
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
}
