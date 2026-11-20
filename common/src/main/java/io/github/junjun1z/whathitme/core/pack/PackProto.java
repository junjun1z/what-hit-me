package io.github.junjun1z.whathitme.core.pack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.github.junjun1z.whathitme.core.data.Json;
import io.github.junjun1z.whathitme.core.net.Packets;

/**
 * Pack editor messages over the existing packets (no new packet type, so every loader carries them unchanged): a JSON
 * object split into chunks. Client to server: {@code Request} kind {@link Packets.Request#PACK} with
 * "seq|i|n|data"; server to client: {@code Message} with key {@link #KEY} and the args "seq|i|n", data.
 */
public final class PackProto {

    /** Message key of a server reply chunk (never printed) */
    public static final String KEY = "whathitme.pack.wire";
    /**
     * Characters per chunk; UTF-8 needs up to 3 bytes per character. Client packets are limited to 32767 bytes, server
     * packets on 1.7.10 to 65535 (unsigned short length)
     */
    public static final int UP_CHUNK = 8000;
    public static final int DOWN_CHUNK = 20000;
    /** Largest client message (characters) */
    public static final int MAX_UP = 320000;
    /** Largest server message (characters) */
    public static final int MAX_DOWN = 8000000;

    private PackProto() {}

    private static int seq;

    private static synchronized int nextSeq() {
        seq = (seq + 1) & 0xFFFFF;
        return seq;
    }

    /** Splits into chunks of at most {@code size} characters, never between the two halves of a surrogate pair */
    static List<String> chunks(String data, int size) {
        List<String> out = new ArrayList<>();
        int at = 0;
        do {
            int end = Math.min(data.length(), at + size);
            if (end < data.length() && end > at + 1 && Character.isHighSurrogate(data.charAt(end - 1))) end--;
            out.add(data.substring(at, end));
            at = end;
        } while (at < data.length());
        return out;
    }

    public static List<Packets.Packet> up(Map<String, Object> msg) {
        List<String> parts = chunks(Json.write(msg), UP_CHUNK);
        int s = nextSeq();
        List<Packets.Packet> out = new ArrayList<>();
        for (int i = 0; i < parts.size(); i++) out.add(new Packets.Request(Packets.Request.PACK, s + "|" + i + "|" + parts.size() + "|" + parts.get(i)));
        return out;
    }

    public static List<Packets.Packet> down(Map<String, Object> msg) {
        List<String> parts = chunks(Json.write(msg), DOWN_CHUNK);
        int s = nextSeq();
        List<Packets.Packet> out = new ArrayList<>();
        for (int i = 0; i < parts.size(); i++) out.add(new Packets.Message(false, KEY, s + "|" + i + "|" + parts.size(), parts.get(i)));
        return out;
    }

    /** Collects the chunks of one sender; {@link #add} returns the whole message when its last chunk arrived */
    public static final class Assembler {
        private final int max;
        private int seq = -1;
        private String[] parts;
        private int have;
        private int size;

        public Assembler(int maxChars) {
            this.max = maxChars;
        }

        /** @return the message, null while incomplete; throws on a malformed or too large message */
        public Map<String, Object> add(String header, String data) {
            String[] h = header.split("\\|");
            if (h.length < 3) throw new IllegalArgumentException("bad chunk header");
            int s = Integer.parseInt(h[0]);
            int i = Integer.parseInt(h[1]);
            int n = Integer.parseInt(h[2]);
            if (n < 1 || n > 1000 || i < 0 || i >= n) throw new IllegalArgumentException("bad chunk count");
            if (s != seq || parts == null || parts.length != n) {
                seq = s;
                parts = new String[n];
                have = 0;
                size = 0;
            }
            if (parts[i] == null) {
                parts[i] = data == null ? "" : data;
                have++;
                size += parts[i].length();
                if (size > max) {
                    parts = null;
                    throw new IllegalArgumentException("message too large");
                }
            }
            if (have < n) return null;
            StringBuilder sb = new StringBuilder(size);
            for (String p : parts) sb.append(p);
            parts = null;
            seq = -1;
            Object o = Json.parse(sb.toString());
            if (!(o instanceof Map)) throw new IllegalArgumentException("not an object");
            return Json.obj(o);
        }

        /** Request arg "seq|i|n|data" */
        public Map<String, Object> addUp(String arg) {
            int a = arg.indexOf('|');
            int b = a < 0 ? -1 : arg.indexOf('|', a + 1);
            int c = b < 0 ? -1 : arg.indexOf('|', b + 1);
            if (c < 0) throw new IllegalArgumentException("bad chunk");
            return add(arg.substring(0, c), arg.substring(c + 1));
        }
    }
}
