package io.github.junjun1z.whathitme.mc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.data.Entry;
import io.github.junjun1z.whathitme.core.data.Registries;
import net.minecraft.core.Registry;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;

/** Registries of 1.16 (entities, effects; damage types are msgIds, no registry) and active effects. */
public final class Registries116 implements Registries {

    /** Damage type names of vanilla 1.16 (melee "mob" / "player" excluded) */
    private static final List<String> VANILLA = Arrays.asList("inFire", "lightningBolt", "onFire", "lava", "hotFloor", "inWall", "cramming", "drown",
        "starve", "cactus", "fall", "flyIntoWall", "outOfWorld", "generic", "magic", "wither", "anvil", "fallingBlock", "dragonBreath", "dryout",
        "sweetBerryBush", "sting", "arrow", "trident", "fireworks", "fireball", "witherSkull", "thrown", "indirectMagic", "thorns", "explosion",
        "explosion.player", "badRespawnPoint");

    /**
     * Curative items of an active effect (names) and whether milk cures it. Vanilla: milk removes every effect. Forge
     * replaces this with MobEffectInstance#getCurativeItems.
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

    private static volatile java.lang.reflect.Field categoryField;

    /** MobEffect#getCategory is client-only on this version: read the field (by type, any mapping) */
    public static boolean harmful(MobEffect e) {
        try {
            java.lang.reflect.Field f = categoryField;
            if (f == null) {
                for (java.lang.reflect.Field c : MobEffect.class.getDeclaredFields()) {
                    if (c.getType() == MobEffectCategory.class) {
                        c.setAccessible(true);
                        categoryField = f = c;
                        break;
                    }
                }
            }
            return f != null && f.get(e) == MobEffectCategory.HARMFUL;
        } catch (Throwable t) {
            return false;
        }
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
        for (EntityType<?> t : Registry.ENTITY_TYPE) {
            ResourceLocation id = Registry.ENTITY_TYPE.getKey(t);
            if (id == null) continue;
            String key = t.getDescriptionId();
            out.add(new Entry(Entry.MOB, id.toString(), tr(key), key, id.getNamespace(), Platform.modName(id.getNamespace()), 0, 0, 0, 0, false, ""));
        }
        return out;
    }

    @Override
    public List<Entry> effects() {
        List<Entry> out = new ArrayList<>();
        for (MobEffect e : Registry.MOB_EFFECT) {
            ResourceLocation id = Registry.MOB_EFFECT.getKey(e);
            if (id == null) continue;
            String key = e.getDescriptionId();
            out.add(new Entry(Entry.EFFECT, id.toString(), tr(key), key, id.getNamespace(), Platform.modName(id.getNamespace()),
                harmful(e) ? 1 : 0, 0, 0, 0, false, ""));
        }
        return out;
    }

    @Override
    public List<String> livingEntityIds() {
        List<String> out = new ArrayList<>();
        for (EntityType<?> t : Registry.ENTITY_TYPE) {
            boolean living;
            try {
                living = DefaultAttributes.hasSupplier(t);
            } catch (Throwable e) {
                living = false;
            }
            if (living) out.add(String.valueOf(Registry.ENTITY_TYPE.getKey(t)));
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
            ResourceLocation id = Registry.MOB_EFFECT.getKey(p);
            String mod = id == null ? "minecraft" : id.getNamespace();
            Cure c;
            try {
                c = cures.apply(pe);
            } catch (Throwable t) {
                c = new Cure(false, "");
            }
            out.add(new Entry(Entry.ACTIVE_EFFECT, id == null ? p.getDescriptionId() : id.toString(), tr(p.getDescriptionId()), p.getDescriptionId(), mod,
                Platform.modName(mod), harmful(p) ? 1 : 0, 0, pe.getAmplifier(), pe.getDuration(), c.milk, c.items));
        }
        return out;
    }
}
