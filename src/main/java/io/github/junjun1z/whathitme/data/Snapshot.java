package io.github.junjun1z.whathitme.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;

/** A frozen copy of the history at a death, a totem pop or a heavy hit. Immutable. */
public final class Snapshot {

    public static final int DEATH = 0;
    public static final int TOTEM = 1;
    public static final int HEAVY = 2;
    /** Lethal damage, but something other than a vanilla totem kept the player alive */
    public static final int PREVENTED = 3;
    public static final int HISTORY = 4;

    public final int kind;
    public final long time;
    /** Oldest first */
    public final List<DamageRecord> records;
    /** Index of the killing blow / trigger hit in records, -1 = none */
    public final int keyIndex;
    /** Vanilla death message as JSON ("" = none) */
    public final String deathMessage;

    public Snapshot(int kind, long time, List<DamageRecord> records, int keyIndex, String deathMessage) {
        this.kind = kind;
        this.time = time;
        this.records = Collections.unmodifiableList(new ArrayList<>(records));
        this.keyIndex = keyIndex;
        this.deathMessage = deathMessage == null ? "" : deathMessage;
    }

    public DamageRecord key() {
        return keyIndex >= 0 && keyIndex < records.size() ? records.get(keyIndex) : null;
    }

    public Snapshot withDeathMessage(String json) {
        return new Snapshot(kind, time, records, keyIndex, json);
    }

    public void write(ByteBuf buf, long now) {
        buf.writeByte(kind);
        buf.writeLong(Math.max(0, now - time));
        buf.writeShort(records.size());
        for (DamageRecord r : records) r.write(buf, now);
        buf.writeShort(keyIndex);
        ByteBufUtils.writeUTF8String(buf, deathMessage.length() > 8000 ? "" : deathMessage);
    }

    public static Snapshot read(ByteBuf buf, long now) {
        int kind = buf.readByte();
        long time = now - buf.readLong();
        int n = buf.readShort();
        List<DamageRecord> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) list.add(DamageRecord.read(buf, now));
        int key = buf.readShort();
        return new Snapshot(kind, time, list, key, ByteBufUtils.readUTF8String(buf));
    }
}
