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
}
