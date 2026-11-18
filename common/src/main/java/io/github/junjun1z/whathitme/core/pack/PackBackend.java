package io.github.junjun1z.whathitme.core.pack;

import java.util.Map;
import java.util.function.Consumer;

/** Where the pack editor sends its edits: the server (pack mode) or the own client config (personal mode). */
public interface PackBackend {

    /** Sends one edit / request; replies ("state", "lists", "result", "test", "denied") reach the listener */
    void send(Map<String, Object> op);

    /** Personal mode (own renameMap / hintMap / ignoredDamageIds, no categories, no server tabs) */
    boolean personal();

    void listen(Consumer<Map<String, Object>> listener);
}
