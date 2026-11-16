package io.github.junjun1z.whathitme.core.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.junjun1z.whathitme.core.net.WireIn;
import io.github.junjun1z.whathitme.core.net.WireOut;

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
    /** Position and equipment at a death (null = unknown) */
    public final Gear gear;

    public Snapshot(int kind, long time, List<DamageRecord> records, int keyIndex, String deathMessage) {
        this(kind, time, records, keyIndex, deathMessage, null);
    }

    public Snapshot(int kind, long time, List<DamageRecord> records, int keyIndex, String deathMessage, Gear gear) {
        this.gear = gear;
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
        return new Snapshot(kind, time, records, keyIndex, json, gear);
    }

    public Snapshot withGear(Gear g) {
        return new Snapshot(kind, time, records, keyIndex, deathMessage, g);
    }

    public Snapshot withRecords(List<DamageRecord> list, int key) {
        return new Snapshot(kind, time, list, key, deathMessage, gear);
    }

    /** Protocol 1 (What Hit Me? 1.0 / 1.1 clients): no armor break lines, no extra fields */
    public void write(WireOut buf, long now) {
        List<DamageRecord> list = new ArrayList<>();
        int key = -1;
        for (int i = 0; i < records.size(); i++) {
            DamageRecord r = records.get(i);
            if (r.has(DamageRecord.ARMOR_BREAK)) continue;
            if (i == keyIndex) key = list.size();
            list.add(r);
        }
        buf.writeByte(kind);
        buf.writeLong(Math.max(0, now - time));
        buf.writeShort(list.size());
        for (DamageRecord r : list) r.write(buf, now);
        buf.writeShort(key);
        buf.writeString(deathMessage.length() > 8000 ? "" : deathMessage);
    }

    /** Protocol 2: everything */
    public void write2(WireOut buf, long now) {
        write2(buf, now, false);
    }

    /** Protocol 2 body; v3 adds the rule result of each record */
    public void write2(WireOut buf, long now, boolean v3) {
        buf.writeByte(kind);
        buf.writeLong(Math.max(0, now - time));
        buf.writeShort(records.size());
        for (DamageRecord r : records) {
            r.write(buf, now);
            r.writeExtra(buf);
            if (v3) r.writeMark(buf);
        }
        buf.writeShort(keyIndex);
        buf.writeString(deathMessage.length() > 8000 ? "" : deathMessage);
        buf.writeBoolean(gear != null);
        if (gear != null) gear.write(buf);
    }

    public static Snapshot read2(WireIn buf, long now) {
        return read2(buf, now, false);
    }

    public static Snapshot read2(WireIn buf, long now, boolean v3) {
        int kind = buf.readByte();
        long time = now - buf.readLong();
        int n = buf.readShort();
        List<DamageRecord> list = new ArrayList<>(Math.max(0, n));
        for (int i = 0; i < n; i++) {
            DamageRecord r = DamageRecord.read(buf, now).readExtra(buf);
            list.add(v3 ? r.readMark(buf) : r);
        }
        int key = buf.readShort();
        String dm = buf.readString();
        Gear g = buf.readBoolean() ? Gear.read(buf) : null;
        return new Snapshot(kind, time, list, key, dm, g);
    }

    public static Snapshot read(WireIn buf, long now) {
        int kind = buf.readByte();
        long time = now - buf.readLong();
        int n = buf.readShort();
        List<DamageRecord> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) list.add(DamageRecord.read(buf, now));
        int key = buf.readShort();
        return new Snapshot(kind, time, list, key, buf.readString());
    }
}
