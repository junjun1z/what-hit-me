package io.github.junjun1z.whathitme.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import io.github.junjun1z.whathitme.text.RecapFormatter;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/** /whm search, /whm effects and /whm dump data. Runs on demand only. */
public final class SearchEngine {

    public static final String[] TAGS = {"#bypasses_armor", "#absolute", "#magic", "#fire", "#explosion", "#projectile", "#damage", "#mob", "#effect"};

    private SearchEngine() {}

    @SuppressWarnings("deprecation")
    private static String tr(String key) {
        if (key == null || key.isEmpty()) return "";
        return net.minecraft.util.text.translation.I18n.translateToLocal(key);
    }

    /** Every mob and effect of the registries plus the damage catalog */
    public static List<Entry> all(DamageCatalog catalog) {
        List<Entry> out = new ArrayList<>(catalog.entries());
        for (EntityEntry e : ForgeRegistries.ENTITIES.getValuesCollection()) {
            ResourceLocation id = e.getRegistryName();
            if (id == null) continue;
            String key = "entity." + e.getName() + ".name";
            String name = tr(key);
            out.add(new Entry(Entry.MOB, id.toString(), name.equals(key) ? e.getName() : name, key, id.getNamespace(), Sources.modName(id.getNamespace()), 0, 0, 0, 0, false, ""));
        }
        for (Potion p : ForgeRegistries.POTIONS.getValuesCollection()) {
            ResourceLocation id = p.getRegistryName();
            if (id == null) continue;
            String key = p.getName();
            out.add(new Entry(Entry.EFFECT, id.toString(), tr(key), key, id.getNamespace(), Sources.modName(id.getNamespace()), p.isBadEffect() ? 1 : 0, 0, 0, 0, false, ""));
        }
        return out;
    }

    /**
     * #tag (damage property or type), @modid, or text matched against ids and names. Several terms can be combined
     * (all must match).
     */
    public static List<Entry> search(String query, DamageCatalog catalog) {
        String[] terms = query.trim().toLowerCase(Locale.ROOT).split("\\s+");
        List<Entry> out = new ArrayList<>();
        for (Entry e : all(catalog)) {
            boolean ok = true;
            for (String t : terms) {
                if (t.isEmpty()) continue;
                if (!matches(e, t)) {
                    ok = false;
                    break;
                }
            }
            if (ok) out.add(e);
        }
        return out;
    }

    private static boolean matches(Entry e, String t) {
        if (t.startsWith("#")) {
            switch (t) {
                case "#bypasses_armor":
                case "#bypassesarmor":
                case "#unblockable":
                    return e.type == Entry.DAMAGE && (e.flags & DamageRecord.BYPASS_ARMOR) != 0;
                case "#absolute":
                case "#bypasses_magic":
                    return e.type == Entry.DAMAGE && (e.flags & DamageRecord.ABSOLUTE) != 0;
                case "#magic":
                    return e.type == Entry.DAMAGE && (e.flags & DamageRecord.MAGIC) != 0;
                case "#fire":
                    return e.type == Entry.DAMAGE && (e.flags & DamageRecord.FIRE) != 0;
                case "#explosion":
                    return e.type == Entry.DAMAGE && (e.flags & DamageRecord.EXPLOSION) != 0;
                case "#projectile":
                    return e.type == Entry.DAMAGE && (e.flags & DamageRecord.PROJECTILE) != 0;
                case "#damage":
                    return e.type == Entry.DAMAGE;
                case "#mob":
                    return e.type == Entry.MOB;
                case "#effect":
                case "#debuff":
                    return e.type == Entry.EFFECT && (!"#debuff".equals(t) || (e.flags & 1) != 0);
                default:
                    return false;
            }
        }
        if (t.startsWith("@")) {
            String mod = t.substring(1);
            return e.modId.toLowerCase(Locale.ROOT).equals(mod) || e.id.toLowerCase(Locale.ROOT).startsWith(mod + ":")
                || e.id.toLowerCase(Locale.ROOT).startsWith("mob:" + mod + ":");
        }
        if (e.id.toLowerCase(Locale.ROOT).contains(t)) return true;
        if (e.name.toLowerCase(Locale.ROOT).contains(t)) return true;
        if (e.type == Entry.DAMAGE && RecapFormatter.pretty(e.name).toLowerCase(Locale.ROOT).contains(t)) return true;
        String loc = tr(e.nameKey);
        return !loc.isEmpty() && loc.toLowerCase(Locale.ROOT).contains(t);
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
            out.add(new Entry(Entry.ACTIVE_EFFECT, id == null ? p.getName() : id.toString(), tr(p.getName()), p.getName(), mod, Sources.modName(mod),
                p.isBadEffect() ? 1 : 0, 0, pe.getAmplifier(), pe.getDuration(), milk, cur.toString()));
        }
        return out;
    }
}
