package io.github.junjun1z.whathitme.mc;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Entity names before 1.11 ("Zombie", "PigZombie", "modid.Name") to the registry ids of 1.11+ ("minecraft:zombie",
 * "minecraft:zombie_pigman", "modid:name"), so damage ids are the same on every version (mob:minecraft:zombie).
 */
public final class EntityIds {

    private static final Map<String, String> VANILLA = new HashMap<>();

    static {
        String[] pairs = {"AreaEffectCloud", "area_effect_cloud", "ArmorStand", "armor_stand", "Arrow", "arrow", "Bat", "bat", "Blaze", "blaze", "Boat",
            "boat", "CaveSpider", "cave_spider", "Chicken", "chicken", "Cow", "cow", "Creeper", "creeper", "DragonFireball", "dragon_fireball", "EnderCrystal",
            "ender_crystal", "EnderDragon", "ender_dragon", "Enderman", "enderman", "Endermite", "endermite", "EntityHorse", "horse", "EyeOfEnderSignal",
            "eye_of_ender_signal", "FallingSand", "falling_block", "Fireball", "fireball", "FireworksRocketEntity", "fireworks_rocket", "Ghast", "ghast",
            "Giant", "giant", "Guardian", "guardian", "Item", "item", "ItemFrame", "item_frame", "LavaSlime", "magma_cube", "LeashKnot", "leash_knot",
            "MinecartChest", "chest_minecart", "MinecartCommandBlock", "commandblock_minecart", "MinecartFurnace", "furnace_minecart", "MinecartHopper",
            "hopper_minecart", "MinecartRideable", "minecart", "MinecartSpawner", "spawner_minecart", "MinecartTNT", "tnt_minecart", "MushroomCow",
            "mooshroom", "Ozelot", "ocelot", "Painting", "painting", "Pig", "pig", "PigZombie", "zombie_pigman", "PolarBear", "polar_bear", "PrimedTnt", "tnt",
            "Rabbit", "rabbit", "Sheep", "sheep", "Shulker", "shulker", "ShulkerBullet", "shulker_bullet", "Silverfish", "silverfish", "Skeleton", "skeleton",
            "Slime", "slime", "SmallFireball", "small_fireball", "SnowMan", "snowman", "Snowball", "snowball", "SpectralArrow", "spectral_arrow", "Spider",
            "spider", "Squid", "squid", "ThrownEgg", "egg", "ThrownEnderpearl", "ender_pearl", "ThrownExpBottle", "xp_bottle", "ThrownPotion", "potion",
            "TippedArrow", "arrow", "Villager", "villager", "VillagerGolem", "villager_golem", "Witch", "witch", "WitherBoss", "wither", "WitherSkull",
            "wither_skull", "Wolf", "wolf", "XPOrb", "xp_orb", "Zombie", "zombie", "LightningBolt", "lightning_bolt", "Mob", "mob", "Monster", "monster"};
        for (int i = 0; i < pairs.length; i += 2) VANILLA.put(pairs[i], "minecraft:" + pairs[i + 1]);
    }

    private EntityIds() {}

    /** The 1.11+ style id of an entity name (null for null) */
    public static String of(String name) {
        if (name == null || name.isEmpty()) return null;
        String v = VANILLA.get(name);
        if (v != null) return v;
        int dot = name.indexOf('.');
        if (dot > 0) return name.substring(0, dot).toLowerCase(Locale.ROOT) + ":" + snake(name.substring(dot + 1));
        return "minecraft:" + snake(name);
    }

    /** "SomeName" -> "some_name" */
    static String snake(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0 && s.charAt(i - 1) != '_' && !Character.isUpperCase(s.charAt(i - 1))) sb.append('_');
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
