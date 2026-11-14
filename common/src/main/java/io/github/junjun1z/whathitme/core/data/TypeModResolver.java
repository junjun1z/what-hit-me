package io.github.junjun1z.whathitme.core.data;

import java.io.File;
import java.net.URL;
import java.security.CodeSource;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Finds the mod that added a damage type on versions without a damage type registry (1.12 - 1.19.3), where a damage
 * source only has a name ("fall", "chaos"). Computed once per damage type and cached. From 1.19.4 on, the DamageType
 * registry key already names the mod and this class is not needed.
 */
public final class TypeModResolver {

    /** Damage type names of vanilla 1.12 - 1.19.3 */
    public static final Set<String> VANILLA_TYPES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("inFire", "lightningBolt", "onFire", "lava",
        "hotFloor", "inWall", "cramming", "drown", "starve", "cactus", "fall", "flyIntoWall", "outOfWorld", "generic", "magic", "wither", "anvil",
        "fallingBlock", "dragonBreath", "fireworks", "mob", "player", "arrow", "thrown", "indirectMagic", "thorns", "explosion", "explosion.player",
        "fireball", "sweeping", "sweetBerryBush", "trident", "dryout", "freeze", "fallingStalactite", "stalagmite", "sting", "witherSkull",
        "sonic_boom", "badRespawnPoint", "onFire")));

    /** What the loader knows about mods */
    public interface ModIndex {
        boolean isMod(String id);

        /** Mod id of the jar / directory a class was loaded from, null for Minecraft, the loader and unknown sources */
        String modOfSource(File source);
    }

    private final ModIndex mods;
    private final Map<String, String> cache = new ConcurrentHashMap<>();
    /** Package prefixes skipped when walking the stack (the game, the loader, us) */
    private final String[] skip;

    public TypeModResolver(ModIndex mods, String... extraSkip) {
        this.mods = mods;
        String[] base = {"java.", "javax.", "sun.", "jdk.", "net.minecraft.", "net.minecraftforge.", "net.neoforged.", "net.fabricmc.", "cpw.mods.",
            "io.github.junjun1z.whathitme.", "com.google.", "io.netty.", "org.spongepowered.", "com.mojang."};
        skip = Arrays.copyOf(base, base.length + extraSkip.length);
        System.arraycopy(extraSkip, 0, skip, base.length, extraSkip.length);
    }

    /**
     * Mod id of a damage type.
     *
     * @param type the msgId
     * @param sourceClass class of the DamageSource (a mod's subclass names its mod)
     * @param vanillaClass true when the source class is a Minecraft class
     */
    public String typeMod(String type, Class<?> sourceClass, boolean vanillaClass) {
        String cached = cache.get(type);
        if (cached != null) return cached;
        String mod = find(type, sourceClass, vanillaClass);
        if (cache.size() < 4096) cache.put(type, mod);
        return mod;
    }

    /** "&lt;mod&gt;:&lt;type&gt;", or the type itself when it already has a namespace */
    public String typeId(String type, Class<?> sourceClass, boolean vanillaClass) {
        if (type.indexOf(':') > 0) return type;
        return typeMod(type, sourceClass, vanillaClass) + ":" + type;
    }

    private String find(String type, Class<?> sourceClass, boolean vanillaClass) {
        if (VANILLA_TYPES.contains(type) && vanillaClass) return "minecraft";
        // 1. a DamageSource subclass from a mod
        String byClass = modOfClass(sourceClass);
        if (byClass != null) return byClass;
        // 2. a prefix that is a mod id ("modid.name", "modid_name")
        for (String sep : new String[] {".", "_", ":"}) {
            int i = type.indexOf(sep);
            if (i > 0) {
                String p = type.substring(0, i).toLowerCase(Locale.ROOT);
                if (mods.isMod(p)) return p;
            }
        }
        if (VANILLA_TYPES.contains(type)) return "minecraft";
        // 3. who called the damage method (only once per new damage type)
        try {
            ClassLoader cl = TypeModResolver.class.getClassLoader();
            for (StackTraceElement el : new Throwable().getStackTrace()) {
                String n = el.getClassName();
                boolean skipIt = false;
                for (String s : skip) {
                    if (n.startsWith(s)) {
                        skipIt = true;
                        break;
                    }
                }
                if (skipIt) continue;
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

    /** Mod whose jar contains the class, null for Minecraft / the loader / unknown */
    public String modOfClass(Class<?> c) {
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
            return mods.modOfSource(f);
        } catch (Throwable t) {
            return null;
        }
    }
}
