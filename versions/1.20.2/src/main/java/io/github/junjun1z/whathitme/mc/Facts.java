package io.github.junjun1z.whathitme.mc;

import java.util.Map;
import java.util.UUID;

import io.github.junjun1z.whathitme.core.data.DamageRecord;
import io.github.junjun1z.whathitme.core.data.EntityFacts;
import io.github.junjun1z.whathitme.core.data.HitFacts;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * Minecraft 1.20.2 damage sources (DamageType registry, 1.19.4+) and entities to plain facts (core {@link HitFacts} /
 * {@link EntityFacts}). The damage id is the DamageType registry key ("minecraft:out_of_world"), the damage type name
 * its msgId ("outOfWorld", the same names as 1.12).
 */
public final class Facts {

    private Facts() {}

    /** Registry id of an entity ("minecraft:player" for players) */
    public static String entityId(Entity e) {
        if (e == null) return "";
        if (e instanceof Player) return "minecraft:player";
        ResourceLocation k = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
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
            if (n.getContents() instanceof TranslatableContents) return ((TranslatableContents) n.getContents()).getKey();
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
        if (src.is(DamageTypeTags.BYPASSES_ARMOR)) f |= DamageRecord.BYPASS_ARMOR;
        if (src.is(DamageTypeTags.BYPASSES_EFFECTS) || src.is(DamageTypeTags.BYPASSES_RESISTANCE)) f |= DamageRecord.ABSOLUTE;
        if (src.is(DamageTypeTags.WITCH_RESISTANT_TO)) f |= DamageRecord.MAGIC;
        if (src.is(DamageTypeTags.IS_FIRE)) f |= DamageRecord.FIRE;
        if (src.is(DamageTypeTags.IS_EXPLOSION)) f |= DamageRecord.EXPLOSION;
        if (src.is(DamageTypeTags.IS_PROJECTILE)) f |= DamageRecord.PROJECTILE;
        if (src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) f |= DamageRecord.BYPASS_CREATIVE;
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
            }
        }
        return f;
    }

    /** Registry key of a damage type ("minecraft:out_of_world"), "unknown:&lt;msgId&gt;" when unregistered */
    public static String typeId(Holder<DamageType> type) {
        return type.unwrapKey().map(ResourceKey::location).map(ResourceLocation::toString).orElse("unknown:" + type.value().msgId());
    }

    /** Melee by an entity: the vanilla attack types, or a mod's type with the msgId "mob" / "player" */
    public static boolean isMelee(String typeId, String msgId) {
        if (typeId.startsWith("minecraft:")) {
            return typeId.equals("minecraft:mob_attack") || typeId.equals("minecraft:mob_attack_no_aggro") || typeId.equals("minecraft:player_attack");
        }
        return "mob".equals(msgId) || "player".equals(msgId);
    }

    /** Facts of a hit on a player */
    public static HitFacts hit(Player victim, DamageSource src) {
        HitFacts h = new HitFacts();
        Entity atk = attacker(src);
        Entity direct = src.getDirectEntity();
        String msg = src.type().msgId();
        h.typeId = typeId(src.typeHolder());
        h.melee = isMelee(h.typeId, msg);
        // mob_projectile, spit, wind_charge share the msgId "mob": name them by their key instead
        h.damageType = !h.melee && ("mob".equals(msg) || "player".equals(msg)) ? h.typeId.substring(h.typeId.indexOf(':') + 1) : msg;
        h.flags = sourceFlags(src);
        if (atk != null && atk != victim) h.attacker = entity(atk, victim);
        h.directName = direct != null && direct != atk && direct != victim ? entityName(direct) : "";
        return h;
    }
}
