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
}
