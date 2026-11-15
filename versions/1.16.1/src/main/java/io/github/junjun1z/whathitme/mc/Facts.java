package io.github.junjun1z.whathitme.mc;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.data.Context;
import io.github.junjun1z.whathitme.core.data.DamageRecord;
import io.github.junjun1z.whathitme.core.data.EntityFacts;
import io.github.junjun1z.whathitme.core.data.Gear;
import io.github.junjun1z.whathitme.core.data.HitFacts;
import io.github.junjun1z.whathitme.core.data.TypeModResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Minecraft 1.16.1 damage sources (msgId, no DamageType registry) and entities to plain facts (core {@link HitFacts} /
 * {@link EntityFacts}). The damage id is "&lt;mod&gt;:&lt;msgId&gt;" ("minecraft:outOfWorld"), the mod found by
 * {@link TypeModResolver}; melee is "mob:&lt;entity id&gt;".
 */
public final class Facts {

    /** Mod id of the jar / directory a class was loaded from (set by the loader), null when unknown */
    public static volatile Function<File, String> modOfSource = f -> null;

    /** Mod of a damage type: vanilla list, DamageSource subclass jar, "modid." prefix or the calling mod */
    public static final TypeModResolver TYPES = new TypeModResolver(new TypeModResolver.ModIndex() {
        @Override
        public boolean isMod(String id) {
            return Platform.mods.modIds().contains(id);
        }

        @Override
        public String modOfSource(File source) {
            return modOfSource.apply(source);
        }
    });

    private Facts() {}

    /** Registry id of an entity ("minecraft:player" for players) */
    public static String entityId(Entity e) {
        if (e == null) return "";
        if (e instanceof Player) return "minecraft:player";
        ResourceLocation k = Registry.ENTITY_TYPE.getKey(e.getType());
        return k == null ? "unknown:" + e.getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT) : k.toString();
    }

    public static String entityName(Entity e) {
        if (e == null) return "";
        try {
            return e.getName().getString();
        } catch (Throwable t) {
            return entityId(e);
        }
    }

    /** Translation key of the default name, "" for players, custom names and names a mod builds itself */
    public static String entityNameKey(Entity e) {
        if (e == null || e instanceof Player || e.hasCustomName()) return "";
        try {
            Component n = e.getName();
            if (n instanceof TranslatableComponent) return ((TranslatableComponent) n).getKey();
        } catch (Throwable ignored) {
            // no key
        }
        return "";
    }

    /** The responsible entity: true source, or the shooter of a projectile, or the direct source */
    public static Entity attacker(DamageSource src) {
        Entity t = src.getEntity();
        if (t != null) return t;
        Entity d = src.getDirectEntity();
        if (d instanceof Projectile && ((Projectile) d).getOwner() != null) return ((Projectile) d).getOwner();
        return d;
    }

    public static int sourceFlags(DamageSource src) {
        int f = 0;
        if (src.isBypassArmor()) f |= DamageRecord.BYPASS_ARMOR;
        if (src.isBypassMagic()) f |= DamageRecord.ABSOLUTE;
        if (src.isMagic()) f |= DamageRecord.MAGIC;
        if (src.isFire()) f |= DamageRecord.FIRE;
        if (src.isExplosion()) f |= DamageRecord.EXPLOSION;
        if (src.isProjectile()) f |= DamageRecord.PROJECTILE;
        if (src.isBypassInvul()) f |= DamageRecord.BYPASS_CREATIVE;
        return f;
    }

    public static String ownerName(Entity e) {
        Entity owner = null;
        UUID id = null;
        if (e instanceof TamableAnimal) {
            owner = ((TamableAnimal) e).getOwner();
            id = ((TamableAnimal) e).getOwnerUUID();
        } else if (e instanceof AbstractHorse) {
            id = ((AbstractHorse) e).getOwnerUUID();
            if (id != null && e.level != null) owner = e.level.getPlayerByUUID(id);
        }
        if (owner != null) return entityName(owner);
        if (id != null) return id.toString().substring(0, 8);
        return "";
    }

    /** "translationKey|level|English name;..." */
    public static String enchantments(ItemStack stack) {
        if (stack.isEmpty() || !stack.isEnchanted()) return "";
        StringBuilder sb = new StringBuilder();
        try {
            for (Map.Entry<Enchantment, Integer> e : EnchantmentHelper.getEnchantments(stack).entrySet()) {
                if (e.getKey() == null) continue;
                if (sb.length() > 0) sb.append(';');
                sb.append(e.getKey().getDescriptionId()).append('|').append(e.getValue()).append('|')
                    .append(e.getKey().getFullname(e.getValue()).getString().replace(";", ","));
            }
        } catch (Throwable ignored) {
            // odd enchantment data
        }
        return sb.toString();
    }

    /** Translation key of an item name when it is the plain (not renamed / dynamic) name, else "" */
    public static String itemKey(ItemStack stack) {
        try {
            if (stack.hasCustomHoverName()) return "";
            String key = stack.getDescriptionId();
            String english = Language.getInstance().getOrDefault(key);
            return english.equals(stack.getHoverName().getString()) ? key : "";
        } catch (Throwable t) {
            return "";
        }
    }

    /** Facts of an entity seen from the victim (null = unknown distance) */
    public static EntityFacts entity(Entity e, Entity victim) {
        EntityFacts f = new EntityFacts();
        f.name = entityName(e);
        f.nameKey = entityNameKey(e);
        f.id = entityId(e);
        f.uuid = e.getUUID().toString();
        f.owner = ownerName(e);
        f.player = e instanceof Player;
        f.invisible = e.isInvisible();
        f.distance = victim == null ? -1f : victim.distanceTo(e);
        if (e instanceof LivingEntity) {
            ItemStack w = ((LivingEntity) e).getMainHandItem();
            if (!w.isEmpty()) {
                f.weaponName = w.getHoverName().getString();
                f.weaponKey = itemKey(w);
                f.weaponEnchants = enchantments(w);
                f.weaponId = itemId(w);
            }
        }
        return f;
    }

    /** Facts of a hit on a player */
    public static HitFacts hit(Player victim, DamageSource src) {
        HitFacts h = new HitFacts();
        Entity atk = attacker(src);
        Entity direct = src.getDirectEntity();
        String type = src.getMsgId();
        h.damageType = type;
        h.melee = "mob".equals(type) || "player".equals(type);
        h.typeId = TYPES.typeId(type, src.getClass(), src.getClass().getName().startsWith("net.minecraft."));
        h.flags = sourceFlags(src);
        if (atk != null && atk != victim) h.attacker = entity(atk, victim);
        else if (atk == victim && h.melee) h.typeId = "mob:minecraft:player";
        h.directName = direct != null && direct != atk && direct != victim ? entityName(direct) : "";
        return h;
    }

    public static String itemId(ItemStack s) {
        try {
            if (s == null || s.isEmpty()) return "";
            ResourceLocation id = Registry.ITEM.getKey(s.getItem());
            return id == null ? "" : id.toString();
        } catch (Throwable t) {
            return "";
        }
    }

    /** An item stack as plain strings (null for an empty stack) */
    public static Gear.Item item(ItemStack s) {
        if (s == null || s.isEmpty()) return null;
        String id = itemId(s);
        int c = id.indexOf(':');
        String name;
        try {
            name = s.getHoverName().getString();
        } catch (Throwable t) {
            name = id;
        }
        return new Gear.Item(id, itemKey(s), name, c > 0 ? id.substring(0, c) : "minecraft", enchantments(s), false);
    }

    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    /** Armor: head, chest, legs, feet */
    public static Gear.Item[] armor(Player p) {
        Gear.Item[] out = new Gear.Item[4];
        for (int i = 0; i < 4; i++) out[i] = item(p.getItemBySlot(ARMOR[i]));
        return out;
    }

    /** 0 head ... 3 feet, -1 = not armor */
    public static int armorSlot(ItemStack s) {
        if (s == null || s.isEmpty()) return -1;
        EquipmentSlot slot = Mob.getEquipmentSlotForItem(s);
        for (int i = 0; i < 4; i++) if (ARMOR[i] == slot) return i;
        return -1;
    }

    private static void block(Context c, String slot, Level w, BlockPos pos) {
        BlockState st = w.getBlockState(pos);
        if (st.isAir()) return;
        Block b = st.getBlock();
        ResourceLocation id = Registry.BLOCK.getKey(b);
        String name;
        try {
            // Block#getName is client-only on this version
            name = new TranslatableComponent(b.getDescriptionId()).getString();
        } catch (Throwable t) {
            name = "";
        }
        if (name == null || name.isEmpty() || name.startsWith("block.")) name = id == null ? b.getClass().getSimpleName() : id.toString();
        c.add(slot, b.getDescriptionId(), name, id == null ? "" : id.getNamespace());
    }

    /** Block / fluid at the feet (or the block stood on) and at the body centre, and the held items */
    public static String environment(Player p) {
        Context c = new Context();
        Level w = p.level;
        BlockPos feet = new BlockPos(p.getX(), p.getY(), p.getZ());
        if (w.isEmptyBlock(feet)) block(c, Context.FEET, w, feet.below());
        else block(c, Context.FEET, w, feet);
        BlockPos body = new BlockPos(p.getX(), p.getY() + p.getBbHeight() / 2.0, p.getZ());
        if (!body.equals(feet)) block(c, Context.BODY, w, body);
        Gear.Item main = item(p.getMainHandItem());
        Gear.Item off = item(p.getOffhandItem());
        if (main != null) c.add(Context.MAIN, main.key, main.name, main.modId);
        if (off != null) c.add(Context.OFF, off.key, off.name, off.modId);
        return c.toString();
    }

    /** "minecraft:overworld", "minecraft:the_nether", ... (mod dimensions: their level key) */
    public static String dimension(Level w) {
        try {
            return w.dimension().location().toString();
        } catch (Throwable t) {
            return "";
        }
    }
}
