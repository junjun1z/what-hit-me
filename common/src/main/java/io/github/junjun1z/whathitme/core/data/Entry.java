package io.github.junjun1z.whathitme.core.data;

import io.github.junjun1z.whathitme.core.net.WireIn;
import io.github.junjun1z.whathitme.core.net.WireOut;

/** One line of a search result, an effect list or a dump. Immutable, network friendly. */
public final class Entry {

    public static final int DAMAGE = 0;
    public static final int MOB = 1;
    public static final int EFFECT = 2;
    /** An effect currently active on a player */
    public static final int ACTIVE_EFFECT = 3;

    public final int type;
    public final String id;
    /** Display text known on the side that made the entry */
    public final String name;
    /** Translation key of the name ("" = use name) */
    public final String nameKey;
    public final String modId;
    public final String modName;
    /** DamageRecord property flags for damage; 1 = bad effect for effects */
    public final int flags;
    /** Times seen (damage) */
    public final int count;
    public final int amplifier;
    /** Ticks left (active effects) */
    public final int duration;
    public final boolean milk;
    /** Curative items, comma separated (active effects) */
    public final String extra;

    public Entry(int type, String id, String name, String nameKey, String modId, String modName, int flags, int count, int amplifier,
                 int duration, boolean milk, String extra) {
        this.type = type;
        this.id = id == null ? "" : id;
        this.name = name == null ? "" : name;
        this.nameKey = nameKey == null ? "" : nameKey;
        this.modId = modId == null ? "" : modId;
        this.modName = modName == null ? "" : modName;
        this.flags = flags;
        this.count = count;
        this.amplifier = amplifier;
        this.duration = duration;
        this.milk = milk;
        this.extra = extra == null ? "" : extra;
    }

    public void write(WireOut buf) {
        buf.writeByte(type);
        buf.writeString(id);
        buf.writeString(name);
        buf.writeString(nameKey);
        buf.writeString(modId);
        buf.writeString(modName);
        buf.writeInt(flags);
        buf.writeInt(count);
        buf.writeInt(amplifier);
        buf.writeInt(duration);
        buf.writeBoolean(milk);
        buf.writeString(extra);
    }

    public static Entry read(WireIn buf) {
        return new Entry(buf.readByte(), buf.readString(), buf.readString(), buf.readString(),
            buf.readString(), buf.readString(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(),
            buf.readBoolean(), buf.readString());
    }
}
