package io.github.junjun1z.whathitme.mc;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.github.junjun1z.whathitme.core.data.Context;
import io.github.junjun1z.whathitme.core.data.DamageRecord;
import io.github.junjun1z.whathitme.core.data.Gear;
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
import net.minecraftforge.common.UsernameCache;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

/** Minecraft 1.8 damage sources and entities to plain facts (core {@link HitFacts} / {@link EntityFacts}). */
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
        String k = EntityIds.of(EntityList.getEntityString(e));
        if (k != null) return k;
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
            if (net.minecraft.util.StatCollector.canTranslate(key) && !net.minecraft.util.StatCollector.translateToLocal(key).equals(e.getName())) return "";
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
        Entity t = src.getEntity();
        if (t != null) return t;
        Entity d = src.getSourceOfDamage();
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
            String s = o.getOwnerId();
            if (s != null && !s.isEmpty()) {
                try {
                    String n = UsernameCache.getLastKnownUsername(UUID.fromString(s));
                    return n == null ? s.substring(0, Math.min(8, s.length())) : n;
                } catch (IllegalArgumentException e) {
                    return s;
                }
            }
        }
        return "";
    }

    /** "translationKey|level|English name;..." */
    @SuppressWarnings("deprecation")
    public static String enchantments(ItemStack stack) {
        if (stack == null || !stack.isItemEnchanted()) return "";
        StringBuilder sb = new StringBuilder();
        try {
            for (Map.Entry<Integer, Integer> e : EnchantmentHelper.getEnchantments(stack).entrySet()) {
                Enchantment en = Enchantment.getEnchantmentById(e.getKey());
                if (en == null) continue;
                if (sb.length() > 0) sb.append(';');
                sb.append(en.getName()).append('|').append(e.getValue()).append('|').append(en.getTranslatedName(e.getValue()).replace(";", ","));
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
            String english = net.minecraft.util.StatCollector.translateToLocal(key);
            return english.equals(stack.getDisplayName()) ? key : "";
        } catch (Throwable t) {
            return "";
        }
    }

    public static ItemStack weapon(Entity attacker) {
        if (attacker instanceof EntityLivingBase) {
            return ((EntityLivingBase) attacker).getHeldItem();
        }
        return null;
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
        if (w != null) {
            f.weaponName = w.getDisplayName();
            f.weaponKey = plainItemKey ? (w.hasDisplayName() ? "" : w.getUnlocalizedName() + ".name") : itemKey(w);
            f.weaponEnchants = enchantments(w);
            f.weaponId = itemId(w);
        }
        return f;
    }

    public static String itemId(ItemStack s) {
        try {
            Object id = s == null || s.getItem() == null ? null : net.minecraft.item.Item.itemRegistry.getNameForObject(s.getItem());
            return id == null ? "" : id.toString();
        } catch (Throwable t) {
            return "";
        }
    }

    /** An item stack as plain strings (null for an empty stack) */
    public static Gear.Item item(ItemStack s) {
        if (s == null || s.getItem() == null) return null;
        String id = itemId(s);
        int c = id.indexOf(':');
        return new Gear.Item(id, itemKey(s), s.getDisplayName(), c > 0 ? id.substring(0, c) : "minecraft", enchantments(s), false);
    }

    /** Armor: head, chest, legs, feet */
    public static Gear.Item[] armor(EntityPlayer p) {
        Gear.Item[] out = new Gear.Item[4];
        for (int i = 0; i < 4; i++) out[i] = item(p.getCurrentArmor(3 - i));
        return out;
    }

    /** 0 head ... 3 feet, -1 = not armor */
    public static int armorSlot(ItemStack s) {
        if (s == null || !(s.getItem() instanceof net.minecraft.item.ItemArmor)) return -1;
        int t = ((net.minecraft.item.ItemArmor) s.getItem()).armorType;
        return t >= 0 && t < 4 ? t : -1;
    }

    private static void block(Context c, String slot, net.minecraft.world.World w, net.minecraft.util.BlockPos pos) {
        net.minecraft.block.state.IBlockState st = w.getBlockState(pos);
        net.minecraft.block.Block b = st.getBlock();
        if (b.isAir(w, pos)) return;
        Object rid = net.minecraft.block.Block.blockRegistry.getNameForObject(b);
        String id = rid == null ? "" : rid.toString();
        String name;
        String key = "";
        try {
            ItemStack pick = new ItemStack(b, 1, b.damageDropped(st));
            name = pick.getItem() == null ? b.getLocalizedName() : pick.getDisplayName();
            if (pick.getItem() != null) key = itemKey(pick);
        } catch (Throwable t) {
            name = b.getLocalizedName();
        }
        if (name == null || name.isEmpty() || name.startsWith("tile.")) name = id.isEmpty() ? b.getClass().getSimpleName() : id;
        int i = id.indexOf(':');
        c.add(slot, key, name, i > 0 ? id.substring(0, i) : id.isEmpty() ? "" : "minecraft");
    }

    /** Block / fluid at the feet (or the block stood on) and at the body centre, and the held item */
    public static String environment(EntityPlayer p) {
        Context c = new Context();
        net.minecraft.world.World w = p.worldObj;
        net.minecraft.util.BlockPos feet = new net.minecraft.util.BlockPos(p.posX, p.posY, p.posZ);
        if (w.isAirBlock(feet)) block(c, Context.FEET, w, feet.down());
        else block(c, Context.FEET, w, feet);
        net.minecraft.util.BlockPos body = new net.minecraft.util.BlockPos(p.posX, p.posY + p.height / 2.0, p.posZ);
        if (!body.equals(feet)) block(c, Context.BODY, w, body);
        Gear.Item main = item(p.getHeldItem());
        if (main != null) c.add(Context.MAIN, main.key, main.name, main.modId);
        return c.toString();
    }

    /** "minecraft:overworld", "minecraft:the_nether", ... (mod dimensions: their provider name) */
    public static String dimension(net.minecraft.world.World w) {
        try {
            int id = w.provider.getDimensionId();
            if (id == 0) return "minecraft:overworld";
            if (id == -1) return "minecraft:the_nether";
            if (id == 1) return "minecraft:the_end";
            String n = w.provider.getDimensionName();
            return n.indexOf(':') >= 0 ? n : "dim:" + n.toLowerCase(java.util.Locale.ROOT).replace(' ', '_');
        } catch (Throwable t) {
            return "";
        }
    }

    /** Facts of a hit on a player */
    public static HitFacts hit(EntityPlayer victim, DamageSource src) {
        HitFacts h = new HitFacts();
        Entity atk = attacker(src);
        Entity direct = src.getSourceOfDamage();
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
