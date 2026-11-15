package io.github.junjun1z.whmdriver;

/**
 * Vanilla commands used by the multiplayer tests, in the syntax of one Minecraft version (1.12 effect / replaceitem,
 * 1.13+ "effect give", 1.17+ "item replace", 1.20.5+ item components, 1.21.5+ "equipment"). Development only.
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

    public String effectClear() {
        return legacy() ? "/effect @p clear" : "/effect clear @p";
    }

    /** e.g. effect("instant_health", 1, 10) */
    public String effect(String id, int seconds, int amplifier) {
        return legacy() ? "/effect @p " + id + " " + seconds + " " + amplifier : "/effect give @p minecraft:" + id + " " + seconds + " " + amplifier;
    }

    public String totemOffhand() {
        if (legacy()) return "/replaceitem entity @p slot.weapon.offhand minecraft:totem_of_undying";
        if (!atLeast(1, 17, 0)) return "/replaceitem entity @p weapon.offhand minecraft:totem_of_undying";
        return "/item replace entity @p weapon.offhand with minecraft:totem_of_undying";
    }

    private String item(String id) {
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
        return "/summon zombie " + pos + " {" + hands("iron_sword") + "}";
    }

    /** A skeleton with a bow and a helmet (so it does not burn at noon) */
    public String summonSkeletonWithBow(String pos) {
        if (atLeast(1, 21, 5)) return "/summon skeleton " + pos + " {equipment:{mainhand:" + item("bow") + "," + helmet("leather_helmet") + "}}";
        return "/summon skeleton " + pos + " {" + hands("bow") + "," + helmet("leather_helmet") + "}";
    }

    public String summonCreeperFuse0(String pos) {
        return "/summon creeper " + pos + " {Fuse:0,ExplosionRadius:1b}";
    }

    /** Kills the own player ("/kill" needs a target on 1.13 - 1.15) */
    public String killSelf() {
        return !legacy() && !atLeast(1, 16, 0) ? "/kill @s" : "/kill";
    }

    public String killNonPlayers() {
        return "/kill @e[type=!player]";
    }

    public String killType(String type) {
        return "/kill @e[type=" + type + "]";
    }
}
