package io.github.junjun1z.whathitme.core.pack;

import java.util.Map;
import java.util.function.Consumer;

import io.github.junjun1z.whathitme.core.Safe;
import io.github.junjun1z.whathitme.core.client.ClientCore;
import io.github.junjun1z.whathitme.core.net.Packets;

/** Pack mode: edits go to the server, which answers with chunked Message packets ({@link PackProto}). */
public final class PackClient implements PackBackend {

    private static final PackProto.Assembler ASM = new PackProto.Assembler(PackProto.MAX_DOWN);
    private static volatile Consumer<Map<String, Object>> listener;

    @Override
    public void send(Map<String, Object> op) {
        for (Packets.Packet p : PackProto.up(op)) ClientCore.port.send(p);
    }

    @Override
    public boolean personal() {
        return false;
    }

    @Override
    public void listen(Consumer<Map<String, Object>> l) {
        listener = l;
    }

    /** A Message with {@link PackProto#KEY} (client thread) */
    public static void onChunk(Packets.Message m) {
        if (m.args.length < 2) return;
        Map<String, Object> msg;
        synchronized (ASM) {
            try {
                msg = ASM.add(m.args[0], m.args[1]);
            } catch (RuntimeException e) {
                Safe.warn("pack editor message", e);
                return;
            }
        }
        Consumer<Map<String, Object>> l = listener;
        if (msg != null && l != null) l.accept(msg);
    }
}
