package io.github.junjun1z.whathitme.mc;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import io.github.junjun1z.whathitme.core.data.DamageRecord;
import io.github.junjun1z.whathitme.core.data.EntityFacts;
import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.data.HitFacts;
import io.github.junjun1z.whathitme.core.data.TypeModResolver;
import net.minecraft.core.Registry;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * Minecraft 1.18.2 damage sources (msgId only, no DamageType registry before 1.19.4) and entities to plain facts (core
 * {@link HitFacts} / {@link EntityFacts}). The damage id is "&lt;mod&gt;:&lt;msgId&gt;" ("minecraft:outOfWorld"), the mod found
 * by {@link TypeModResolver}.
 */
public final class Facts {

    /** Mod id of the jar / directory a class was loaded from (null = Minecraft / loader / unknown); set by the loader */
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
        if (e instanceof OwnableEntity) {
            OwnableEntity o = (OwnableEntity) e;
            Entity owner = o.getOwner();
            if (owner != null) return entityName(owner);
            UUID id = o.getOwnerUUID();
            if (id != null) return id.toString().substring(0, 8);
        }
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
            net.minecraft.resources.ResourceLocation id = net.minecraft.core.Registry.ITEM.getKey(s.getItem());
            return id == null ? "" : id.toString();
        } catch (Throwable t) {
            return "";
        }
    }

    /** An item stack as plain strings (null for an empty stack) */
    public static io.github.junjun1z.whathitme.core.data.Gear.Item item(ItemStack s) {
        if (s == null || s.isEmpty()) return null;
        String id = itemId(s);
        int c = id.indexOf(':');
        String name;
        try {
            name = s.getHoverName().getString();
        } catch (Throwable t) {
            name = id;
        }
        return new io.github.junjun1z.whathitme.core.data.Gear.Item(id, itemKey(s), name, c > 0 ? id.substring(0, c) : "minecraft", enchantments(s), false);
    }

    private static final net.minecraft.world.entity.EquipmentSlot[] ARMOR = {net.minecraft.world.entity.EquipmentSlot.HEAD,
        net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET};

    /** Armor: head, chest, legs, feet */
    public static io.github.junjun1z.whathitme.core.data.Gear.Item[] armor(Player p) {
        io.github.junjun1z.whathitme.core.data.Gear.Item[] out = new io.github.junjun1z.whathitme.core.data.Gear.Item[4];
        for (int i = 0; i < 4; i++) out[i] = item(p.getItemBySlot(ARMOR[i]));
        return out;
    }

    /** 0 head ... 3 feet, -1 = not armor */
    public static int armorSlot(ItemStack s) {
        if (s == null || s.isEmpty()) return -1;
        net.minecraft.world.entity.EquipmentSlot slot = net.minecraft.world.entity.Mob.getEquipmentSlotForItem(s);
        for (int i = 0; i < 4; i++) if (ARMOR[i] == slot) return i;
        return -1;
    }

    private static void block(io.github.junjun1z.whathitme.core.data.Context c, String slot, net.minecraft.world.level.Level w, net.minecraft.core.BlockPos pos) {
        net.minecraft.world.level.block.state.BlockState st = w.getBlockState(pos);
        if (st.isAir()) return;
        net.minecraft.world.level.block.Block b = st.getBlock();
        net.minecraft.resources.ResourceLocation id = net.minecraft.core.Registry.BLOCK.getKey(b);
        String name;
        try {
            name = new net.minecraft.network.chat.TranslatableComponent(b.getDescriptionId()).getString();
        } catch (Throwable t) {
            name = "";
        }
        if (name == null || name.isEmpty() || name.startsWith("block.")) name = id == null ? b.getClass().getSimpleName() : id.toString();
        c.add(slot, b.getDescriptionId(), name, id == null ? "" : id.getNamespace());
    }

    private static net.minecraft.core.BlockPos pos(double x, double y, double z) {
        return new net.minecraft.core.BlockPos(net.minecraft.util.Mth.floor(x), net.minecraft.util.Mth.floor(y), net.minecraft.util.Mth.floor(z));
    }

    /** Block / fluid at the feet (or the block stood on) and at the body centre, and the held items */
    public static String environment(Player p) {
        io.github.junjun1z.whathitme.core.data.Context c = new io.github.junjun1z.whathitme.core.data.Context();
        net.minecraft.world.level.Level w = p.level;
        net.minecraft.core.BlockPos feet = pos(p.getX(), p.getY(), p.getZ());
        if (w.isEmptyBlock(feet)) block(c, io.github.junjun1z.whathitme.core.data.Context.FEET, w, feet.below());
        else block(c, io.github.junjun1z.whathitme.core.data.Context.FEET, w, feet);
        net.minecraft.core.BlockPos body = pos(p.getX(), p.getY() + p.getBbHeight() / 2.0, p.getZ());
        if (!body.equals(feet)) block(c, io.github.junjun1z.whathitme.core.data.Context.BODY, w, body);
        io.github.junjun1z.whathitme.core.data.Gear.Item main = item(p.getMainHandItem());
        io.github.junjun1z.whathitme.core.data.Gear.Item off = item(p.getOffhandItem());
        if (main != null) c.add(io.github.junjun1z.whathitme.core.data.Context.MAIN, main.key, main.name, main.modId);
        if (off != null) c.add(io.github.junjun1z.whathitme.core.data.Context.OFF, off.key, off.name, off.modId);
        return c.toString();
    }

    /** "minecraft:overworld", "minecraft:the_nether", ... (mod dimensions: their level key) */
    public static String dimension(net.minecraft.world.level.Level w) {
        try {
            return w.dimension().location().toString();
        } catch (Throwable t) {
            return "";
        }
    }
}
