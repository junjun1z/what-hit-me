package io.github.junjun1z.whathitme.core.data;

import java.util.ArrayList;
import java.util.List;

import io.github.junjun1z.whathitme.core.net.WireIn;
import io.github.junjun1z.whathitme.core.net.WireOut;

/** Where the player was and what it wore at a death: appended to [Copy Recap] (includeEquipmentInCopy). Immutable data. */
public final class Gear {

    /** One item stack as plain strings */
    public static final class Item {
        public final String id;
        public final String key;
        public final String name;
        public final String modId;
        /** "translationKey|level|English name" separated by ';' */
        public final String enchants;
        public final boolean broken;

        public Item(String id, String key, String name, String modId, String enchants, boolean broken) {
            this.id = id == null ? "" : id;
            this.key = key == null ? "" : key;
            this.name = name == null ? "" : name;
            this.modId = modId == null ? "" : modId;
            this.enchants = enchants == null ? "" : enchants;
            this.broken = broken;
        }

        public Item broken() {
            return new Item(id, key, name, modId, enchants, true);
        }

        void write(WireOut o) {
            o.writeString(id);
            o.writeString(key);
            o.writeString(name);
            o.writeString(modId);
            o.writeString(enchants);
            o.writeBoolean(broken);
        }

        static Item read(WireIn in) {
            return new Item(in.readString(), in.readString(), in.readString(), in.readString(), in.readString(), in.readBoolean());
        }
    }

    public static final String[] SLOTS = {"head", "chest", "legs", "feet"};

    public final String dimension;
    /** NaN = unknown / hidden */
    public final double x;
    public final double y;
    public final double z;
    public final float maxHealth;
    /** head, chest, legs, feet; null = empty */
    public final Item[] armor;
    /** Active effects ({@link Entry#ACTIVE_EFFECT}) */
    public final List<Entry> effects;

    public Gear(String dimension, double x, double y, double z, float maxHealth, Item[] armor, List<Entry> effects) {
        this.dimension = dimension == null ? "" : dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.maxHealth = maxHealth;
        this.armor = new Item[4];
        if (armor != null) System.arraycopy(armor, 0, this.armor, 0, Math.min(4, armor.length));
        this.effects = effects == null ? new ArrayList<>() : new ArrayList<>(effects);
    }

    public boolean hasPosition() {
        return !Double.isNaN(x);
    }

    /** A copy without the position (includeCoordsInCopy off on the server) */
    public Gear withoutPosition() {
        return new Gear(dimension, Double.NaN, Double.NaN, Double.NaN, maxHealth, armor, effects);
    }

    /** Marks pieces that broke shortly before (from the armor break lines of a recap) */
    public Gear withBroken(List<DamageRecord> records) {
        Item[] a = armor.clone();
        for (DamageRecord r : records) {
            if (!r.has(DamageRecord.ARMOR_BREAK)) continue;
            Context.Part p = Context.find(r.context, Context.ARMOR);
            if (p == null) continue;
            int slot = -1;
            for (int i = 0; i < 4; i++) if (r.damageType.equals("armorBroken." + SLOTS[i])) slot = i;
            if (slot >= 0 && a[slot] == null) a[slot] = new Item("", p.key, p.name, p.modId, "", true);
        }
        return new Gear(dimension, x, y, z, maxHealth, a, effects);
    }

    public void write(WireOut o) {
        o.writeString(dimension);
        o.writeBoolean(hasPosition());
        if (hasPosition()) {
            o.writeFloat((float) x);
            o.writeFloat((float) y);
            o.writeFloat((float) z);
        }
        o.writeFloat(maxHealth);
        for (Item i : armor) {
            o.writeBoolean(i != null);
            if (i != null) i.write(o);
        }
        int n = Math.min(effects.size(), 64);
        o.writeByte(n);
        for (int i = 0; i < n; i++) effects.get(i).write(o);
    }

    public static Gear read(WireIn in) {
        String dim = in.readString();
        double x = Double.NaN;
        double y = Double.NaN;
        double z = Double.NaN;
        if (in.readBoolean()) {
            x = in.readFloat();
            y = in.readFloat();
            z = in.readFloat();
        }
        float max = in.readFloat();
        Item[] a = new Item[4];
        for (int i = 0; i < 4; i++) if (in.readBoolean()) a[i] = Item.read(in);
        int n = in.readUnsignedByte();
        List<Entry> eff = new ArrayList<>();
        for (int i = 0; i < n; i++) eff.add(Entry.read(in));
        return new Gear(dim, x, y, z, max, a, eff);
    }
}
