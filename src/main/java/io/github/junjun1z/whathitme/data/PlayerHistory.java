package io.github.junjun1z.whathitme.data;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Damage history of one player: a fixed-size ring buffer of {@link DamageRecord}s, the last death and a few
 * totem / heavy-hit snapshots. Not thread safe: used from one thread only (server thread, or the client thread for
 * the client fallback).
 */
public final class PlayerHistory {

    private DamageRecord[] ring;
    private int head; // next write position
    private int size;

    // LivingHurtEvent -> LivingDamageEvent hand-over (no references kept, only numbers)
    public float pendingRaw;
    public float pendingAbsorption;
    public int pendingSourceHash;
    public long pendingTick = Long.MIN_VALUE;
    public boolean pendingBlocking;
    public boolean pendingIFrameBypass;
    public long lastHurtTick = Long.MIN_VALUE;

    /** The last death has not been summarized in chat yet */
    public boolean summaryPending;

    private Snapshot lastDeath;
    private final ArrayDeque<Snapshot> snapshots = new ArrayDeque<>();

    public PlayerHistory(int capacity) {
        ring = new DamageRecord[Math.max(1, capacity)];
    }

    public void resize(int capacity) {
        capacity = Math.max(1, capacity);
        if (capacity == ring.length) return;
        List<DamageRecord> keep = records();
        ring = new DamageRecord[capacity];
        head = 0;
        size = 0;
        int from = Math.max(0, keep.size() - capacity);
        for (int i = from; i < keep.size(); i++) push(keep.get(i));
    }

    private void push(DamageRecord r) {
        ring[head] = r;
        head = (head + 1) % ring.length;
        if (size < ring.length) size++;
    }

    /** Newest record or null */
    public DamageRecord last() {
        return size == 0 ? null : ring[(head - 1 + ring.length) % ring.length];
    }

    /**
     * Adds a record. When the newest record has the same id and source and is at most {@code mergeWindowMs} old, the two
     * are merged into one line (hit counter).
     *
     * @return the record now stored (merged or not)
     */
    public DamageRecord add(DamageRecord r, long mergeWindowMs) {
        DamageRecord prev = last();
        if (prev != null && mergeWindowMs > 0 && prev.sameSource(r) && r.time - prev.time <= mergeWindowMs && !prev.has(DamageRecord.KILLING | DamageRecord.TOTEM | DamageRecord.HEAVY)) {
            DamageRecord m = prev.merge(r);
            ring[(head - 1 + ring.length) % ring.length] = m;
            return m;
        }
        // interleaved damage (poison ticks between mob hits): merge with one of the last few lines and move it to the end
        if (mergeWindowMs > 0 && size > 1) {
            int look = Math.min(size, 6);
            for (int i = 2; i <= look; i++) {
                int idx = (head - i + ring.length * 2) % ring.length;
                DamageRecord old = ring[idx];
                if (old == null) break;
                if (r.time - old.time > mergeWindowMs) break;
                if (old.sameSource(r) && !old.has(DamageRecord.KILLING | DamageRecord.TOTEM | DamageRecord.HEAVY)) {
                    // shift the newer records back by one and put the merged record last
                    for (int j = i; j > 1; j--) {
                        int to = (head - j + ring.length * 2) % ring.length;
                        int from = (head - j + 1 + ring.length * 2) % ring.length;
                        ring[to] = ring[from];
                    }
                    DamageRecord m = old.merge(r);
                    ring[(head - 1 + ring.length) % ring.length] = m;
                    return m;
                }
            }
        }
        push(r);
        return r;
    }

    /** Replaces a stored record (identity) by another one, e.g. to add a marker flag */
    public boolean replace(DamageRecord old, DamageRecord now) {
        for (int i = 0; i < ring.length; i++) {
            if (ring[i] == old) {
                ring[i] = now;
                return true;
            }
        }
        return false;
    }

    /** All records, oldest first */
    public List<DamageRecord> records() {
        List<DamageRecord> out = new ArrayList<>(size);
        int start = (head - size + ring.length) % ring.length;
        for (int i = 0; i < size; i++) out.add(ring[(start + i) % ring.length]);
        return out;
    }

    /** Records not older than {@code windowMs} before {@code now}, oldest first, at most {@code max} */
    public List<DamageRecord> recent(long now, long windowMs, int max) {
        List<DamageRecord> all = records();
        List<DamageRecord> out = new ArrayList<>();
        for (DamageRecord r : all) if (now - r.time <= windowMs) out.add(r);
        return out.size() > max ? new ArrayList<>(out.subList(out.size() - max, out.size())) : out;
    }

    public void clear() {
        for (int i = 0; i < ring.length; i++) ring[i] = null;
        head = 0;
        size = 0;
    }

    public Snapshot lastDeath() {
        return lastDeath;
    }

    public void setLastDeath(Snapshot s) {
        lastDeath = s;
    }

    public void addSnapshot(Snapshot s, int max) {
        snapshots.addFirst(s);
        while (snapshots.size() > Math.max(1, max)) snapshots.removeLast();
    }

    /** Newest first */
    public List<Snapshot> snapshots() {
        return new ArrayList<>(snapshots);
    }
}
