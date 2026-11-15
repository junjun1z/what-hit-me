package io.github.junjun1z.whathitme.core.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import io.github.junjun1z.whathitme.core.config.WhmConfig;

/** Server side state: one {@link PlayerHistory} per online player, the damage catalog and which clients have the mod. */
public final class DamageTracker {

    public static final DamageTracker SERVER = new DamageTracker();

    private final Map<UUID, PlayerHistory> histories = new HashMap<>();
    private final Set<UUID> moddedClients = new HashSet<>();
    private final Map<UUID, Integer> protocols = new HashMap<>();
    public final DamageCatalog catalog = new DamageCatalog();

    public synchronized PlayerHistory get(UUID id) {
        PlayerHistory h = histories.get(id);
        int cap = WhmConfig.common.historySize;
        if (h == null) {
            h = new PlayerHistory(cap);
            histories.put(id, h);
        } else {
            h.resize(cap);
        }
        return h;
    }

    public synchronized PlayerHistory peek(UUID id) {
        return histories.get(id);
    }

    public synchronized void remove(UUID id) {
        histories.remove(id);
        moddedClients.remove(id);
        protocols.remove(id);
    }

    public synchronized void setModded(UUID id) {
        setModded(id, 1);
    }

    public synchronized void setModded(UUID id, int protocol) {
        moddedClients.add(id);
        protocols.put(id, protocol);
    }

    /** Protocol the client said hello with (1 = What Hit Me? 1.0 / 1.1) */
    public synchronized int protocol(UUID id) {
        Integer p = protocols.get(id);
        return p == null ? 1 : p;
    }

    public synchronized boolean isModded(UUID id) {
        return moddedClients.contains(id);
    }

    public synchronized Set<UUID> modded() {
        return new HashSet<>(moddedClients);
    }

    /** Server stopped (also the integrated server of single player) */
    public synchronized void clear() {
        histories.clear();
        moddedClients.clear();
        protocols.clear();
        catalog.clear();
    }
}
