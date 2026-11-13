package io.github.junjun1z.whathitme.data;

import java.io.File;
import java.net.URL;
import java.security.CodeSource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

/** Turns a DamageSource + entities into plain data: normalized id, mod, names, flags. Caches per damage type. */
public final class Sources {

    private static final Set<String> VANILLA_TYPES = new HashSet<>(Arrays.asList("inFire", "lightningBolt", "onFire", "lava", "hotFloor",
        "inWall", "cramming", "drown", "starve", "cactus", "fall", "flyIntoWall", "outOfWorld", "generic", "magic", "wither", "anvil",
        "fallingBlock", "dragonBreath", "fireworks", "mob", "player", "arrow", "thrown", "indirectMagic", "thorns", "explosion",
        "explosion.player", "fireball", "sweeping"));

    /** damage type -> mod id (computed once per type) */
    private static final Map<String, String> TYPE_MOD = new ConcurrentHashMap<>();
    private static volatile Map<File, String> sourceToMod;

    private Sources() {}

    public static String modName(String modId) {
        if (modId == null || modId.isEmpty()) return "";
        if ("minecraft".equals(modId)) return "Minecraft";
        ModContainer c = Loader.instance().getIndexedModList().get(modId);
        return c == null ? modId : c.getName();
    }

    /** Registry name of an entity ("minecraft:player" for players, "" when unknown) */
    public static String entityId(Entity e) {
        if (e == null) return "";
        if (e instanceof EntityPlayer) return "minecraft:player";
        ResourceLocation k = EntityList.getKey(e);
        if (k != null) return k.toString();
        return "unknown:" + e.getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT);
    }

    /** Translation key of an entity's default name, "" for players and custom names */
    public static String entityNameKey(Entity e) {
        if (e == null || e instanceof EntityPlayer || e.hasCustomName()) return "";
        String s = EntityList.getEntityString(e);
        return s == null ? "" : "entity." + s + ".name";
    }

    public static String entityName(Entity e) {
        if (e == null) return "";
        try {
            return e.getName();
        } catch (Throwable t) {
            return entityId(e);
        }
    }

    public static String modOfEntity(Entity e) {
        String id = entityId(e);
        int c = id.indexOf(':');
        return c > 0 ? id.substring(0, c) : "minecraft";
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

    /** Normalized id: mob:&lt;entity&gt; for melee, otherwise &lt;modid&gt;:&lt;damage type&gt; */
    public static String damageId(DamageSource src, Entity attacker) {
        String type = src.getDamageType();
        if (attacker != null && ("mob".equals(type) || "player".equals(type))) return "mob:" + entityId(attacker);
        if (type.indexOf(':') > 0) return type;
        return typeMod(src) + ":" + type;
    }

    /** Mod that added a damage type */
    public static String typeMod(DamageSource src) {
        String type = src.getDamageType();
        String cached = TYPE_MOD.get(type);
        if (cached != null) return cached;
        String mod = findTypeMod(src, type);
        if (TYPE_MOD.size() < 4096) TYPE_MOD.put(type, mod);
        return mod;
    }

    private static String findTypeMod(DamageSource src, String type) {
        if (VANILLA_TYPES.contains(type) && src.getClass().getName().startsWith("net.minecraft.")) return "minecraft";
        // 1. a DamageSource subclass from a mod
        String byClass = modOfClass(src.getClass());
        if (byClass != null) return byClass;
        // 2. a prefix that is a mod id ("modid.name", "modid_name")
        Map<String, ModContainer> mods = Loader.instance().getIndexedModList();
        for (String sep : new String[] {".", "_", ":"}) {
            int i = type.indexOf(sep);
            if (i > 0) {
                String p = type.substring(0, i).toLowerCase(java.util.Locale.ROOT);
                if (mods.containsKey(p)) return p;
            }
        }
        if (VANILLA_TYPES.contains(type)) return "minecraft";
        // 3. who called attackEntityFrom (only once per new damage type)
        try {
            ClassLoader cl = Sources.class.getClassLoader();
            for (StackTraceElement el : new Throwable().getStackTrace()) {
                String n = el.getClassName();
                if (n.startsWith("java.") || n.startsWith("sun.") || n.startsWith("jdk.") || n.startsWith("net.minecraft.") || n.startsWith("net.minecraftforge.")
                    || n.startsWith("io.github.junjun1z.whathitme.") || n.startsWith("com.google.") || n.startsWith("io.netty.")) continue;
                try {
                    String m = modOfClass(Class.forName(n, false, cl));
                    if (m != null) return m;
                } catch (Throwable ignored) {
                    // next frame
                }
            }
        } catch (Throwable ignored) {
            // fall through
        }
        return "unknown";
    }

    /** Mod whose jar contains the class, null for Minecraft / Forge / unknown */
    public static String modOfClass(Class<?> c) {
        if (c == null) return null;
        String n = c.getName();
        if (n.startsWith("net.minecraft.") || n.startsWith("java.")) return null;
        try {
            CodeSource cs = c.getProtectionDomain().getCodeSource();
            if (cs == null) return null;
            URL url = cs.getLocation();
            if (url == null) return null;
            String path = url.getPath();
            int bang = path.indexOf("!/");
            if (bang >= 0) path = path.substring(0, bang);
            if (path.startsWith("file:")) path = path.substring(5);
            File f = new File(java.net.URLDecoder.decode(path, "UTF-8")).getCanonicalFile();
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
        } catch (Throwable t) {
            return null;
        }
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
            String key = stack.getTranslationKey() + ".name";
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
}
