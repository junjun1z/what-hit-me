package io.github.junjun1z.whathitme.mc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.data.Entry;
import io.github.junjun1z.whathitme.core.data.Registries;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;

/** Registries of 1.21.3 (entities, effects, damage types) and active effects. */
public final class Registries121 implements Registries {

    /** Melee damage types: listed as mob:&lt;entity&gt; instead */
    private static final List<String> MELEE = Arrays.asList("minecraft:mob_attack", "minecraft:mob_attack_no_aggro", "minecraft:player_attack");

    /**
     * Curative items of an active effect (names) and whether milk cures it. Vanilla: milk removes every effect.
     * NeoForge replaces this with its effect cures.
     */
    public static Function<MobEffectInstance, Cure> cures = e -> new Cure(true, "Milk Bucket");

    public static final class Cure {
        public final boolean milk;
        public final String items;

        public Cure(boolean milk, String items) {
            this.milk = milk;
            this.items = items;
        }
    }

    private final Supplier<RegistryAccess> access;

    public Registries121(Supplier<RegistryAccess> access) {
        this.access = access;
    }

    private static String tr(String key) {
        if (key == null || key.isEmpty()) return "";
        return Language.getInstance().getOrDefault(key);
    }

    @Override
    public String translate(String key) {
        return tr(key);
    }

    @Override
    public List<Entry> mobs() {
        List<Entry> out = new ArrayList<>();
        for (EntityType<?> t : BuiltInRegistries.ENTITY_TYPE) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(t);
            if (id == null) continue;
            String key = t.getDescriptionId();
            out.add(new Entry(Entry.MOB, id.toString(), tr(key), key, id.getNamespace(), Platform.modName(id.getNamespace()), 0, 0, 0, 0, false, ""));
        }
        return out;
    }

    @Override
    public List<Entry> effects() {
        List<Entry> out = new ArrayList<>();
        for (MobEffect e : BuiltInRegistries.MOB_EFFECT) {
            ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(e);
            if (id == null) continue;
            String key = e.getDescriptionId();
            out.add(new Entry(Entry.EFFECT, id.toString(), tr(key), key, id.getNamespace(), Platform.modName(id.getNamespace()),
                e.getCategory() == MobEffectCategory.HARMFUL ? 1 : 0, 0, 0, 0, false, ""));
        }
        return out;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> livingEntityIds() {
        List<String> out = new ArrayList<>();
        for (EntityType<?> t : BuiltInRegistries.ENTITY_TYPE) {
            boolean living;
            try {
                living = DefaultAttributes.hasSupplier((EntityType<? extends LivingEntity>) t);
            } catch (Throwable e) {
                living = false;
            }
            if (living) out.add(String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(t)));
        }
        return out;
    }

    private Registry<DamageType> damageTypes() {
        RegistryAccess a = access.get();
        return a == null ? null : a.lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE);
    }

    @Override
    public List<String> vanillaDamageIds() {
        List<String> out = new ArrayList<>();
        Registry<DamageType> reg = damageTypes();
        if (reg == null) return out;
        for (ResourceLocation k : reg.keySet()) {
            String id = k.toString();
            if ("minecraft".equals(k.getNamespace()) && !MELEE.contains(id)) out.add(id);
        }
        return out;
    }

    /**
     * Registry key of the vanilla damage type with this msgId ("outOfWorld" -> "minecraft:out_of_world"), for client
     * estimates. Several types can share a msgId: the one whose path matches the msgId in snake_case wins.
     */
    public Function<String, String> msgIdToTypeId() {
        Map<String, String> cache = new HashMap<>();
        Object[] cachedFor = {null};
        return msg -> {
            Registry<DamageType> reg = damageTypes();
            // no world yet: do not cache the guess
            if (reg == null) return "minecraft:" + msg;
            synchronized (cache) {
                if (cachedFor[0] != reg) {
                    cache.clear();
                    cachedFor[0] = reg;
                }
                return cache.computeIfAbsent(msg, m -> {
                    String snake = m.replaceAll("([a-z])([A-Z])", "$1_$2").replace('.', '_').toLowerCase(java.util.Locale.ROOT);
                    String first = null;
                    for (Map.Entry<ResourceKey<DamageType>, DamageType> e : reg.entrySet()) {
                        ResourceLocation k = e.getKey().location();
                        if (!"minecraft".equals(k.getNamespace()) || !m.equals(e.getValue().msgId())) continue;
                        if (k.getPath().equals(snake)) return k.toString();
                        if (first == null) first = k.toString();
                    }
                    return first != null ? first : "minecraft:" + m;
                });
            }
        };
    }

    /** Active effects of an entity */
    public static List<Entry> effects(LivingEntity entity) {
        List<Entry> out = new ArrayList<>();
        for (MobEffectInstance pe : entity.getActiveEffects()) {
            MobEffect p = pe.getEffect().value();
            ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(p);
            String mod = id == null ? "minecraft" : id.getNamespace();
            Cure c;
            try {
                c = cures.apply(pe);
            } catch (Throwable t) {
                c = new Cure(false, "");
            }
            out.add(new Entry(Entry.ACTIVE_EFFECT, id == null ? p.getDescriptionId() : id.toString(), tr(p.getDescriptionId()), p.getDescriptionId(), mod,
                Platform.modName(mod), p.getCategory() == MobEffectCategory.HARMFUL ? 1 : 0, 0, pe.getAmplifier(), pe.getDuration(), c.milk, c.items));
        }
        return out;
    }

    /** Every key of the DamageType registry (vanilla and mods), name = msgId, flags from its tags */
    @Override
    public List<Entry> damageTypeEntries() {
        Registry<DamageType> reg = damageTypes();
        if (reg == null) return Registries.super.damageTypeEntries();
        List<Entry> out = new ArrayList<>();
        for (Map.Entry<ResourceKey<DamageType>, DamageType> e : reg.entrySet()) {
            try {
                ResourceLocation k = e.getKey().location();
                String mod = k.getNamespace();
                out.add(new Entry(Entry.DAMAGE, k.toString(), e.getValue().msgId(), "", mod, Platform.modName(mod),
                    Facts.typeFlags(reg.getOrThrow(e.getKey())), 0, 0, 0, false, ""));
            } catch (Throwable ignored) {
                // skip odd entries
            }
        }
        return out;
    }
}
