package io.github.junjun1z.whathitme.mc;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.github.junjun1z.whathitme.core.data.DamageRecord;
import io.github.junjun1z.whathitme.core.data.EntityFacts;
import io.github.junjun1z.whathitme.core.data.HitFacts;
import io.github.junjun1z.whathitme.core.data.TypeModResolver;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityOwnable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.UsernameCache;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

/** Minecraft 1.11 damage sources and entities to plain facts (core {@link HitFacts} / {@link EntityFacts}). */
public final class Facts {

    private static volatile Map<File, String> sourceToMod;

    /** Mod of a damage type: vanilla list, DamageSource subclass jar, "modid." prefix or the calling mod */
    public static final TypeModResolver TYPES = new TypeModResolver(new TypeModResolver.ModIndex() {
        @Override
        public boolean isMod(String id) {
            return Loader.instance().getIndexedModList().containsKey(id);
        }

        @Override
        public String modOfSource(File f) {
            Map<File, String> map = sourceToMod;
            if (map == null) {
                map = new HashMap<>();
                for (ModContainer mc : Loader.instance().getActiveModList()) {
                    File s = mc.getSource();
                    if (s == null) continue;
                    String id = mc.getModId();
                    if ("minecraft".equals(id) || "mcp".equals(id) || "FML".equals(id) || "forge".equals(id)) continue;
                    try {
                        map.putIfAbsent(s.getCanonicalFile(), id);
                    } catch (Exception ignored) {
                        // skip
                    }
                }
                sourceToMod = map;
            }
            return map.get(f);
        }
    });

    private Facts() {}

    /** Registry name of an entity ("minecraft:player" for players, "unknown:&lt;class&gt;" when unknown) */
    public static String entityId(Entity e) {
        if (e == null) return "";
        if (e instanceof EntityPlayer) return "minecraft:player";
        ResourceLocation k = EntityList.getKey(e);
        if (k != null) return k.toString();
        return "unknown:" + e.getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * Translation key of an entity's default name, "" for players, custom names and names a mod builds itself (e.g.
     * "Ashen Ventoraptor"): then the plain name is shown.
     */
    @SuppressWarnings("deprecation")
    public static String entityNameKey(Entity e) {
        if (e == null || e instanceof EntityPlayer || e.hasCustomName()) return "";
        String s = EntityList.getEntityString(e);
        if (s == null) return "";
        String key = "entity." + s + ".name";
        try {
            if (net.minecraft.util.text.translation.I18n.canTranslate(key) && !net.minecraft.util.text.translation.I18n.translateToLocal(key).equals(e.getName())) return "";
        } catch (Throwable ignored) {
            // keep the key
        }
        return key;
    }

    public static String entityName(Entity e) {
        if (e == null) return "";
        try {
            return e.getName();
        } catch (Throwable t) {
            return entityId(e);
        }
    }

    /** The responsible entity: true source, or the shooter of a projectile, or the direct source */
    public static Entity attacker(DamageSource src) {
        Entity t = src.getTrueSource();
        if (t != null) return t;
        Entity d = src.getImmediateSource();
        if (d instanceof EntityArrow && ((EntityArrow) d).shootingEntity != null) return ((EntityArrow) d).shootingEntity;
        if (d instanceof EntityThrowable && ((EntityThrowable) d).getThrower() != null) return ((EntityThrowable) d).getThrower();
        if (d instanceof EntityFireball && ((EntityFireball) d).shootingEntity != null) return ((EntityFireball) d).shootingEntity;
        return d;
    }

    public static int sourceFlags(DamageSource src) {
        int f = 0;
        if (src.isUnblockable()) f |= DamageRecord.BYPASS_ARMOR;
        if (src.isDamageAbsolute()) f |= DamageRecord.ABSOLUTE;
        if (src.isMagicDamage()) f |= DamageRecord.MAGIC;
        if (src.isFireDamage()) f |= DamageRecord.FIRE;
        if (src.isExplosion()) f |= DamageRecord.EXPLOSION;
        if (src.isProjectile()) f |= DamageRecord.PROJECTILE;
        if (src.canHarmInCreative()) f |= DamageRecord.BYPASS_CREATIVE;
        return f;
    }

    public static String ownerName(Entity attacker) {
        if (attacker instanceof IEntityOwnable) {
            IEntityOwnable o = (IEntityOwnable) attacker;
            Entity owner = o.getOwner();
            if (owner != null) return entityName(owner);
            UUID id = o.getOwnerId();
            if (id != null) {
                String n = UsernameCache.getLastKnownUsername(id);
                return n == null ? id.toString().substring(0, 8) : n;
            }
        }
        return "";
    }

    /** "translationKey|level|English name;..." */
    @SuppressWarnings("deprecation")
    public static String enchantments(ItemStack stack) {
        if (stack.isEmpty() || !stack.isItemEnchanted()) return "";
        StringBuilder sb = new StringBuilder();
        try {
            for (Map.Entry<Enchantment, Integer> e : EnchantmentHelper.getEnchantments(stack).entrySet()) {
                if (e.getKey() == null) continue;
                if (sb.length() > 0) sb.append(';');
                sb.append(e.getKey().getName()).append('|').append(e.getValue()).append('|').append(e.getKey().getTranslatedName(e.getValue()).replace(";", ","));
            }
        } catch (Throwable ignored) {
            // odd enchantment data
        }
        return sb.toString();
    }

    /** Translation key of an item stack name when it is a plain (not dynamic / renamed) name, else "" */
    @SuppressWarnings("deprecation")
    public static String itemKey(ItemStack stack) {
        try {
            if (stack.hasDisplayName()) return "";
            String key = stack.getUnlocalizedName() + ".name";
            String english = net.minecraft.util.text.translation.I18n.translateToLocal(key);
            return english.equals(stack.getDisplayName()) ? key : "";
        } catch (Throwable t) {
            return "";
        }
    }

    public static ItemStack weapon(Entity attacker) {
        if (attacker instanceof EntityLivingBase) {
            ItemStack s = ((EntityLivingBase) attacker).getHeldItemMainhand();
            return s == null ? ItemStack.EMPTY : s;
        }
        return ItemStack.EMPTY;
    }

    /**
     * Facts of an entity seen from the victim.
     *
     * @param victim for the distance (null = unknown)
     * @param plainItemKey weapon key without the "is it really the English name" check (client estimates)
     */
    public static EntityFacts entity(Entity e, Entity victim, boolean plainItemKey) {
        EntityFacts f = new EntityFacts();
        f.name = entityName(e);
        f.nameKey = entityNameKey(e);
        f.id = entityId(e);
        f.uuid = e.getUniqueID().toString();
        f.owner = ownerName(e);
        f.player = e instanceof EntityPlayer;
        f.invisible = e.isInvisible();
        f.distance = victim == null ? -1f : victim.getDistanceToEntity(e);
        ItemStack w = weapon(e);
        if (!w.isEmpty()) {
            f.weaponName = w.getDisplayName();
            f.weaponKey = plainItemKey ? (w.hasDisplayName() ? "" : w.getUnlocalizedName() + ".name") : itemKey(w);
            f.weaponEnchants = enchantments(w);
        }
        return f;
    }

    /** Facts of a hit on a player */
    public static HitFacts hit(EntityPlayer victim, DamageSource src) {
        HitFacts h = new HitFacts();
        Entity atk = attacker(src);
        Entity direct = src.getImmediateSource();
        String type = src.getDamageType();
        h.damageType = type;
        h.melee = "mob".equals(type) || "player".equals(type);
        h.typeId = TYPES.typeId(type, src.getClass(), src.getClass().getName().startsWith("net.minecraft."));
        h.flags = sourceFlags(src);
        if (atk != null && atk != victim) h.attacker = entity(atk, victim, false);
        else if (atk == victim && h.melee) h.typeId = "mob:minecraft:player";
        h.directName = direct != null && direct != atk && direct != victim ? entityName(direct) : "";
        return h;
    }
}
