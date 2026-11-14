package io.github.junjun1z.whmdriver;

/**
 * Vanilla commands used by the multiplayer tests, in the syntax of one Minecraft version (1.12 effect / replaceitem,
 * 1.13+ "effect give", 1.17+ "item replace", 1.20.5+ item components, 1.21.5+ "equipment"; before 1.11 capitalised
 * entity names, before 1.9 "Equipment" and numeric effect ids, 1.7.10 numeric item ids and no @e selector). Development only.
 * A command may be several commands separated by '\n'.
 */
public final class VanillaCommands {

    private final int[] v;

    private VanillaCommands(String mcVersion) {
        String[] p = mcVersion.split("[.\\-]");
        v = new int[3];
        for (int i = 0; i < 3 && i < p.length; i++) {
            try {
                v[i] = Integer.parseInt(p[i]);
            } catch (NumberFormatException e) {
                v[i] = 0;
            }
        }
    }

    public static VanillaCommands of(String mcVersion) {
        return new VanillaCommands(mcVersion);
    }

    /** Is this version at least major.minor.patch? (26.x counts as newer than every 1.x) */
    public boolean atLeast(int major, int minor, int patch) {
        if (v[0] != major) return v[0] > major;
        if (v[1] != minor) return v[1] > minor;
        return v[2] >= patch;
    }

    private boolean legacy() {
        return !atLeast(1, 13, 0);
    }

    /** The Totem of Undying exists (1.11+) */
    public boolean hasTotem() {
        return atLeast(1, 11, 0);
    }

    /** 1.7.10 has no @e selector */
    private boolean noEntitySelector() {
        return !atLeast(1, 8, 0);
    }

    /** Entity name for /summon and selectors: "zombie" (1.11+) or "Zombie" */
    private String mob(String id) {
        if (atLeast(1, 11, 0)) return id;
        return Character.toUpperCase(id.charAt(0)) + id.substring(1);
    }

    /** Numeric potion ids (needed by 1.7.10, accepted up to 1.12) */
    private static int potionNumber(String id) {
        switch (id) {
            case "instant_health":
                return 6;
            case "instant_damage":
                return 7;
            case "resistance":
                return 11;
            case "fire_resistance":
                return 12;
            case "poison":
                return 19;
            default:
                throw new IllegalArgumentException(id);
        }
    }

    /** Numeric item ids of 1.7.10 */
    private static int itemNumber(String id) {
        switch (id) {
            case "iron_sword":
                return 267;
            case "bow":
                return 261;
            case "leather_helmet":
                return 298;
            default:
                throw new IllegalArgumentException(id);
        }
    }

    public String effectClear() {
        return legacy() ? "/effect @p clear" : "/effect clear @p";
    }

    /** e.g. effect("instant_health", 1, 10) */
    public String effect(String id, int seconds, int amplifier) {
        if (!atLeast(1, 9, 0)) return "/effect @p " + potionNumber(id) + " " + seconds + " " + amplifier;
        return legacy() ? "/effect @p " + id + " " + seconds + " " + amplifier : "/effect give @p minecraft:" + id + " " + seconds + " " + amplifier;
    }

    public String totemOffhand() {
        if (legacy()) return "/replaceitem entity @p slot.weapon.offhand minecraft:totem_of_undying";
        if (!atLeast(1, 17, 0)) return "/replaceitem entity @p weapon.offhand minecraft:totem_of_undying";
        return "/item replace entity @p weapon.offhand with minecraft:totem_of_undying";
    }

    private String item(String id) {
        if (noEntitySelector()) return "{id:" + itemNumber(id) + ",Count:1b}";
        return atLeast(1, 20, 5) ? "{id:\"minecraft:" + id + "\",count:1}" : "{id:\"minecraft:" + id + "\",Count:1b}";
    }

    private String hands(String mainhand) {
        if (atLeast(1, 21, 5)) return "equipment:{mainhand:" + item(mainhand) + "}";
        return "HandItems:[" + item(mainhand) + ",{}]";
    }

    private String helmet(String id) {
        if (atLeast(1, 21, 5)) return "head:" + item(id);
        return "ArmorItems:[{},{},{}," + item(id) + "]";
    }

    public String summonZombieWithSword(String pos) {
        if (!atLeast(1, 9, 0)) return "/summon Zombie " + pos + " {Equipment:[" + item("iron_sword") + ",{},{},{},{}]}";
        if (!atLeast(1, 11, 0)) return "/summon Zombie " + pos + " {" + hands("iron_sword") + "}";
        return "/summon zombie " + pos + " {" + hands("iron_sword") + "}";
    }

    /** A skeleton with a bow and a helmet (so it does not burn at noon) */
    public String summonSkeletonWithBow(String pos) {
        if (!atLeast(1, 9, 0)) return "/summon Skeleton " + pos + " {Equipment:[" + item("bow") + ",{},{},{}," + item("leather_helmet") + "]}";
        if (!atLeast(1, 11, 0)) return "/summon Skeleton " + pos + " {" + hands("bow") + "," + helmet("leather_helmet") + "}";
        if (atLeast(1, 21, 5)) return "/summon skeleton " + pos + " {equipment:{mainhand:" + item("bow") + "," + helmet("leather_helmet") + "}}";
        return "/summon skeleton " + pos + " {" + hands("bow") + "," + helmet("leather_helmet") + "}";
    }

    public String summonCreeperFuse0(String pos) {
        return "/summon " + mob("creeper") + " " + pos + " {Fuse:0,ExplosionRadius:1b}";
    }

    /** Peaceful removes every monster (the only way on 1.7.10, which has no @e) */
    private static final String PEACEFUL = "/difficulty peaceful\n/difficulty normal";

    public String killNonPlayers() {
        if (noEntitySelector()) return PEACEFUL;
        return "/kill @e[type=!" + mob("player") + "]";
    }

    /** Kills every entity of a (hostile) type */
    public String killType(String type) {
        if (noEntitySelector()) return PEACEFUL;
        return "/kill @e[type=" + mob(type) + "]";
    }
}
