package io.github.junjun1z.whathitme.mc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.data.Entry;
import io.github.junjun1z.whathitme.core.data.Registries;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;

/** Registries of 1.19.3 (entities, effects, damage types) and active effects. */
public final class Registries1193 implements Registries {

    /** Vanilla damage type names (msgIds) of 1.19.3 except melee ("mob" / "player": listed as mob:&lt;entity&gt;) */
    private static final List<String> VANILLA = Arrays.asList("inFire", "lightningBolt", "onFire", "lava", "hotFloor", "inWall", "cramming", "drown",
        "starve", "cactus", "fall", "flyIntoWall", "outOfWorld", "generic", "magic", "wither", "anvil", "fallingBlock", "dragonBreath", "dryout",
        "sweetBerryBush", "freeze", "fallingStalactite", "stalagmite", "fireworks", "arrow", "trident", "thrown", "indirectMagic", "thorns",
        "explosion", "explosion.player", "fireball", "witherSkull", "sting", "sonic_boom", "badRespawnPoint");

    /**
     * Curative items of an active effect (names) and whether milk cures it. Vanilla: milk removes every effect. Forge
     * 1.19.3 replaces this with MobEffectInstance#getCurativeItems.
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

    public Registries1193(Supplier<RegistryAccess> access) {
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

    @Override
    public List<String> vanillaDamageIds() {
        List<String> out = new ArrayList<>();
        for (String t : VANILLA) out.add("minecraft:" + t);
        return out;
    }

    /** Active effects of an entity */
    public static List<Entry> effects(LivingEntity entity) {
        List<Entry> out = new ArrayList<>();
        for (MobEffectInstance pe : entity.getActiveEffects()) {
            MobEffect p = pe.getEffect();
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

    /** Mod jars / directories with their mod id (set by the loader; without What Hit Me? itself) */
    public static volatile java.util.function.Supplier<java.util.Map<java.io.File, String>> modFiles = java.util.HashMap::new;

    /** Every static DamageSource field of DamageSource (vanilla) with its exact properties, plus the other vanilla type names */
    @Override
    public List<Entry> damageTypeEntries() {
        List<Entry> out = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (java.lang.reflect.Field f : DamageSource.class.getDeclaredFields()) {
            try {
                if (!java.lang.reflect.Modifier.isStatic(f.getModifiers()) || f.getType() != DamageSource.class) continue;
                f.setAccessible(true);
                DamageSource src = (DamageSource) f.get(null);
                if (src == null) continue;
                String type = src.getMsgId();
                String id = Facts.TYPES.typeId(type, src.getClass(), true);
                if (!seen.add(id)) continue;
                int c = id.indexOf(':');
                String mod = c > 0 ? id.substring(0, c) : "minecraft";
                out.add(new Entry(Entry.DAMAGE, id, type, "", mod, Platform.modName(mod), Facts.sourceFlags(src), 0, 0, 0, false, ""));
            } catch (Throwable ignored) {
                // skip odd fields
            }
        }
        for (Entry e : Registries.super.damageTypeEntries()) if (seen.add(e.id)) out.add(e);
        return out;
    }

    private static volatile java.util.Map<String, Integer> setters;

    /** Setters of DamageSource by their runtime name, found by calling each on a fresh instance and comparing the flags */
    static java.util.Map<String, Integer> setters() {
        java.util.Map<String, Integer> m = setters;
        if (m != null) return m;
        m = new java.util.HashMap<>();
        for (java.lang.reflect.Method me : DamageSource.class.getDeclaredMethods()) {
            if (java.lang.reflect.Modifier.isStatic(me.getModifiers()) || me.getParameterCount() != 0 || me.getReturnType() != DamageSource.class) continue;
            try {
                DamageSource d = new DamageSource("whathitme_probe") {
                };
                int before = Facts.sourceFlags(d);
                me.setAccessible(true);
                if (me.invoke(d) != d) continue;
                m.put(me.getName(), Facts.sourceFlags(d) & ~before);
            } catch (Throwable ignored) {
                // not a setter
            }
        }
        setters = m;
        return m;
    }

    /** DamageSource and its vanilla subclasses by their runtime names, and every mod file */
    @Override
    public io.github.junjun1z.whathitme.core.data.DamageIdScanner.Input scanInput() {
        List<String> base = new ArrayList<>();
        for (Class<?> c : new Class<?>[] {DamageSource.class, net.minecraft.world.damagesource.EntityDamageSource.class,
            net.minecraft.world.damagesource.IndirectEntityDamageSource.class}) base.add(c.getName().replace('.', '/'));
        return new io.github.junjun1z.whathitme.core.data.DamageIdScanner.Input(base, modFiles.get(), Platform.mods.modIds(), setters());
    }
}
