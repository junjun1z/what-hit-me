package io.github.junjun1z.whathitme.mc;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.data.DamageRecord;
import io.github.junjun1z.whathitme.core.data.EntityFacts;
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
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * Minecraft 1.17.1 damage sources (msgId, no DamageType registry) and entities to plain facts (core {@link HitFacts} /
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
}
